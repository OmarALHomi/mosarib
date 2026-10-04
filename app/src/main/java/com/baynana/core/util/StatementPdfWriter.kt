package com.baynana.core.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.baynana.domain.ledger.StatementDocument
import java.io.File
import java.io.FileOutputStream

/**
 * سطح الرسم: كل ما يحتاجه كاتب الكشف من «ورقة».
 *
 * الفائدة: منطق التوزيع على الصفحات وسطور النصّ يعيش في [StatementPdfWriter] النقيّ، وAndroid
 * يبقى في [AndroidPdfTextSink] وحده.
 *
 * تنبيه مقصود: الكسور العشرية (Float) هنا **قياس طباعة** لا مال: المال في هذا التطبيق بالفلس وحده
 * (ADR-04)، وحاجز CI يمنع الكسور في مسارات المال لا في إحداثيات الورق. فيصير بالإمكان اختبار أن **نصّ الورق هو نصّ المستند حرفيًّا**
 * في اختبار وحدة عادي بلا جهاز ولا ملفّ PDF.
 */
interface PdfTextSink {
    val pageWidth: Float
    val pageHeight: Float

    /** صفحة جديدة. تُنادى أولًا قبل أي رسم. */
    fun newPage()

    /** عرض النصّ بالنقاط، ليُحاذى يمينًا بلا قطع. */
    fun measure(text: String, textSize: Float, bold: Boolean): Float

    /**
     * رسم سطر. `yFromTop` من أعلى الصفحة (أسهل للقراءة في اختبار الورق)، و`rightAligned`
     * لأن العربية تُحاذى يمينًا.
     */
    fun drawText(
        text: String,
        yFromTop: Float,
        textSize: Float,
        bold: Boolean,
        rightAligned: Boolean
    )
}

/**
 * كاتب كشف الطرف على الورق: يأخذ [StatementDocument] ويرسم [StatementDocument.flatLines] واحدًا
 * واحدًا بترتيبه، بلا إعادة صياغة وبلا رقم يُحسب هنا.
 *
 * **شرط البوابة الذهبية:** ما يُرسم هو ما في المستند حرفيًّا، وما في المستند هو ما على الشاشة.
 * فالاختبار لا يقارن «شكلًا» بل نصوصًا: نفس السطور، نفس الترتيب، نفس الأرقام.
 */
class StatementPdfWriter(private val sink: PdfTextSink) {

    /** ما رُسم فعلًا بالترتيب — يُستعمل في الاختبار، وهو الدليل لا التخمين. */
    val drawnLines = mutableListOf<String>()

    var pageCount: Int = 0
        private set

    fun write(document: StatementDocument) {
        sink.newPage()
        pageCount = 1
        var y = TOP_MARGIN

        document.flatLines.forEachIndexed { index, line ->
            val isHeading = index <= HEADING_LINES
            val size = if (isHeading) HEADING_SIZE else BODY_SIZE
            val step = if (isHeading) HEADING_STEP else BODY_STEP
            if (y + step > sink.pageHeight - BOTTOM_MARGIN) {
                sink.newPage()
                pageCount += 1
                y = TOP_MARGIN
            }
            sink.drawText(
                text = line,
                yFromTop = y,
                textSize = size,
                bold = isHeading,
                rightAligned = true
            )
            drawnLines += line
            y += step
        }
    }

    private companion object {
        const val TOP_MARGIN = 60f
        const val BOTTOM_MARGIN = 60f
        const val HEADING_SIZE = 12f
        const val BODY_SIZE = 10f
        const val HEADING_STEP = 22f
        const val BODY_STEP = 18f

        /** أول أربعة سطور (الهوية والعنوان والعملة والملخّص) تُرسم بحجم عنوان. */
        const val HEADING_LINES = 3
    }
}

/**
 * المحوّل الحقيقي إلى PDF بقياس A4 بالنقاط (595×842)، والعربية تُحاذى يمينًا كما تُقرأ.
 *
 * لا يقرّر شيئًا: يرسم ما يُطلب منه فقط، فكل كلمة في الورق أصلها من المستند.
 */
class AndroidPdfTextSink(private val document: PdfDocument) : PdfTextSink {

    override val pageWidth: Float = PAGE_WIDTH
    override val pageHeight: Float = PAGE_HEIGHT

    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun newPage() {
        finishPage()
        pageNumber += 1
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create()
        page = document.startPage(info)
    }

    override fun measure(text: String, textSize: Float, bold: Boolean): Float {
        paint.textSize = textSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        return paint.measureText(text)
    }

    override fun drawText(text: String, yFromTop: Float, textSize: Float, bold: Boolean, rightAligned: Boolean) {
        val canvas: Canvas = page?.canvas ?: return
        paint.textSize = textSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        paint.color = if (bold) INK else INK_MUTED
        paint.textAlign = if (rightAligned) Paint.Align.RIGHT else Paint.Align.LEFT
        val x = if (rightAligned) PAGE_WIDTH - SIDE_MARGIN else SIDE_MARGIN
        canvas.drawText(text, x, yFromTop + textSize, paint)
    }

    /** يُنادى من الكاتب بعد آخر سطر. */
    fun finishPage() {
        page?.let { document.finishPage(it) }
        page = null
    }

    private companion object {
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val SIDE_MARGIN = 40f
        val INK = 0xFF14202E.toInt()
        val INK_MUTED = 0xFF57616C.toInt()
    }
}

/**
 * يكتب مستند الكشف في ملفّ PDF داخل مجلّد التقارير المسموح في `file_paths.xml`، ثم يشاركه.
 * المجلّد هو نفسه الذي سمح به `FileProvider` منذ البداية: لا توسيع للصلاحيات.
 */
object StatementPdfFiles {

    const val REPORTS_DIR = "reports"

    fun fileFor(context: android.content.Context, document: StatementDocument): File {
        val dir = File(context.cacheDir, REPORTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, document.fileName + ".pdf")
    }

    fun write(context: android.content.Context, document: StatementDocument): File {
        val pdf = PdfDocument()
        val sink = AndroidPdfTextSink(pdf)
        StatementPdfWriter(sink).write(document)
        sink.finishPage()
        val file = fileFor(context, document)
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    /** يكتب الملفّ ثم يشاركه عبر `FileSharingHelper` (سلوك المشاركة الموجود في التطبيق). */
    fun share(context: android.content.Context, document: StatementDocument): File {
        val file = write(context, document)
        FileSharingHelper.sharePdf(context, file, title = document.title)
        return file
    }
}
