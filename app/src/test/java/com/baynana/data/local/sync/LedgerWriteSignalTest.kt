package com.baynana.data.local.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerRepository
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.NewDebtSpec
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.sync.TransportPort
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * «نسخة قديمة تعمل بلا تعديل» لا تعني قناةً ميتة: يجب أن يصل القيد إلى الطرف الآخر **بعد كتابته**،
 * لا في فحص دوري عشوائي. هذا الملفّ يتحقّق من ثلاثة أمور:
 *
 * ١. **الكتابة المحلية تُنبّه المزامنة** — مرة واحدة لكل قيد، ولا مرة إن لم تُضبط قناة.
 * ٢. **فشل التنبيه لا يُفشل حفظ القيد** — القاعدة الأولى: دفتر صاحبه أولًا.
 * ٣. **بلا قناة مضبوطة لا عمل يُجدَّل أصلًا** — فلا استيقاظ جهاز ولا طلب شبكة في نسخة بلا خادم.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerWriteSignalTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private var idCounter = 0
    private val now = 1_767_225_600_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = LedgerRepository(db, newId = { "id-${++idCounter}" }, now = { now })
    }

    @After
    fun tearDown() {
        LedgerWriteSignal.listener = null
        SyncTransportProvider.install(null)
        db.close()
    }

    private suspend fun seedRoom() {
        val dao = db.ledgerDao()
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
        dao.upsertMember(RoomMember("room-1", "me", "المسرب", isMe = true, joinedAt = now))
    }

    private suspend fun record(operationId: String) = repository.recordDebt(
        NewDebtSpec(
            id = "session-$operationId",
            operationId = operationId,
            roomId = "room-1",
            type = EntryType.WATER_SESSION,
            debtorMemberId = "farmer",
            creditorMemberId = "me",
            amountMinor = 1_500_000,
            currency = "YER_NEW",
            occurredAt = now,
            description = "سقية"
        )
    )

    @Test
    fun `حفظ قيد ينبّه المزامنة مرة واحدة`() = runBlocking {
        seedRoom()
        var signals = 0
        LedgerWriteSignal.listener = { signals++ }

        record("op-1")
        assertEquals("قيد واحد = تنبيه واحد", 1, signals)

        record("op-2")
        assertEquals("قيد ثانٍ = تنبيه ثانٍ، ولا تجميع يفقد أحدهما", 2, signals)
    }

    @Test
    fun `إعادة نفس العملية لا تُنبّه مرتين`() = runBlocking {
        seedRoom()
        var signals = 0
        LedgerWriteSignal.listener = { signals++ }

        record("op-1")
        val second = record("op-1")

        assertFalse("لم يُكتب صف ثانٍ", second.created)
        assertEquals("إعادة نداء لا تعني تغييرًا جديدًا يُرسل", 1, signals)
    }

    @Test
    fun `فشل التنبيه لا يُفشل حفظ القيد`() = runBlocking {
        seedRoom()
        LedgerWriteSignal.listener = { error("تعذّر جدولة العمل") }

        val result = record("op-1")

        assertTrue("القيد محفوظ في دفتر صاحبه رغم فشل التنبيه", result.created)
        assertEquals("op-1", result.entry.operationId)
    }

    @Test
    fun `بلا قناة مضبوطة لا يُجدَّل أي عمل`() = runBlocking {
        seedRoom()
        val workManager = WorkManager.getInstance(context)

        // لا قناة: لا تنبيه مُوصَل، ولا عمل يُطلب.
        assertFalse(SyncBridge.install(context))
        assertNull("لا قناة بلا عنوان ورمز", SyncTransportProvider.transport())
        LedgerWriteSignal.fire()
        assertEquals(0, workManager.getWorkInfosForUniqueWork(SyncScheduler.WORK_NAME).get().size)
        assertEquals(0, workManager.getWorkInfosForUniqueWork(SyncScheduler.PERIODIC_WORK_NAME).get().size)
    }

    @Test
    fun `التنبيه بعد ضبط القناة يُجدَّل عملًا واحدًا فريدًا`() = runBlocking {
        val workManager = WorkManager.getInstance(context)

        SyncScheduler.requestSync(context)
        SyncScheduler.requestSync(context) // كتابتان متتاليتان: عمل واحد لا طابور

        val infos = workManager.getWorkInfosForUniqueWork(SyncScheduler.WORK_NAME).get()
        assertEquals("عمل فريد لكل المزامنة", 1, infos.size)
    }

    @Test
    fun `العمل الدوري الاحتياطي يُجدَّل مرة واحدة لا أكثر`() = runBlocking {
        val workManager = WorkManager.getInstance(context)

        SyncScheduler.schedulePeriodic(context)
        SyncScheduler.schedulePeriodic(context)

        assertEquals(
            "KEEP: إعادة الضبط عند كل تشغيل لا تُنشئ أعمالًا متراكمة",
            1,
            workManager.getWorkInfosForUniqueWork(SyncScheduler.PERIODIC_WORK_NAME).get().size
        )
    }

    @Test
    fun `حقن قناة لا يُشغّل إرسالًا من تلقاء نفسه`() {
        var pushed = 0
        SyncBridge.installTransport(object : TransportPort {
            override suspend fun push(envelopes: List<com.baynana.domain.sync.OutboxEnvelope>) =
                emptyList<com.baynana.domain.sync.PushOutcome>().also { pushed++ }

            override suspend fun pull(cursor: String?) = com.baynana.domain.sync.PullPage(emptyList(), cursor.orEmpty())
        })

        assertEquals("التثبيت لا يُرسل شيئًا؛ الإرسال عمل العامل عند الطلب", 0, pushed)
    }
}
