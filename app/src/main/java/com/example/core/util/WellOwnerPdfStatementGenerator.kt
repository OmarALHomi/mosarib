package com.example.core.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.features.customers.Customer
import com.example.features.customers.CustomerWithBalance
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WellOwnerPdfStatementGenerator {

    fun generateStatementPdf(
        context: Context,
        owner: Customer,
        balanceInfo: CustomerWithBalance,
        purchases: List<WellOwnerPurchase>,
        vouchers: List<Voucher>,
        distributorName: String = "مُسَرِّب المياه",
        currencySymbol: String = "ر.ي"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            color = Color.rgb(0, 122, 135) // PrimaryTeal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.RIGHT
        }
        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.rgb(15, 23, 42)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Header
        canvas.drawText("كشف حساب ومطابقة مشتريات البئر", 595f / 2, 45f, titlePaint)
        canvas.drawText("الموزع: $distributorName • تاريخ الاستخراج: ${SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())}", 595f / 2, 65f, subPaint)

        // Owner Info Card Box
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(30f, 85f, 565f, 155f, 8f, 8f, paint)

        canvas.drawText("اسم صاحب البئر: ${owner.name}", 545f, 110f, boldTextPaint)
        canvas.drawText("رقم الهاتف: ${owner.phone.ifBlank { "غير مسجل" }}", 545f, 132f, textPaint)
        canvas.drawText("صافي المستحق له (عليك): ${Formatters.formatCurrency(balanceInfo.payableBalance, currencySymbol)}", 260f, 110f, boldTextPaint)
        canvas.drawText("إجمالي المشتريات: ${Formatters.formatCurrency(balanceInfo.totalPurchaseAmount, currencySymbol)}", 260f, 132f, textPaint)

        // Purchases Table Header
        var y = 185f
        paint.color = Color.rgb(0, 122, 135)
        canvas.drawRoundRect(30f, y, 565f, y + 26f, 6f, 6f, paint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("التاريخ", 550f, y + 17f, headerTextPaint)
        canvas.drawText("المدة الإجمالية", 450f, y + 17f, headerTextPaint)
        canvas.drawText("هدر البئر", 360f, y + 17f, headerTextPaint)
        canvas.drawText("الساعات الصافية", 270f, y + 17f, headerTextPaint)
        canvas.drawText("سعر الساعة", 170f, y + 17f, headerTextPaint)
        canvas.drawText("المبلغ المستحق ($currencySymbol)", 95f, y + 17f, headerTextPaint)

        y += 32f

        // Table Rows
        var isOdd = false
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

        purchases.take(15).forEach { p ->
            if (isOdd) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(30f, y - 12f, 565f, y + 14f, paint)
            }
            isOdd = !isOdd

            val dateTimestamp = if (p.startTime > 0) p.startTime else p.date
            val dateStr = dateFormat.format(Date(dateTimestamp))
            val grossHours = Formatters.formatMinutesToHoursMinutes(p.durationMinutes)
            val wasteHours = Formatters.formatMinutesToHoursMinutes(p.wastedMinutesOnOwner)
            val netMins = WellOwnerPurchaseMath.chargeableMinutes(p)
            val netHours = Formatters.formatMinutesToHoursMinutes(netMins)
            val rate = Formatters.formatNumber(p.purchaseRatePerHour)
            val amount = Formatters.formatNumber(WellOwnerPurchaseMath.payableAmount(p))

            canvas.drawText(dateStr, 550f, y, textPaint)
            canvas.drawText(grossHours, 450f, y, textPaint)
            canvas.drawText(wasteHours, 360f, y, textPaint)
            canvas.drawText(netHours, 270f, y, textPaint)
            canvas.drawText(rate, 170f, y, textPaint)
            canvas.drawText(amount, 95f, y, boldTextPaint)

            y += 22f
        }

        // Summary Line
        y += 10f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 20f

        val totalDisbursed = vouchers.filter { it.type == VoucherType.EXPENSE }.sumOf { it.amount }
        canvas.drawText("إجمالي المبالغ المسددة لصاحب البئر (سندات صرف): ${Formatters.formatCurrency(totalDisbursed, currencySymbol)}", 545f, y, textPaint)
        y += 20f
        canvas.drawText("الرصيد المتبقي له بذمة الموزع: ${Formatters.formatCurrency(balanceInfo.payableBalance, currencySymbol)}", 545f, y, boldTextPaint)

        pdfDoc.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "كشف_حساب_بئر_${owner.name.replace(" ", "_")}.pdf")
        FileOutputStream(file).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return file
    }
}
