package com.example.core.util

import com.example.core.speech.SpeechTextBuilder
import java.util.Calendar

/**
 * One plain Arabic sentence per new operation, shown in «تأكيد العملية» before saving.
 * Each builder has a written form (digits) and a spoken form (amounts and duration as words).
 */
object OperationStatement {

    private fun cal(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }

    private fun dayMonth(millis: Long): String {
        val c = cal(millis)
        return "${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
    }

    private fun clock(millis: Long): String {
        val c = cal(millis)
        val h = c.get(Calendar.HOUR_OF_DAY)
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12:${"%02d".format(c.get(Calendar.MINUTE))}"
    }

    private fun period(millis: Long) = if (cal(millis).get(Calendar.HOUR_OF_DAY) < 12) "صباحاً" else "مساءً"

    private fun range(start: Long, end: Long): String =
        if (end <= start) "في ${clock(start)} ${period(start)}"
        else if (period(start) == period(end)) "من ${clock(start)} إلى ${clock(end)} ${period(end)}"
        else "من ${clock(start)} ${period(start)} إلى ${clock(end)} ${period(end)}"

    private fun name(n: String) = n.trim().ifBlank { "عميل غير محدد" }

    private fun sessionHead(customerName: String, start: Long, end: Long) =
        "سقي لـ ${name(customerName)} يوم ${MessageFormatHelper.formatWeekdayArabic(start)} ${dayMonth(start)} ${range(start, end)}"

    fun session(
        customerName: String, startMillis: Long, endMillis: Long, durationMinutes: Int,
        total: Double, paid: Double, remaining: Double, currency: String
    ): String {
        val rest = if (remaining > 0) Formatters.formatNumber(remaining) else "لا شيء (خالص)"
        return "${sessionHead(customerName, startMillis, endMillis)}، ${Formatters.formatDurationArabic(durationMinutes)}، " +
            "المطلوب ${Formatters.formatCurrency(total, currency)}، الواصل ${Formatters.formatNumber(paid)}، المتبقي عليه $rest"
    }

    fun sessionSpoken(
        customerName: String, startMillis: Long, endMillis: Long, durationMinutes: Int,
        total: Double, paid: Double, remaining: Double, currency: String
    ): String {
        val words = { a: Double -> SpeechTextBuilder.amountToSpeechWords(a, currency) }
        val rest = if (remaining > 0) words(remaining) else "لا شيء، خالص"
        return "${sessionHead(customerName, startMillis, endMillis)}، ${SpeechTextBuilder.formatDurationWords(durationMinutes)}، " +
            "المطلوب ${words(total)}، الواصل ${words(paid)}، المتبقي عليه $rest"
    }

    private fun voucherHead(isReceipt: Boolean, party: String) =
        if (isReceipt) "قبض من ${name(party)}" else "صرف لـ ${name(party)}"

    private fun notePart(notes: String) = notes.trim().let { if (it.isEmpty()) "" else "، البيان: $it" }

    fun voucher(isReceipt: Boolean, partyName: String, amount: Double, notes: String, currency: String): String =
        "${voucherHead(isReceipt, partyName)} مبلغ ${Formatters.formatCurrency(amount, currency)}${notePart(notes)}"

    fun voucherSpoken(isReceipt: Boolean, partyName: String, amount: Double, notes: String, currency: String): String =
        "${voucherHead(isReceipt, partyName)} مبلغ ${SpeechTextBuilder.amountToSpeechWords(amount, currency)}${notePart(notes)}"

    fun purchase(
        ownerName: String, durationMinutes: Int, total: Double, paid: Double, currency: String
    ): String =
        "شراء ${Formatters.formatDurationArabic(durationMinutes)} من ${name(ownerName)}، الإجمالي ${Formatters.formatCurrency(total, currency)}، المسلَّم ${Formatters.formatNumber(paid)}"

    fun purchaseSpoken(
        ownerName: String, durationMinutes: Int, total: Double, paid: Double, currency: String
    ): String =
        "شراء ${SpeechTextBuilder.formatDurationWords(durationMinutes)} من ${name(ownerName)}، الإجمالي ${SpeechTextBuilder.amountToSpeechWords(total, currency)}، المسلَّم ${SpeechTextBuilder.amountToSpeechWords(paid, currency)}"
}
