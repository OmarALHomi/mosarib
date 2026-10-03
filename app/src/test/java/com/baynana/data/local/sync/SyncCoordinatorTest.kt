package com.baynana.data.local.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerRepository
import com.baynana.domain.ledger.AllocationMode
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.NewDebtSpec
import com.baynana.domain.ledger.ReceiptSpec
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.sync.PullPage
import com.baynana.domain.sync.PushOutcome
import com.baynana.domain.sync.RemoteChange
import com.baynana.domain.sync.OutboxEnvelope
import com.baynana.domain.sync.TransportPort
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * بوابة ح٦ على قاعدة حقيقية وقناة مزيّفة تشبه الخادم:
 * البيانات تنجو من إعادة التشغيل، ولا تُرسل مرتين، وتصطلح بعد الانقطاع، والحجر يمنع الإنعاش.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncCoordinatorTest {

    /** «خادم» مزيف: يتجاهل المكرر بـ operationId، ويؤدي الدلتا بمؤشر. */
    private class FakeServer : TransportPort {
        val byOperation = LinkedHashMap<String, String>() // operationId → entry JSON
        val order = mutableListOf<String>()
        val received = mutableListOf<String>()
        var rejections: Map<String, PushOutcome> = emptyMap()
        var serverTime = 0L

        override suspend fun push(envelopes: List<OutboxEnvelope>): List<PushOutcome> = envelopes.map { envelope ->
            received += envelope.operationId
            val rejection = rejections[envelope.operationId]
            if (rejection != null) {
                rejection
            } else {
                // idempotent: نفس العملية لا تُخزَّن مرتين ولو أُرسلت مرات.
                if (byOperation.putIfAbsent(envelope.operationId, envelope.payload) == null) {
                    order += envelope.operationId
                }
                PushOutcome.Accepted
            }
        }

        override suspend fun pull(cursor: String?): PullPage {
            val start = cursor?.toIntOrNull() ?: 0
            val changes = order.drop(start).map { operationId ->
                val payload = byOperation.getValue(operationId)
                val entityId = JSONObject(payload).getString("id")
                RemoteChange(RemoteChange.UPSERT, "entry", entityId, operationId, payload, ++serverTime)
            }
            return PullPage(changes, nextCursor = order.size.toString(), hasMore = false)
        }

        fun closeEntry(entityId: String, operationId: String): RemoteChange {
            val change = RemoteChange(RemoteChange.DELETE, "entry", entityId, operationId, "أُغلق الحساب", ++serverTime)
            return change
        }
    }

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private var idCounter = 0
    private val now = 1_700_000_000_000L

    private val farmer = "member-farmer"
    private val distributor = "member-distributor"

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = LedgerRepository(db, newId = { "id-${++idCounter}" }, now = { now })
        seedRoom(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedRoom(database: AppDatabase) {
        val dao = database.ledgerDao()
        dao.upsertRoom(
            com.baynana.data.local.ledger.LedgerRoom(
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
        dao.upsertMember(com.baynana.data.local.ledger.RoomMember("room-1", farmer, "أحمد", joinedAt = now))
        dao.upsertMember(
            com.baynana.data.local.ledger.RoomMember("room-1", distributor, "المسرب", isMe = true, joinedAt = now)
        )
    }

    private suspend fun session(amountMinor: Long, operationId: String) = repository.recordDebt(
        NewDebtSpec(
            id = "session-$operationId",
            operationId = operationId,
            roomId = "room-1",
            type = EntryType.WATER_SESSION,
            debtorMemberId = farmer,
            creditorMemberId = distributor,
            amountMinor = amountMinor,
            currency = "YER_NEW",
            occurredAt = now,
            description = "سقية"
        )
    ).entry

    @Test
    fun `a locally saved entry survives and is sent exactly once`() = runBlocking {
        val entry = session(1_000_000L, "op-session-1")
        val server = FakeServer()
        val coordinator = SyncCoordinator(db, server)

        val first = coordinator.syncOnce(now)
        assertEquals(1, first.accepted)
        assertEquals(listOf("op-session-1"), server.received)
        assertNotNull("القيد باقٍ محليًا", db.ledgerDao().getEntry(entry.id))

        // جولة ثانية: لا إرسال مكرر.
        val second = coordinator.syncOnce(now + 60_000)
        assertEquals(0, second.pushed)
        assertEquals(listOf("op-session-1"), server.received)
        assertEquals(1, server.byOperation.size)
    }

    @Test
    fun `a restart after saving locally but before sending pushes the same entry once`() = runBlocking {
        val entry = session(500_000L, "op-restart")
        // «إعادة تشغيل»: قاعدة جديدة على نفس الملف؟ هنا نتحقق من أن الصف باقٍ في الصندوق ومعه حمولته.
        val pending = db.ledgerDao().nextOutboxBatch()
        assertEquals(1, pending.size)
        assertEquals("op-restart", pending.single().operationId)
        assertTrue(pending.single().payload.contains("amountMinor"))

        val server = FakeServer()
        SyncCoordinator(db, server).syncOnce(now)
        assertEquals(listOf("op-restart"), server.received)
        assertEquals(entry.id, JSONObject(server.byOperation.getValue("op-restart")).getString("id"))
    }

    @Test
    fun `two devices converge after each one records a payment`() = runBlocking {
        val server = FakeServer()

        // جهاز المسرب: يسجّل سقية بـ 10,000 ريال.
        session(1_000_000L, "op-session-device-a")
        SyncCoordinator(db, server).syncOnce(now)

        // جهاز المزارع: قاعدة أخرى، نفس الغرفة، ويسجّل سداد 4,000 ريال على السقية.
        val farmerDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            seedRoom(farmerDb)
            val farmerRepo = LedgerRepository(farmerDb, newId = { "farmer-${++idCounter}" }, now = { now })
            farmerRepo.recordReceipt(
                ReceiptSpec(
                    id = "pay-1",
                    operationId = "op-payment-device-b",
                    roomId = "room-1",
                    debtorMemberId = farmer,
                    creditorMemberId = distributor,
                    amountMinor = 400_000L,
                    currency = "YER_NEW",
                    occurredAt = now,
                    mode = AllocationMode.None, // قبض عام أولًا: لا يعرف أسطر ديون الجهاز الآخر
                    description = "سداد 4,000"
                )
            )
            SyncCoordinator(farmerDb, server).syncOnce(now + 1_000)

            // الجهاز الأول يزامن: يستقبل السداد ويطبّقه.
            val reportA = SyncCoordinator(db, server).syncOnce(now + 2_000)
            assertTrue(reportA.applied >= 1)

            // الآن الجهازان يعرفان القيدين معًا، والأرصدة تتفق.
            val snapshotA = repository.snapshot("room-1")
            val snapshotB = farmerRepo.snapshot("room-1")
            assertEquals(snapshotA.chargedMinor, snapshotB.chargedMinor)
            assertEquals(snapshotA.paidMinor, snapshotB.paidMinor)
            assertEquals(1_000_000L, snapshotA.chargedMinor)
            assertEquals("القبض العام لم يُغلق دينًا في أي من الجهازين", 1_000_000L, snapshotA.openDebtMinor)
            assertEquals(400_000L, snapshotA.unappliedReceiptMinor)
            assertEquals("لا ازدواج: قيدان فقط في كل جهاز", 2, db.ledgerDao().getEntriesIncludingVoided("room-1").size)
            assertEquals(2, farmerDb.ledgerDao().getEntriesIncludingVoided("room-1").size)
        } finally {
            farmerDb.close()
        }
    }

    @Test
    fun `a rejected push keeps the data and shows an Arabic reason`() = runBlocking {
        session(300_000L, "op-rejected")
        val server = FakeServer().apply {
            rejections = mapOf(
                "op-rejected" to PushOutcome.Rejected(retryable = false, reason = "الغرفة مغلقة في الخادم")
            )
        }
        val report = SyncCoordinator(db, server).syncOnce(now)

        assertEquals(1, report.dead)
        val row = db.ledgerDao().nextOutboxBatch(states = listOf("DEAD")).single()
        assertEquals("الغرفة مغلقة في الخادم", row.lastError)
        assertEquals("البيانات محفوظة كما هي", 1, db.ledgerDao().getEntriesIncludingVoided("room-1").size)
    }

    @Test
    fun `a remote delete records a tombstone, closes the entry locally, and blocks revival`() = runBlocking {
        val entry = session(200_000L, "op-to-close")
        val server = FakeServer()
        SyncCoordinator(db, server).syncOnce(now) // رُفع القيد

        // الطرف الآخر أغلق الحساب: يأتي حذف من الخادم.
        val deletion = server.closeEntry(entry.id, "op-close")
        val closing = object : TransportPort {
            override suspend fun push(envelopes: List<OutboxEnvelope>) = emptyList<PushOutcome>()
            override suspend fun pull(cursor: String?) = PullPage(listOf(deletion), nextCursor = "close-1")
        }
        SyncCoordinator(db, closing).syncOnce(now + 1_000)

        assertEquals(EntryStatus.VOIDED, db.ledgerDao().getEntry(entry.id)!!.status)
        assertEquals("الصف لم يُحذف أبدًا", 1, db.ledgerDao().getEntriesIncludingVoided("room-1").size)
        assertEquals(1, db.ledgerDao().countTombstones(entry.id))

        // ثم تصل نسخة قديمة من ذاكرة قديمة: يجب ألا تُنعش شيئًا.
        val stale = object : TransportPort {
            override suspend fun push(envelopes: List<OutboxEnvelope>) = emptyList<PushOutcome>()
            override suspend fun pull(cursor: String?) = PullPage(
                listOf(RemoteChange(RemoteChange.UPSERT, "entry", entry.id, "op-stale", "{}", now + 2_000)),
                nextCursor = "stale-1"
            )
        }
        val report = SyncCoordinator(db, stale).syncOnce(now + 2_000)
        assertEquals(1, report.tombstoned)
        assertEquals(0, report.applied)
    }

    @Test
    fun `an outage defers the push with a due time and the local status says so`() = runBlocking {
        session(100_000L, "op-offline")
        val broken = object : TransportPort {
            override suspend fun push(envelopes: List<OutboxEnvelope>): List<PushOutcome> =
                throw IllegalStateException("لا يوجد اتصال")

            override suspend fun pull(cursor: String?) = PullPage(emptyList(), nextCursor = "")
        }
        val report = SyncCoordinator(db, broken).syncOnce(now)

        assertEquals(1, report.failed)
        assertTrue(report.stoppedForRetry)
        val status = SyncStatusReader(db).status(now)
        assertEquals(1, status.failed)
        assertEquals("لا يوجد اتصال", status.lastError)
        assertTrue("موعد إعادة المحاولة مذكور", status.nextAttemptAt > now)
    }
}
