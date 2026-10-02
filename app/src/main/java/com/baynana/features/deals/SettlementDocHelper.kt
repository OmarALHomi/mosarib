package com.baynana.features.deals

import android.content.Context
import com.baynana.core.util.FileSharingHelper
import com.baynana.core.util.Formatters
import java.text.NumberFormat
import java.util.Locale

/**
 * Generates formal agricultural settlement and deal documents formatted
 * for WhatsApp and record-keeping in Yemeni agricultural customs.
 */
object SettlementDocHelper {

    fun generateContractText(deal: SettlementDeal): String {
        val nf = NumberFormat.getNumberInstance(Locale.US)
        val formattedDate = Formatters.formatDate(deal.dealDate)
        val formattedDue = deal.dueDate?.let { "📅 موعد الوفاء بالمتبقي: ${Formatters.formatDate(it)}\n" } ?: ""

        return """
══════════════════════════════
📜 وثيقة عقد صلح وبيع ثمرة زراعية 📜
رقم السند: ${deal.dealNumber}
تاريخ العقد: $formattedDate
══════════════════════════════
🌾 نوع المحصول: ${deal.cropTitle} (${deal.cropType})
📍 الموقع: ${deal.location.ifBlank { "العزلة" }}

👤 الطرف الأول (البائع / المزارع):
   • الاسم: ${deal.sellerName}
   • الهاتف: ${deal.sellerPhone.ifBlank { "غير مسجل" }}

👤 الطرف الثاني (المشتري / المجبري):
   • الاسم: ${deal.buyerName}
   • الهاتف: ${deal.buyerPhone.ifBlank { "غير مسجل" }}

🤝 الوسيط المعتمد (الدلال):
   • الاسم: ${deal.dallalName.ifBlank { "دلال معتمد" }}
   • الهاتف: ${deal.dallalPhone.ifBlank { "غير مسجل" }}

──────────────────────────────
💰 القيمة الإجمالية: ${nf.format(deal.totalAmount)} ريال
💵 العربون المقبوض: ${nf.format(deal.advancePayment)} ريال
⏳ المبلغ المتبقي: ${nf.format(deal.remainingAmount)} ريال
🤝 سعاية الدلال: ${nf.format(deal.dallalCommission)} ريال
$formattedDue──────────────────────────────
📝 الشروط والملاحظات:
${deal.termsNotes.ifBlank { "تم البيع والمعاينة برضا الأطراف والاتفاق التام دون أي إكراه." }}
══════════════════════════════
حرر هذا الصلح إلكترونياً عبر تطبيق [بيننا] — المنظومة الزراعية الشاملة.
        """.trimIndent()
    }

    fun shareContract(context: Context, deal: SettlementDeal, targetPhone: String = "") {
        val contract = generateContractText(deal)
        if (targetPhone.isNotBlank()) {
            FileSharingHelper.sendWhatsAppMessage(context, targetPhone, contract)
        } else {
            FileSharingHelper.shareText(context, contract, "وثيقة صلح رقم ${deal.dealNumber}")
        }
    }
}
