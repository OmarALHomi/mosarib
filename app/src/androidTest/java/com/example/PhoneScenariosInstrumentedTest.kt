package com.example

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import com.example.core.ui.LocalOperationGate
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.core.database.AppDatabase
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.customers.CustomerRepository
import com.example.features.customers.CustomersViewModel
import com.example.features.customers.LedgerRunningBalance
import com.example.features.customers.calculateCustomerBalance
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.WaterSessionRepository
import com.example.features.vouchers.OperationsFilter
import com.example.features.vouchers.VoucherRepository
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.VouchersScreen
import com.example.features.vouchers.VouchersViewModel
import com.example.features.wellowners.WellOwnerPurchase
import com.example.ui.theme.MosaribTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhoneScenariosInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var realApp: Application
    private lateinit var sandboxDir: File
    private lateinit var sandboxApp: Application
    private lateinit var db: AppDatabase
    private val trackedPrefNames = mutableSetOf<String>()

    @Before
    fun setupSandbox() {
        realApp = ApplicationProvider.getApplicationContext()
        sandboxDir = File(realApp.cacheDir, "phone_test_sandbox").apply {
            mkdirs()
        }
        sandboxApp = object : Application() {
            init {
                attachBaseContext(realApp)
            }

            override fun getApplicationContext(): Context = this

            override fun getDatabasePath(name: String): File = File(sandboxDir, name)

            override fun getFilesDir(): File = File(sandboxDir, "files").apply { mkdirs() }

            override fun getNoBackupFilesDir(): File = File(sandboxDir, "nobackup").apply { mkdirs() }

            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val prefixed = "androidtest_sandbox_$name"
                trackedPrefNames.add(prefixed)
                return realApp.getSharedPreferences(prefixed, mode)
            }
        }
        sandboxApp.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("debug_unlimited_ops", true).apply()
        // Initialize AppDatabase singleton onto sandboxDir BEFORE any ViewModel touches it,
        // keeping the phone's real water_distributor_db untouched.
        db = AppDatabase.getDatabase(sandboxApp)
        runBlocking {
            db.clearAllTables()
        }
    }

    @After
    fun cleanupSandbox() {
        runBlocking {
            db.clearAllTables()
        }
        trackedPrefNames.forEach { pref ->
            realApp.getSharedPreferences(pref, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

    @Test
    fun scenario1_vouchersScreenTotalReceipts_doesNotDoubleCountSettledSession() {
        val sessionsVm = SessionsViewModel(sandboxApp)
        val vouchersVm = VouchersViewModel(sandboxApp)

        val now = System.currentTimeMillis()
        runBlocking {
            val custId = db.customerDao().insertCustomer(Customer(name = "مزارع اختبار المقبوض"))
            // 1. Add session: 2h @ 5000 = 10,000; initial paid = 4,000; debt = 6,000
            sessionsVm.saveManualSession(
                customerId = custId,
                startTime = now - 7_200_000L,
                endTime = now,
                hours = 2,
                minutes = 0,
                pricePerHour = 5000.0,
                amountPaid = 4000.0,
                notes = "جلسة اختبار"
            )
            // Wait until Room inserts the session
            var sessions = db.waterSessionDao().getAllSessions().first()
            var attempts = 0
            while (sessions.isEmpty() && attempts < 50) {
                kotlinx.coroutines.delay(50)
                sessions = db.waterSessionDao().getAllSessions().first()
                attempts++
            }
            assertEquals(1, sessions.size)
            val s = sessions.first()
            assertEquals(4000.0, s.amountPaid, 0.01)
            assertEquals(6000.0, s.remainingDebt, 0.01)

            // 2. Settle 3,000 via linked RECEIPT voucher -> session.amountPaid = 7,000 AND linked voucher = 3,000
            sessionsVm.settleSessionDebt(s, amountToPay = 3000.0, paymentMethod = "نقداً", notes = "سداد جزئي")
            attempts = 0
            while (db.voucherDao().getAllVouchers().first().isEmpty() && attempts < 50) {
                kotlinx.coroutines.delay(50)
                attempts++
            }

            // 3. Add standalone RECEIPT voucher of 2,000 (sessionId = null)
            vouchersVm.addVoucher(
                type = VoucherType.RECEIPT,
                customerId = custId,
                amount = 2000.0,
                category = "سند قبض",
                paymentMethod = "نقداً",
                notes = "سند مستقل"
            )
            attempts = 0
            while (db.voucherDao().getAllVouchers().first().size < 2 && attempts < 50) {
                kotlinx.coroutines.delay(50)
                attempts++
            }
        }

        val expectedReceiptsText = Formatters.formatCurrency(9000.0, vouchersVm.appConfig.value.currencySymbol) // 7,000 + 2,000 (NOT 12,000!)

        composeRule.setContent {
            MosaribTheme(darkTheme = false) {
                CompositionLocalProvider(LocalOperationGate provides { action -> action() }) {
                    VouchersScreen(
                        viewModel = vouchersVm,
                        sessionsViewModel = sessionsVm
                    )
                }
            }
        }

        // Wait for summary tile to render 9,000 ر.ي
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText(expectedReceiptsText).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("مقبوض").assertIsDisplayed()
        composeRule.onNodeWithText(expectedReceiptsText).assertIsDisplayed()

        // Switch filter to RECEIPTS: summary must STILL show 9,000 ر.ي (never 5,000 or 12,000)
        composeRule.runOnUiThread {
            vouchersVm.setOperationsFilter(OperationsFilter.RECEIPTS)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(expectedReceiptsText).assertIsDisplayed()
    }

    @Test
    fun scenario2_wellOwnerOverpaymentOnSession_notDoubleDeductedAndNoSurplusVouchers() {
        val sessionsVm = SessionsViewModel(sandboxApp)
        val vouchersVm = VouchersViewModel(sandboxApp)

        val now = System.currentTimeMillis()
        var ownerId = 0L
        var farmerId = 0L
        runBlocking {
            ownerId = db.customerDao().insertCustomer(Customer(name = "صاحب بئر اختبار", isWellOwner = true))
            farmerId = db.customerDao().insertCustomer(Customer(name = "مزارع تابع", isWellOwner = false))

            // Purchase from well owner: 10h @ 2,000 = 20,000 owed TO owner
            db.wellOwnerPurchaseDao().insertPurchase(
                WellOwnerPurchase(
                    ownerCustomerId = ownerId,
                    date = now - 20_000_000L,
                    startTime = now - 20_000_000L,
                    endTime = now - 20_000_000L + 36_000_000L,
                    durationMinutes = 10 * 60,
                    purchaseRatePerHour = 2000.0,
                    amountPaid = 0.0,
                    notes = "شراء 10 ساعات"
                )
            )

            // 1. Direct session for owner: 1h @ 5,000 = 5,000 total, OVERPAID 8,000!
            sessionsVm.saveManualSession(
                customerId = ownerId,
                startTime = now - 10_000_000L,
                endTime = now - 6_400_000L,
                hours = 1,
                minutes = 0,
                pricePerHour = 5000.0,
                amountPaid = 8000.0,
                notes = "سقي مباشر لصاحب البئر مع دفع زائد"
            )

            // 2. Farmer session billed to owner: 1h @ 2,000 = 2,000 total, OVERPAID 3,000!
            sessionsVm.saveManualSession(
                customerId = farmerId,
                startTime = now - 5_000_000L,
                endTime = now - 1_400_000L,
                hours = 1,
                minutes = 0,
                pricePerHour = 2000.0,
                amountPaid = 3000.0,
                notes = "سقي مزارع على حساب صاحب البئر",
                billedToCustomerId = ownerId
            )

            var attempts = 0
            while (db.waterSessionDao().getAllSessions().first().size < 2 && attempts < 60) {
                kotlinx.coroutines.delay(50)
                attempts++
            }
        }

        val allSessions = runBlocking { db.waterSessionDao().getAllSessions().first() }
        val allVouchers = runBlocking { db.voucherDao().getAllVouchers().first() }
        val allPurchases = runBlocking { db.wellOwnerPurchaseDao().getAllPurchases().first() }
        val owner = runBlocking { db.customerDao().getCustomerByIdDirect(ownerId)!! }
        val farmer = runBlocking { db.customerDao().getCustomerByIdDirect(farmerId)!! }

        assertEquals(2, allSessions.size)
        // Must NOT create any automatic surplus receipt vouchers when isOwnerOrBilled == true
        assertTrue("No excess settlement/surplus vouchers should be created", allVouchers.isEmpty())

        val ownerBalance = calculateCustomerBalance(owner, allSessions, allVouchers, purchases = allPurchases)
        val farmerBalance = calculateCustomerBalance(farmer, allSessions, allVouchers, purchases = allPurchases)
        val ledger = LedgerRunningBalance.compute(owner, allSessions, allVouchers, allPurchases)

        // Expected net payable to owner:
        // (20,000 purchase + 8,000 + 3,000 cash collected) - (5,000 + 2,000 water sales) = 24,000
        assertEquals(24000.0, ownerBalance.payableBalance, 0.01)
        assertEquals(0.0, ownerBalance.receivableBalance, 0.01)
        assertEquals(24000.0, ownerBalance.balance, 0.01)
        assertEquals(24000.0, ledger.finalBalance, 0.01)
        assertEquals(0.0, farmerBalance.balance, 0.01)

        // Verify VouchersScreen UI displays exact receipts total (8,000 + 3,000 = 11,000)
        val expectedReceiptsText = Formatters.formatCurrency(11000.0, vouchersVm.appConfig.value.currencySymbol)
        composeRule.setContent {
            MosaribTheme(darkTheme = false) {
                CompositionLocalProvider(LocalOperationGate provides { action -> action() }) {
                    VouchersScreen(
                        viewModel = vouchersVm,
                        sessionsViewModel = sessionsVm
                    )
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText(expectedReceiptsText).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(expectedReceiptsText).assertIsDisplayed()
    }
}
