package com.baynana.features.statements

import com.baynana.domain.ledger.LineDirection
import com.baynana.domain.ledger.MemberStatement
import com.baynana.domain.ledger.StatementDocument
import com.baynana.domain.ledger.StatementDocumentBuilder
import com.baynana.domain.ledger.StatementLine
import com.baynana.domain.ledger.StatementText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ملفّ المشاركة يجب أن يكون **صورة حرفية** من مستند واحد: نفس سطور الشاشة ونفس الأرقام بلا إعادة
 * تنسيق. هذه هي البوّابة التي تمنع أشهر زلّة في التقارير: شاشة تقول رقمًا وورقة تقول غيره.
 */
class StatementExportTest {

    private fun line(
        id: String,
        amountMinor: Long,
        remainingMinor: Long,
        runningNetMinor: Long,
        description: String = "سقية 5 ساعات",
        direction: LineDirection = LineDirection.CHARGE,
        occurredAt: Long = 1_700_000_000_000L
    ) = StatementLine(
        entryId = id,
        occurredAt = occurredAt,
        type = "IRRIGATION",
        description = description,
        direction = direction,
        amountMinor = amountMinor,
        allocatedMinor = amountMinor - remainingMinor,
        remainingMinor = remainingMinor,
        status = "ACKNOWLEDGED",
        runningNetMinor = runningNetMinor
    )

    private fun statement(lines: List<StatementLine>) = MemberStatement(
        memberId = "me",
        currency = "YER_NEW",
        lines = lines,
        chargedMinor = 1_500_000,
        paidMinor = 500_000,
        remainingMinor = 1_000_000,
        unappliedMinor = 0,
        netMinor = 1_000_000
    )

    private val fixedDate: (Long) -> String = { "2026-10-03" }

    private fun build(lines: List<StatementLine>, roomTitle: String = "بيت الوالد", subject: String = "كشف: أحمد") =
        StatementExport.build(
            statement = statement(lines),
            roomTitle = roomTitle,
            subject = subject,
            appVersion = "1.0.0",
            exportedAt = 1_700_000_000_000L,
            dateText = fixedDate
        )

    @Test
    fun `الملف يبدأ بسطر الهوية ثم الكشف`() {
        val export = build(listOf(line("e1", 300_000, 200_000, -200_000)))
        val lines = export.body.trimEnd().split(StatementExport.NEWLINE)
        assertEquals("بيننا — إصدار 1.0.0 — مستودع حساباتك ومعاملاتك", lines[0])
        assertEquals("كشف: أحمد — بيت الوالد", lines[1])
        assertTrue(lines[2].startsWith("ريال يمني جديد (ر.ي)"))
    }

    @Test
    fun `الأرقام تُكتب بالريال وبفواصل الآلاف كما على الشاشة`() {
        val export = build(emptyList())
        assertTrue(
            "ترويسة الأرقام يجب أن تطابق نصّ المحرّك",
            export.body.contains("المبلغ: 15,000 ر.ي • المسدَّد: 5,000 ر.ي • الباقي: 10,000 ر.ي")
        )
        assertEquals(StatementText.summary(statement(emptyList())), export.document.summary)
    }

    @Test
    fun `كل سطر في الملف هو نفسه خلايا السطر في المستند`() {
        val statement = statement(listOf(line("e1", 300_000, 200_000, -200_000)))
        val export = build(statement.lines)
        val cells = StatementText.rowCells(statement.lines[0], statement.currency, fixedDate(0))
        assertEquals(
            listOf("2026-10-03", "سقية 5 ساعات", "3,000 ر.ي", "عليك 2,000 ر.ي"),
            cells
        )
        assertTrue(export.body.contains(cells.joinToString(StatementDocument.CELL_SEPARATOR)))
    }

    @Test
    fun `السداد يُكتب بعلامة ناقص ظاهرة لا باللون وحده`() {
        val export = build(
            listOf(
                line("d1", 500_000, 0, -500_000),
                line("p1", 200_000, 0, -300_000, description = "سداد نقدي", direction = LineDirection.PAYMENT)
            )
        )
        assertTrue(export.body.contains("− 2,000 ر.ي"))
    }

    @Test
    fun `الدفتر الفارغ يقول ذلك بصراحة ولا يطبع جدولًا فارغًا`() {
        val export = build(emptyList())
        assertTrue(export.body.contains(StatementDocumentBuilder.EMPTY_NOTE))
        assertFalse(export.body.contains(StatementText.COLUMN_TITLES.first() + StatementDocument.CELL_SEPARATOR))
    }

    @Test
    fun `الملف ينتهي بسطر يقول إنه محلّي ولم يُرسل`() {
        val export = build(listOf(line("e1", 300_000, 200_000, -200_000)))
        val lines = export.body.trimEnd().split(StatementExport.NEWLINE)
        assertEquals(StatementDocumentBuilder.FOOTNOTE, lines.last())
        assertTrue(StatementDocumentBuilder.FOOTNOTE.contains("لم يُرسل"))
    }

    @Test
    fun `اسم الملف آمن للمشاركة ولا يحمل فواصل مسارات`() {
        val export = build(emptyList(), roomTitle = "بيت/الوالد: الشمال")
        assertEquals(".txt", export.fileName.takeLast(4))
        assertFalse(export.fileName.contains("/"))
        assertFalse(export.fileName.contains(":"))
        assertFalse(export.fileName.contains("\\"))
        assertTrue(export.fileName.contains("بيننا"))
    }

    @Test
    fun `العنوان الفارغ لا يُنتج اسم ملفّ فارغًا ولا عنوانًا مبتورًا`() {
        val export = build(emptyList(), roomTitle = "   ", subject = "")
        assertTrue(export.body.contains(StatementDocumentBuilder.DEFAULT_SUBJECT))
        assertTrue(export.fileName.isNotBlank())
        assertTrue(export.fileName.length <= 70)
    }

    @Test
    fun `الرصيد الدائن المعلّق يُذكر نصًّا في المستند`() {
        val withCredit = MemberStatement(
            memberId = "me",
            currency = "YER_NEW",
            lines = emptyList(),
            chargedMinor = 0,
            paidMinor = 0,
            remainingMinor = 0,
            unappliedMinor = 700_000,
            netMinor = 700_000
        )
        val export = StatementExport.build(
            statement = withCredit,
            roomTitle = "بيت الوالد",
            subject = "أحمد",
            appVersion = "1.0.0",
            dateText = fixedDate
        )
        assertTrue(export.body.contains("رصيد دائن معلّق: 7,000 ر.ي"))
        assertTrue(export.body.contains("لم يُخصَّص"))
    }
}
