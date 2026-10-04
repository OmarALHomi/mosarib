package com.baynana.features.statements

import com.baynana.domain.ledger.LineDirection
import com.baynana.domain.ledger.MemberStatement
import com.baynana.domain.ledger.StatementLine
import com.baynana.domain.ledger.StatementText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ملفّ المشاركة يجب أن يكون **صورة حرفية** من نصّ الشاشة: نفس السطور ونفس الأرقام بلا إعادة تنسيق.
 * هذه هي البوّابة التي تمنع أشهر زلّة في التقارير: شاشة تقول رقمًا وورقة تقول غيره.
 */
class StatementExportTest {

    private fun line(
        id: String,
        amountMinor: Long,
        remainingMinor: Long,
        description: String = "سقية 5 ساعات",
        direction: LineDirection = LineDirection.CHARGE,
        occurredAt: Long = 1_700_000_000_000
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
        runningNetMinor = remainingMinor
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

    @Test
    fun `الملف يبدأ بترويسة الهوية ويذكر الغرفة وصاحب الكشف`() {
        val export = StatementExport.build(
            statement = statement(listOf(line("e1", 300_000, 200_000))),
            roomTitle = "بيت الوالد",
            subject = "كشف: أحمد",
            exportedAt = 1_700_000_000_000,
            dateText = fixedDate
        )
        val lines = export.body.split(StatementExport.NEWLINE)
        assertEquals(StatementExport.HEADER, lines[0])
        assertTrue(lines[1].contains("كشف: أحمد"))
        assertTrue(lines[1].contains("بيت الوالد"))
    }

    @Test
    fun `الأرقام تُكتب بالريال وبفواصل الآلاف كما على الشاشة`() {
        val export = StatementExport.build(
            statement = statement(emptyList()),
            roomTitle = "بيت الوالد",
            subject = "أحمد",
            dateText = fixedDate
        )
        // 1,500,000 فلس = 15,000 ر.ي — الرقم يأتي من المحرّك، والكتابة من MoneyFormat وحده.
        assertTrue(
            "ترويسة الأرقام يجب أن تطابق نصّ المحرّك",
            export.body.contains("المبلغ: 15,000 ر.ي • المسدَّد: 5,000 ر.ي • الباقي: 10,000 ر.ي")
        )
        assertTrue(export.body.contains(StatementText.summary(statement(emptyList()))))
    }

    @Test
    fun `كل سطر في الملف هو نفسه سطر الشاشة`() {
        val statement = statement(listOf(line("e1", 300_000, 200_000)))
        val export = StatementExport.build(
            statement = statement,
            roomTitle = "بيت الوالد",
            subject = "أحمد",
            dateText = fixedDate
        )
        val expected = StatementText.line(statement.lines[0], statement.currency, fixedDate(0))
        assertTrue("السطر المتوقّع: $expected", export.body.contains(expected))
        assertTrue(export.body.contains("2026-10-03 • سقية 5 ساعات • 3,000 ر.ي • على العضو • المتبقي 2,000 ر.ي"))
    }

    @Test
    fun `الدفتر الفارغ يقول ذلك بصراحة ولا يطبع جدولًا فارغًا`() {
        val export = StatementExport.build(
            statement = statement(emptyList()),
            roomTitle = "غرفة جديدة",
            subject = "أحمد",
            dateText = fixedDate
        )
        assertTrue(export.body.contains("لا سطور بعد"))
        assertFalse(export.body.contains("المتبقي"))
    }

    @Test
    fun `الملف ينتهي بسطر يقول إنه محلّي ولم يُرسل`() {
        val export = StatementExport.build(
            statement = statement(listOf(line("e1", 300_000, 200_000))),
            roomTitle = "بيت الوالد",
            subject = "أحمد",
            dateText = fixedDate
        )
        val lines = export.body.trimEnd().split(StatementExport.NEWLINE)
        assertEquals(StatementExport.LOCAL_ONLY_FOOTER, lines.last())
        assertTrue(StatementExport.LOCAL_ONLY_FOOTER.contains("لم يُرسل"))
    }

    @Test
    fun `اسم الملف آمن للمشاركة ولا يحمل فواصل مسارات`() {
        val export = StatementExport.build(
            statement = statement(emptyList()),
            roomTitle = "بيت/الوالد: الشمال",
            subject = "أحمد",
            exportedAt = 1_700_000_000_000,
            dateText = fixedDate
        )
        assertEquals(".txt", export.fileName.takeLast(4))
        assertFalse(export.fileName.contains("/"))
        assertFalse(export.fileName.contains(":"))
        assertFalse(export.fileName.contains("\\"))
        assertTrue(export.fileName.contains("بيننا"))
    }

    @Test
    fun `العنوان الفارغ لا يُنتج اسم ملفّ فارغًا ولا عنوانًا مبتورًا`() {
        val export = StatementExport.build(
            statement = statement(emptyList()),
            roomTitle = "   ",
            subject = "",
            exportedAt = 1_700_000_000_000,
            dateText = fixedDate
        )
        assertTrue(export.body.contains("كشف حساب"))
        assertTrue(export.fileName.isNotBlank())
        assertTrue(export.fileName.length <= 70)
    }
}
