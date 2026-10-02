package com.baynana.core.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.baynana.features.customers.Customer
import com.baynana.features.sessions.WaterSession
import com.baynana.features.settings.AppConfig
import com.baynana.features.vouchers.Voucher
import com.baynana.features.vouchers.VoucherType
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
    val netProfit: Double
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
    val balance: Double
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
        vouchers: List<Voucher>
    ): File {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard point width
        val pageHeight = 842 // A4 standard point height
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val primaryColor = 0xFF007A87.toInt()
        val darkTextColor = 0xFF132228.toInt()
        val lightGray = 0xFFF0F4F7.toInt()
        val dividerGray = 0xFFD2DFE5.toInt()
        val debtRed = 0xFFD32F2F.toInt()
        val paidGreen = 0xFF2E7D32.toInt()

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header Background Banner
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("كشف حساب عميل - توزيع مياه", pageWidth / 2f, 40f, paint)

        // Distributor Name & Phone
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${config.distributorName}  |  هاتف: ${config.distributorPhone}", pageWidth / 2f, 65f, paint)

        // Customer Info Card
        val cardRect = RectF(25f, 110f, pageWidth - 25f, 195f)
        paint.color = lightGray
        canvas.drawRoundRect(cardRect, 8f, 8f, paint)

        // Customer details text (RTL align right)
        paint.color = darkTextColor
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("العميل: ${customer.name}", pageWidth - 45f, 135f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        canvas.drawText("الهاتف: ${customer.phone.ifEmpty { "غير محدد" }}", pageWidth - 45f, 155f, paint)
        canvas.drawText("المزرعة / الموقع: ${customer.farmName.ifEmpty { customer.location.ifEmpty { "عام" } }}", pageWidth - 45f, 175f, paint)

        // Date of statement on left
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("تاريخ التقرير: ${Formatters.formatDate(System.currentTimeMillis())}", 45f, 135f, paint)
        canvas.drawText("العملة: ${config.currencySymbol}", 45f, 155f, paint)

        // Financial Summary Badges
        val totalWaterCost = sessions.sumOf { it.totalAmount }
        val totalSessionPaid = sessions.sumOf { it.amountPaid }
        val totalReceiptVouchers = vouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount }
        val totalDiscount = vouchers.filter { it.type == VoucherType.DISCOUNT }.sumOf { it.amount }
        val totalPaidOverall = totalSessionPaid + totalReceiptVouchers + totalDiscount
        val finalBalance = totalWaterCost - totalPaidOverall
        val totalMinutes = sessions.sumOf { it.durationMinutes }

        // Draw 3 Summary Boxes
        val boxWidth = (pageWidth - 70f) / 3f
        val boxY = 210f
        val boxH = 55f

        // Box 1: Total Water Duration & Cost
        drawSummaryBox(canvas, 25f, boxY, boxWidth, boxH, "إجمالي مبيعات الماء", "${Formatters.formatCurrency(totalWaterCost, config.currencySymbol)} (${Formatters.formatDurationArabic(totalMinutes)})", primaryColor)

        // Box 2: Total Paid
        drawSummaryBox(canvas, 25f + boxWidth + 10f, boxY, boxWidth, boxH, "إجمالي المسدد", Formatters.formatCurrency(totalPaidOverall, config.currencySymbol), paidGreen)

        // Box 3: Net Balance
        val balanceLabel = if (finalBalance > 0) "المبلغ المتبقي بذمته" else if (finalBalance < 0) "رصيد دائن للعميل" else "الحساب خالص"
        val balanceColor = if (finalBalance > 0) debtRed else paidGreen
        drawSummaryBox(canvas, 25f + (boxWidth + 10f) * 2, boxY, boxWidth, boxH, balanceLabel, Formatters.formatCurrency(Math.abs(finalBalance), config.currencySymbol), balanceColor)

        // Statement Entries Table
        var curY = 290f
        paint.color = primaryColor
        canvas.drawRect(25f, curY, pageWidth - 25f, curY + 25f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        // Headers: التاريخ | البيان / تفاصيل الري | المدة | السعر/ساعة | المبلغ | المسدد | المتبقي
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("التاريخ", pageWidth - 35f, curY + 16f, paint)
        canvas.drawText("البيان والنشاط", pageWidth - 105f, curY + 16f, paint)
        canvas.drawText("المدة والسعر", pageWidth - 260f, curY + 16f, paint)
        canvas.drawText("المبلغ المطلوب", pageWidth - 370f, curY + 16f, paint)
        canvas.drawText("المدفوع", pageWidth - 450f, curY + 16f, paint)
        canvas.drawText("الرصيد المتبقي", 40f + 50f, curY + 16f, paint)

        curY += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Combine and sort events
        data class StatementRow(
            val date: Long,
            val title: String,
            val subtitle: String,
            val amount: Double,
            val paid: Double,
            val isSession: Boolean
        )

        val rows = mutableListOf<StatementRow>()
        sessions.forEach { s ->
            rows.add(
                StatementRow(
                    date = s.startTime,
                    title = "ساقية ماء",
                    subtitle = "${Formatters.formatDurationArabic(s.durationMinutes)} @ ${Formatters.formatNumber(s.pricePerHour)}",
                    amount = s.totalAmount,
                    paid = s.amountPaid,
                    isSession = true
                )
            )
        }
        vouchers.forEach { v ->
            val label = when (v.type) {
                VoucherType.RECEIPT -> "سند قبض نقدي (${v.voucherNumber})"
                VoucherType.DISCOUNT -> "سند تسوية / خصم"
                VoucherType.EXPENSE -> "سند صرف"
            }
            rows.add(
                StatementRow(
                    date = v.date,
                    title = label,
                    subtitle = v.notes.ifEmpty { v.paymentMethod },
                    amount = 0.0,
                    paid = v.amount,
                    isSession = false
                )
            )
        }
        rows.sortBy { it.date }

        var runningDebt = 0.0

        rows.take(22).forEachIndexed { index, row ->
            if (row.isSession) {
                runningDebt += (row.amount - row.paid)
            } else {
                runningDebt -= row.paid
            }

            paint.color = if (index % 2 == 0) Color.WHITE else lightGray
            canvas.drawRect(25f, curY, pageWidth - 25f, curY + 22f, paint)

            paint.color = dividerGray
            paint.strokeWidth = 0.5f
            canvas.drawLine(25f, curY + 22f, pageWidth - 25f, curY + 22f, paint)

            paint.color = darkTextColor
            paint.textSize = 9.5f
            paint.textAlign = Paint.Align.RIGHT

            canvas.drawText(Formatters.formatDate(row.date), pageWidth - 35f, curY + 15f, paint)
            canvas.drawText(row.title, pageWidth - 105f, curY + 15f, paint)
            canvas.drawText(row.subtitle, pageWidth - 260f, curY + 15f, paint)
            canvas.drawText(if (row.amount > 0) Formatters.formatNumber(row.amount) else "-", pageWidth - 370f, curY + 15f, paint)

            paint.color = paidGreen
            canvas.drawText(if (row.paid > 0) Formatters.formatNumber(row.paid) else "-", pageWidth - 450f, curY + 15f, paint)

            paint.color = if (runningDebt > 0) debtRed else paidGreen
            canvas.drawText(Formatters.formatNumber(runningDebt), 40f + 50f, curY + 15f, paint)

            curY += 22f
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
        val pageWidth = 595
        val pageHeight = 650
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val primaryColor = 0xFF007A87.toInt()
        val darkTextColor = 0xFF132228.toInt()
        val lightGray = 0xFFF5F9FA.toInt()
        val dividerGray = 0xFFD2DFE5.toInt()
        val debtRed = 0xFFD32F2F.toInt()
        val paidGreen = 0xFF2E7D32.toInt()

        val paint = Paint().apply { isAntiAlias = true }

        // Header
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("فاتورة توزيع وري مياه", pageWidth / 2f, 42f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${config.distributorName} - جوال: ${config.distributorPhone}", pageWidth / 2f, 68f, paint)

        // Invoice Number & Date
        paint.color = darkTextColor
        paint.textSize = 12f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("رقم الفاتورة: #${session.id}", pageWidth - 35f, 120f, paint)
        canvas.drawText("تاريخ الري: ${Formatters.formatDateTime(session.startTime)}", pageWidth - 35f, 140f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("حالة السداد: ${if (session.remainingDebt <= 0) "مسدد بالكامل" else "متبقي آجل"}", 35f, 130f, paint)

        // Customer details block
        val rectCustomer = RectF(30f, 160f, pageWidth - 30f, 225f)
        paint.color = lightGray
        canvas.drawRoundRect(rectCustomer, 8f, 8f, paint)

        paint.color = darkTextColor
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("العميل: ${customer.name}", pageWidth - 45f, 185f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        canvas.drawText("المزرعة: ${customer.farmName.ifEmpty { "غير محدد" }}  |  الهاتف: ${customer.phone}", pageWidth - 45f, 207f, paint)

        // Session Specs Table
        var y = 250f
        paint.color = primaryColor
        canvas.drawRect(30f, y, pageWidth - 30f, y + 25f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("وقت البدء والانتهاء", pageWidth - 45f, y + 17f, paint)
        canvas.drawText("المدة الإجمالية", pageWidth - 200f, y + 17f, paint)
        canvas.drawText("سعر الساعة", pageWidth - 340f, y + 17f, paint)
        canvas.drawText("المبلغ الإجمالي", 50f + 60f, y + 17f, paint)

        y += 25f
        paint.color = Color.WHITE
        canvas.drawRect(30f, y, pageWidth - 30f, y + 35f, paint)
        paint.color = dividerGray
        paint.strokeWidth = 1f
        canvas.drawLine(30f, y + 35f, pageWidth - 30f, y + 35f, paint)

        paint.color = darkTextColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        paint.textAlign = Paint.Align.RIGHT

        val timeRange = "${Formatters.formatTime(session.startTime)} - ${Formatters.formatTime(session.endTime)}"
        canvas.drawText(timeRange, pageWidth - 45f, y + 22f, paint)
        canvas.drawText(Formatters.formatDurationArabic(session.durationMinutes), pageWidth - 200f, y + 22f, paint)
        canvas.drawText(Formatters.formatCurrency(session.pricePerHour, config.currencySymbol), pageWidth - 340f, y + 22f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Formatters.formatCurrency(session.totalAmount, config.currencySymbol), 50f + 60f, y + 22f, paint)

        // Financial summary totals
        y += 55f
        val sumRect = RectF(pageWidth - 260f, y, pageWidth - 30f, y + 110f)
        paint.color = lightGray
        canvas.drawRoundRect(sumRect, 8f, 8f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = darkTextColor
        paint.textSize = 11f
        canvas.drawText("إجمالي قيمة الري:", pageWidth - 45f, y + 28f, paint)
        canvas.drawText(Formatters.formatCurrency(session.totalAmount, config.currencySymbol), pageWidth - 160f, y + 28f, paint)

        paint.color = paidGreen
        canvas.drawText("المبلغ المسدد مقدماً:", pageWidth - 45f, y + 58f, paint)
        canvas.drawText(Formatters.formatCurrency(session.amountPaid, config.currencySymbol), pageWidth - 160f, y + 58f, paint)

        paint.color = if (session.remainingDebt > 0) debtRed else paidGreen
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("المتبقي بذمة العميل:", pageWidth - 45f, y + 88f, paint)
        canvas.drawText(Formatters.formatCurrency(session.remainingDebt, config.currencySymbol), pageWidth - 160f, y + 88f, paint)

        // Notes box
        if (session.notes.isNotEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = darkTextColor
            paint.textSize = 10f
            canvas.drawText("ملاحظات: ${session.notes}", 40f + 180f, y + 40f, paint)
        }

        // Footer
        paint.color = dividerGray
        canvas.drawLine(30f, pageHeight - 70f, pageWidth - 30f, pageHeight - 70f, paint)

        paint.color = darkTextColor
        paint.textSize = 10f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("توقيع المستلم: ....................", pageWidth - 50f, pageHeight - 45f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("توقيع المسرب: ....................", 50f, pageHeight - 45f, paint)

        drawDocumentFooter(canvas, pageWidth, pageHeight)

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
        canvas.drawText("سند قبض وتحصيل نقدي", pageWidth / 2f, 38f, paint)

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
        canvas.drawText("نوع المعاملة: سداد حساب ري", 35f, 135f, paint)

        // Customer details block
        val rectCustomer = RectF(30f, 155f, pageWidth - 30f, 215f)
        paint.color = lightGray
        canvas.drawRoundRect(rectCustomer, 8f, 8f, paint)

        paint.color = darkTextColor
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("وصلنا من العميل: ${customer.name}", pageWidth - 45f, 180f, paint)
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
        canvas.drawText("سند صرف نقدي", pageWidth / 2f, 38f, paint)

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
        canvas.drawText("يُصرف للمستفيد / السيد: $beneficiaryName", pageWidth - 45f, 180f, paint)
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

        val paint = Paint().apply { isAntiAlias = true }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas

        fun drawHeader(subtitle: String) {
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

            paint.color = Color.WHITE
            paint.textSize = 19f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("التقرير المحاسبي الشامل - توزيع المياه", pageWidth / 2f, 40f, paint)

            paint.textSize = 11.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(subtitle, pageWidth / 2f, 66f, paint)
        }

        val rowHeight = 22f
        var curY = 320f

        fun drawTableHeader() {
            paint.color = primaryColor
            canvas.drawRect(20f, curY, pageWidth - 20f, curY + 25f, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("العميل", pageWidth - 30f, curY + 16f, paint)
            canvas.drawText("المزرعة / الموقع", pageWidth - 150f, curY + 16f, paint)
            canvas.drawText("السقايات / الدقائق", pageWidth - 265f, curY + 16f, paint)
            canvas.drawText("المبلغ المطلوب", pageWidth - 375f, curY + 16f, paint)
            canvas.drawText("المدفوع", pageWidth - 455f, curY + 16f, paint)
            canvas.drawText("المتبقي", 30f, curY + 16f, paint)

            curY += 25f
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
            drawHeader("${config.distributorName} - متابعة كشف العملاء")
            curY = 130f
            drawTableHeader()
        }

        // ---------- Page 1: distributor-wide summary ----------
        drawHeader("${config.distributorName}  |  هاتف: ${config.distributorPhone.ifEmpty { "غير محدد" }}")

        paint.color = darkTextColor
        paint.textSize = 11f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("الفترة: $periodTitle", pageWidth - 40f, 122f, paint)
        canvas.drawText("تاريخ الإصدار: ${Formatters.formatDate(System.currentTimeMillis())}", pageWidth - 40f, 142f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("عدد العملاء: ${customerRows.size}", 40f, 122f, paint)
        canvas.drawText("العملة: ${config.currencySymbol}", 40f, 142f, paint)

        val boxWidth = (pageWidth - 70f) / 3f
        val boxHeight = 55f

        drawSummaryBox(canvas, 25f, 170f, boxWidth, boxHeight, "إجمالي مبيعات الماء", Formatters.formatCurrency(totals.totalRevenue, config.currencySymbol), primaryColor)
        drawSummaryBox(canvas, 25f + boxWidth + 10f, 170f, boxWidth, boxHeight, "إجمالي ما تم تحصيله", Formatters.formatCurrency(totals.totalCollected, config.currencySymbol), paidGreen)
        drawSummaryBox(canvas, 25f + (boxWidth + 10f) * 2, 170f, boxWidth, boxHeight, "إجمالي الديون المتبقية", Formatters.formatCurrency(totals.totalOutstandingDebt, config.currencySymbol), debtRed)

        drawSummaryBox(canvas, 25f, 232f, boxWidth, boxHeight, "إجمالي المصروفات", Formatters.formatCurrency(totals.totalExpenses, config.currencySymbol), 0xFFE65100.toInt())
        drawSummaryBox(canvas, 25f + boxWidth + 10f, 232f, boxWidth, boxHeight, "صافي الربح التشغيلي", Formatters.formatCurrency(totals.netProfit, config.currencySymbol), if (totals.netProfit >= 0) paidGreen else debtRed)
        drawSummaryBox(canvas, 25f + (boxWidth + 10f) * 2, 232f, boxWidth, boxHeight, "إجمالي الري", "${Formatters.formatDurationArabic(totals.waterMinutes)} (${totals.sessionsCount} ساقية)", darkTextColor)

        // ---------- Customers table (paginated) ----------
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
                canvas.drawText(truncate(row.customerName, 20), pageWidth - 30f, curY + 15f, paint)
                canvas.drawText(truncate(row.farmName.ifEmpty { row.phone.ifEmpty { "-" } }, 16), pageWidth - 150f, curY + 15f, paint)
                canvas.drawText("${row.sessionsCount} (${row.waterMinutes} د)", pageWidth - 265f, curY + 15f, paint)
                canvas.drawText(Formatters.formatNumber(row.billed), pageWidth - 375f, curY + 15f, paint)

                paint.color = paidGreen
                canvas.drawText(Formatters.formatNumber(row.paid), pageWidth - 455f, curY + 15f, paint)

                paint.color = if (row.balance > 0.0) debtRed else paidGreen
                canvas.drawText(Formatters.formatNumber(row.balance), 30f, curY + 15f, paint)

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
            canvas.drawText("الإجمالي العام", pageWidth - 30f, curY + 16f, paint)
            canvas.drawText(Formatters.formatNumber(totals.totalRevenue), pageWidth - 375f, curY + 16f, paint)

            paint.color = paidGreen
            canvas.drawText(Formatters.formatNumber(totals.totalCollected), pageWidth - 455f, curY + 16f, paint)

            paint.color = if (totals.totalOutstandingDebt > 0.0) debtRed else paidGreen
            canvas.drawText(Formatters.formatNumber(totals.totalOutstandingDebt), 30f, curY + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            curY += rowHeight + 3f
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

    private fun truncate(value: String, maxChars: Int): String =
        if (value.length <= maxChars) value else value.take(maxChars - 3) + "..."

    private fun drawSummaryBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        accentColor: Int
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        val rect = RectF(x, y, x + width, y + height)

        paint.color = 0xFFF7FAFC.toInt()
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        // Top colored accent bar
        paint.color = accentColor
        canvas.drawRoundRect(RectF(x, y, x + width, y + 4f), 2f, 2f, paint)

        paint.color = 0xFF546E7A.toInt()
        paint.textSize = 9.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, x + (width / 2f), y + 22f, paint)

        paint.color = accentColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
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
        pageNumber: Int? = null
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            color = 0xFF64748B.toInt()
        }
        val pageSuffix = if (pageNumber != null) "  |  صفحة $pageNumber" else ""
        val text = "تطبيق المُسَرِّبْ  •  تطوير: م. عمر الحومي  •  واتساب: wa.me/967773712030$pageSuffix"
        canvas.drawText(text, pageWidth / 2f, pageHeight - 18f, paint)
    }
}
