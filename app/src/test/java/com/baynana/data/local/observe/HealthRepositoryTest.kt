package com.baynana.data.local.observe

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerEntry
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.OutboxItem
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.OutboxState
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.observe.HealthReport
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
import java.io.File

/**
 * تقرير الصحّة على قاعدة حقيقية (ح٢٤): الأرقام تُقرأ من القاعدة لا من نموذج.
 *
 * الفكرة: بوابة تُقاس بمخرَج، لا بنيّة. فهذه الاختبارات تُدخل بيانات في Room ثم تسأل: هل قرأ التقرير
 * الرقم الصحيح؟ وهل سقطت البوابة التي يجب أن تسقط؟ وهل بقي المجهول مجهولًا؟
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: HealthRepository
    private val now = 1_767_225_600_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = HealthRepository(context, db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seed() {
        val dao = db.ledgerDao()
        dao.upsertRoom(
            LedgerRoom(
                id = "room-1", kind = RoomKind.WATER, currency = "YER_NEW", title = "غرفة الري",
                status = RoomStatus.ACTIVE, linkCode = "LNK-1", createdAt = now, updatedAt = now
            )
        )
        dao.upsertMember(RoomMember("room-1", "farmer", "أحمد", joinedAt = now))
        // عضو **بلا اسم** عن قصد: بوابة G4 يجب أن تسقط، فيُقاس السقوط بمخرَج لا بنيّة.
        dao.upsertMember(RoomMember("room-1", "distributor", "", isMe = true, joinedAt = now))

        dao.insertEntryIfNew(
            LedgerEntry(
                id = "e-1", roomId = "room-1", operationId = "op-1", type = EntryType.WATER_SESSION,
                owedByMemberId = "farmer", owedToMemberId = "distributor", amountMinor = 1_500_000,
                currency = "YER_NEW", occurredAt = now, description = "سقية", status = EntryStatus.SENT,
                createdByMemberId = "distributor", createdAt = now, updatedAt = now
            )
        )
        dao.insertEntryIfNew(
            LedgerEntry(
                id = "e-2", roomId = "room-1", operationId = "op-2", type = EntryType.WATER_SESSION,
                owedByMemberId = "farmer", owedToMemberId = "distributor", amountMinor = 700_000,
                currency = "YER_NEW", occurredAt = now, description = "مسودة", status = EntryStatus.DRAFT,
                createdByMemberId = "distributor", createdAt = now, updatedAt = now
            )
        )
        // حركة ميتة بسبب مكتوب: بوابة G2 يجب أن تسقط وتذكر السبب نفسه.
        dao.enqueueOutbox(
            OutboxItem(
                operationId = "op-dead", entityType = "entry", entityId = "e-1", action = "UPSERT",
                payload = "{}", state = OutboxState.DEAD, attempts = 4,
                lastError = "رمز الجهاز مرفوض", createdAt = now, updatedAt = now
            )
        )
    }

    @Test
    fun `الأرقام تُقرأ من القاعدة والمسودة تُعدّ ولا تدخل النشط`() = runBlocking {
        seed()
        val report = repository.report(now)

        val metrics = report.metrics.toMap()
        assertEquals("1", metrics[HealthReport.Metrics.LABEL_ROOMS])
        assertEquals("2", metrics[HealthReport.Metrics.LABEL_MEMBERS])
        assertEquals("1", metrics[HealthReport.Metrics.LABEL_ACTIVE])
        assertEquals("1", metrics[HealthReport.Metrics.LABEL_DRAFTS])
        assertEquals("1", metrics[HealthReport.Metrics.LABEL_DEAD])
    }

    @Test
    fun `عضو بلا اسم وحركة ميتة تُسقطان بوابتيهما بالسبب`() = runBlocking {
        seed()
        val report = repository.report(now)

        val naming = report.gates.first { it.id == "G4" }
        assertEquals(HealthReport.GateState.FAIL, naming.state)
        assertTrue(naming.measured.contains("1"))

        val silent = report.gates.first { it.id == "G2" }
        assertEquals(HealthReport.GateState.FAIL, silent.state)
        assertTrue("السبب الظاهر يُقرأ من القاعدة", silent.measured.contains("رمز الجهاز مرفوض"))
        assertFalse(report.isHealthy)
    }

    @Test
    fun `بلا قناة مضبوطة تبقى بوابة المشاركة مجهولة لا سليمة`() = runBlocking {
        seed()
        val report = repository.report(now)
        assertEquals(HealthReport.GateState.UNKNOWN, report.gates.first { it.id == "G3" }.state)
        assertTrue(report.verdictText().contains("لم تُقس"))
    }

    @Test
    fun `أحدث نسخة احتياطية تُقرأ من الملفّ فعلًا فتُرفع البوابة`() = runBlocking {
        seed()
        assertTrue(
            "بلا نسخة تسقط البوابة",
            repository.report(now).gates.first { it.id == "G5" }.state == HealthReport.GateState.FAIL
        )

        val backups = File(context.filesDir, "backups").apply { mkdirs() }
        File(backups, "baynana-backup-1.json").writeText("{}")
        File(backups, "baynana-backup-1.json").setLastModified(now - 60_000)

        val gate = repository.report(now).gates.first { it.id == "G5" }
        assertEquals(HealthReport.GateState.PASS, gate.state)
        assertTrue(gate.measured.contains("قبل"))
    }

    @Test
    fun `التقرير المُشارَك يبقى بلا معرّفات الأعضاء`() = runBlocking {
        seed()
        val text = repository.report(now).shareText()
        assertFalse(text.contains("farmer"))
        assertFalse(text.contains("distributor"))
        assertFalse("لا مبالغ", text.contains("1500000"))
        assertTrue(text.contains("لم يُرسل إلى أي جهة"))
    }
}
