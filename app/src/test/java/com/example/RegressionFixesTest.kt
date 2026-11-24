package com.example

import com.example.core.voice.SpokenNumberParser
import com.example.core.voice.VoiceCommandParser
import com.example.core.voice.VoiceCommandType
import com.example.features.customers.Customer
import com.example.features.customers.LedgerRunningBalance
import com.example.features.customers.calculateCustomerBalance
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionWithCustomer
import com.example.features.vouchers.AccountTypeFilter
import com.example.features.vouchers.OperationsFilterEngine
import com.example.features.vouchers.OperationsFilterState
import com.example.features.vouchers.UnifiedOperation
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegressionFixesTest {

    @Test
    fun spokenNumberParser_preservesArabicDecimalAndGluedWaw() {
        assertEquals(2.5, SpokenNumberParser.parseSpokenNumber("2٫5")!!, 0.001)
        assertEquals(5.0, SpokenNumberParser.parseSpokenNumber("و5")!!, 0.001)
        assertEquals(1.0, SpokenNumberParser.parseSpokenNumber("وحدة")!!, 0.001)
    }

    @Test
    fun voiceCommandParser_maDafaIsDebtNotPaidFull() {
        val cmd = VoiceCommandParser.parse("سقي علي ساعتين من 5000 ما دفع")
        assertEquals(VoiceCommandType.SESSION, cmd.type)
        assertFalse("ma dafa must not be treated as paidFull", cmd.paidFull)
        assertNull(cmd.paid)
    }

    @Test
    fun operationsFilterEngine_usesBilledToSponsorAndCountsReceiptVouchersOnce() {
        val lessee = Customer(id = 1L, name = "مستأجر", customGroup = "المستأجرين")
        val owner = Customer(id = 2L, name = "المالك الأصلي", customGroup = "الملاك", isWellOwner = true)
        val customers = mapOf(1L to lessee, 2L to owner)

        val session = WaterSession(
            id = 50L,
            customerId = 1L,
            billedToCustomerId = 2L,
            startTime = 1_700_000_000_000L,
            durationMinutes = 60,
            totalAmount = 10_000.0,
            amountPaid = 4_000.0,
            remainingDebt = 0.0
        )
        val ops = listOf(
            UnifiedOperation.SessionOp(
                WaterSessionWithCustomer(
                    session = session,
                    customer = lessee,
                    billedToCustomer = owner
                )
            )
        )

        val ownerFiltered = OperationsFilterEngine.apply(
            ops = ops,
            state = OperationsFilterState(accountType = AccountTypeFilter.WELL_OWNER, customerId = 2L, group = "الملاك"),
            customers = customers,
            query = "",
            now = 1_700_000_000_000L
        )
        assertEquals(1, ownerFiltered.size)

        val lesseeFiltered = OperationsFilterEngine.apply(
            ops = ops,
            state = OperationsFilterState(customerId = 1L),
            customers = customers,
            query = "",
            now = 1_700_000_000_000L
        )
        assertTrue(lesseeFiltered.isEmpty())
    }

    @Test
    fun ledgerRunningBalance_linkedDiscountAndReceiptMatchCustomerBalance() {
        val customer = Customer(id = 7L, name = "عميل اختبار")
        val session = WaterSession(
            id = 70L,
            customerId = customer.id,
            startTime = 1000L,
            durationMinutes = 60,
            totalAmount = 5000.0,
            amountPaid = 2000.0,
            remainingDebt = 3000.0,
            discountAmount = 500.0
        )
        val linkedReceipt = Voucher(
            id = 701L,
            customerId = customer.id,
            sessionId = session.id,
            date = 1500L,
            type = VoucherType.RECEIPT,
            amount = 2000.0
        )
        val linkedDiscount = Voucher(
            id = 702L,
            customerId = customer.id,
            sessionId = session.id,
            date = 1600L,
            type = VoucherType.DISCOUNT,
            amount = 500.0
        )

        val running = LedgerRunningBalance.compute(
            customer = customer,
            sessions = listOf(session),
            vouchers = listOf(linkedReceipt, linkedDiscount)
        )
        val expected = calculateCustomerBalance(
            customer = customer,
            sessions = listOf(session),
            vouchers = listOf(linkedReceipt, linkedDiscount)
        )
        assertEquals(expected.balance, running.finalBalance, 0.01)
    }

    @Test
    fun activationKeyRegex_acceptsWebDashboardThreeSegmentKeysAndLegacyTwoSegmentKeys() {
        val pattern = Regex("""^ACTV(-[MY])?-[A-Z0-9]{4}-[A-Z0-9]{4}(-[A-Z0-9]{4})?$""")
        assertTrue("ACTV-M-A1B2-C3D4-E5F6".matches(pattern))
        assertTrue("ACTV-Y-1234-ABCD-9876".matches(pattern))
        assertTrue("ACTV-ABCD-EF12".matches(pattern))
        assertFalse("ACTV-BAD".matches(pattern))
    }
}
