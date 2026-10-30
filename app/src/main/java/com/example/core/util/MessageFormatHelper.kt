package com.example.core.util

import com.example.features.customers.Customer
import com.example.features.customers.CustomerWithBalance
import com.example.features.sessions.WaterSession
import com.example.features.settings.AppConfig
import com.example.features.vouchers.SettlementResult
import com.example.features.vouchers.Voucher
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath

/**
 * منسق الرسائل الموحد لتطبيق المُسَرِّبْ.
 * يدعم صيغة واتساب الأصلية كاملة مع التذييل والإيموجيات،
 * وصيغة SMS المختصرة للبيانات مع الحفاظ على الإيموجيات والتذييل.
 */
object MessageFormatHelper {

    /**
     * احتساب عدد أجزاء رسالة SMS باللغة العربية (UCS-2 Encoding):
     * الرسالة الأولى حتى 70 حرفاً = جزء واحد.
     * أكثر من 70 حرفاً = 67 حرفاً لكل جزء إضافي.
     */
    fun calculateSmsSegments(text: String): Int {
        val len = text.length
        return when {
            len == 0 -> 0
            len <= 70 -> 1
            else -> kotlin.math.ceil(len / 67.0).toInt()
        }
    }

    /**
     * 1. فاتورة دورة السقي (جلسة ري)
     */
    fun buildSessionBill(
        session: WaterSession,
        customer: Customer,
        config: AppConfig,
        isSms: Boolean,
        sponsorCustomer: Customer? = null,
        beneficiaryCustomer: Customer? = null
    ): String {
        val isSponsored = (session.billedToCustomerId != null && session.billedToCustomerId != session.customerId) || sponsorCustomer != null

        return if (isSponsored) {
            val farmerName = beneficiaryCustomer?.name
                ?: if (customer.id == session.customerId) customer.name else "المزارع"
            val ownerName = sponsorCustomer?.name
                ?: if (customer.id == session.billedToCustomerId) customer.name else "صاحب البئر"

            if (isSms) {
                val duration = Formatters.formatDurationCompact(session.durationMinutes)
                val date = Formatters.formatDateShort(session.startTime)
                val total = Formatters.formatNumber(session.totalAmount)
                val msg = """
💧 فاتورة ري #${session.id}
👤 المزارع: ${farmerName.take(16)}
💳 على حساب: ${ownerName.take(16)}
⏱️ المدة: $duration
💵 المبلغ: $total
✅ مسدد مقاصة مع المالك
📅 التاريخ: $date
                """.trimIndent()
                FileSharingHelper.attachMessageFooter(msg)
            } else {
                val timeRange = "من ${Formatters.formatTime(session.startTime)} إلى ${Formatters.formatTime(session.endTime)}"
                val appTitle = config.distributorName.ifEmpty { "المسرّب" }
                val farmText = if (beneficiaryCustomer?.farmName?.isNotBlank() == true) {
                    " (${beneficiaryCustomer.farmName})"
                } else if (customer.id == session.customerId && customer.farmName.isNotBlank()) {
                    " (${customer.farmName})"
                } else ""
                val msg = """
*فاتورة ري - $appTitle*
👤 المزارع (المستفيد): $farmerName$farmText
💳 على حساب صاحب البئر: $ownerName
⏱️ الوقت: $timeRange (${Formatters.formatDurationArabic(session.durationMinutes)})
💵 إجمالي المبلغ: ${Formatters.formatCurrency(session.totalAmount, config.currencySymbol)}
📊 الحالة: مسدد بالكامل مقاصة على حساب صاحب البئر ✅
📅 التاريخ: ${Formatters.formatDate(session.startTime)}
                """.trimIndent()
                FileSharingHelper.attachMessageFooter(msg)
            }
        } else if (isSms) {
            val duration = Formatters.formatDurationCompact(session.durationMinutes)
            val date = Formatters.formatDateShort(session.startTime)
            val total = Formatters.formatNumber(session.totalAmount)
            val paid = Formatters.formatNumber(session.amountPaid)
            val rem = Formatters.formatNumber(session.remainingDebt)
            val curr = config.currencySymbol
            val statusEmoji = if (session.remainingDebt > 0) "⚠️ المتبقي: $rem $curr" else "✅ مسدد"
            val msg = """
💧 فاتورة ري #${session.id}
👤 العميل: ${customer.name.take(18)}
⏱️ المدة: $duration
💵 المبلغ: $total
💰 المسدد: $paid
$statusEmoji
📅 التاريخ: $date
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        } else {
            val timeRange = "من ${Formatters.formatTime(session.startTime)} إلى ${Formatters.formatTime(session.endTime)}"
            val debtStatus = if (session.remainingDebt > 0) {
                "المتبقي: ${Formatters.formatCurrency(session.remainingDebt, config.currencySymbol)}"
            } else {
                "مسدد ✅"
            }
            val appTitle = config.distributorName.ifEmpty { "المسرّب" }
            val farm = if (customer.farmName.isNotBlank()) " (${customer.farmName})" else ""
            val msg = """
*فاتورة ري - $appTitle*
👤 العميل: ${customer.name}$farm
⏱️ الوقت: $timeRange (${Formatters.formatDurationArabic(session.durationMinutes)})
💵 المبلغ: ${Formatters.formatCurrency(session.totalAmount, config.currencySymbol)} | مسدد: ${Formatters.formatCurrency(session.amountPaid, config.currencySymbol)}
📊 الحالة: $debtStatus
📅 التاريخ: ${Formatters.formatDate(session.startTime)}
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        }
    }

    /**
     * 2. سند القبض النقدي
     */
    fun buildReceiptVoucher(
        voucher: Voucher,
        customer: Customer,
        sessionNumber: Long? = null,
        config: AppConfig,
        isSms: Boolean
    ): String {
        val vNum = voucher.voucherNumber.ifEmpty { voucher.id.toString() }
        val date = if (isSms) Formatters.formatDateShort(voucher.date) else Formatters.formatDateTime(voucher.date)
        val amountNum = Formatters.formatNumber(voucher.amount)
        val curr = config.currencySymbol

        return if (isSms) {
            val sessionPart = if (sessionNumber != null && sessionNumber > 0) "سداد دورة: #$sessionNumber\n" else ""
            val msg = """
🧾 سند قبض #$vNum
👤 استلمنا من: ${customer.name.take(18)}
💵 مبلغ: $amountNum $curr
${sessionPart}📅 التاريخ: $date
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        } else {
            val sessionPart = if (sessionNumber != null && sessionNumber > 0) "سداد دورة سقي #$sessionNumber\n" else ""
            val msg = """
*سند قبض #$vNum*
${sessionPart}استلمنا من الأخ: ${customer.name}
المبلغ: ${Formatters.formatCurrency(voucher.amount, curr)} (${Formatters.amountToArabicWords(voucher.amount, curr)})
طريقة الدفع: ${voucher.paymentMethod}
التاريخ: $date
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        }
    }

    /**
     * 3. سند الصرف النقدي
     */
    fun buildDisbursementVoucher(
        voucher: Voucher,
        customer: Customer,
        config: AppConfig,
        isSms: Boolean
    ): String {
        val vNum = voucher.voucherNumber.ifEmpty { voucher.id.toString() }
        val date = if (isSms) Formatters.formatDateShort(voucher.date) else Formatters.formatDateTime(voucher.date)
        val amountNum = Formatters.formatNumber(voucher.amount)
        val curr = config.currencySymbol

        return if (isSms) {
            val notePart = if (voucher.notes.isNotBlank()) "📝 البيان: ${voucher.notes.take(20)}\n" else ""
            val msg = """
💸 سند صرف #$vNum
👤 المستلم: ${customer.name.take(18)}
💵 مبلغ: $amountNum $curr
${notePart}📅 التاريخ: $date
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        } else {
            val reason = voucher.notes.ifEmpty { voucher.category }
            val msg = """
*سند صرف #$vNum*
المستلم: ${customer.name}
المبلغ: ${Formatters.formatCurrency(voucher.amount, curr)} (${Formatters.amountToArabicWords(voucher.amount, curr)})
البيان: $reason
طريقة الصرف: ${voucher.paymentMethod}
التاريخ: $date
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        }
    }

    fun buildPurchaseBill(
        purchase: WellOwnerPurchase,
        owner: Customer,
        config: AppConfig,
        isSms: Boolean
    ): String {
        val chargeable = WellOwnerPurchaseMath.chargeableMinutes(purchase)
        val totalAmount = WellOwnerPurchaseMath.payableAmount(purchase)
        val remainingDebt = (totalAmount - purchase.amountPaid).coerceAtLeast(0.0)
        return buildPurchaseBill(
            purchase = purchase,
            owner = owner,
            chargeableMinutes = chargeable,
            wasteMinutes = purchase.wastedMinutesOnOwner,
            totalAmount = totalAmount,
            paidAmount = purchase.amountPaid,
            remainingDebt = remainingDebt,
            config = config,
            isSms = isSms
        )
    }

    fun buildPurchaseBill(
        purchase: WellOwnerPurchase,
        owner: Customer,
        chargeableMinutes: Int,
        wasteMinutes: Int,
        totalAmount: Double,
        paidAmount: Double,
        remainingDebt: Double,
        config: AppConfig,
        isSms: Boolean
    ): String {
        val curr = config.currencySymbol
        val date = if (isSms) Formatters.formatDateShort(purchase.date) else Formatters.formatDateTime(purchase.date)

        return if (isSms) {
            val wastePart = if (wasteMinutes > 0) " (هدر ${Formatters.formatDurationCompact(wasteMinutes)})" else ""
            val remText = if (remainingDebt > 0) "⚠️ المتبقي لكم: ${Formatters.formatNumber(remainingDebt)} $curr" else "✅ مسدد"
            val msg = """
💧 شراء ساعات #${purchase.id}
👤 صاحب البئر: ${owner.name.take(18)}
⏱️ الساعات: ${Formatters.formatDurationCompact(chargeableMinutes)}$wastePart
💵 الإجمالي: ${Formatters.formatNumber(totalAmount)}
💸 المسلم: ${Formatters.formatNumber(paidAmount)}
$remText
📅 التاريخ: $date
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        } else {
            val wastePart = if (wasteMinutes > 0) "\nهدر على صاحب البئر: ${Formatters.formatDurationArabic(wasteMinutes)}" else ""
            val msg = """
*بيان شراء ساعات مياه*
صاحب البئر: ${owner.name}
ساعات الشراء: ${Formatters.formatDurationArabic(purchase.durationMinutes)}
المحتسب بعد الهدر: ${Formatters.formatDurationArabic(chargeableMinutes)}$wastePart
الإجمالي: ${Formatters.formatCurrency(totalAmount, curr)}
المصروف (المسلم نقداً): ${Formatters.formatCurrency(paidAmount, curr)}
المتبقي (عليك): ${Formatters.formatCurrency(remainingDebt, curr)}
التاريخ: $date
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        }
    }

    /**
     * 5. إشعار تسوية متعددة وتوزيع سند قبض
     */
    fun buildSettlementMessage(
        result: SettlementResult,
        isSms: Boolean
    ): String {
        val curr = result.currencySymbol
        return if (isSms) {
            val lines = result.items.take(3).joinToString("\n") { item ->
                val status = if (item.isFullyPaid) "مسدد ✅" else "متبقي ${Formatters.formatNumber(item.remainingDebtAfter)}"
                "• دورة #${item.sessionId}: ${Formatters.formatNumber(item.allocatedAmount)} ($status)"
            }
            val discSms = if (result.discountAmount > 0.0) " (خصم تسوية: ${Formatters.formatNumber(result.discountAmount)})" else ""
            val msg = """
🧾 تسوية سداد للأخ: ${result.customerName.take(18)}
💵 المبلغ: ${Formatters.formatNumber(result.totalAmount)} $curr$discSms
تم سداد:
$lines
📅 التاريخ: ${Formatters.formatDateShort(System.currentTimeMillis())}
            """.trimIndent()
            FileSharingHelper.attachMessageFooter(msg)
        } else {
            val sb = StringBuilder()
            sb.append("السلام عليكم ورحمة الله وبركاته\n")
            sb.append("الأخ/ ${result.customerName} المحترم\n")
            sb.append("تم استلام وتسجيل سند قبض بمبلغ: ${Formatters.formatCurrency(result.totalAmount, curr)}\n")
            if (result.discountAmount > 0.0) {
                sb.append("🏷️ خصم تسوية ومسامحة: ${Formatters.formatCurrency(result.discountAmount, curr)}\n")
            }
            sb.append("\n📋 تفاصيل تسديد الجلسات:\n")
            result.items.forEach { item ->
                val dateStr = Formatters.formatDate(item.date)
                val status = if (item.isFullyPaid) "مسدد ✅" else "متبقي: ${Formatters.formatCurrency(item.remainingDebtAfter, curr)} ⏳"
                sb.append("• دورة #${item.sessionId} ($dateStr):\n")
                sb.append("   - المسدد نقد: ${Formatters.formatCurrency(item.allocatedAmount, curr)}\n")
                if (item.allocatedDiscount > 0.0) {
                    sb.append("   - خصم تسوية: ${Formatters.formatCurrency(item.allocatedDiscount, curr)}\n")
                }
                sb.append("   - الحالة: $status\n")
            }
            if (result.surplusAmount > 0.0) {
                sb.append("• رصيد فائض لحسابكم: ${Formatters.formatCurrency(result.surplusAmount, curr)} 💰\n")
            }
            sb.append("\nالتاريخ: ${Formatters.formatDate(System.currentTimeMillis())}")
            FileSharingHelper.attachMessageFooter(sb.toString())
        }
    }

    /**
     * 6. كشف حساب العميل أو صاحب البئر الإجمالي
     */
    fun buildCustomerStatement(
        customer: Customer,
        item: CustomerWithBalance,
        config: AppConfig,
        isSms: Boolean
    ): String {
        val curr = config.currencySymbol
        val date = if (isSms) Formatters.formatDateShort(System.currentTimeMillis()) else Formatters.formatDate(System.currentTimeMillis())

        return if (customer.isWellOwner) {
            if (isSms) {
                val netDue = item.payableBalance - item.receivableBalance
                val status = when {
                    netDue > 0 -> "⚠️ المتبقي لكم: ${Formatters.formatNumber(netDue)} $curr"
                    netDue < 0 -> "⚠️ المستحق عليكم: ${Formatters.formatNumber(-netDue)} $curr"
                    else -> "✅ الحساب مسدد"
                }
                val msg = """
📊 كشف حساب بئر: ${customer.name.take(18)}
🛒 قيمة الشراء: ${Formatters.formatNumber(item.totalPurchaseAmount)}
💸 المسدد لكم: ${Formatters.formatNumber(item.totalDisbursedAmount)}
$status
📅 حتى: $date
                """.trimIndent()
                FileSharingHelper.attachMessageFooter(msg)
            } else {
                val appTitle = config.distributorName.ifEmpty { "المسرّب" }
                val netStatus = when {
                    item.payableBalance > 0 -> "⚠️ صافي الرصيد المستحق لكم (في ذمتنا): ${Formatters.formatCurrency(item.payableBalance, curr)}"
                    item.receivableBalance > 0 -> "⚠️ صافي الرصيد المستحق لنا (في ذمتكم): ${Formatters.formatCurrency(item.receivableBalance, curr)}"
                    else -> "✅ الحساب مسدد ومطابق بالكامل"
                }
                val ownerWasteMinutes = (item.totalPurchasedMinutes - item.totalChargeablePurchasedMinutes).coerceAtLeast(0)
                val msg = """
*كشف حساب صاحب البئر - $appTitle*
💧 صاحب البئر: ${customer.name}
📍 البئر / الموقع: ${customer.farmName.ifEmpty { customer.location.ifEmpty { "عام" } }}
🛒 شراء الساعات: ${Formatters.formatDurationArabic(item.totalPurchasedMinutes)}، المحتسب بعد الهدر: ${Formatters.formatDurationArabic(item.totalChargeablePurchasedMinutes)}
🕒 هدر على صاحب البئر: ${Formatters.formatDurationArabic(ownerWasteMinutes)} (خصم ${Formatters.formatCurrency(item.totalOwnerWasteCredit, curr)})
💰 صافي قيمة المشتريات: ${Formatters.formatCurrency(item.totalPurchaseAmount, curr)}
💸 المسدد نقداً لصاحب البئر: ${Formatters.formatCurrency(item.totalDisbursedAmount, curr)}
------------------------
💧 سقي / بيع مقيد على حسابه: ${Formatters.formatDurationArabic(item.totalSoldMinutes)}، بقيمة ${Formatters.formatCurrency(item.totalBilledAmount, curr)}
💵 المحصل منه نقداً: ${Formatters.formatCurrency(item.totalPaidAmount, curr)}
------------------------
$netStatus
------------------------
                """.trimIndent()
                FileSharingHelper.attachMessageFooter(msg)
            }
        } else {
            if (isSms) {
                val debtText = when {
                    item.receivableBalance > 0 -> "⚠️ المطلوب: ${Formatters.formatNumber(item.receivableBalance)} $curr"
                    item.receivableBalance < 0 -> "✅ لكم رصيد: ${Formatters.formatNumber(-item.receivableBalance)} $curr"
                    else -> "✅ مسدد"
                }
                val msg = """
📊 كشف حساب: ${customer.name.take(18)}
⏱️ السقي: ${Formatters.formatDurationCompact(item.totalSoldMinutes)}
💰 القيمة: ${Formatters.formatNumber(item.totalBilledAmount)}
💵 المسدد: ${Formatters.formatNumber(item.totalPaidAmount)}
$debtText
📅 حتى: $date
                """.trimIndent()
                FileSharingHelper.attachMessageFooter(msg)
            } else {
                val appTitle = config.distributorName.ifEmpty { "المسرّب" }
                val debtStatus = if (item.receivableBalance > 0) {
                    "⚠️ المطلوب بذمتكم: ${Formatters.formatCurrency(item.receivableBalance, curr)}"
                } else if (item.receivableBalance < 0) {
                    "✅ لديكم رصيد دائن: ${Formatters.formatCurrency(kotlin.math.abs(item.receivableBalance), curr)}"
                } else {
                    "✅ الحساب مسدد"
                }
                val msg = """
*كشف حساب مياه - $appTitle*
👤 العميل: ${customer.name}
📍 المزرعة: ${customer.farmName.ifEmpty { "عام" }}
⏱️ ساعات البيع بعد هدر المسرب: ${Formatters.formatDurationArabic(item.totalSoldMinutes)}
💰 إجمالي قيمة المسارب: ${Formatters.formatCurrency(item.totalBilledAmount, curr)}
💵 إجمالي المسدد نقداً: ${Formatters.formatCurrency(item.totalPaidAmount, curr)}
------------------------
$debtStatus
------------------------
                """.trimIndent()
                FileSharingHelper.attachMessageFooter(msg)
            }
        }
    }
}
