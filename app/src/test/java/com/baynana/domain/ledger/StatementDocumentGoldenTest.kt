package com.baynana.domain.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **البوّابة الذهبية** لعشرة سيناريوهات: كل سطر متوقّع مكتوب صراحةً في الاختبار، لا مُحصَّل من
 * الدالة نفسها. فإذا تغيّر أي تنسيق — عملة، أو علامة ناقص، أو كلمة «عليك» — يفشل الاختبار ويُطلب
 * قرارًا واعيًا بدل أن تتغيّر الورقة صامتةً.
 *
 * وكل خلية هنا هي نفسها التي ترسمها الشاشة (`StatementSheet`) ونفسها التي يرسمها PDF، لأن
 * الثلاثة يقرؤون [StatementDocument] واحدًا.
 */
class StatementDocumentGoldenTest {

    private val date: (Long) -> String = { "2026-10-03" }

    private fun line(
        id: String,
        type: String = EntryType.WATER_SESSION,
        description: String = "سقية 5 ساعات",
        direction: String = LineDirection.CHARGE,
        amountMinor: Long,
        allocatedMinor: Long = 0,
        remainingMinor: Long = amountMinor,
        status: String = EntryStatus.ACKNOWLEDGED,
        runningNetMinor: Long
    ) = StatementLine(
        entryId = id,
        occurredAt = 1_700_000_000_000L,
        type = type,
        description = description,
        direction = direction,
        amountMinor = amountMinor,
        allocatedMinor = allocatedMinor,
        remainingMinor = remainingMinor,
        status = status,
        runningNetMinor = runningNetMinor
    )

    private fun statement(
        lines: List<StatementLine>,
        currency: String = "YER_NEW",
        charged: Long = lines.sumOf { if (it.direction == LineDirection.CHARGE) it.amountMinor else 0L },
        paid: Long = lines.sumOf { if (it.direction == LineDirection.PAYMENT) it.amountMinor else 0L },
        unapplied: Long = 0
    ) = MemberStatement(
        memberId = "me",
        currency = currency,
        lines = lines,
        chargedMinor = charged,
        paidMinor = paid,
        remainingMinor = charged - paid,
        unappliedMinor = unapplied,
        netMinor = remainingMinorOf(charged, paid, unapplied)
    )

    private fun remainingMinorOf(charged: Long, paid: Long, unapplied: Long): Long =
        charged - paid - unapplied

    private fun document(statement: MemberStatement, room: String = "بيت الوالد", subject: String = "كشف: أحمد") =
        StatementDocumentBuilder.build(
            statement = statement,
            roomTitle = room,
            subject = subject,
            appVersion = "1.0.0",
            fileNameStamp = "2026-10-03",
            dateText = date
        ).flatLines

    // ٠١ — كشف فارغ: يقول إنه فارغ ولا يطبع جدولًا بلا صفوف.
    @Test
    fun `٠١ كشف فارغ`() {
        assertEquals(
            listOf(
                "بيننا — إصدار 1.0.0 — مستودع حساباتك ومعاملاتك",
                "كشف: أحمد — بيت الوالد",
                "ريال يمني جديد (ر.ي) • من دفترك على هذا الجهاز",
                "المبلغ: 0 ر.ي • المسدَّد: 0 ر.ي • الباقي: 0 ر.ي",
                "لا سطور بعد: الدفتر فارغ.",
                "هذا الكشف من دفترك على هذا الهاتف، ولم يُرسل إلى أي خادم."
            ),
            document(statement(emptyList()))
        )
    }

    // ٠٢ — دين واحد لم يُسدَّد: الرصيد الجاري «عليك».
    @Test
    fun `٠٢ دين واحد`() {
        val lines = document(
            statement(
                listOf(line("e1", amountMinor = 3_500_000, runningNetMinor = -3_500_000)),
                charged = 3_500_000
            )
        )
        assertEquals("التاريخ • البيان • المبلغ • الرصيد الجاري", lines[4])
        assertEquals("2026-10-03 • سقية 5 ساعات • 3,500 ر.ي • عليك 3,500 ر.ي", lines[5])
        assertEquals("المبلغ: 3,500 ر.ي • المسدَّد: 0 ر.ي • الباقي: 3,500 ر.ي", lines[3])
    }

    // ٠٣ — دينان متتاليان: الرصيد الجاري يتراكم سطرًا سطرًا (لا يُعاد حسابه بالجمع الذهني).
    @Test
    fun `٠٣ دينان والرصيد يتراكم`() {
        val lines = document(
            statement(
                listOf(
                    line("e1", amountMinor = 1_000_000, runningNetMinor = -1_000_000),
                    line(
                        "e2",
                        description = "دين سلعة: كيس أرز",
                        type = EntryType.GOODS_DEBT,
                        amountMinor = 2_000_000,
                        runningNetMinor = -3_000_000
                    )
                ),
                charged = 3_000_000
            )
        )
        assertEquals("2026-10-03 • سقية 5 ساعات • 1,000 ر.ي • عليك 1,000 ر.ي", lines[5])
        assertEquals("2026-10-03 • دين سلعة: كيس أرز • 2,000 ر.ي • عليك 3,000 ر.ي", lines[6])
    }

    // ٠٤ — سداد جزئي: يُكتب بعلامة ناقص ظاهرة، والرصيد ينقص بعده.
    @Test
    fun `٠٤ سداد جزئي`() {
        val lines = document(
            statement(
                listOf(
                    line("e1", amountMinor = 5_000_000, allocatedMinor = 2_000_000, remainingMinor = 3_000_000, runningNetMinor = -5_000_000),
                    line(
                        "p1",
                        type = EntryType.PAYMENT,
                        description = "سداد نقدي",
                        direction = LineDirection.PAYMENT,
                        amountMinor = 2_000_000,
                        allocatedMinor = 2_000_000,
                        remainingMinor = 0,
                        runningNetMinor = -3_000_000
                    )
                ),
                charged = 5_000_000,
                paid = 2_000_000
            )
        )
        assertEquals("2026-10-03 • سداد نقدي • − 2,000 ر.ي • عليك 3,000 ر.ي", lines[6])
        assertEquals("المبلغ: 5,000 ر.ي • المسدَّد: 2,000 ر.ي • الباقي: 3,000 ر.ي", lines[3])
    }

    // ٠٥ — سداد زائد: يبقى رصيد دائن معلّق، ويُقال ذلك نصًّا لا بلون.
    @Test
    fun `٠٥ سداد زائد ورصيد دائن معلّق`() {
        val lines = document(
            statement(
                listOf(
                    line("e1", amountMinor = 1_000_000, allocatedMinor = 1_000_000, remainingMinor = 0, runningNetMinor = -1_000_000),
                    line(
                        "p1",
                        type = EntryType.GENERAL_RECEIPT,
                        description = "قبض عام",
                        direction = LineDirection.PAYMENT,
                        amountMinor = 1_500_000,
                        allocatedMinor = 1_000_000,
                        remainingMinor = 0,
                        runningNetMinor = 0
                    )
                ),
                charged = 1_000_000,
                paid = 1_500_000,
                unapplied = 500_000
            )
        )
        assertTrue(lines.contains("رصيد دائن معلّق: 5,000 ر.ي — لم يُخصَّص على دَين بعد"))
        assertEquals("المبلغ: 1,000 ر.ي • المسدَّد: 1,500 ر.ي • الباقي: -5,000 ر.ي", lines[3])
    }

    // ٠٦ — الريال السعودي: الاسم والرمز يتبعان العملة، لا نصًّا ثابتًا.
    @Test
    fun `٠٦ عملة الريال السعودي`() {
        val lines = document(
            statement(
                listOf(line("e1", amountMinor = 120_000, runningNetMinor = -120_000)),
                currency = "SAR",
                charged = 120_000
            )
        )
        assertEquals("ريال سعودي (ر.س) • من دفترك على هذا الجهاز", lines[2])
        assertEquals("2026-10-03 • سقية 5 ساعات • 1,200 ر.س • عليك 1,200 ر.س", lines[5])
    }

    // ٠٧ — الدولار: الرمز «$» كما هو، ولا ترجمة مخترعة.
    @Test
    fun `٠٧ الدولار الأمريكي`() {
        val lines = document(
            statement(
                listOf(line("e1", amountMinor = 5_050, runningNetMinor = -5_050)),
                currency = "USD",
                charged = 5_050
            )
        )
        assertEquals("دولار أمريكي ($) • من دفترك على هذا الجهاز", lines[2])
        assertEquals("2026-10-03 • سقية 5 ساعات • 50.50 $ • عليك 50.50 $", lines[5])
    }

    // ٠٨ — الريال القديم: يُميَّز عن الجديد بالاسم والرمز، فلا يُظنّ أن الرقمين عملة واحدة.
    @Test
    fun `٠٨ الريال اليمني القديم`() {
        val lines = document(
            statement(
                listOf(line("e1", amountMinor = 900_000, runningNetMinor = -900_000)),
                currency = "YER_OLD",
                charged = 900_000
            )
        )
        assertEquals("ريال يمني قديم (ر.ي قديم) • من دفترك على هذا الجهاز", lines[2])
        assertTrue(lines[5].contains("9,000 ر.ي قديم"))
    }

    // ٠٩ — بيان عربي بأرقام عربية-هندية: يُنقل كما كتبه صاحبه بلا تحويل.
    @Test
    fun `٠٩ بيان بأرقام عربية هندية`() {
        val lines = document(
            statement(
                listOf(
                    line(
                        "e1",
                        description = "سقية ٥ ساعات — البئر الشرقي",
                        amountMinor = 250_000,
                        runningNetMinor = -250_000
                    )
                ),
                charged = 250_000
            )
        )
        assertEquals("2026-10-03 • سقية ٥ ساعات — البئر الشرقي • 2,500 ر.ي • عليك 2,500 ر.ي", lines[5])
    }

    // ١٠ — مبلغ بكسور: الكسر يظهر فقط حين يوجد، وفواصل الآلاف تبقى مكانها.
    @Test
    fun `١٠ مبلغ بكسور وفواصل آلاف`() {
        val lines = document(
            statement(
                listOf(line("e1", amountMinor = 1_234_567, runningNetMinor = -1_234_567)),
                charged = 1_234_567
            )
        )
        assertEquals("2026-10-03 • سقية 5 ساعات • 12,345.67 ر.ي • عليك 12,345.67 ر.ي", lines[5])
        assertEquals("المبلغ: 12,345.67 ر.ي • المسدَّد: 0 ر.ي • الباقي: 12,345.67 ر.ي", lines[3])
    }

    @Test
    fun `اسم الملف يحمل اسم التطبيق والطرف والغرفة والتاريخ`() {
        val document = StatementDocumentBuilder.build(
            statement = statement(emptyList()),
            roomTitle = "بيت الوالد",
            subject = "كشف: أحمد",
            appVersion = "1.0.0",
            fileNameStamp = "2026-10-03",
            dateText = date
        )
        assertEquals("بيننا-كشف-أحمد-بيت-الوالد-2026-10-03", document.fileName)
    }
}
