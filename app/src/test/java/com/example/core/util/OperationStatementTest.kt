package com.example.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class OperationStatementTest {

    // 2026-11-22 (Sunday) at hour:minute
    private fun at(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 22, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun session_written_sentence() {
        val s = OperationStatement.session("أحمد", at(20, 10), at(21, 10), 60, 5000.0, 2000.0, 3000.0, "ريال")
        assertEquals(
            "سقي لـ أحمد يوم الأحد 22/11 من 8:10 إلى 9:10 مساءً، ساعة واحدة، المطلوب 5,000 ريال، الواصل 2,000، المتبقي عليه 3,000",
            s
        )
    }

    @Test
    fun session_across_noon_names_both_periods_and_settled_says_khalis() {
        val s = OperationStatement.session("علي", at(11, 30), at(12, 30), 60, 1000.0, 1000.0, 0.0, "ريال")
        assertTrue(s.contains("من 11:30 صباحاً إلى 12:30 مساءً"))
        assertTrue(s.endsWith("المتبقي عليه لا شيء (خالص)"))
    }

    @Test
    fun session_spoken_has_no_digits_for_amounts() {
        val s = OperationStatement.sessionSpoken("أحمد", at(20, 10), at(21, 10), 60, 5000.0, 2000.0, 3000.0, "ريال")
        assertTrue(s.contains("المطلوب خمسة آلاف"))
        assertFalse(s.contains("5,000"))
        assertFalse(s.contains("2,000"))
    }

    @Test
    fun voucher_receipt_and_expense_with_note() {
        assertEquals("قبض من أحمد مبلغ 3,000 ريال", OperationStatement.voucher(true, "أحمد", 3000.0, "", "ريال"))
        assertEquals(
            "صرف لـ سالم مبلغ 500 ريال، البيان: ديزل",
            OperationStatement.voucher(false, "سالم", 500.0, " ديزل ", "ريال")
        )
        assertFalse(OperationStatement.voucherSpoken(true, "أحمد", 3000.0, "", "ريال").contains("3,000"))
    }

    @Test
    fun blank_name_falls_back_and_purchase_sentence() {
        assertTrue(OperationStatement.voucher(true, "  ", 10.0, "", "ريال").startsWith("قبض من عميل غير محدد"))
        assertEquals(
            "شراء ساعتان من الشيخ، الإجمالي 4,000 ريال، المسلَّم 1,000",
            OperationStatement.purchase("الشيخ", 120, 4000.0, 1000.0, "ريال")
        )
        assertFalse(OperationStatement.purchaseSpoken("الشيخ", 120, 4000.0, 1000.0, "ريال").contains("4,000"))
    }
}
