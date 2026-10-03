package com.baynana.domain.ledger

import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money

/**
 * طلبات الكتابة في الدفتر: تُبنى في الشاشة، وتُفحص هنا (في `domain`) قبل أن تصل القاعدة.
 * الرفض هنا برسالة مفهومة أفضل من قيد يفشل لاحقًا في SQLite.
 *
 * العملات أربع لا خامسة، والمبلغ بالوحدة الصغرى (فلس) وبسقف المال المعلن (ADR-04).
 */
private fun requireAmount(amountMinor: Long) {
    require(amountMinor > 0L) { "المبلغ يجب أن يكون أكبر من صفر" }
    require(amountMinor <= Money.MAX_MINOR) { "المبلغ يتجاوز السقف المسموح" }
}

private fun requireCurrency(currency: String) {
    require(Currency.fromCode(currency).code == currency) { "عملة غير معروفة: $currency" }
}

private fun requireMembers(first: String, second: String) {
    require(first.isNotBlank() && second.isNotBlank()) { "طرفا القيد مطلوبان" }
    require(first != second) { "لا قيد من عضو إلى نفسه" }
}

/** سقية أو دين سلعة أو صلح: بيان يستحق على طرف لصالح آخر. */
data class NewDebtSpec(
    val id: String,
    val operationId: String,
    val roomId: String,
    val type: String,
    val debtorMemberId: String,
    val creditorMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val occurredAt: Long,
    val description: String = "",
    val quantityNote: String = "",
    val createdByMemberId: String = "",
    val listingId: String? = null
) {
    init {
        require(id.isNotBlank() && operationId.isNotBlank() && roomId.isNotBlank()) { "معرّفات ناقصة" }
        require(type in EntryType.debts) { "نوع القيد ليس دينًا: $type" }
        requireAmount(amountMinor)
        requireCurrency(currency)
        requireMembers(debtorMemberId, creditorMemberId)
        require(occurredAt > 0L) { "تاريخ القيد مطلوب" }
        require(description.trim().isNotEmpty() || quantityNote.trim().isNotEmpty()) {
            "البيان مطلوب: لا قيد بلا وصف يفهمه الطرف الآخر"
        }
    }
}

/**
 * سداد أو قبض.
 *
 * [mode] هو الفرق بين الحالات المعتمدة في §4.3:
 * - [AllocationMode.None] قبض عام: لا يُغلق أي دين.
 * - [AllocationMode.OldestFirst] «هذا سداد للديون» بلا اختيار: الأقدم فالأقدم بعد تأكيد ظاهر.
 * - [AllocationMode.Selected] أسطر مختارة فقط.
 *
 * الطرفان يُدخلان بمعنى **الدَّين** (من كان عليه ومن كان له)، والقيد يُخزَّن مرآةً لهما.
 */
data class ReceiptSpec(
    val id: String,
    val operationId: String,
    val roomId: String,
    val debtorMemberId: String,
    val creditorMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val occurredAt: Long,
    val mode: AllocationMode,
    val description: String = "",
    val createdByMemberId: String = ""
) {
    init {
        require(id.isNotBlank() && operationId.isNotBlank() && roomId.isNotBlank()) { "معرّفات ناقصة" }
        requireAmount(amountMinor)
        requireCurrency(currency)
        requireMembers(debtorMemberId, creditorMemberId)
        require(occurredAt > 0L) { "تاريخ الدفع مطلوب وثابت" }
        if (mode is AllocationMode.Selected) {
            require(mode.entryIds.isNotEmpty()) { "لم تُختَر أي أسطر: اختر أسطرًا أو استخدم الأقدم فالأقدم" }
        }
    }
}
