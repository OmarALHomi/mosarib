package com.baynana.data.local.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerRepository
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.AllocationMode
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.NewDebtSpec
import com.baynana.domain.ledger.ReceiptSpec
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.sync.OutboxEnvelope
import com.baynana.domain.sync.PullPage
import com.baynana.domain.sync.PushOutcome
import com.baynana.domain.sync.TransportPort
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * الترحيل من داخل التطبيق (ح٢٣): **دفتر عائلة ينتقل فعلًا**، والجرد هو الحاكم.
 *
 * هذا الاختبار يقيس السيناريو الحقيقي كاملًا: دفتر على جهاز فيه سقية وسداد وإلغاء ومسودة ⇒ ملفّ
 * ترحيل ⇒ استيراد على **دفتر آخر فارغ** عبر قناة تشبه الخادم ⇒ صفر فرق في الجرد. ثم يقيس ما يهمّ
 * أكثر: أن **الملفّ المعدَّل أو المبتور يُكتشف**، وأن إعادة الاستيراد لا تُضاعف، وأن الجرد لا
 * يكذب إذا نقص شيء.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationRepositoryTest {

    /** «خادم» يشبه العقد: يتجاهل المكرر بـoperationId، ويؤدي الدلتا بمؤشر. */
    private class FakeServer : TransportPort {
        val changes = LinkedHashMap<String, com.baynana.domain.sync.RemoteChange>()
        val order = mutableListOf<String>()
        var serverTime = 1_767_225_600_000L

        override suspend fun push(envelopes: List<OutboxEnvelope>): List<PushOutcome> = envelopes.map { envelope ->
            if (changes.containsKey(envelope.operationId)) {
                PushOutcome.Accepted // مكرر: مقبول بلا أثر ثانٍ (وهذا ما ينصّ عليه العقد)
            } else {
                serverTime += 1_000
                changes[envelope.operationId] = com.baynana.domain.sync.RemoteChange(
                    kind = if (envelope.action == "VOID") com.baynana.domain.sync.RemoteChange.VOID else com.baynana.domain.sync.RemoteChange.UPSERT,
                    entityType = envelope.entityType,
                    entityId = envelope.entityId,
                    operationId = envelope.operationId,
                    payload = envelope.payload,
                    serverTime = serverTime
                )
                order += envelope.operationId
                PushOutcome.Accepted
            }
        }

        override suspend fun pull(cursor: String?): PullPage {
            val start = if (cursor.isNullOrBlank()) 0 else (cursor.toIntOrNull() ?: 0)
            val slice = order.drop(start).take(100).map { changes.getValue(it) }
            return PullPage(slice, nextCursor = (start + slice.size).toString(), hasMore = start + slice.size < order.size)
        }
    }

    private lateinit var context: Context
    private lateinit var sourceDb: AppDatabase
    private lateinit var targetDb: AppDatabase
    private lateinit var source: LedgerRepository
    private lateinit var target: LedgerRepository
    private lateinit var sourceMigration: MigrationRepository
    private lateinit var targetMigration: MigrationRepository
    private val server = FakeServer()
    private var idCounter = 0
    private val now = 1_767_225_600_000L

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        sourceDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        source = LedgerRepository(sourceDb, newId = { "s-${++idCounter}" }, now = { now })
        target = LedgerRepository(targetDb, newId = { "t-${++idCounter}" }, now = { now })
        sourceMigration = MigrationRepository(sourceDb, newId = { "src-device-1" }, now = { now })
        targetMigration = MigrationRepository(targetDb, newId = { "tgt-device-2" }, now = { now })
        seed(sourceDb)
        seed(targetDb)
    }

    @After
    fun tearDown() {
        sourceDb.close()
        targetDb.close()
    }

    private suspend fun seed(database: AppDatabase) {
        val dao = database.ledgerDao()
        dao.upsertRoom(
            LedgerRoom(
                id = "room-1",
                kind = RoomKind.WATER,
                currency = "YER_NEW",
                title = "غرفة الري",
                status = RoomStatus.ACTIVE,
                linkCode = "LNK-1",
                createdAt = now,
                updatedAt = now
            )
        )
        dao.upsertMember(RoomMember("room-1", "farmer", "أحمد", joinedAt = now))
        dao.upsertMember(RoomMember("room-1", "distributor", "المالك", isMe = true, joinedAt = now))
    }

    /** دفتر المصدر: سقيتان، سداد مخصَّص على الأقدم، وإلغاء للسقية الثانية، ومسودة. */
    private suspend fun buildSourceLedger() {
        val first = source.recordDebt(
            NewDebtSpec(
                id = "session-1", operationId = "op-water-1", roomId = "room-1",
                type = EntryType.WATER_SESSION, debtorMemberId = "farmer", creditorMemberId = "distributor",
                amountMinor = 1_500_000, currency = "YER_NEW", occurredAt = now, description = "سقية"
            )
        ).entry
        val second = source.recordDebt(
            NewDebtSpec(
                id = "session-2", operationId = "op-water-2", roomId = "room-1",
                type = EntryType.WATER_SESSION, debtorMemberId = "farmer", creditorMemberId = "distributor",
                amountMinor = 2_500_000, currency = "YER_NEW", occurredAt = now + 60_000, description = "سقية ثانية"
            )
        ).entry
        source.recordReceipt(
            ReceiptSpec(
                id = "payment-1", operationId = "op-payment-1", roomId = "room-1",
                debtorMemberId = "farmer", creditorMemberId = "distributor", amountMinor = 1_000_000,
                currency = "YER_NEW", occurredAt = now + 120_000, mode = AllocationMode.OldestFirst
            )
        )
        assertEquals(1_500_000L, first.amountMinor)
        source.reverseEntry(entryId = second.id, reason = "سقية مكرّرة", operationId = "op-void-1", occurredAt = now + 180_000)

        // مسودة محلية: لا تُشارك فهي ليست في صندوق الصادر أصلًا.
        sourceDb.ledgerDao().insertEntryIfNew(
            com.baynana.data.local.ledger.LedgerEntry(
                id = "draft-1", roomId = "room-1", operationId = "op-draft-1", type = EntryType.WATER_SESSION,
                owedByMemberId = "farmer", owedToMemberId = "distributor", amountMinor = 700_000,
                currency = "YER_NEW", occurredAt = now + 240_000, description = "مسودة",
                status = com.baynana.domain.ledger.EntryStatus.DRAFT, createdByMemberId = "distributor",
                createdAt = now + 240_000, updatedAt = now + 240_000
            )
        )
    }

    @Test
    fun `ملفّ الترحيل يُستورد على دفتر فارغ والجرد مطابق بلا فرق`() = runBlocking {
        buildSourceLedger()
        val file = sourceMigration.export()
        val snapshotBefore = sourceMigration.localInventory()
        assertEquals("المسودة تُعدّ وتبقى على جهاز صاحبها", 1, snapshotBefore.drafts)

        val report = targetMigration.import(file.text, server)

        assertTrue("كل الأحداث قُبلت: ${report.rejections}", report.rejected == 0)
        assertEquals(file.events, report.accepted)
        assertTrue("الجرد يجب أن يطابق: ${report.diff.differences}", report.isClean)
        val local = targetMigration.localInventory()
        assertEquals("غياب المسودة عن الوجهة مقصود: لم تشارك", 0, local.drafts)
        assertEquals(snapshotBefore.activeTotal, local.activeTotal)
        assertEquals(snapshotBefore.voidedTotal, local.voidedTotal)
        assertEquals(snapshotBefore.reversalsTotal, local.reversalsTotal)
        assertEquals(
            snapshotBefore.rooms.single().netByMember,
            local.rooms.single().netByMember
        )
    }

    @Test
    fun `إعادة الاستيراد لا تُضاعف قيدًا`() = runBlocking {
        buildSourceLedger()
        val file = sourceMigration.export()
        targetMigration.import(file.text, server)
        val afterFirst = targetMigration.localInventory()

        val second = targetMigration.import(file.text, server)

        val afterSecond = targetMigration.localInventory()
        assertTrue("لا أثر ثانٍ لإعادة الاستيراد", second.isClean)
        assertEquals(afterFirst.activeTotal, afterSecond.activeTotal)
        assertEquals(
            afterFirst.rooms.single().netByMember,
            afterSecond.rooms.single().netByMember
        )
    }

    @Test
    fun `ملفّ ناقص يُكتشف فيرتفع الفرق ولا يُعلن نجاحًا`() = runBlocking {
        buildSourceLedger()
        val file = sourceMigration.export()
        val dropped = file.text.lineSequence()
            .filterNot { it.contains("op-payment-1") }
            .joinToString("\n")

        val report = targetMigration.import(dropped, server)

        assertFalse("نقص قيد يجب أن يظهر", report.isClean)
        assertTrue(
            "الفرق يُسمّي ما نقص: ${report.diff.differences}",
            report.diff.differences.any { it.contains("op-payment-1") }
        )
        assertTrue("وسطر الملخّص يقول إن الجرد فيه فرق", report.summaryText().contains("فرق"))
    }

    @Test
    fun `ملفّ معدَّل في مبلغه يُكتشف`() = runBlocking {
        buildSourceLedger()
        val file = sourceMigration.export()
        val tampered = file.text.replace("\"amountMinor\":\"1500000\"", "\"amountMinor\":\"1400000\"")

        val report = targetMigration.import(tampered, server)

        assertFalse("الجرد يكشف المبلغ المعدَّل", report.isClean)
        assertTrue(report.diff.differences.any { it.contains("المجموع") })
    }

    @Test
    fun `المطابقة بلا كتابة تعطي الحكم نفسه`() = runBlocking {
        buildSourceLedger()
        val file = sourceMigration.export()

        val before = targetMigration.verify(file.text)
        assertFalse("دفتر فارغ لا يطابق ملفًّا فيه قيود", before.isClean)

        targetMigration.import(file.text, server)
        assertTrue(targetMigration.verify(file.text).isClean)
    }

    @Test
    fun `المسودة وحدها لا تُنقل ولا تُفشل الترحيل لكنها تُعلن`() = runBlocking {
        sourceDb.ledgerDao().insertEntryIfNew(
            com.baynana.data.local.ledger.LedgerEntry(
                id = "draft-only", roomId = "room-1", operationId = "op-draft-only", type = EntryType.WATER_SESSION,
                owedByMemberId = "farmer", owedToMemberId = "distributor", amountMinor = 500_000,
                currency = "YER_NEW", occurredAt = now, description = "مسودة وحيدة",
                status = com.baynana.domain.ledger.EntryStatus.DRAFT, createdByMemberId = "distributor",
                createdAt = now, updatedAt = now
            )
        )
        val file = sourceMigration.export()
        assertEquals("المسودة ليست حدثًا فلا تُصدَّر", 0, file.events)

        val report = targetMigration.import(file.text, server)

        assertTrue(report.isClean)
        assertTrue(
            "المسودة تُعلن للمستخدم ولا تُفشل: ${report.diff.notes}",
            report.diff.notes.any { it.contains("مسودات محلية") }
        )
    }

    @Test
    fun `ملفّ مُعدَّل مع بقاء ترويسته لا يمرّ ولو طابق الخادم`() = runBlocking {
        buildSourceLedger()
        val file = sourceMigration.export()
        // الملفّ كامل يُستورد إلى الخادم، ثم يُقدَّم للتحقق ملفٌّ أسقط سطر حدثه وبقيت ترويسته تحمل
        // أرقام الأصل: لو قُرئت الترويسة وحدها لقال الفحص «مطابق» — وهذا بالضبط ما يجب منعه.
        targetMigration.import(file.text, server)
        val truncated = file.text.lineSequence()
            .filterNot { it.startsWith("{\"kind\":\"event\"") && it.contains("op-payment-1") }
            .joinToString("\n")

        val diff = targetMigration.verify(truncated)

        assertFalse("ملفّ متناقض مع نفسه لا يُقال عنه مطابق", diff.isClean)
        assertTrue(
            "الفرق يحمل وسم الاتّساق صريحًا: ${diff.differences}",
            diff.differences.any { it.startsWith("الملفّ غير متّسق مع ترويسته") }
        )
        assertTrue(diff.differences.any { it.contains("op-payment-1") })
    }

    @Test
    fun `ملفّ بلا أحداث يُرفض بجملة عربية`() = runBlocking {
        val emptyFile = sourceMigration.export()
        val error = runCatching { targetMigration.import(emptyFile.text, server) }.exceptionOrNull()
        assertTrue("لا استيراد بلا أحداث", error is IllegalArgumentException)
        assertTrue(error!!.message.orEmpty().contains("لا يحمل أحداثًا"))
    }

    @Test
    fun `الملفّ المكتوب ثم المقروء يعطي الجرد نفسه`() = runBlocking {
        buildSourceLedger()
        val file = sourceMigration.export()
        val parsed = com.baynana.domain.migration.LedgerMigration.parse(file.text)
        assertEquals(
            com.baynana.domain.migration.LedgerMigration.inventoryJson(file.inventory).toString(),
            com.baynana.domain.migration.LedgerMigration.inventoryJson(parsed.inventory).toString()
        )
        assertTrue(file.fileName.startsWith("baynana-migration-"))
    }
}
