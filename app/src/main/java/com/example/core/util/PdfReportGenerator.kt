package com.example.core.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.settings.AppConfig
import com.example.features.vouchers.UnifiedOperation
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import java.io.File
import java.io.FileOutputStream

/**
 * Distributor-wide totals shown in the summary section of the PDF produced by
 * [PdfReportGenerator.generateComprehensiveReportPdf].
 */
data class ComprehensiveReportTotals(
    val sessionsCount: Int,
    val waterMinutes: Int,
    val totalRevenue: Double,
    val totalCollected: Double,
    val totalOutstandingDebt: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val distributorWasteMinutes: Int = 0,
    val purchasedMinutes: Int = 0,
    val chargeablePurchasedMinutes: Int = 0,
    val ownerWasteMinutes: Int = 0,
    val ownerPurchaseAmount: Double = 0.0,
    val ownerWasteCredit: Double = 0.0,
    val ownerPayments: Double = 0.0,
    val ownerPayable: Double = 0.0
)

/**
 * A single row of the "all customers" table of the PDF produced by
 * [PdfReportGenerator.generateComprehensiveReportPdf].
 */
data class ComprehensiveCustomerRow(
    val customerName: String,
    val phone: String,
    val farmName: String,
    val sessionsCount: Int,
    val waterMinutes: Int,
    val billed: Double,
    val paid: Double,
    val balance: Double,
    val isWellOwner: Boolean = false,
    val purchasedMinutes: Int = 0,
    val chargeablePurchasedMinutes: Int = 0,
    val ownerWasteMinutes: Int = 0,
    val ownerWasteCredit: Double = 0.0,
    val purchaseAmount: Double = 0.0,
    val ownerPayments: Double = 0.0,
    val payableBalance: Double = 0.0,
    val receivableBalance: Double = balance
)

object PdfReportGenerator {

    /**
     * Generates a Customer Statement PDF and returns the file URI
     */
    fun generateCustomerStatementPdf(
        context: Context,
        config: AppConfig,
        customer: Customer,
        sessions: List<WaterSession>,
        vouchers: List<Voucher>,
        purchases: List<WellOwnerPurchase> = emptyList()
    ): File {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard point width
        val pageHeight = 842 // A4 standard point height
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val fontBold = PdfThemeHelper.getCairoBold(context)
        val fontRegular = PdfThemeHelper.getCairoRegular(context)
        val fontSemiBold = PdfThemeHelper.getCairoSemiBold(context)

        val primaryColor = PdfThemeHelper.COLOR_PRIMARY
        val primaryDark = PdfThemeHelper.COLOR_PRIMARY_DARK
        val darkTextColor = PdfThemeHelper.COLOR_DARK_TEXT
        val lightGray = PdfThemeHelper.COLOR_CARD_BG
        val dividerGray = PdfThemeHelper.COLOR_BORDER
        val debtRed = PdfThemeHelper.COLOR_DANGER
        val paidGreen = PdfThemeHelper.COLOR_SUCCESS

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header Background Banner
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 105f, paint)

        // Subtle Gold accent bar under banner
        paint.color = PdfThemeHelper.COLOR_GOLD
        canvas.drawRect(0f, 102f, pageWidth.toFloat(), 105f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 21f
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (customer.isWellOwner) "كشف حساب صاحب بئر ومطابقة مشتريات" else "كشف حساب عميل - توزيع مياه", pageWidth / 2f, 44f, paint)

        // Distributor Name & Phone
        paint.textSize = 12f
        paint.typeface = fontRegular
        canvas.drawText("${config.distributorName}  |  هاتف: ${config.distributorPhone}", pageWidth / 2f, 72f, paint)

        // Customer Info Card
        val cardRect = RectF(25f, 118f, pageWidth - 25f, 202f)
        paint.color = lightGray
        canvas.drawRoundRect(cardRect, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = dividerGray
        paint.strokeWidth = 1f
        canvas.drawRoundRect(cardRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Customer details text (RTL align right)
        paint.color = darkTextColor
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 13f
        paint.typeface = fontBold
        canvas.drawText("${if (customer.isWellOwner) "صاحب البئر" else "العميل"}: ${customer.name}", pageWidth - 45f, 143f, paint)

        paint.typeface = fontRegular
        paint.textSize = 11f
        canvas.drawText("الهاتف: ${customer.phone.ifEmpty { "غير محدد" }}", pageWidth - 45f, 165f, paint)
        canvas.drawText("المزرعة / الموقع: ${customer.farmName.ifEmpty { customer.location.ifEmpty { "عام" } }}", pageWidth - 45f, 185f, paint)

        // Date of statement on left
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("تاريخ التقرير: ${Formatters.formatDate(System.currentTimeMillis())}", 45f, 143f, paint)
        canvas.drawText("العملة: ${config.currencySymbol}", 45f, 165f, paint)

        // Financial summary: pure shared calculateCustomerBalance guarantees exact match with app
        val cwb = com.example.features.customers.calculateCustomerBalance(customer, sessions, vouchers, purchases = purchases)
        val ownerPurchases = if (customer.isWellOwner) purchases.filter { it.ownerCustomerId == customer.id } else emptyList()
        val totalPurchasePayable = cwb.totalPurchaseAmount
        val totalPurchasedMinutes = cwb.totalPurchasedMinutes
        val chargeablePurchasedMinutes = cwb.totalChargeablePurchasedMinutes
        val ownerWasteMinutes = (totalPurchasedMinutes - chargeablePurchasedMinutes).coerceAtLeast(0)
        val totalOwnerWasteCredit = cwb.totalOwnerWasteCredit
        val totalWaterCost = cwb.totalBilledAmount
        val totalPaidOverall = cwb.totalPaidAmount
        val totalExpenses = cwb.totalDisbursedAmount
        val receivableBalance = cwb.receivableBalance
        val payableBalance = cwb.payableBalance
        val totalMinutes = cwb.totalSoldMinutes

        // Summary badges
        val boxWidth = (pageWidth - 70f) / 3f
        val boxH = 52f
        if (customer.isWellOwner) {
            val netStatusLabel = when {
                payableBalance > 0 -> "عليك لصاحب البئر (متبقي)"
                receivableBalance > 0 -> "لك على صاحب البئر (سقي)"
                else -> "الحساب مسدد"
            }
            val netAmount = if (payableBalance > 0) payableBalance else receivableBalance
            val netColor = if (payableBalance > 0) debtRed else paidGreen

            drawSummaryBox(canvas, 25f, 210f, boxWidth, boxH, "قيمة الشراء بعد الهدر", Formatters.formatCurrency(totalPurchasePayable, config.currencySymbol), primaryColor)
            drawSummaryBox(canvas, 25f + boxWidth + 10f, 210f, boxWidth, boxH, "المسدد له نقداً", Formatters.formatCurrency(totalExpenses, config.currencySymbol), debtRed)
            drawSummaryBox(canvas, 25f + (boxWidth + 10f) * 2, 210f, boxWidth, boxH, "سقي / بيع على حسابه", Formatters.formatCurrency(totalWaterCost, config.currencySymbol), primaryColor)
            drawSummaryBox(canvas, 25f, 268f, boxWidth, boxH, "ساعات الشراء المحتسبة", Formatters.formatDurationArabic(chargeablePurchasedMinutes), primaryColor)
            drawSummaryBox(canvas, 25f + boxWidth + 10f, 268f, boxWidth, boxH, "خصم هدر صاحب البئر", Formatters.formatCurrency(totalOwnerWasteCredit, config.currencySymbol), debtRed)
            drawSummaryBox(canvas, 25f + (boxWidth + 10f) * 2, 268f, boxWidth, boxH, netStatusLabel, Formatters.formatCurrency(netAmount, config.currencySymbol), netColor)
        } else {
            val finalBalance = receivableBalance
            drawSummaryBox(canvas, 25f, 210f, boxWidth, boxH, "السقي بعد هدر المسرب", "${Formatters.formatCurrency(totalWaterCost, config.currencySymbol)} (${Formatters.formatDurationArabic(totalMinutes)})", primaryColor)
            drawSummaryBox(canvas, 25f + boxWidth + 10f, 210f, boxWidth, boxH, "إجمالي المسدد", Formatters.formatCurrency(totalPaidOverall, config.currencySymbol), paidGreen)
            val balanceLabel = if (finalBalance > 0) "المبلغ المتبقي بذمته" else if (finalBalance < 0) "رصيد دائن للعميل" else "الحساب مسدد"
            val balanceColor = if (finalBalance > 0) debtRed else paidGreen
            drawSummaryBox(canvas, 25f + (boxWidth + 10f) * 2, 210f, boxWidth, boxH, balanceLabel, Formatters.formatCurrency(kotlin.math.abs(finalBalance), config.currencySymbol), balanceColor)
        }

        // Owner statements show the payable and receivable ledgers in separate columns.
        var curY = if (customer.isWellOwner) 386f else 290f
        paint.color = primaryColor
        canvas.drawRect(25f, curY, pageWidth - 25f, curY + 25f, paint)
        paint.color = Color.WHITE
        paint.textSize = if (customer.isWellOwner) 8.5f else 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("التاريخ", pageWidth - 35f, curY + 16f, paint)
        canvas.drawText("البيان والنشاط", pageWidth - 105f, curY + 16f, paint)
        canvas.drawText("المدة والسعر", pageWidth - 205f, curY + 16f, paint)
        canvas.drawText("المبلغ", pageWidth - 300f, curY + 16f, paint)
        canvas.drawText("المدفوع / التسوية", pageWidth - 380f, curY + 16f, paint)
        if (customer.isWellOwner) {
            canvas.drawText("رصيد الشراء علينا", 145f, curY + 16f, paint)
            canvas.drawText("رصيد السقي له", 55f, curY + 16f, paint)
        } else {
            canvas.drawText("الرصيد المتبقي", 90f, curY + 16f, paint)
        }
        curY += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        data class StatementRow(
            val date: Long,
            val title: String,
            val subtitle: String,
            val amount: Double,
            val paid: Double,
            val receivableDelta: Double = 0.0,
            val payableDelta: Double = 0.0
        )

        val rows = mutableListOf<StatementRow>()
        sessions.forEach { session ->
            val linkedReceipts = vouchers
                .filter { it.type == VoucherType.RECEIPT && it.sessionId == session.id }
                .sumOf { it.amount.coerceAtLeast(0.0) }
            val paidOutsideVouchers = (session.amountPaid - linkedReceipts).coerceAtLeast(0.0)
            val sessionDurationMinutes = session.durationMinutes.coerceAtLeast(0)
            val recordedWasteMinutes = session.wastedMinutes.coerceIn(0, sessionDurationMinutes)
            val netMinutes = sessionDurationMinutes - recordedWasteMinutes
            val wasteText = if (recordedWasteMinutes > 0) "، هدر ${Formatters.formatDurationArabic(recordedWasteMinutes)}" else ""
            rows.add(
                StatementRow(
                    date = session.startTime,
                    title = if (customer.isWellOwner) "سقي لصاحب البئر" else "ساقية ماء",
                    subtitle = "${Formatters.formatDurationArabic(netMinutes)}$wasteText @ ${Formatters.formatNumber(session.pricePerHour)}",
                    amount = session.totalAmount.coerceAtLeast(0.0),
                    paid = paidOutsideVouchers,
                    receivableDelta = (session.totalAmount - paidOutsideVouchers).coerceAtLeast(-session.totalAmount)
                )
            )
        }
        ownerPurchases.forEach { purchase ->
            val payableAmount = WellOwnerPurchaseMath.payableAmount(purchase)
            val purchaseDate = if (purchase.startTime > 0) purchase.startTime else purchase.date
            rows.add(
                StatementRow(
                    date = purchaseDate,
                    title = "شراء ساعات",
                    subtitle = "${Formatters.formatDurationArabic(purchase.durationMinutes)} - هدر ${Formatters.formatDurationArabic(purchase.wastedMinutesOnOwner)} @ ${Formatters.formatNumber(purchase.purchaseRatePerHour)}",
                    amount = payableAmount,
                    paid = 0.0,
                    payableDelta = payableAmount
                )
            )
        }
        vouchers.forEach { voucher ->
            val label = when (voucher.type) {
                VoucherType.RECEIPT -> if (customer.isWellOwner) "تحصيل عن السقي" else "سند قبض (${voucher.voucherNumber})"
                VoucherType.DISCOUNT -> "سند تسوية / خصم"
                VoucherType.EXPENSE -> if (customer.isWellOwner) "سداد لصاحب البئر" else "سند صرف"
            }
            val linkedToSession = voucher.sessionId != null && voucher.sessionId > 0
            val receivableChange = when (voucher.type) {
                VoucherType.RECEIPT -> -voucher.amount
                VoucherType.DISCOUNT -> -voucher.amount
                VoucherType.EXPENSE -> if (customer.isWellOwner) 0.0 else voucher.amount
            }
            val payableChange = if (customer.isWellOwner && voucher.type == VoucherType.EXPENSE) -voucher.amount else 0.0
            rows.add(
                StatementRow(
                    date = voucher.date,
                    title = label,
                    subtitle = voucher.notes.ifEmpty { voucher.paymentMethod },
                    amount = 0.0,
                    paid = voucher.amount,
                    receivableDelta = receivableChange,
                    payableDelta = payableChange
                )
            )
        }
        rows.sortBy { it.date }

        var runningReceivable = 0.0
        var runningPayable = 0.0
        val maxRows = if (customer.isWellOwner) 16 else 20
        val rowHeight = 22f
        rows.take(maxRows).forEachIndexed { index, row ->
            runningReceivable += row.receivableDelta
            runningPayable += row.payableDelta

            paint.color = if (index % 2 == 0) Color.WHITE else lightGray
            canvas.drawRect(25f, curY, pageWidth - 25f, curY + rowHeight, paint)
            paint.color = dividerGray
            paint.strokeWidth = 0.5f
            canvas.drawLine(25f, curY + rowHeight, pageWidth - 25f, curY + rowHeight, paint)
            paint.color = darkTextColor
            paint.textSize = if (customer.isWellOwner) 8f else 9f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(Formatters.formatDate(row.date), pageWidth - 35f, curY + 15f, paint)
            canvas.drawText(truncate(row.title, 16), pageWidth - 105f, curY + 15f, paint)
            canvas.drawText(truncate(row.subtitle, 25), pageWidth - 205f, curY + 15f, paint)
            canvas.drawText(if (row.amount > 0) Formatters.formatNumber(row.amount) else "-", pageWidth - 300f, curY + 15f, paint)
            paint.color = if (row.paid > 0) paidGreen else darkTextColor
            canvas.drawText(if (row.paid > 0) Formatters.formatNumber(row.paid) else "-", pageWidth - 380f, curY + 15f, paint)
            if (customer.isWellOwner) {
                paint.color = if (runningPayable > 0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(runningPayable), 145f, curY + 15f, paint)
                paint.color = if (runningReceivable > 0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(runningReceivable), 55f, curY + 15f, paint)
            } else {
                paint.color = if (runningReceivable > 0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(runningReceivable), 90f, curY + 15f, paint)
            }
            curY += rowHeight
        }
        if (rows.size > maxRows) {
            paint.color = darkTextColor
            paint.textSize = 9f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("يعرض هذا الكشف ${maxRows} حركة من أصل ${rows.size} حركة", pageWidth / 2f, curY + 12f, paint)
        }

        // Footer & Signatures
        paint.color = dividerGray
        paint.strokeWidth = 1f
        canvas.drawLine(25f, pageHeight - 70f, pageWidth - 25f, pageHeight - 70f, paint)

        paint.color = darkTextColor
        paint.textSize = 10f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("توقيع المستلم / العميل: ....................", pageWidth - 40f, pageHeight - 45f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("توقيع وختم المسرب / الموزع: ....................", 40f, pageHeight - 45f, paint)

        drawDocumentFooter(canvas, pageWidth, pageHeight)

        document.finishPage(page)

        // Save to cache directory
        val reportDir = File(context.cacheDir, "reports")
        if (!reportDir.exists()) reportDir.mkdirs()

        val safeCustomerName = customer.name.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val file = File(reportDir, "statement_${safeCustomerName}_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()

        return file
    }

    /**
     * Generates a Single Irrigation/Water Session Invoice PDF
     */
    fun generateSessionInvoicePdf(
        context: Context,
        config: AppConfig,
        customer: Customer,
        session: WaterSession
    ): File {
        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 width
        val pageHeight = 842 // Standard A4 height
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val fontBold = PdfThemeHelper.getCairoBold(context)
        val fontRegular = PdfThemeHelper.getCairoRegular(context)
        val fontSemiBold = PdfThemeHelper.getCairoSemiBold(context)

        val primaryColor = PdfThemeHelper.COLOR_PRIMARY
        val darkTextColor = PdfThemeHelper.COLOR_DARK_TEXT
        val mutedTextColor = PdfThemeHelper.COLOR_MUTED_TEXT
        val cardBgColor = PdfThemeHelper.COLOR_CARD_BG
        val borderColor = PdfThemeHelper.COLOR_BORDER
        val debtRed = PdfThemeHelper.COLOR_DANGER
        val paidGreen = PdfThemeHelper.COLOR_SUCCESS
        val goldColor = PdfThemeHelper.COLOR_GOLD

        val paint = Paint().apply { isAntiAlias = true }

        // 1. Header Banner
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 105f, paint)

        // Luxury Gold accent line
        paint.color = goldColor
        canvas.drawRect(0f, 102f, pageWidth.toFloat(), 105f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 21f
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("فاتورة توزيع وري مياه", pageWidth / 2f, 42f, paint)

        // Distributor Subtitle
        paint.textSize = 11.5f
        paint.typeface = fontRegular
        val subText = "${config.distributorName}  •  جوال: ${config.distributorPhone.ifEmpty { "غير محدد" }}"
        canvas.drawText(subText, pageWidth / 2f, 68f, paint)

        paint.textSize = 9.5f
        paint.color = 0xD9FFFFFF.toInt()
        canvas.drawText("سند وفاتورة محاسبية رسمية صادرة عن تطبيق المُسَرِّبْ", pageWidth / 2f, 88f, paint)

        // 2. Metadata Strip
        var curY = 130f
        paint.color = darkTextColor
        paint.textSize = 11.5f
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("فاتورة رقم: #${session.id}", pageWidth - 30f, curY, paint)

        paint.textSize = 10f
        paint.typeface = fontRegular
        paint.color = mutedTextColor
        canvas.drawText("تاريخ ووقت الري: ${Formatters.formatDateTime(session.startTime)}", pageWidth - 30f, curY + 18f, paint)

        // Status Badge (Left)
        val isSettled = session.remainingDebt <= 0
        val badgeColor = if (isSettled) paidGreen else debtRed
        val badgeBg = if (isSettled) 0xFFE8F5E9.toInt() else 0xFFFFEBEE.toInt()
        val badgeText = if (isSettled) "مسدد بالكامل ✓" else "متبقي آجل بذمة العميل"

        paint.color = badgeBg
        val badgeRect = RectF(30f, curY - 14f, 180f, curY + 22f)
        canvas.drawRoundRect(badgeRect, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = (badgeColor and 0x00FFFFFF) or 0x77000000
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(badgeRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = badgeColor
        paint.textSize = 10.5f
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(badgeText, badgeRect.centerX(), curY + 9f, paint)

        // 3. Customer & Farm Details Card
        curY += 45f
        val custRect = RectF(30f, curY, pageWidth - 30f, curY + 75f)
        paint.color = cardBgColor
        canvas.drawRoundRect(custRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = borderColor
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(custRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Top edge accent in primary color
        paint.color = primaryColor
        canvas.drawRoundRect(RectF(30f, curY, pageWidth - 30f, curY + 3.5f), 2f, 2f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = primaryColor
        paint.textSize = 9.5f
        paint.typeface = fontBold
        canvas.drawText("بيانات العميل المستفيد:", pageWidth - 45f, curY + 18f, paint)

        paint.color = darkTextColor
        paint.textSize = 13.5f
        paint.typeface = fontBold
        canvas.drawText(customer.name, pageWidth - 45f, curY + 40f, paint)

        paint.color = mutedTextColor
        paint.textSize = 10f
        paint.typeface = fontRegular
        val farmPhone = "المزرعة: ${customer.farmName.ifEmpty { "عام / غير محدد" }}    |    الهاتف: ${customer.phone.ifEmpty { "غير مسجل" }}"
        canvas.drawText(farmPhone, pageWidth - 45f, curY + 60f, paint)

        if (session.pumpName.isNotBlank()) {
            paint.textAlign = Paint.Align.LEFT
            paint.color = darkTextColor
            paint.textSize = 10f
            paint.typeface = fontSemiBold
            canvas.drawText("المضخة / البئر: ${session.pumpName}", 45f, curY + 60f, paint)
        }

        // 4. Session Specifications Table Header
        curY += 92f
        paint.color = primaryColor
        canvas.drawRoundRect(RectF(30f, curY, pageWidth - 30f, curY + 26f), 5f, 5f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.RIGHT

        canvas.drawText("فترة السقي (البدء والانتهاء)", pageWidth - 45f, curY + 17f, paint)
        canvas.drawText("المدة الإجمالية", pageWidth - 190f, curY + 17f, paint)
        canvas.drawText("هدر معفى", pageWidth - 280f, curY + 17f, paint)
        canvas.drawText("سعر الساعة", pageWidth - 365f, curY + 17f, paint)
        canvas.drawText("الإجمالي قبل الخصم", 45f + 95f, curY + 17f, paint)

        // Table Data Row
        curY += 26f
        paint.color = Color.WHITE
        canvas.drawRect(30f, curY, pageWidth - 30f, curY + 36f, paint)

        paint.color = borderColor
        paint.strokeWidth = 0.8f
        canvas.drawLine(30f, curY + 36f, pageWidth - 30f, curY + 36f, paint)

        paint.color = darkTextColor
        paint.textSize = 10f
        paint.typeface = fontRegular
        paint.textAlign = Paint.Align.RIGHT

        val timeRange = "${Formatters.formatTime(session.startTime)} - ${Formatters.formatTime(session.endTime)}"
        canvas.drawText(timeRange, pageWidth - 45f, curY + 22f, paint)

        paint.typeface = fontSemiBold
        canvas.drawText(Formatters.formatDurationArabic(session.durationMinutes), pageWidth - 190f, curY + 22f, paint)

        paint.typeface = fontRegular
        if (session.wastedMinutes > 0) {
            paint.color = debtRed
            canvas.drawText(Formatters.formatDurationArabic(session.wastedMinutes), pageWidth - 280f, curY + 22f, paint)
            paint.color = darkTextColor
        } else {
            canvas.drawText("-", pageWidth - 280f, curY + 22f, paint)
        }

        canvas.drawText("${Formatters.formatNumber(session.pricePerHour)} ${config.currencySymbol}", pageWidth - 365f, curY + 22f, paint)

        paint.typeface = fontBold
        paint.color = primaryColor
        canvas.drawText(Formatters.formatCurrency(session.totalAmount, config.currencySymbol), 45f + 95f, curY + 22f, paint)

        // 5. Tafqeet Banner (المبلغ كتابة)
        curY += 48f
        val tafqeetRect = RectF(30f, curY, pageWidth - 30f, curY + 36f)
        paint.color = 0xFFF8FAFC.toInt()
        canvas.drawRoundRect(tafqeetRect, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = 0xFFE2E8F0.toInt()
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(tafqeetRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Left vertical indicator
        paint.color = goldColor
        canvas.drawRoundRect(RectF(pageWidth - 34f, curY, pageWidth - 30f, curY + 36f), 2f, 2f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = darkTextColor
        paint.textSize = 10f
        paint.typeface = fontSemiBold
        val words = Formatters.amountToArabicWords(session.totalAmount, config.currencySymbol)
        canvas.drawText("المبلغ المطلوب كتابةً: فقط $words", pageWidth - 45f, curY + 22f, paint)

        // 6. Financial Summary 3-Card Strip
        curY += 50f
        val cardW = (pageWidth - 60f - 20f) / 3f
        val cardH = 65f

        // Card 1: إجمالي قيمة السقي
        var startX = 30f
        drawSummaryBox(
            canvas = canvas,
            x = startX,
            y = curY,
            width = cardW,
            height = cardH,
            title = "إجمالي قيمة السقي",
            value = Formatters.formatCurrency(session.totalAmount, config.currencySymbol),
            accentColor = primaryColor,
            fontBold = fontBold,
            fontRegular = fontRegular
        )

        // Card 2: المدفوع نقداً / المسدد
        startX += cardW + 10f
        drawSummaryBox(
            canvas = canvas,
            x = startX,
            y = curY,
            width = cardW,
            height = cardH,
            title = "المسدد نقداً",
            value = Formatters.formatCurrency(session.amountPaid, config.currencySymbol),
            accentColor = paidGreen,
            fontBold = fontBold,
            fontRegular = fontRegular
        )

        // Card 3: المتبقي بذمته
        startX += cardW + 10f
        drawSummaryBox(
            canvas = canvas,
            x = startX,
            y = curY,
            width = cardW,
            height = cardH,
            title = if (session.remainingDebt > 0) "المتبقي بذمة العميل" else "حالة الرصيد",
            value = if (session.remainingDebt > 0) Formatters.formatCurrency(session.remainingDebt, config.currencySymbol) else "خالص (مسدد)",
            accentColor = if (session.remainingDebt > 0) debtRed else paidGreen,
            fontBold = fontBold,
            fontRegular = fontRegular
        )

        // If there's a discount, display a dedicated small banner below the cards
        if (session.discountAmount > 0) {
            curY += cardH + 10f
            val discRect = RectF(30f, curY, pageWidth - 30f, curY + 24f)
            paint.color = 0xFFFFF8E1.toInt()
            canvas.drawRoundRect(discRect, 4f, 4f, paint)

            paint.color = 0xFFF57F17.toInt()
            paint.textSize = 9.5f
            paint.typeface = fontBold
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("★ تم منح خصم خاص للعميل بمبلغ: ${Formatters.formatCurrency(session.discountAmount, config.currencySymbol)}", pageWidth - 45f, curY + 16f, paint)
        } else {
            curY += cardH
        }

        // 7. Notes & Wasted Reason Block
        val hasNotes = session.notes.isNotBlank()
        val hasWasteReason = session.wastedReason.isNotBlank()
        if (hasNotes || hasWasteReason) {
            curY += 15f
            val noteRect = RectF(30f, curY, pageWidth - 30f, curY + 45f)
            paint.color = cardBgColor
            canvas.drawRoundRect(noteRect, 6f, 6f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = borderColor
            paint.strokeWidth = 0.8f
            canvas.drawRoundRect(noteRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.textAlign = Paint.Align.RIGHT
            paint.color = primaryColor
            paint.textSize = 9f
            paint.typeface = fontBold
            canvas.drawText("ملاحظات وبيان العملية:", pageWidth - 45f, curY + 16f, paint)

            paint.color = darkTextColor
            paint.textSize = 9.5f
            paint.typeface = fontRegular
            val noteContent = buildString {
                if (hasNotes) append(session.notes)
                if (hasNotes && hasWasteReason) append("  |  ")
                if (hasWasteReason) append("سبب الهدر: ${session.wastedReason}")
            }
            canvas.drawText(noteContent, pageWidth - 45f, curY + 33f, paint)
            curY += 45f
        }

        // 8. Signatures Block
        val sigY = pageHeight - 110f
        paint.color = borderColor
        paint.strokeWidth = 0.8f
        canvas.drawLine(30f, sigY, pageWidth - 30f, sigY, paint)

        paint.color = darkTextColor
        paint.textSize = 10f
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("توقيع واستلام العميل / المزارع:", pageWidth - 50f, sigY + 25f, paint)

        paint.typeface = fontRegular
        paint.color = mutedTextColor
        canvas.drawText("الاسم: ......................................................", pageWidth - 50f, sigY + 45f, paint)
        canvas.drawText("التوقيع: ....................................................", pageWidth - 50f, sigY + 65f, paint)

        paint.color = darkTextColor
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("توقيع وختم موزع المياه (المسرب):", 50f, sigY + 25f, paint)

        paint.typeface = fontRegular
        paint.color = mutedTextColor
        canvas.drawText("الاسم: ${config.distributorName.ifEmpty { ".............................." }}", 50f, sigY + 45f, paint)
        canvas.drawText("التوقيع / الختم: .........................................", 50f, sigY + 65f, paint)

        // 9. Standard Document Footer
        drawDocumentFooter(canvas, pageWidth, pageHeight, null, fontRegular)

        document.finishPage(page)

        val reportDir = File(context.cacheDir, "reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val file = File(reportDir, "invoice_session_${session.id}_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()

        return file
    }

    /**
     * Generates a Cash Receipt / Payment Voucher PDF
     */
    fun generateReceiptVoucherPdf(
        context: Context,
        config: AppConfig,
        customer: Customer,
        voucher: Voucher
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 540
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val primaryColor = 0xFF007A87.toInt()
        val darkTextColor = 0xFF132228.toInt()
        val lightGray = 0xFFF5F9FA.toInt()
        val dividerGray = 0xFFD2DFE5.toInt()
        val paidGreen = 0xFF2E7D32.toInt()

        val paint = Paint().apply { isAntiAlias = true }

        // Header
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 85f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (customer.isWellOwner) "تحصيل عن سقي صاحب البئر" else "سند قبض وتحصيل نقدي", pageWidth / 2f, 38f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${config.distributorName} - جوال: ${config.distributorPhone}", pageWidth / 2f, 64f, paint)

        // Voucher metadata
        paint.color = darkTextColor
        paint.textSize = 12f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("رقم السند: ${voucher.voucherNumber.ifEmpty { "#${voucher.id}" }}", pageWidth - 35f, 115f, paint)
        canvas.drawText("تاريخ السند: ${Formatters.formatDateTime(voucher.date)}", pageWidth - 35f, 135f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("طريقة الدفع: ${voucher.paymentMethod}", 35f, 115f, paint)
        canvas.drawText(if (customer.isWellOwner) "نوع المعاملة: تحصيل عن السقي" else "نوع المعاملة: سداد حساب ري", 35f, 135f, paint)

        // Customer details block
        val rectCustomer = RectF(30f, 155f, pageWidth - 30f, 215f)
        paint.color = lightGray
        canvas.drawRoundRect(rectCustomer, 8f, 8f, paint)

        paint.color = darkTextColor
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("${if (customer.isWellOwner) "وصلنا من صاحب البئر عن السقي" else "وصلنا من العميل"}: ${customer.name}", pageWidth - 45f, 180f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        canvas.drawText("المزرعة: ${customer.farmName.ifEmpty { "غير محدد" }}  |  الهاتف: ${customer.phone}", pageWidth - 45f, 202f, paint)

        // Amount Box
        val amtRect = RectF(30f, 230f, pageWidth - 30f, 320f)
        paint.color = 0xFFE8F5E9.toInt()
        canvas.drawRoundRect(amtRect, 10f, 10f, paint)

        paint.color = paidGreen
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("مبلغ وقدره:", pageWidth - 45f, 260f, paint)

        paint.textSize = 22f
        canvas.drawText(Formatters.formatCurrency(voucher.amount, config.currencySymbol), pageWidth - 140f, 262f, paint)

        paint.color = darkTextColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val words = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)
        canvas.drawText("فقط: $words", pageWidth - 45f, 298f, paint)

        // Notes box
        if (voucher.notes.isNotEmpty()) {
            paint.color = darkTextColor
            paint.textSize = 11f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("ملاحظات: ${voucher.notes}", pageWidth - 35f, 355f, paint)
        }

        // Footer & Signatures
        paint.color = dividerGray
        canvas.drawLine(30f, pageHeight - 70f, pageWidth - 30f, pageHeight - 70f, paint)

        paint.color = darkTextColor
        paint.textSize = 10.5f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("توقيع المستلم (المسرب): ....................", pageWidth - 50f, pageHeight - 45f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("توقيع المسدد (العميل): ....................", 50f, pageHeight - 45f, paint)

        drawDocumentFooter(canvas, pageWidth, pageHeight)

        document.finishPage(page)

        val reportDir = File(context.cacheDir, "reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val file = File(reportDir, "voucher_${voucher.id}_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()

        return file
    }

    /**
     * Generates an Expense Voucher PDF and returns the file URI
     */
    fun generateExpenseVoucherPdf(
        context: Context,
        config: AppConfig,
        customer: Customer?,
        voucher: Voucher
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 540
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val primaryColor = 0xFFC62828.toInt()
        val darkTextColor = 0xFF132228.toInt()
        val lightGray = 0xFFF5F9FA.toInt()
        val dividerGray = 0xFFD2DFE5.toInt()
        val expenseRed = 0xFFC62828.toInt()

        val paint = Paint().apply { isAntiAlias = true }

        // Header
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 85f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (customer?.isWellOwner == true) "سداد مستحقات صاحب البئر" else "سند صرف نقدي", pageWidth / 2f, 38f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${config.distributorName} - جوال: ${config.distributorPhone}", pageWidth / 2f, 64f, paint)

        // Voucher metadata
        paint.color = darkTextColor
        paint.textSize = 12f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("رقم السند: ${voucher.voucherNumber.ifEmpty { "#${voucher.id}" }}", pageWidth - 35f, 115f, paint)
        canvas.drawText("تاريخ السند: ${Formatters.formatDateTime(voucher.date)}", pageWidth - 35f, 135f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("طريقة الصرف: ${voucher.paymentMethod}", 35f, 115f, paint)
        canvas.drawText("نوع المعاملة: سند صرف مالي", 35f, 135f, paint)

        // Beneficiary / Customer details block
        val rectCustomer = RectF(30f, 155f, pageWidth - 30f, 215f)
        paint.color = lightGray
        canvas.drawRoundRect(rectCustomer, 8f, 8f, paint)

        paint.color = darkTextColor
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        val beneficiaryName = customer?.name ?: "مصروف عام / جهة غير محددة"
        canvas.drawText("${if (customer?.isWellOwner == true) "يُسدد لصاحب البئر" else "يُصرف للمستفيد / السيد"}: $beneficiaryName", pageWidth - 45f, 180f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        val farmOrDesc = if (customer != null) "المزرعة: ${customer.farmName.ifEmpty { "عام" }}  |  الهاتف: ${customer.phone}" else "بيان الصرف: ${voucher.notes.ifBlank { voucher.category }}"
        canvas.drawText(farmOrDesc, pageWidth - 45f, 202f, paint)

        // Amount Box
        val amtRect = RectF(30f, 230f, pageWidth - 30f, 320f)
        paint.color = 0xFFFFEBEE.toInt()
        canvas.drawRoundRect(amtRect, 10f, 10f, paint)

        paint.color = expenseRed
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("مبلغ وقدره:", pageWidth - 45f, 260f, paint)

        paint.textSize = 22f
        canvas.drawText(Formatters.formatCurrency(voucher.amount, config.currencySymbol), pageWidth - 140f, 262f, paint)

        paint.color = darkTextColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val words = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)
        canvas.drawText("فقط: $words", pageWidth - 45f, 298f, paint)

        // Notes box
        val notes = voucher.notes.ifBlank { voucher.category }
        if (notes.isNotEmpty()) {
            paint.color = darkTextColor
            paint.textSize = 11f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("البيان: $notes", pageWidth - 35f, 355f, paint)
        }

        // Footer & Signatures
        paint.color = dividerGray
        canvas.drawLine(30f, pageHeight - 70f, pageWidth - 30f, pageHeight - 70f, paint)

        paint.color = darkTextColor
        paint.textSize = 10.5f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("توقيع المحاسب / المسرب: ....................", pageWidth - 50f, pageHeight - 45f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("توقيع المستلم: ....................", 50f, pageHeight - 45f, paint)

        drawDocumentFooter(canvas, pageWidth, pageHeight)

        document.finishPage(page)

        val reportDir = File(context.cacheDir, "reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val file = File(reportDir, "expense_voucher_${voucher.id}_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()

        return file
    }

    /**
     * Generates the comprehensive ("all customers") accounting report: a summary page with the
     * distributor-wide totals followed by a paginated table that lists every customer with his
     * water minutes, billed amount, paid amount and outstanding balance.
     *
     * This is the report the comprehensive-report button of the reports screen is meant to export;
     * before it existed the ViewModel silently produced a statement for one arbitrary customer.
     */
    fun generateComprehensiveReportPdf(
        context: Context,
        config: AppConfig,
        periodTitle: String,
        totals: ComprehensiveReportTotals,
        customerRows: List<ComprehensiveCustomerRow>
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val primaryColor = 0xFF007A87.toInt()
        val darkTextColor = 0xFF132228.toInt()
        val lightGray = 0xFFF0F4F7.toInt()
        val dividerGray = 0xFFD2DFE5.toInt()
        val debtRed = 0xFFD32F2F.toInt()
        val paidGreen = 0xFF2E7D32.toInt()
        val goldAccent = 0xFFB8860B.toInt()

        val paint = Paint().apply { isAntiAlias = true }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas

        fun drawHeader(subtitle: String) {
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 88f, paint)

            // Gold accent strip at bottom of header
            paint.color = goldAccent
            canvas.drawRect(0f, 85f, pageWidth.toFloat(), 88f, paint)

            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("التقرير المحاسبي والإحصائي الشامل", pageWidth / 2f, 38f, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(subtitle, pageWidth / 2f, 62f, paint)
        }

        val rowHeight = 22f
        var curY = 320f

        fun drawTableHeader() {
            paint.color = primaryColor
            canvas.drawRoundRect(RectF(20f, curY, pageWidth - 20f, curY + 24f), 4f, 4f, paint)

            paint.color = Color.WHITE
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("العميل", 565f, curY + 16f, paint)
            canvas.drawText("المزرعة / الهاتف", 435f, curY + 16f, paint)
            canvas.drawText("السقي / صافي المدة", 335f, curY + 16f, paint)
            canvas.drawText("المبلغ المطلوب", 250f, curY + 16f, paint)
            canvas.drawText("المدفوع", 175f, curY + 16f, paint)
            canvas.drawText("المتبقي بذمته", 95f, curY + 16f, paint)

            curY += 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        fun finishCurrentPage() {
            drawDocumentFooter(canvas, pageWidth, pageHeight, pageNumber)
            document.finishPage(page)
        }

        fun startNewPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            drawHeader("${config.distributorName} - كشف حسابات العملاء")
            curY = 120f
            drawTableHeader()
        }

        // ---------- Page 1: distributor-wide summary ----------
        drawHeader("${config.distributorName}  |  هاتف: ${config.distributorPhone.ifEmpty { "غير محدد" }}")

        // Metadata Bar
        paint.color = darkTextColor
        paint.textSize = 10.5f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("الفترة: $periodTitle", pageWidth - 30f, 114f, paint)
        canvas.drawText("تاريخ التقرير: ${Formatters.formatDate(System.currentTimeMillis())}", pageWidth - 30f, 130f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("عدد العملاء: ${customerRows.size} عميل", 30f, 114f, paint)
        canvas.drawText("العملة: ${config.currencySymbol}", 30f, 130f, paint)

        // KPI Section 1: Financial Performance (4 clean columns)
        val cardSpacing = 8f
        val cardWidth4 = (pageWidth - 50f - (3 * cardSpacing)) / 4f
        val cardH = 46f

        val hasOwnerPurchases = totals.purchasedMinutes > 0 || totals.ownerPurchaseAmount > 0.0

        // Row 1: Financial KPIs (y = 146f)
        var startX = 25f
        drawSummaryBox(canvas, startX, 146f, cardWidth4, cardH, "صافي الأرباح", Formatters.formatCurrency(totals.netProfit, config.currencySymbol), if (totals.netProfit >= 0) paidGreen else debtRed)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 146f, cardWidth4, cardH, "إجمالي الإيرادات", Formatters.formatCurrency(totals.totalRevenue, config.currencySymbol), primaryColor)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 146f, cardWidth4, cardH, "المحصل الفعلي", Formatters.formatCurrency(totals.totalCollected, config.currencySymbol), paidGreen)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 146f, cardWidth4, cardH, "ديون العملاء", Formatters.formatCurrency(totals.totalOutstandingDebt, config.currencySymbol), debtRed)

        // Row 2: Operational KPIs (y = 198f)
        startX = 25f
        drawSummaryBox(canvas, startX, 198f, cardWidth4, cardH, "ساعات السقي المباعة", Formatters.formatDurationArabic(totals.waterMinutes), primaryColor)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 198f, cardWidth4, cardH, "فاقد شبكة السقي", Formatters.formatDurationArabic(totals.distributorWasteMinutes), debtRed)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 198f, cardWidth4, cardH, "مصروفات التشغيل", Formatters.formatCurrency(totals.totalExpenses, config.currencySymbol), 0xFFE65100.toInt())
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 198f, cardWidth4, cardH, "دورات السقي", "${totals.sessionsCount} دورة", darkTextColor)

        // Row 3: Well Owner Purchases (Only if well owner purchases exist)
        if (hasOwnerPurchases) {
            startX = 25f
            drawSummaryBox(canvas, startX, 250f, cardWidth4, cardH, "مشتريات الآبار", Formatters.formatCurrency(totals.ownerPurchaseAmount, config.currencySymbol), primaryColor)
            startX += cardWidth4 + cardSpacing
            drawSummaryBox(canvas, startX, 250f, cardWidth4, cardH, "المسدد للمالكين", Formatters.formatCurrency(totals.ownerPayments, config.currencySymbol), paidGreen)
            startX += cardWidth4 + cardSpacing
            drawSummaryBox(canvas, startX, 250f, cardWidth4, cardH, "مستحقات أصحاب الآبار", Formatters.formatCurrency(totals.ownerPayable, config.currencySymbol), if (totals.ownerPayable > 0) debtRed else paidGreen)
            startX += cardWidth4 + cardSpacing
            drawSummaryBox(canvas, startX, 250f, cardWidth4, cardH, "هدر صاحب البئر", Formatters.formatDurationArabic(totals.ownerWasteMinutes), debtRed)
            curY = 312f
        } else {
            curY = 260f
        }

        // Section header for Customer Ledger
        paint.color = darkTextColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("جدول كشف حسابات العملاء وساعات التوزيع:", pageWidth - 25f, curY - 6f, paint)

        // ---------- Customers table (paginated)
        drawTableHeader()

        if (customerRows.isEmpty()) {
            paint.color = 0xFF78909C.toInt()
            paint.textSize = 11f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("لا يوجد عملاء مسجلون حتى الآن", pageWidth / 2f, curY + 24f, paint)
            curY += 44f
        } else {
            customerRows.forEachIndexed { index, row ->
                if (curY + rowHeight > pageHeight - 90f) {
                    finishCurrentPage()
                    startNewPage()
                }

                paint.color = if (index % 2 == 0) Color.WHITE else lightGray
                canvas.drawRect(20f, curY, pageWidth - 20f, curY + rowHeight, paint)

                paint.color = dividerGray
                paint.strokeWidth = 0.5f
                canvas.drawLine(20f, curY + rowHeight, pageWidth - 20f, curY + rowHeight, paint)

                paint.textSize = 9.5f
                paint.textAlign = Paint.Align.RIGHT

                paint.color = darkTextColor
                canvas.drawText(truncate(row.customerName, 22), 565f, curY + 15f, paint)
                canvas.drawText(truncate(row.farmName.ifEmpty { row.phone.ifEmpty { "-" } }, 18), 435f, curY + 15f, paint)
                canvas.drawText("${row.sessionsCount} (${Formatters.formatDurationFriendly(row.waterMinutes)})", 335f, curY + 15f, paint)
                canvas.drawText(Formatters.formatNumber(row.billed), 250f, curY + 15f, paint)

                paint.color = paidGreen
                canvas.drawText(Formatters.formatNumber(row.paid), 175f, curY + 15f, paint)

                paint.color = if (row.balance > 0.0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(row.balance), 95f, curY + 15f, paint)

                curY += rowHeight
            }

            // Grand total line
            if (curY + rowHeight + 4f > pageHeight - 90f) {
                finishCurrentPage()
                startNewPage()
            }

            paint.color = 0xFFE0F2F1.toInt()
            canvas.drawRect(20f, curY, pageWidth - 20f, curY + rowHeight + 3f, paint)

            paint.color = darkTextColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("الإجمالي العام", 565f, curY + 16f, paint)
            canvas.drawText(Formatters.formatNumber(totals.totalRevenue), 250f, curY + 16f, paint)

            paint.color = paidGreen
            canvas.drawText(Formatters.formatNumber(totals.totalCollected), 175f, curY + 16f, paint)

            paint.color = if (totals.totalOutstandingDebt > 0.0) debtRed else paidGreen
            canvas.drawText(Formatters.formatNumber(totals.totalOutstandingDebt), 95f, curY + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            curY += rowHeight + 3f
        }

        // Separate supplier-ledger table: owner purchases and owner irrigation are never netted.
        val ownerRows = customerRows.filter {
            it.isWellOwner && (
                it.purchasedMinutes > 0 || it.sessionsCount > 0 || it.ownerPayments > 0.0 ||
                    it.billed > 0.0 || it.paid > 0.0 || it.receivableBalance != 0.0
                )
        }
        if (ownerRows.isNotEmpty()) {
            finishCurrentPage()

            fun drawOwnerTableHeader() {
                paint.color = primaryColor
                canvas.drawRect(20f, curY, pageWidth - 20f, curY + 25f, paint)
                paint.color = Color.WHITE
                paint.textSize = 8.3f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("صاحب البئر", 565f, curY + 16f, paint)
                canvas.drawText("شراء: مسجل / محتسب / هدر", 435f, curY + 16f, paint)
                canvas.drawText("صافي الشراء", 305f, curY + 16f, paint)
                canvas.drawText("المسدد له", 230f, curY + 16f, paint)
                canvas.drawText("متبقي له علينا", 160f, curY + 16f, paint)
                canvas.drawText("دين السقي عليه", 90f, curY + 16f, paint)
                curY += 25f
            }

            fun startOwnerPage(subtitle: String) {
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                drawHeader(subtitle)
                curY = 130f
                drawOwnerTableHeader()
            }

            startOwnerPage("${config.distributorName} | $periodTitle | حسابا صاحب البئر مستقلان")
            val ownerRowHeight = 42f
            ownerRows.forEachIndexed { index, row ->
                if (curY + ownerRowHeight > pageHeight - 90f) {
                    finishCurrentPage()
                    startOwnerPage("${config.distributorName} | $periodTitle | متابعة أصحاب الآبار")
                }
                paint.color = if (index % 2 == 0) Color.WHITE else lightGray
                canvas.drawRect(20f, curY, pageWidth - 20f, curY + ownerRowHeight, paint)
                paint.color = dividerGray
                paint.strokeWidth = 0.5f
                canvas.drawLine(20f, curY + ownerRowHeight, pageWidth - 20f, curY + ownerRowHeight, paint)
                paint.textAlign = Paint.Align.RIGHT
                paint.textSize = 8.2f
                paint.color = darkTextColor
                canvas.drawText(truncate(row.customerName, 18), 565f, curY + 14f, paint)
                val hoursText = "${Formatters.formatDurationShort(row.purchasedMinutes)} / ${Formatters.formatDurationShort(row.chargeablePurchasedMinutes)} / ${Formatters.formatDurationShort(row.ownerWasteMinutes)}"
                canvas.drawText(truncate(hoursText, 25), 435f, curY + 14f, paint)
                canvas.drawText(Formatters.formatNumber(row.purchaseAmount), 305f, curY + 14f, paint)
                paint.color = paidGreen
                canvas.drawText(Formatters.formatNumber(row.ownerPayments), 230f, curY + 14f, paint)
                paint.color = if (row.payableBalance > 0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(row.payableBalance), 160f, curY + 14f, paint)
                paint.color = if (row.receivableBalance > 0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(row.receivableBalance), 90f, curY + 14f, paint)

                paint.color = darkTextColor
                paint.textSize = 8f
                paint.textAlign = Paint.Align.RIGHT
                val detail = "هدر صاحب البئر: ${Formatters.formatDurationShort(row.ownerWasteMinutes)} / خصمه ${Formatters.formatNumber(row.ownerWasteCredit)}  •  سقي له بعد هدر المسرب: ${Formatters.formatDurationShort(row.waterMinutes)} / بيع ${Formatters.formatNumber(row.billed)} / تحصيل ${Formatters.formatNumber(row.paid)}"
                canvas.drawText(truncate(detail, 100), 565f, curY + 32f, paint)
                curY += ownerRowHeight
            }
        }

        // ---------- Signatures ----------
        if (curY + 70f < pageHeight - 60f) {
            paint.color = dividerGray
            paint.strokeWidth = 1f
            canvas.drawLine(25f, pageHeight - 85f, pageWidth - 25f, pageHeight - 85f, paint)

            paint.color = darkTextColor
            paint.textSize = 10f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("توقيع المسرب / الموزع: ....................", pageWidth - 40f, pageHeight - 60f, paint)

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("الختم: ....................", 40f, pageHeight - 60f, paint)
        }

        finishCurrentPage()

        val reportDir = File(context.cacheDir, "reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val file = File(reportDir, "comprehensive_report_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()

        return file
    }

    /**
     * كشف دليل وسجل العملاء وأرصدتهم (طباعة مخصصة لشاشة العملاء بدون تعقيدات أو حواشي مشوهة)
     */
    fun generateCustomerDirectoryPdf(
        context: Context,
        config: AppConfig,
        customerRows: List<ComprehensiveCustomerRow>
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val fontBold = PdfThemeHelper.getCairoBold(context)
        val fontRegular = PdfThemeHelper.getCairoRegular(context)

        val primaryColor = PdfThemeHelper.COLOR_PRIMARY
        val darkTextColor = PdfThemeHelper.COLOR_DARK_TEXT
        val lightGray = PdfThemeHelper.COLOR_CARD_BG
        val dividerGray = PdfThemeHelper.COLOR_BORDER
        val debtRed = PdfThemeHelper.COLOR_DANGER
        val paidGreen = PdfThemeHelper.COLOR_SUCCESS
        val goldAccent = PdfThemeHelper.COLOR_GOLD

        val paint = Paint().apply { isAntiAlias = true }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas

        fun drawHeader() {
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 88f, paint)

            paint.color = goldAccent
            canvas.drawRect(0f, 85f, pageWidth.toFloat(), 88f, paint)

            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.typeface = fontBold
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("كشف وحسابات دليل العملاء", pageWidth / 2f, 38f, paint)

            paint.textSize = 11f
            paint.typeface = fontRegular
            val subtitle = "${config.distributorName}  |  هاتف: ${config.distributorPhone.ifEmpty { "غير محدد" }}"
            canvas.drawText(subtitle, pageWidth / 2f, 62f, paint)
        }

        val rowHeight = 22f
        var curY = 225f

        fun drawTableHeader() {
            paint.color = primaryColor
            canvas.drawRoundRect(RectF(20f, curY, pageWidth - 20f, curY + 24f), 4f, 4f, paint)

            paint.color = Color.WHITE
            paint.textSize = 9.5f
            paint.typeface = fontBold
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("م", 565f, curY + 16f, paint)
            canvas.drawText("اسم العميل / المشترك", 540f, curY + 16f, paint)
            canvas.drawText("المزرعة / الهاتف", 390f, curY + 16f, paint)
            canvas.drawText("الجلسات", 295f, curY + 16f, paint)
            canvas.drawText("المطلوب", 245f, curY + 16f, paint)
            canvas.drawText("المسدد", 170f, curY + 16f, paint)
            canvas.drawText("الرصيد المتبقي", 95f, curY + 16f, paint)

            curY += 26f
            paint.typeface = fontRegular
        }

        fun finishCurrentPage() {
            drawDocumentFooter(canvas, pageWidth, pageHeight, pageNumber, fontRegular)
            document.finishPage(page)
        }

        fun startNewPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            drawHeader()
            curY = 110f
            drawTableHeader()
        }

        drawHeader()

        // Metadata Bar
        paint.color = darkTextColor
        paint.textSize = 10f
        paint.typeface = fontRegular
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("تاريخ الكشف: ${Formatters.formatDate(System.currentTimeMillis())}", pageWidth - 25f, 110f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("العملة: ${config.currencySymbol}", 25f, 110f, paint)

        // Summary KPI Strip (4 cards)
        val cardSpacing = 8f
        val cardWidth4 = (pageWidth - 50f - (3 * cardSpacing)) / 4f
        val cardH = 46f

        val totalBilled = customerRows.sumOf { it.billed }
        val totalPaid = customerRows.sumOf { it.paid }
        val totalDebts = customerRows.sumOf { it.balance.coerceAtLeast(0.0) }

        var startX = 25f
        drawSummaryBox(canvas, startX, 126f, cardWidth4, cardH, "إجمالي العملاء", "${customerRows.size} عميل", primaryColor, fontBold, fontRegular)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 126f, cardWidth4, cardH, "إجمالي السقي", Formatters.formatCurrency(totalBilled, config.currencySymbol), darkTextColor, fontBold, fontRegular)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 126f, cardWidth4, cardH, "المسدد الفعلي", Formatters.formatCurrency(totalPaid, config.currencySymbol), paidGreen, fontBold, fontRegular)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 126f, cardWidth4, cardH, "صافي الديون المطلوبة", Formatters.formatCurrency(totalDebts, config.currencySymbol), debtRed, fontBold, fontRegular)

        curY = 195f
        paint.color = darkTextColor
        paint.textSize = 11f
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("جدول تفصيلي بأرصدة وحسابات العملاء:", pageWidth - 25f, curY - 6f, paint)

        drawTableHeader()

        if (customerRows.isEmpty()) {
            paint.color = PdfThemeHelper.COLOR_MUTED_TEXT
            paint.textSize = 11f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("لا يوجد عملاء مسجلون حتى الآن", pageWidth / 2f, curY + 24f, paint)
            curY += 44f
        } else {
            customerRows.forEachIndexed { index, row ->
                if (curY + rowHeight > pageHeight - 90f) {
                    finishCurrentPage()
                    startNewPage()
                }

                paint.color = if (index % 2 == 0) Color.WHITE else lightGray
                canvas.drawRect(20f, curY, pageWidth - 20f, curY + rowHeight, paint)

                paint.color = dividerGray
                paint.strokeWidth = 0.5f
                canvas.drawLine(20f, curY + rowHeight, pageWidth - 20f, curY + rowHeight, paint)

                paint.textSize = 9f
                paint.textAlign = Paint.Align.RIGHT

                paint.color = darkTextColor
                paint.typeface = fontRegular
                canvas.drawText("${index + 1}", 565f, curY + 15f, paint)
                canvas.drawText(truncate(row.customerName, 26), 540f, curY + 15f, paint)
                val farmOrPhone = row.farmName.ifEmpty { row.phone.ifEmpty { "-" } }
                canvas.drawText(truncate(farmOrPhone, 20), 390f, curY + 15f, paint)
                canvas.drawText("${row.sessionsCount} (${Formatters.formatDurationFriendly(row.waterMinutes)})", 295f, curY + 15f, paint)
                canvas.drawText(Formatters.formatNumber(row.billed), 245f, curY + 15f, paint)

                paint.color = paidGreen
                canvas.drawText(Formatters.formatNumber(row.paid), 170f, curY + 15f, paint)

                paint.color = if (row.balance > 0.0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(row.balance), 95f, curY + 15f, paint)

                curY += rowHeight
            }

            // Grand Total Row
            if (curY + rowHeight + 4f > pageHeight - 90f) {
                finishCurrentPage()
                startNewPage()
            }

            paint.color = 0xFFE0F2F1.toInt()
            canvas.drawRect(20f, curY, pageWidth - 20f, curY + rowHeight + 3f, paint)

            paint.color = darkTextColor
            paint.typeface = fontBold
            paint.textSize = 9.5f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("الإجمالي العام", 540f, curY + 16f, paint)
            canvas.drawText(Formatters.formatNumber(totalBilled), 245f, curY + 16f, paint)

            paint.color = paidGreen
            canvas.drawText(Formatters.formatNumber(totalPaid), 170f, curY + 16f, paint)

            paint.color = if (totalDebts > 0.0) debtRed else paidGreen
            canvas.drawText(Formatters.formatNumber(totalDebts), 95f, curY + 16f, paint)

            curY += rowHeight + 3f
        }

        // Signatures
        if (curY + 70f < pageHeight - 60f) {
            paint.color = dividerGray
            paint.strokeWidth = 1f
            canvas.drawLine(25f, pageHeight - 85f, pageWidth - 25f, pageHeight - 85f, paint)

            paint.color = darkTextColor
            paint.typeface = fontRegular
            paint.textSize = 10f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("توقيع المسرب / الموزع: ....................", pageWidth - 40f, pageHeight - 60f, paint)

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("الختم: ....................", 40f, pageHeight - 60f, paint)
        }

        finishCurrentPage()

        val reportDir = File(context.cacheDir, "reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val file = File(reportDir, "customer_directory_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()

        return file
    }

    /**
     * كشف سجل العمليات والحركات (طباعة مخصصة لشاشة سجل العمليات بجميع أنواعها)
     */
    fun generateOperationsLogPdf(
        context: Context,
        config: AppConfig,
        filterTitle: String,
        operations: List<UnifiedOperation>
    ): File {
        val document = PdfDocument()
        val pageWidth = 842  // A4 Landscape for comfortable table columns
        val pageHeight = 595

        val fontBold = PdfThemeHelper.getCairoBold(context)
        val fontRegular = PdfThemeHelper.getCairoRegular(context)

        val primaryColor = PdfThemeHelper.COLOR_PRIMARY
        val darkTextColor = PdfThemeHelper.COLOR_DARK_TEXT
        val lightGray = PdfThemeHelper.COLOR_CARD_BG
        val dividerGray = PdfThemeHelper.COLOR_BORDER
        val debtRed = PdfThemeHelper.COLOR_DANGER
        val paidGreen = PdfThemeHelper.COLOR_SUCCESS
        val goldAccent = PdfThemeHelper.COLOR_GOLD

        val paint = Paint().apply { isAntiAlias = true }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas

        fun drawHeader() {
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 75f, paint)

            paint.color = goldAccent
            canvas.drawRect(0f, 72f, pageWidth.toFloat(), 75f, paint)

            paint.color = Color.WHITE
            paint.textSize = 17f
            paint.typeface = fontBold
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("كشف سجل العمليات والحركات المالية", pageWidth / 2f, 32f, paint)

            paint.textSize = 10.5f
            paint.typeface = fontRegular
            val subtitle = "${config.distributorName}  |  هاتف: ${config.distributorPhone.ifEmpty { "غير محدد" }}  |  فلترة: $filterTitle"
            canvas.drawText(subtitle, pageWidth / 2f, 55f, paint)
        }

        val rowHeight = 22f
        var curY = 195f

        fun drawTableHeader() {
            paint.color = primaryColor
            canvas.drawRoundRect(RectF(20f, curY, pageWidth - 20f, curY + 24f), 4f, 4f, paint)

            paint.color = Color.WHITE
            paint.textSize = 9f
            paint.typeface = fontBold
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("م", 815f, curY + 16f, paint)
            canvas.drawText("التاريخ والوقت", 785f, curY + 16f, paint)
            canvas.drawText("نوع العملية", 685f, curY + 16f, paint)
            canvas.drawText("العميل / الطرف", 605f, curY + 16f, paint)
            canvas.drawText("البيان والتفاصيل", 450f, curY + 16f, paint)
            canvas.drawText("المبلغ المطلوب", 255f, curY + 16f, paint)
            canvas.drawText("المدفوع نقداً", 170f, curY + 16f, paint)
            canvas.drawText("المتبقي / الرصيد", 90f, curY + 16f, paint)

            curY += 26f
            paint.typeface = fontRegular
        }

        fun finishCurrentPage() {
            drawDocumentFooter(canvas, pageWidth, pageHeight, pageNumber, fontRegular)
            document.finishPage(page)
        }

        fun startNewPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            drawHeader()
            curY = 95f
            drawTableHeader()
        }

        drawHeader()

        // Metadata Bar
        paint.color = darkTextColor
        paint.textSize = 10f
        paint.typeface = fontRegular
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("تاريخ التقرير: ${Formatters.formatDateTime(System.currentTimeMillis())}", pageWidth - 25f, 95f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("العملة: ${config.currencySymbol}", 25f, 95f, paint)

        // Summary Strip (4 cards)
        var totalAmount = 0.0
        var totalPaid = 0.0
        var totalDebt = 0.0

        operations.forEach { op ->
            when (op) {
                is UnifiedOperation.SessionOp -> {
                    totalAmount += op.sessionWithCustomer.session.totalAmount
                    totalPaid += op.sessionWithCustomer.session.amountPaid
                    totalDebt += op.sessionWithCustomer.session.remainingDebt
                }
                is UnifiedOperation.VoucherOp -> {
                    val v = op.voucherWithCustomer.voucher
                    if (v.type == VoucherType.RECEIPT) {
                        totalPaid += v.amount
                    } else {
                        totalAmount += v.amount
                    }
                }
                is UnifiedOperation.PurchaseOp -> {
                    val cost = WellOwnerPurchaseMath.payableAmount(op.purchase)
                    totalAmount += cost
                    totalPaid += op.purchase.amountPaid
                    totalDebt += (cost - op.purchase.amountPaid).coerceAtLeast(0.0)
                }
            }
        }

        val cardSpacing = 10f
        val cardWidth4 = (pageWidth - 50f - (3 * cardSpacing)) / 4f
        val cardH = 44f

        var startX = 25f
        drawSummaryBox(canvas, startX, 110f, cardWidth4, cardH, "إجمالي العمليات", "${operations.size} عملية", primaryColor, fontBold, fontRegular)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 110f, cardWidth4, cardH, "المقبوضات", Formatters.formatCurrency(totalPaid, config.currencySymbol), paidGreen, fontBold, fontRegular)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 110f, cardWidth4, cardH, "إجمالي السقي والمصروف", Formatters.formatCurrency(totalAmount, config.currencySymbol), darkTextColor, fontBold, fontRegular)
        startX += cardWidth4 + cardSpacing
        drawSummaryBox(canvas, startX, 110f, cardWidth4, cardH, "إجمالي الديون / المتبقي", Formatters.formatCurrency(totalDebt, config.currencySymbol), debtRed, fontBold, fontRegular)

        curY = 168f
        paint.color = darkTextColor
        paint.textSize = 10.5f
        paint.typeface = fontBold
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("تفاصيل الحركات المسجلة:", pageWidth - 25f, curY - 5f, paint)

        drawTableHeader()

        if (operations.isEmpty()) {
            paint.color = PdfThemeHelper.COLOR_MUTED_TEXT
            paint.textSize = 11f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("لا توجد عمليات تطابق الفلترة المحددة", pageWidth / 2f, curY + 24f, paint)
            curY += 44f
        } else {
            operations.forEachIndexed { index, op ->
                if (curY + rowHeight > pageHeight - 65f) {
                    finishCurrentPage()
                    startNewPage()
                }

                paint.color = if (index % 2 == 0) Color.WHITE else lightGray
                canvas.drawRect(20f, curY, pageWidth - 20f, curY + rowHeight, paint)

                paint.color = dividerGray
                paint.strokeWidth = 0.5f
                canvas.drawLine(20f, curY + rowHeight, pageWidth - 20f, curY + rowHeight, paint)

                paint.textSize = 8.5f
                paint.typeface = fontRegular
                paint.textAlign = Paint.Align.RIGHT

                // Extraction
                val rowData = when (op) {
                    is UnifiedOperation.SessionOp -> {
                        val s = op.sessionWithCustomer.session
                        val cName = op.sessionWithCustomer.customer?.name ?: "عميل غير محدد"
                        val dStr = "${Formatters.formatDurationFriendly(s.durationMinutes)}${if (s.wastedMinutes > 0) " (هدر: ${Formatters.formatDurationFriendly(s.wastedMinutes)})" else ""}"
                        OperationRowData(
                            dt = Formatters.formatDateTime(s.startTime),
                            typeStr = "سقي ماء",
                            party = cName,
                            detail = dStr,
                            opAmt = s.totalAmount,
                            opPaid = s.amountPaid,
                            opDebt = s.remainingDebt
                        )
                    }
                    is UnifiedOperation.VoucherOp -> {
                        val v = op.voucherWithCustomer.voucher
                        val isRec = v.type == VoucherType.RECEIPT
                        val cName = op.voucherWithCustomer.customer?.name ?: (if (isRec) "عميل نقدي" else v.category.ifEmpty { "مصروف عام" })
                        val dStr = "#${v.voucherNumber} • ${v.paymentMethod}${if (v.notes.isNotBlank() && v.notes != "عام") " (${v.notes})" else ""}"
                        OperationRowData(
                            dt = Formatters.formatDateTime(v.date),
                            typeStr = if (isRec) "سند قبض" else "سند صرف",
                            party = cName,
                            detail = dStr,
                            opAmt = v.amount,
                            opPaid = if (isRec) v.amount else 0.0,
                            opDebt = 0.0
                        )
                    }
                    is UnifiedOperation.PurchaseOp -> {
                        val p = op.purchase
                        val oName = op.owner?.name ?: "صاحب بئر #${p.ownerCustomerId}"
                        val charged = WellOwnerPurchaseMath.chargeableMinutes(p)
                        val cost = WellOwnerPurchaseMath.payableAmount(p)
                        val waste = p.wastedMinutesOnOwner
                        val dStr = "${Formatters.formatDurationArabic(charged)}${if (waste > 0) " (هدر: ${Formatters.formatDurationArabic(waste)})" else ""} • ${Formatters.formatNumber(p.purchaseRatePerHour)}/س"
                        OperationRowData(
                            dt = Formatters.formatDateTime(p.date),
                            typeStr = "شراء ساعات",
                            party = oName,
                            detail = dStr,
                            opAmt = cost,
                            opPaid = p.amountPaid,
                            opDebt = (cost - p.amountPaid).coerceAtLeast(0.0)
                        )
                    }
                }

                paint.color = darkTextColor
                canvas.drawText("${index + 1}", 815f, curY + 15f, paint)
                canvas.drawText(truncate(rowData.dt, 18), 785f, curY + 15f, paint)

                paint.color = when (rowData.typeStr) {
                    "سند قبض" -> paidGreen
                    "سند صرف" -> debtRed
                    "شراء ساعات" -> 0xFF0288D1.toInt()
                    else -> primaryColor
                }
                canvas.drawText(rowData.typeStr, 685f, curY + 15f, paint)

                paint.color = darkTextColor
                canvas.drawText(truncate(rowData.party, 22), 605f, curY + 15f, paint)
                canvas.drawText(truncate(rowData.detail, 32), 450f, curY + 15f, paint)

                canvas.drawText(Formatters.formatNumber(rowData.opAmt), 255f, curY + 15f, paint)

                paint.color = paidGreen
                canvas.drawText(Formatters.formatNumber(rowData.opPaid), 170f, curY + 15f, paint)

                paint.color = if (rowData.opDebt > 0.0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(rowData.opDebt), 90f, curY + 15f, paint)

                curY += rowHeight
            }

            // Grand Total Line
            if (curY + rowHeight + 4f > pageHeight - 65f) {
                finishCurrentPage()
                startNewPage()
            }

            paint.color = 0xFFE0F2F1.toInt()
            canvas.drawRect(20f, curY, pageWidth - 20f, curY + rowHeight + 3f, paint)

            paint.color = darkTextColor
            paint.typeface = fontBold
            paint.textSize = 9f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("الإجمالي الكلي", 450f, curY + 16f, paint)
            canvas.drawText(Formatters.formatNumber(totalAmount), 255f, curY + 16f, paint)

            paint.color = paidGreen
            canvas.drawText(Formatters.formatNumber(totalPaid), 170f, curY + 16f, paint)

            paint.color = if (totalDebt > 0.0) debtRed else paidGreen
            canvas.drawText(Formatters.formatNumber(totalDebt), 90f, curY + 16f, paint)

            curY += rowHeight + 3f
        }

        finishCurrentPage()

        val reportDir = File(context.cacheDir, "reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val file = File(reportDir, "operations_log_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()

        return file
    }

    private data class OperationRowData(
        val dt: String,
        val typeStr: String,
        val party: String,
        val detail: String,
        val opAmt: Double,
        val opPaid: Double,
        val opDebt: Double
    )

    private fun truncate(value: String, maxChars: Int): String =
        if (value.length <= maxChars) value
        else if (maxChars <= 3) value.take(maxChars)
        else value.take(maxChars - 3) + "..."

    private fun drawSummaryBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        accentColor: Int,
        fontBold: Typeface? = null,
        fontRegular: Typeface? = null
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        val rect = RectF(x, y, x + width, y + height)

        paint.color = PdfThemeHelper.COLOR_CARD_BG
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Subtle border
        paint.style = Paint.Style.STROKE
        paint.color = PdfThemeHelper.COLOR_BORDER
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Top colored accent bar
        paint.color = accentColor
        canvas.drawRoundRect(RectF(x, y, x + width, y + 4f), 2f, 2f, paint)

        paint.color = PdfThemeHelper.COLOR_MUTED_TEXT
        paint.textSize = 9.5f
        paint.typeface = fontRegular ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, x + (width / 2f), y + 22f, paint)

        paint.color = accentColor
        paint.typeface = fontBold ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11.5f
        canvas.drawText(value, x + (width / 2f), y + 42f, paint)
    }

    /**
     * رسم الفوتر الرسمي الموحد لجميع التقارير والمستندات والطباعات
     */
    private fun drawDocumentFooter(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int? = null,
        fontRegular: Typeface? = null
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = fontRegular ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            color = PdfThemeHelper.COLOR_MUTED_TEXT
        }
        val pageSuffix = if (pageNumber != null) "  |  صفحة $pageNumber" else ""
        val text = "تطبيق المُسَرِّبْ  •  تطوير: م. عمر الحومي  •  واتساب: wa.me/967773712030$pageSuffix"
        canvas.drawText(text, pageWidth / 2f, pageHeight - 18f, paint)
    }
}
