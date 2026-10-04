package com.baynana.core.util

import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.LineDirection
import com.baynana.domain.ledger.MemberStatement
import com.baynana.domain.ledger.StatementDocument
import com.baynana.domain.ledger.StatementDocumentBuilder
import com.baynana.domain.ledger.StatementLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * البوّابة الذهبية للورق: ما يُرسم في PDF هو **نفس** سطور المستند بالترتيب، لا صياغة ثانية.
 *
 * ويُقاس ذلك بمُسجِّل نصوص يحلّ محلّ الورق، فلا نحتاج جهازًا ولا ملفًّا لنتأكد أن الورقة والشاشة
 * تقولان الشيء نفسه. والورق هنا كامل: من سطر الهوية إلى سطر «لم يُرسل إلى أي خادم».
 */
class StatementPdfWriterTest {

    private class RecordingSink(
        override val pageWidth: Float = 595f,
        override val pageHeight: Float = 842f,
        private val linesPerPage: Int = Int.MAX_VALUE
    ) : PdfTextSink {
        val pages = mutableListOf<MutableList<String>>()
        var current: MutableList<String>? = null

        override fun newPage() {
            current = mutableListOf()
            pages += current!!
        }

        override fun measure(text: String, textSize: Float, bold: Boolean): Float = text.length * textSize * 0.5f

        override fun drawText(text: String, yFromTop: Float, textSize: Float, bold: Boolean, rightAligned: Boolean) {
            val page = current ?: return
            if (page.size >= linesPerPage) {
                newPage()
                current!!.add(text)
            } else {
                page.add(text)
            }
        }

        fun drawn(): List<String> = pages.flatten()
    }

    private val date: (Long) -> String = { "2026-10-03" }

    private fun statement(lines: List<StatementLine>, currency: String = "YER_NEW") = MemberStatement(
        memberId = "me",
        currency = currency,
        lines = lines,
        chargedMinor = lines.filter { it.direction == LineDirection.CHARGE }.sumOf { it.amountMinor },
        paidMinor = lines.filter { it.direction == LineDirection.PAYMENT }.sumOf { it.amountMinor },
        remainingMinor = 2_500_000,
        unappliedMinor = 0,
        netMinor = 2_500_000
    )

    private fun document(lines: List<StatementLine> = emptyList()): StatementDocument =
        StatementDocumentBuilder.build(
            statement = statement(lines),
            roomTitle = "بيت الوالد",
            subject = "كشف: أحمد",
            appVersion = "1.0.0",
            fileNameStamp = "2026-10-03",
            dateText = date
        )

    private val sampleLines = listOf(
        StatementLine(
            entryId = "e1",
            occurredAt = 1_700_000_000_000L,
            type = EntryType.WATER_SESSION,
            description = "سقية 5 ساعات",
            direction = LineDirection.CHARGE,
            amountMinor = 5_000_000,
            allocatedMinor = 2_500_000,
            remainingMinor = 2_500_000,
            status = EntryStatus.ACKNOWLEDGED,
            runningNetMinor = -5_000_000
        ),
        StatementLine(
            entryId = "p1",
            occurredAt = 1_700_000_000_000L,
            type = EntryType.PAYMENT,
            description = "سداد نقدي",
            direction = LineDirection.PAYMENT,
            amountMinor = 2_500_000,
            allocatedMinor = 2_500_000,
            remainingMinor = 0,
            status = EntryStatus.ACKNOWLEDGED,
            runningNetMinor = -2_500_000
        )
    )

    @Test
    fun `ما يُرسم هو سطور المستند نفسها بالترتيب`() {
        val sink = RecordingSink()
        val document = document(sampleLines)
        StatementPdfWriter(sink).write(document)

        assertEquals(document.flatLines, sink.drawn())

        // والكاتب يسجّل ما رسمه بنفسه أيضًا، فلا نعتمد على السجل الخارجي وحده.
        val recording = RecordingSink()
        val writer = StatementPdfWriter(recording)
        writer.write(document)
        assertEquals(document.flatLines, writer.drawnLines)
    }

    @Test
    fun `أول صفحة تبدأ بسطر الهوية وسطر البيان`() {
        val sink = RecordingSink()
        val document = document(sampleLines)
        StatementPdfWriter(sink).write(document)
        val firstPage = sink.pages.first()
        assertTrue(firstPage[0].startsWith("بيننا — إصدار"))
        assertTrue(firstPage[1].startsWith("كشف: أحمد"))
    }

    @Test
    fun `الورق ينتهي بسطر «لم يُرسل إلى أي خادم»`() {
        val sink = RecordingSink()
        val document = document(sampleLines)
        StatementPdfWriter(sink).write(document)
        assertTrue(sink.drawn().last().contains("لم يُرسل"))
    }

    @Test
    fun `الدفتر الفارغ يُطبع بورقة مفهومة لا جدولًا فارغًا`() {
        val sink = RecordingSink()
        StatementPdfWriter(sink).write(document())
        assertTrue(sink.drawn().any { it.contains("لا سطور بعد") })
    }

    @Test
    fun `العديد من السطور يُوزَّع على صفحات ولا يُفقد سطر`() {
        val sink = RecordingSink(pageHeight = 200f)
        val many = (1..40).map { index ->
            StatementLine(
                entryId = "e$index",
                occurredAt = 1_700_000_000_000L,
                type = EntryType.WATER_SESSION,
                description = "سقية رقم $index",
                direction = LineDirection.CHARGE,
                amountMinor = 100_000L * index,
                allocatedMinor = 0,
                remainingMinor = 100_000L * index,
                status = EntryStatus.SENT,
                runningNetMinor = -100_000L * index
            )
        }
        val document = document(many)
        val writer = StatementPdfWriter(sink)
        writer.write(document)

        assertEquals(document.flatLines, sink.drawn())
        assertTrue("يجب أن تتعدّد الصفحات", writer.pageCount > 1)
        assertEquals(writer.pageCount, sink.pages.size)
        assertTrue(sink.drawn().any { it.contains("سقية رقم 40") })
    }

    @Test
    fun `لا رقم يُحسب في الورق — الأرقام من المستند`() {
        val sink = RecordingSink()
        val document = document(sampleLines)
        StatementPdfWriter(sink).write(document)
        assertTrue(sink.drawn().any { it.contains("2026-10-03 • سقية 5 ساعات • 5,000 ر.ي • عليك 5,000 ر.ي") })
        assertTrue(sink.drawn().any { it.contains("− 2,500 ر.ي") })
        assertTrue(sink.drawn().any { it.contains("المبلغ: 5,000 ر.ي") })
    }
}
