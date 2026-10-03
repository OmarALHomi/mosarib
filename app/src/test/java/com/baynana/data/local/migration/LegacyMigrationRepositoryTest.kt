package com.baynana.data.local.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerRepository
import com.baynana.domain.ledger.EntryStatus
import com.baynana.features.customers.Customer
import com.baynana.features.sessions.WaterSession
import com.baynana.features.vouchers.Voucher
import com.baynana.features.vouchers.VoucherType
import kotlinx.coroutines.runBlocking
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
 * بوابة ح٧ على قاعدة حقيقية: نزرع بيانات الإرث كما كانت (ريال عشري)، نرحّلها، ثم نتأكد أن
 * **الجرد مطابق**، وأن جداول الإرث لم تُمَس، وأن إعادة الترحيل لا تُنشئ صفًا ثانيًا.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LegacyMigrationRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var migration: LegacyMigrationRepository
    private val day1 = 1_600_000_000_000L
    private val day2 = 1_600_100_000_000L
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        migration = LegacyMigrationRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedLegacy() {
        val customerId = db.customerDao().insertCustomer(
            Customer(name = "أحمد المزارع", phone = "777111222", createdAt = day1, linkCode = "ABC12")
        )
        val archivedId = db.customerDao().insertCustomer(
            Customer(name = "عميل قديم مؤرشف", createdAt = day1, isArchived = true)
        )
        db.waterSessionDao().insertSession(
            WaterSession(
                customerId = customerId, pumpName = "البئر", startTime = day1, endTime = day1 + 3_600_000,
                durationMinutes = 60, pricePerHour = 5_000.0, totalAmount = 15_000.0,
                amountPaid = 2_000.0, remainingDebt = 13_000.0, createdAt = day1
            )
        )
        db.waterSessionDao().insertSession(
            WaterSession(
                customerId = customerId, pumpName = "البئر", startTime = day2, endTime = day2 + 1_800_000,
                durationMinutes = 30, pricePerHour = 5_000.0, totalAmount = 2_500.0,
                amountPaid = 2_500.0, remainingDebt = 0.0, createdAt = day2
            )
        )
        db.voucherDao().insertVoucher(
            Voucher(
                voucherNumber = "1", type = VoucherType.RECEIPT, customerId = customerId,
                amount = 2_000.0, date = day1 + 1_000, createdAt = day1
            )
        )
        db.waterSessionDao().insertSession(
            WaterSession(
                customerId = archivedId, pumpName = "بئر آخر", startTime = day2, endTime = day2 + 3_600_000,
                durationMinutes = 60, pricePerHour = 3_000.0, totalAmount = 900.0,
                amountPaid = 300.0, remainingDebt = 600.0, createdAt = day2
            )
        )
    }

    @Test
    fun `legacy history becomes ledger entries with matching totals and untouched source tables`() = runBlocking {
        seedLegacy()
        val plan = migration.buildPlan()

        // الجرد قبل التنفيذ: 13,000 ريال + 600 ريال = 1,360,000 فلس (×100).
        assertEquals(1_360_000L, plan.legacyTotalMinor)
        assertTrue(plan.allReconcile)

        val outcome = migration.apply(plan, now = now)
        assertEquals(2, outcome.roomsWritten)
        assertEquals(2, outcome.entriesWritten)
        assertTrue(outcome.isClean)
        assertTrue(outcome.summaryText().contains("غرف جديدة: 2"))

        // القيود تحمل تواريخ السقيات الحقيقية لا وقت الترحيل.
        val first = db.ledgerDao().getEntry("legacy-session-1")!!
        assertEquals(day1, first.occurredAt)
        assertEquals(1_300_000L, first.amountMinor)
        assertEquals("water_sessions", first.sourceTable)
        // كود الربط القديم يبقى للعميل حتى يستطيع المزارع إعادة الربط به.
        assertEquals("ABC12", db.ledgerDao().getRoom("legacy-room-1")!!.linkCode)
        assertEquals(EntryStatus.ACKNOWLEDGED, first.status)

        // جداول الإرث كما هي: لا حذف ولا تعديل.
        assertEquals(2, db.customerDao().getAllCustomersForBackup().size)
        assertEquals(3, db.waterSessionDao().getAllSessionsForBackup().size)
        assertEquals(1, db.voucherDao().getAllVouchersForBackup().size)
        assertEquals(13_000.0, db.waterSessionDao().getSessionById(1)!!.remainingDebt, 0.0)
    }

    @Test
    fun `the room balance after migration equals what the old ledger showed`() = runBlocking {
        seedLegacy()
        val outcome = migration.apply(migration.buildPlan(), now = now)
        assertTrue(outcome.isClean)

        val repository = LedgerRepository(db, now = { now })
        val snapshot = repository.snapshot("legacy-room-1")
        val customerId = db.customerDao().getAllCustomersForBackup().first { it.name == "أحمد المزارع" }.id

        // 13,000 ريال قديمة = 1,300,000 فلس جديدة (١ ريال يمني جديد = ١٠٠ فلس).
        assertEquals(1_300_000L, snapshot.chargedMinor)
        assertEquals(0L, snapshot.paidMinor)
        assertEquals(1_300_000L, snapshot.remainingMinor)
        assertEquals(1_300_000L, snapshot.openDebtOf("legacy-customer-$customerId"))
    }

    @Test
    fun `re-running the migration writes nothing new and never duplicates a debt`() = runBlocking {
        seedLegacy()
        val plan = migration.buildPlan()
        migration.apply(plan, now = now)
        val second = migration.apply(plan, now = now + 60_000)
        assertEquals("عميلان في الخطة", 2, plan.reconciliations.size)

        assertEquals("لا غرفة جديدة", 0, second.roomsWritten)
        assertEquals("لا قيد جديد", 0, second.entriesWritten)
        assertEquals("2 قيد كان مُرحَّلًا", 2, second.entriesAlreadyPresent)
        assertEquals("قيد واحد للغرفة الأولى (السقية الثانية مسدَّدة)", 1, db.ledgerDao().getEntriesIncludingVoided("legacy-room-1").size)
        assertEquals(1, db.ledgerDao().getEntriesIncludingVoided("legacy-room-2").size)
    }

    @Test
    fun `an archived customer gets a read-only room whose records are kept`() = runBlocking {
        seedLegacy()
        migration.apply(migration.buildPlan(), now = now)

        val archivedCustomer = db.customerDao().getAllCustomersForBackup().first { it.isArchived }
        val room = db.ledgerDao().getRoom("legacy-room-${archivedCustomer.id}")!!
        assertEquals("CLOSED", room.status)
        assertNotNull("سجل العميل مؤرشف لكن محفوظ", db.ledgerDao().getEntry("legacy-session-3"))
        assertEquals(60_000L, db.ledgerDao().getEntry("legacy-session-3")!!.amountMinor)
    }

    @Test
    fun `migrating one customer only leaves the others for later, without drift`() = runBlocking {
        seedLegacy()
        val plan = migration.buildPlan()
        val firstCustomer = db.customerDao().getAllCustomersForBackup().minBy { it.id }

        val partial = migration.apply(plan, onlyCustomers = setOf(firstCustomer.id), now = now)
        assertEquals(1, partial.roomsWritten)
        assertEquals(1, partial.entriesWritten)
        assertEquals(1, db.ledgerDao().getEntriesIncludingVoided("legacy-room-${firstCustomer.id}").size)

        val rest = migration.apply(plan, now = now + 1_000)
        assertEquals(1, rest.roomsWritten)
        assertEquals(1, rest.entriesWritten)
        assertEquals(2, rest.plan.reconciliations.size)
        assertTrue(rest.isClean)
    }

    @Test
    fun `the migration leaves a marker so a later run can explain itself`() = runBlocking {
        seedLegacy()
        val outcome = migration.apply(migration.buildPlan(), now = now)
        val marker = db.ledgerDao().getSyncState(LegacyMigrationRepository.MARKER_KEY)

        assertNotNull(marker)
        assertEquals("كل قيود الخطة صارت في الدفتر", outcome.plan.entries.size.toString(), marker!!.cursor)
        assertEquals(now, marker.lastSyncAt)
        assertEquals("", marker.lastError)
    }
}
