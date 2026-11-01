package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.features.customers.Customer
import com.example.features.customers.CustomerDao
import com.example.features.customers.CustomerRepository
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionDao
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherDao
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecycleBinSafeDeleteTest {

    private lateinit var db: AppDatabase
    private lateinit var customerDao: CustomerDao
    private lateinit var sessionDao: WaterSessionDao
    private lateinit var voucherDao: VoucherDao
    private lateinit var purchaseDao: WellOwnerPurchaseDao
    private lateinit var customerRepo: CustomerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Enable foreign key constraints in SQLite to reproduce and verify RESTRICT / CASCADE behavior
        db.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = ON;")

        customerDao = db.customerDao()
        sessionDao = db.waterSessionDao()
        voucherDao = db.voucherDao()
        purchaseDao = db.wellOwnerPurchaseDao()
        customerRepo = CustomerRepository(
            customerDao = customerDao,
            sessionFlow = flowOf(emptyList()),
            voucherFlow = flowOf(emptyList())
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `permanentlyDeleteCustomer removes customer and all related records atomically`() = runBlocking {
        // Create an archived customer
        val custId = customerDao.insertCustomer(
            Customer(name = "أحمد محمد", isWellOwner = true, isArchived = true)
        )

        // Insert related water sessions
        val s1 = sessionDao.insertSession(WaterSession(customerId = custId, durationMinutes = 60))
        val s2 = sessionDao.insertSession(WaterSession(customerId = custId, durationMinutes = 120))

        // Insert related vouchers
        val v1 = voucherDao.insertVoucher(Voucher(customerId = custId, amount = 1000.0))
        val v2 = voucherDao.insertVoucher(Voucher(customerId = custId, amount = 2000.0))

        // Insert related well owner purchases (which have RESTRICT FK)
        val p1 = purchaseDao.insertPurchase(
            WellOwnerPurchase(ownerCustomerId = custId, durationMinutes = 180, purchaseRatePerHour = 5000.0)
        )
        val p2 = purchaseDao.insertPurchase(
            WellOwnerPurchase(ownerCustomerId = custId, durationMinutes = 240, purchaseRatePerHour = 5000.0)
        )

        // Check delete counts
        val counts = customerRepo.getCustomerDeleteCounts(custId)
        assertEquals(2, counts?.sessions)
        assertEquals(2, counts?.vouchers)
        assertEquals(2, counts?.purchases)

        // Perform permanent delete
        customerRepo.permanentlyDeleteCustomer(custId)

        // Customer must be deleted
        assertNull(customerDao.getCustomerByIdDirect(custId))

        // All related water sessions must be deleted
        assertNull(sessionDao.getSessionById(s1))
        assertNull(sessionDao.getSessionById(s2))

        // All related vouchers must be deleted (not left as orphaned records with customerId = null)
        assertNull(voucherDao.getVoucherById(v1))
        assertNull(voucherDao.getVoucherById(v2))
        assertEquals(0, voucherDao.getVouchersCountDirect())

        // All related well owner purchases must be deleted
        assertNull(purchaseDao.getPurchaseById(p1))
        assertNull(purchaseDao.getPurchaseById(p2))
        assertEquals(0, purchaseDao.getPurchasesCountDirect())
    }

    @Test
    fun `permanentlyDeleteCustomer does NOT delete active customer`() = runBlocking {
        // Active customer (isArchived = false)
        val activeCustId = customerDao.insertCustomer(
            Customer(name = "عميل نشط", isWellOwner = true, isArchived = false)
        )

        val sId = sessionDao.insertSession(WaterSession(customerId = activeCustId, durationMinutes = 60))
        val vId = voucherDao.insertVoucher(Voucher(customerId = activeCustId, amount = 1000.0))
        val pId = purchaseDao.insertPurchase(
            WellOwnerPurchase(ownerCustomerId = activeCustId, durationMinutes = 120, purchaseRatePerHour = 5000.0)
        )

        // Counts should be 0 because the customer is not archived
        val counts = customerRepo.getCustomerDeleteCounts(activeCustId)
        assertEquals(0, counts?.sessions)
        assertEquals(0, counts?.vouchers)
        assertEquals(0, counts?.purchases)

        // Attempting to permanently delete active customer
        customerRepo.permanentlyDeleteCustomer(activeCustId)

        // Customer and all records must still exist intact!
        assertNotNull(customerDao.getCustomerByIdDirect(activeCustId))
        assertNotNull(sessionDao.getSessionById(sId))
        assertNotNull(voucherDao.getVoucherById(vId))
        assertNotNull(purchaseDao.getPurchaseById(pId))
    }

    @Test
    fun `emptyRecycleBin removes all archived customers and their records while preserving active ones`() = runBlocking {
        // Archived Customer 1
        val arch1 = customerDao.insertCustomer(Customer(name = "مؤرشف 1", isWellOwner = true, isArchived = true))
        val s1 = sessionDao.insertSession(WaterSession(customerId = arch1, durationMinutes = 60))
        val v1 = voucherDao.insertVoucher(Voucher(customerId = arch1, amount = 1500.0))
        val p1 = purchaseDao.insertPurchase(
            WellOwnerPurchase(ownerCustomerId = arch1, durationMinutes = 90, purchaseRatePerHour = 4000.0)
        )

        // Archived Customer 2
        val arch2 = customerDao.insertCustomer(Customer(name = "مؤرشف 2", isWellOwner = false, isArchived = true))
        val s2 = sessionDao.insertSession(WaterSession(customerId = arch2, durationMinutes = 45))
        val v2 = voucherDao.insertVoucher(Voucher(customerId = arch2, amount = 2500.0))

        // Active Customer 3
        val activeCust = customerDao.insertCustomer(Customer(name = "عميل نشط", isWellOwner = true, isArchived = false))
        val sActive = sessionDao.insertSession(WaterSession(customerId = activeCust, durationMinutes = 100))
        val vActive = voucherDao.insertVoucher(Voucher(customerId = activeCust, amount = 3000.0))
        val pActive = purchaseDao.insertPurchase(
            WellOwnerPurchase(ownerCustomerId = activeCust, durationMinutes = 120, purchaseRatePerHour = 5000.0)
        )

        // Check total counts in recycle bin
        val totalCounts = customerRepo.getRecycleBinTotalCounts()
        assertEquals(2, totalCounts?.sessions)
        assertEquals(2, totalCounts?.vouchers)
        assertEquals(1, totalCounts?.purchases)

        // Empty recycle bin
        customerRepo.emptyRecycleBin()

        // Archived customers should be gone
        assertNull(customerDao.getCustomerByIdDirect(arch1))
        assertNull(customerDao.getCustomerByIdDirect(arch2))
        assertNull(sessionDao.getSessionById(s1))
        assertNull(sessionDao.getSessionById(s2))
        assertNull(voucherDao.getVoucherById(v1))
        assertNull(voucherDao.getVoucherById(v2))
        assertNull(purchaseDao.getPurchaseById(p1))

        // Active customer and all related records must remain completely untouched!
        assertNotNull(customerDao.getCustomerByIdDirect(activeCust))
        assertNotNull(sessionDao.getSessionById(sActive))
        assertNotNull(voucherDao.getVoucherById(vActive))
        assertNotNull(purchaseDao.getPurchaseById(pActive))

        assertEquals(1, sessionDao.getSessionsCountDirect())
        assertEquals(1, voucherDao.getVouchersCountDirect())
        assertEquals(1, purchaseDao.getPurchasesCountDirect())
    }
}

