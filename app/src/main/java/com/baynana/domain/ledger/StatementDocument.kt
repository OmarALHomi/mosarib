package com.baynana.domain.ledger

/**
 * **مستند الكشف**: كل ما يُعرض أو يُطبع، مبنيًّا في مكان واحد قبل أن يُرسم.
 *
 * الفكرة التي يقوم عليها الملفّ: الشاشة والورق ليسا سطحين يُنسَّقان مرتين، بل **قارئان لمستند
 * واحد**. تُبنى خلايا الصفوف هنا مرة واحدة من `StatementText`، ثم:
 * - الشاشة (`StatementSheet`) ترسم [rows]،
 * - ملف PDF يرسم [flatLines] بالسطر،
 * - والمشاركة النصّية تُخرج [text] نفسه.
 *
 * فحين ينجح الاختبار الذهبي (١٠ سيناريوهات) يكون معنى نجاحه: ما تراه على الشاشة هو ما في الورقة
 * حرفًا بحرف، لا «قريبًا منه».
 *
 * ولا حساب هنا: كل الأرقام جاءت من `StatementEngine` ومن `StatementText`، وهذا الملفّ يرتّب فقط.
 */
data class StatementDocumentRow(
    val entryId: String,
    /** دين على العضو (يُلوَّن عاديًّا) أم سداد (يُلوَّن مخضرًّا بعلامة ناقص). */
    val isCharge: Boolean,
    /** الخلايا الأربع: التاريخ، البيان، المبلغ، الرصيد الجاري. */
    val cells: List<String>
)

data class StatementDocument(
    /** سطر الهوية: التطبيق وإصداره — يظهر في ترويسة الورق كي يُعرف من أي نسخة خرج. */
    val identityLine: String,
    val title: String,
    val subtitle: String,
    val summary: String,
    /** الأرقام الثلاثة كما عرضتها `NumbersHeader` — تُحمل ههنا فلا تُمرّر الشاشة كشفًا آخر. */
    val chargedMinor: Long,
    val paidMinor: Long,
    val remainingMinor: Long,
    val currency: String,
    val columnTitles: List<String>,
    val rows: List<StatementDocumentRow>,
    /** تظهر وحدها إن كان الدفتر فارغًا، فتُطبع ورقة مفهومة لا جدولًا فارغًا. */
    val emptyNote: String?,
    val unappliedNote: String?,
    val footnote: String,
    val fileName: String
) {
    /** السطور المسطّحة بترتيب الرسم: هي ما يُطبع، وهي ما يُقارَن في الاختبار الذهبي. */
    val flatLines: List<String>
        get() = buildList {
            add(identityLine)
            add(title)
            add(subtitle)
            add(summary)
            if (rows.isEmpty()) {
                emptyNote?.let { add(it) }
            } else {
                add(columnTitles.joinToString(CELL_SEPARATOR))
                rows.forEach { add(it.cells.joinToString(CELL_SEPARATOR)) }
            }
            unappliedNote?.let { add(it) }
            add(footnote)
        }

    /** النصّ الكامل: هو نفسه ما يُشارَك في واتساب وما يُرسل كملفّ. */
    val text: String get() = flatLines.joinToString("\n")

    companion object {
        const val CELL_SEPARATOR = " • "
    }
}

/**
 * يبني [StatementDocument] من كشف عضو. لا يُعيد حساب أي رقم: `statement` جاهز من المحرّك.
 */
object StatementDocumentBuilder {

    const val APP_NAME = "بيننا"
    const val FOOTNOTE = "هذا الكشف من دفترك على هذا الهاتف، ولم يُرسل إلى أي خادم."
    const val EMPTY_NOTE = "لا سطور بعد: الدفتر فارغ."
    const val DEFAULT_SUBJECT = "كشف حساب"
    const val DEFAULT_ROOM = "غرفة"

    /**
     * @param appVersion نصّ الإصدار كما يظهر للمستخدم (يُحقن من طبقة Android، فالمستند نقيّ).
     * @param dateText كيف يُكتب التاريخ — يُحقن من نفس مُنسّق الشاشة.
     * @param fileNameStamp ختم التاريخ في اسم الملفّ، يُحقن من المتصل (فلا تقويم في domain).
     */
    fun build(
        statement: MemberStatement,
        roomTitle: String,
        subject: String,
        appVersion: String,
        fileNameStamp: String,
        dateText: (Long) -> String
    ): StatementDocument {
        val room = roomTitle.trim().ifBlank { DEFAULT_ROOM }
        val who = subject.trim().ifBlank { DEFAULT_SUBJECT }
        return StatementDocument(
            identityLine = "$APP_NAME — إصدار $appVersion — مستودع حساباتك ومعاملاتك",
            title = "$who — $room",
            subtitle = listOf(
                currencyLabel(statement.currency),
                "من دفترك على هذا الجهاز"
            ).joinToString(" • "),
            summary = StatementText.summary(statement),
            chargedMinor = statement.chargedMinor,
            paidMinor = statement.paidMinor,
            remainingMinor = statement.remainingMinor,
            currency = statement.currency,
            columnTitles = StatementText.COLUMN_TITLES,
            rows = statement.lines.map { line ->
                StatementDocumentRow(
                    entryId = line.entryId,
                    isCharge = line.direction == LineDirection.CHARGE,
                    cells = StatementText.rowCells(line, statement.currency, dateText(line.occurredAt))
                )
            },
            emptyNote = EMPTY_NOTE.takeIf { statement.lines.isEmpty() },
            unappliedNote = if (statement.unappliedMinor > 0L) {
                "رصيد دائن معلّق: ${StatementText.amount(statement.unappliedMinor, statement.currency)}" +
                    " — لم يُخصَّص على دَين بعد"
            } else {
                null
            },
            footnote = FOOTNOTE,
            fileName = fileName(who, room, fileNameStamp)
        )
    }

    /** اسم ملفّ آمن: بلا فواصل مسارات ولا رموز محجوزة، ولا يطول حتى ترفضه المشاركة. */
    fun fileName(subject: String, roomTitle: String, fileNameStamp: String): String {
        val raw = "$APP_NAME-$subject-$roomTitle-$fileNameStamp"
        val safe = raw.map { ch ->
            when {
                ch.isLetterOrDigit() -> ch
                ch == '-' || ch == '_' -> ch
                else -> '-'
            }
        }.joinToString("")
            .replace(Regex("-{2,}"), "-")
            .trim('-')
            .take(MAX_NAME_CHARS)
            .trim('-')
        return safe.ifBlank { "$APP_NAME-كشف" }
    }

    private const val MAX_NAME_CHARS = 60

    /** العملة نصًّا: الاسم العربي ورمزه («ريال يمني جديد — ر.ي»)، ولا رمز غريب عن المستخدم. */
    private fun currencyLabel(code: String): String {
        val currency = com.baynana.domain.money.Currency.fromCode(code) ?: return code
        return "${currency.arabicName} (${currency.symbol})"
    }
}
