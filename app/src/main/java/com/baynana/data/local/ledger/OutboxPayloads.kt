package com.baynana.data.local.ledger

import com.baynana.domain.ledger.AllocationMode
import com.baynana.domain.ledger.AllocationPlan
import org.json.JSONArray
import org.json.JSONObject

/**
 * صيغة السلك: نصّ JSON بإصدار معلن `v`، وكل مبلغ **نصًّا بالوحدة الصغرى** لا رقمًا عشريًا
 * (ADR-04): `{"amountMinor":"1500000","currency":"YER_NEW"}`. الصيغة تُخزَّن في صف صندوق
 * الصادر كما هي، فيستطيع العامل (ح٦) إرسالها بلا إعادة حساب، ويستطيع الخادم رفض نسخة لا يفهمها.
 *
 * الحقول النصية تمرّ من `JSONObject` فتُهرَّب تلقائيًا: وصف فيه علامة تنصيص أو سطر جديد لا
 * يفسد الرسالة.
 */
object OutboxPayloads {
    const val VERSION = 1

    private fun money(amountMinor: Long, currency: String): JSONObject =
        JSONObject().put("amountMinor", amountMinor.toString()).put("currency", currency)

    private fun entryJson(entry: LedgerEntry): JSONObject = JSONObject()
        .put("id", entry.id)
        .put("operationId", entry.operationId)
        .put("roomId", entry.roomId)
        .put("type", entry.type)
        .put("owedByMemberId", entry.owedByMemberId)
        .put("owedToMemberId", entry.owedToMemberId)
        .put("amountMinor", entry.amountMinor.toString())
        .put("currency", entry.currency)
        .put("occurredAt", entry.occurredAt)
        .put("status", entry.status)
        .put("description", entry.description)
        .put("quantityNote", entry.quantityNote)
        .put("createdByMemberId", entry.createdByMemberId)
        .put("createdAt", entry.createdAt)
        .put("updatedAt", entry.updatedAt)
        .apply {
            entry.listingId?.let { put("listingId", it) }
            entry.reversesEntryId?.let { put("reversesEntryId", it) }
            entry.sourceTable?.let { put("sourceTable", it) }
            entry.sourceId?.let { put("sourceId", it) }
        }

    private fun allocationsJson(rows: List<EntryAllocation>): JSONArray {
        val array = JSONArray()
        rows.forEach { row ->
            array.put(
                JSONObject()
                    .put("paymentEntryId", row.paymentEntryId)
                    .put("debtEntryId", row.debtEntryId)
                    .put("amountMinor", row.amountMinor.toString())
                    .put("currency", row.currency)
                    .put("createdAt", row.createdAt)
            )
        }
        return array
    }

    /** قيد دين جديد (سقية/دين سلعة/صلح). */
    fun entry(entry: LedgerEntry): String = JSONObject()
        .put("v", VERSION)
        .put("kind", "ENTRY")
        .put("entry", entryJson(entry))
        .toString()

    /** سداد أو قبض مع إسقاطاته: `unappliedMinor` هو الفائض الذي بقي رصيدًا دائنًا في الغرفة. */
    fun receipt(entry: LedgerEntry, allocations: List<EntryAllocation>, mode: AllocationMode, unappliedMinor: Long): String {
        val modeJson = when (mode) {
            is AllocationMode.None -> JSONObject().put("kind", "NONE")
            is AllocationMode.OldestFirst -> JSONObject().put("kind", "OLDEST_FIRST")
            is AllocationMode.Selected -> JSONObject().put("kind", "SELECTED").put("entryIds", JSONArray(mode.entryIds))
        }
        return JSONObject()
            .put("v", VERSION)
            .put("kind", "RECEIPT")
            .put("entry", entryJson(entry))
            .put("allocations", allocationsJson(allocations))
            .put("mode", modeJson)
            .put("unappliedMinor", unappliedMinor.toString())
            .toString()
    }

    /** تخصيص لاحق لقبض عام: بالتاريخ الجديد وبسببه، وبلا مساس بتاريخ الدفع الأصلي. */
    fun allocation(receipt: LedgerEntry, plan: AllocationPlan, reason: String, decidedAt: Long): String {
        val rows = plan.allocations.map {
            EntryAllocation(
                paymentEntryId = receipt.id,
                debtEntryId = it.debtEntryId,
                amountMinor = it.amountMinor,
                currency = receipt.currency,
                createdAt = decidedAt
            )
        }
        return JSONObject()
            .put("v", VERSION)
            .put("kind", "ALLOCATE")
            .put("operationId", receipt.operationId)
            .put("entryId", receipt.id)
            .put("paidAt", receipt.occurredAt)
            .put("decidedAt", decidedAt)
            .put("reason", reason)
            .put("allocations", allocationsJson(rows))
            .put("unappliedMinor", plan.unappliedMinor.toString())
            .toString()
    }

    /** إلغاء بقيد عكسي: الأصل، والعكسي، والإسقاطات المحرَّرة. */
    fun reversal(original: LedgerEntry, reversal: LedgerEntry, reason: String, operationId: String): String =
        JSONObject()
            .put("v", VERSION)
            .put("kind", "VOID")
            .put("operationId", operationId)
            .put("reversesEntryId", original.id)
            .put("reason", reason)
            .put("entry", entryJson(reversal))
            .toString()

    /** صف صادر بلا حمولة معروفة: يُرفض في العامل ولا يُرسل صامتًا. */
    fun unknown(kind: String): String = JSONObject().put("v", VERSION).put("kind", kind).toString()
}
