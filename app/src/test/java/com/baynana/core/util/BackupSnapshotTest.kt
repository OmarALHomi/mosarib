package com.baynana.core.util

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.features.customers.Customer
import com.baynana.features.deals.DealPayment
import com.baynana.features.deals.SettlementDeal
import com.baynana.features.farmer.FarmExpense
import com.baynana.features.farmer.LinkedMusrib
import com.baynana.features.market.CropListing
import com.baynana.features.pumps.PumpSource
import com.baynana.features.sessions.WaterSession
import com.baynana.features.settings.AppSetting
import com.baynana.features.vouchers.Voucher
import com.baynana.features.vouchers.VoucherType
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * بوابة v4 §14: «النسخة: round-trip لكل الجداول/كل الحقول وروابطها — نفس الأرصدة والدفعات
 * والعميل المتحمّل للدين» و«النسخة: ملف مخالف/نسخة مستقبلية/مبالغ غير صالحة/ID مكرر — رفض
 * قبل اعتماد التغييرات».
 *
 * هذه الاختبارات لا تُغني عن تجربة يدوية على جهاز، لكنها تثبت أن المحرّك ينسخ الجداول
 * العشرة ويعيدها كما كانت، وأنه يرفض الملفات الفاسدة قبل أن تلمس القاعدة.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupSnapshotTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val restoredDb: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @After
    fun closeDatabases() {
        db.close()
        restoredDb.close()
    }

    // ------------------------------------------------------------- المُعينات

    private suspend fun seedAllTenTables(database: AppDatabase) {
        database.customerDao().insertCustomer(
            Customer(
                id = 1, name = "أحمد المزارع", phone = "777111222", farmName = "مزرعة الوادي",
                location = "العزلة", notes = "ملاحظة", customPricePerHour = 4500.0,
                isBeneficiary = true, createdAt = 1000L, isArchived = false, linkCode = "ABC12"
            )
        )
        database.customerDao().insertCustomer(
            Customer(id = 2, name = "عميل مؤرشف", isArchived = true, createdAt = 900L)
        )
        database.pumpSourceDao().insertPump(
            PumpSource(id = 1, name = "البئر الكبير", locationOrWellNumber = "وادي 3", defaultPricePerHour = 6000.0, powerType = "ديزل")
        )
        database.pumpSourceDao().insertPump(
            PumpSource(id = 2, name = "مضخة معطلة", isActive = false)
        )
        database.appSettingDao().saveSetting(AppSetting("distributor_name", "المسرب"))
        database.appSettingDao().saveSetting(AppSetting("currency", "YER_NEW"))
        database.linkedMusribDao().insert(
            LinkedMusrib(linkCode = "LNK99", musribName = "سالم المسرب", currentBalance = 12500.0, totalDebit = 20000.0, totalPaid = 7500.0)
        )
        // السقية محسوبة على حساب مستفيد آخر: عميل الديون.
        database.waterSessionDao().insertSession(
            WaterSession(
                id = 1, customerId = 1, pumpName = "البئر الكبير", startTime = 2000L, endTime = 3000L,
                durationMinutes = 60, pricePerHour = 5000.0, totalAmount = 10000.0,
                amountPaid = 2000.0, remainingDebt = 5000.0, billedToCustomerId = 2L, createdAt = 2000L
            )
        )
        database.voucherDao().insertVoucher(
            Voucher(
                id = 1, voucherNumber = "V-1", type = VoucherType.RECEIPT, customerId = 1, sessionId = 1L,
                amount = 3000.0, category = "سداد حساب", paymentMethod = "نقداً", date = 4000L, createdAt = 4000L
            )
        )
        database.cropListingDao().insertListing(
            CropListing(
                id = "listing-1", title = "رمان", cropType = "رمان", district = "بني العوام",
                priceEstimate = 250000.0, farmerPhone = "777111222", hideFarmerPhone = true, createdAt = 5000L
            )
        )
        database.settlementDealDao().insertDeal(
            SettlementDeal(
                id = "deal-1", dealNumber = "SLH-1", cropTitle = "رمان", cropType = "رمان",
                sellerName = "أحمد", buyerName = "محمد", totalAmount = 1000000.0, advancePayment = 400000.0,
                dallalCommission = 50000.0, commissionPaid = 20000.0, remainingAmount = 600000.0,
                dealDate = 6000L, dueDate = 7000L, termsNotes = "شرط"
            )
        )
        database.settlementDealDao().insertPayment(
            DealPayment(id = "payment-1", dealId = "deal-1", amount = 100000.0, paidBy = "BUYER", paymentType = "INSTALLMENT", paymentDate = 6500L)
        )
        database.farmExpenseDao().insertExpense(
            FarmExpense(id = "expense-1", farmName = "مزرعة الوادي", expenseCategory = "سماد وتغذية", amount = 30000.0, date = 8000L)
        )
    }

    private suspend fun snapshotJson(database: AppDatabase): JSONObject = BackupSnapshot.toJson(BackupSnapshot.readAll(database))

    // ------------------------------------------------- ذهاب وعودة للجداول العشرة

    @Test
    fun `snapshot declares itself complete and carries all ten tables`() = runBlocking {
        seedAllTenTables(db)
        val json = snapshotJson(db)

        assertEquals(BackupSnapshot.FORMAT_VERSION, json.getInt("formatVersion"))
        assertTrue(json.getBoolean("complete"))
        val tables = json.getJSONObject("tables")
        BackupSnapshot.TABLE_ORDER.forEach { key ->
            assertTrue("الجدول $key مفقود من النسخة", tables.has(key))
        }
    }

    @Test
    fun `all ten tables survive a full round trip with their links`() = runBlocking {
        seedAllTenTables(db)
        val json = snapshotJson(db)

        val original = BackupSnapshot.readAll(db)
        val plan = BackupSnapshot.plan(json, null)
        assertTrue("خطة الاسترجاع يجب أن تكون سليمة: ${plan.errors}", plan.canRestore)
        assertTrue(plan.isComplete)

        restoredDb.withTransactionCompat { BackupSnapshot.apply(restoredDb, plan) }

        val restored = BackupSnapshot.readAll(restoredDb)

        // الأبوان لا يُسقطان أبناءهما: المؤرشف والمعطّل يُنسخان أيضًا.
        assertEquals(original.customers.size, restored.customers.size)
        assertEquals(original.customers.first { it.id == 2L }, restored.customers.first { it.id == 2L })
        assertEquals(original.pumps.size, restored.pumps.size)
        assertEquals(1, restored.pumps.count { !it.isActive })

        // نسبة السقية إلى العميل المتحمّل للدين وإلى السند المربوط بها.
        assertEquals(original.sessions, restored.sessions)
        assertNotNull(restored.sessions.first().billedToCustomerId)
        assertEquals(2L, restored.sessions.first().billedToCustomerId)
        assertEquals(original.vouchers, restored.vouchers)
        assertEquals(1L, restored.vouchers.first().sessionId)

        // الجداول التي لم تكن تُنسخ قبل هذا الإصلاح.
        assertEquals(original.linkedMusribs, restored.linkedMusribs)
        assertEquals(original.cropListings, restored.cropListings)
        assertEquals(original.settlementDeals, restored.settlementDeals)
        assertEquals(original.dealPayments, restored.dealPayments)
        assertEquals(original.farmExpenses, restored.farmExpenses)
        assertEquals(100000.0, restored.dealPayments.first().amount, 0.0)
    }

    @Test
    fun `restoring an old five-table file keeps the newer tables untouched`() = runBlocking {
        seedAllTenTables(db)

        val legacy = JSONObject()
        legacy.put("app", "Mosarib")
        legacy.put("version", 1)
        legacy.put("customers", JSONArray().apply {
            put(JSONObject().apply { put("id", 1); put("name", "أحمد المزارع"); put("linkCode", "ABC12") })
        })
        legacy.put("settings", JSONArray().apply { put(JSONObject().apply { put("key", "distributor_name"); put("value", "المسرب") }) })

        val plan = BackupSnapshot.plan(legacy, BackupSnapshot.readAll(db))
        assertTrue(plan.canRestore)
        assertFalse("ملف الصيغة الأولى ليس كاملًا", plan.isComplete)
        assertTrue(
            "يجب تحذير المستخدم أن الملف ناقص",
            plan.warnings.any { it.contains("نسخة قديمة") }
        )

        restoredDb.withTransactionCompat { BackupSnapshot.apply(restoredDb, plan) }

        val restored = BackupSnapshot.readAll(restoredDb)
        assertEquals(1, restored.customers.size)
        assertEquals(0, restored.settlementDeals.size)
        assertEquals(0, restored.dealPayments.size)
    }

    // ------------------------------------------------------ رفض الملفات الفاسدة

    @Test
    fun `a future format version is refused before touching the database`() = runBlocking {
        seedAllTenTables(db)
        val json = snapshotJson(db)
        json.put("formatVersion", BackupSnapshot.FORMAT_VERSION + 1)

        val plan = BackupSnapshot.plan(json, BackupSnapshot.readAll(db))
        assertFalse(plan.canRestore)
        assertTrue(plan.errors.any { it.contains("أحدث") })
    }

    @Test
    fun `a file from a newer database version is refused`() = runBlocking {
        seedAllTenTables(db)
        val json = snapshotJson(db)
        json.put("dbVersion", 99)

        val plan = BackupSnapshot.plan(json, BackupSnapshot.readAll(db))
        assertFalse(plan.canRestore)
        assertTrue(plan.errors.any { it.contains("أحدث") })
    }

    @Test
    fun `duplicate ids inside the file are refused`() = runBlocking {
        val json = JSONObject().apply {
            put("formatVersion", BackupSnapshot.FORMAT_VERSION)
            put("tables", JSONObject().apply {
                put("customers", JSONArray().apply {
                    put(JSONObject().apply { put("id", 7); put("name", "أول") })
                    put(JSONObject().apply { put("id", 7); put("name", "ثاني") })
                })
            })
        }

        val plan = BackupSnapshot.plan(json, null)
        assertFalse(plan.canRestore)
        assertTrue(plan.errors.any { it.contains("مكرر") })
    }

    @Test
    fun `invalid amounts are refused`() = runBlocking {
        val json = JSONObject().apply {
            put("formatVersion", BackupSnapshot.FORMAT_VERSION)
            put("tables", JSONObject().apply {
                put("farmExpenses", JSONArray().apply {
                    put(JSONObject().apply { put("id", "e1"); put("amount", -500.0); put("farmName", "مزرعة") })
                })
            })
        }

        val plan = BackupSnapshot.plan(json, null)
        assertFalse(plan.canRestore)
        assertTrue(plan.errors.any { it.contains("سالبة") })
    }

    @Test
    fun `a session pointing at an unknown customer is refused`() = runBlocking {
        val json = JSONObject().apply {
            put("formatVersion", BackupSnapshot.FORMAT_VERSION)
            put("tables", JSONObject().apply {
                put("sessions", JSONArray().apply {
                    put(JSONObject().apply { put("id", 3); put("customerId", 12345); put("totalAmount", 100.0) })
                })
            })
        }

        val plan = BackupSnapshot.plan(json, null)
        assertFalse(plan.canRestore)
        assertTrue(plan.errors.any { it.contains("عميل غير موجود") })
    }

    @Test
    fun `a payment pointing at an unknown deal is refused`() = runBlocking {
        val json = JSONObject().apply {
            put("formatVersion", BackupSnapshot.FORMAT_VERSION)
            put("tables", JSONObject().apply {
                put("dealPayments", JSONArray().apply {
                    put(JSONObject().apply { put("id", "p1"); put("dealId", "ghost"); put("amount", 50.0) })
                })
            })
        }

        val plan = BackupSnapshot.plan(json, null)
        assertFalse(plan.canRestore)
        assertTrue(plan.errors.any { it.contains("صلح غير موجود") })
    }

    @Test
    fun `an invalid plan cannot be applied`() = runBlocking {
        seedAllTenTables(db)
        val json = snapshotJson(db)
        json.put("formatVersion", BackupSnapshot.FORMAT_VERSION + 1)
        val plan = BackupSnapshot.plan(json, BackupSnapshot.readAll(db))
        assertFalse(plan.canRestore)

        var threw = false
        try {
            restoredDb.withTransactionCompat { BackupSnapshot.apply(restoredDb, plan) }
        } catch (error: IllegalStateException) {
            threw = true
        }
        assertTrue("يجب رفض الخطة غير السليمة قبل الكتابة", threw)
        assertEquals(0, BackupSnapshot.readAll(restoredDb).customers.size)
    }

    @Test
    fun `preview counts what will be overwritten without writing anything`() = runBlocking {
        seedAllTenTables(db)
        val json = snapshotJson(db)

        val plan = BackupSnapshot.plan(json, BackupSnapshot.readAll(db))
        assertTrue(plan.canRestore)
        // كل الجداول المزروعة موجودة مسبقًا بنفس المفاتيح، فتُعدّ مستبدَلة في المعاينة.
        assertEquals(1, plan.reports.first { it.tableKey == BackupSnapshot.KEY_SETTLEMENT_DEALS }.rowsToOverwrite)
        assertEquals(1, plan.reports.first { it.tableKey == BackupSnapshot.KEY_DEAL_PAYMENTS }.rowsToOverwrite)
        assertEquals(1, plan.reports.first { it.tableKey == BackupSnapshot.KEY_FARM_EXPENSES }.rowsToOverwrite)
        assertEquals(13, plan.totalRowsInFile)

        // المعاينة لا تكتب شيئًا.
        assertEquals(0, BackupSnapshot.readAll(restoredDb).customers.size)
    }

    /** اختصار لتنفيذ كتلة داخل معاملة Room مع فحص الأخطاء. */
    private suspend fun <T> AppDatabase.withTransactionCompat(block: suspend () -> T): T = withTransaction(block)
}
