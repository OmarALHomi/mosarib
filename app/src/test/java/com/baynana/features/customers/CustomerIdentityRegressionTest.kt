package com.baynana.features.customers

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.features.sessions.WaterSession
import com.baynana.features.vouchers.Voucher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomerIdentityRegressionTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java
    ).build()

    @After
    fun closeDatabase() { db.close() }

    @Test
    fun `upserting a customer preserves its financial children and their attribution`() = runBlocking {
        val customer = Customer(id = 1, name = "العميل", linkCode = "ABC23")
        val session = WaterSession(id = 1, customerId = 1, totalAmount = 5000.0)
        val voucher = Voucher(id = 1, customerId = 1, amount = 1000.0)
        db.customerDao().insertCustomer(customer)
        db.waterSessionDao().insertSession(session)
        db.voucherDao().insertVoucher(voucher)

        db.customerDao().insertCustomer(customer.copy(name = "الاسم المستعاد"))

        assertEquals(session, db.waterSessionDao().getSessionById(1))
        assertEquals(voucher, db.voucherDao().getVoucherById(1))
    }

    @Test
    fun `editing a customer from a form without link code preserves the existing link and creation date`() = runBlocking {
        db.customerDao().insertCustomer(Customer(id = 1, name = "السابق", linkCode = "ABC23", createdAt = 100L))
        val repo = CustomerRepository(db.customerDao(), flowOf(emptyList()), flowOf(emptyList()))

        repo.updateCustomer(Customer(id = 1, name = "الجديد", phone = "771234567", createdAt = 999L))

        val updated = db.customerDao().getCustomerByIdDirect(1)!!
        assertEquals("الجديد", updated.name)
        assertEquals("771234567", updated.phone)
        assertEquals("ABC23", updated.linkCode)
        assertEquals(100L, updated.createdAt)
    }

    @Test
    fun `an explicitly supplied link code is not overwritten by normal editing`() = runBlocking {
        db.customerDao().insertCustomer(Customer(id = 1, name = "العميل", linkCode = "ABC23", createdAt = 100L))
        val repo = CustomerRepository(db.customerDao(), flowOf(emptyList()), flowOf(emptyList()))

        repo.updateCustomer(Customer(id = 1, name = "العميل", linkCode = "XYZ87"))

        val updated = db.customerDao().getCustomerByIdDirect(1)!!
        assertEquals("XYZ87", updated.linkCode)
        assertEquals(100L, updated.createdAt)
    }
}
