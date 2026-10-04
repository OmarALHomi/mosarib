package com.baynana.features.statements

import com.baynana.domain.ledger.StatementDocument
import com.baynana.domain.ledger.StatementDocumentBuilder
import com.baynana.domain.ledger.StatementText
import com.baynana.domain.ledger.MemberStatement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * الكشف كما يُشارَك فعلًا: **نصّ مستند واحد** يخرج من نفس الصفوف التي تُرى على الشاشة.
 *
 * الملفّ صار غلافًا رقيقًا حول [StatementDocumentBuilder]: لا صياغة هنا، ولا رقم يُحسب. كل سطر
 * يأتي من `StatementText`، والمستند هو ما ترسمه الشاشة وما يرسمه PDF (د٥) وما يُنسخ في واتساب.
 * فحين يتغيّر التنسيق أو العملة أو اسم التطبيق، تتغيّر الثلاثة معًا في وقت واحد.
 */
object StatementExport {

    /** ترويسة الشاشة/الملفّ: سطر الهوية نفسه المستعمل في الورق. */
    const val HEADER: String = StatementDocumentBuilder.APP_NAME

    /** يقول بوضوح إنّ هذه نسخة محلّية، فلا يُظنّ أنها وصلت إلى أحد. */
    const val LOCAL_ONLY_FOOTER: String = StatementDocumentBuilder.FOOTNOTE

    const val NEWLINE = "\n"

    /** ما يخرج: اسم ملفّ آمن للمشاركة، ومحتواه النصّي، والمستند الذي بُني منه. */
    data class Export(val fileName: String, val body: String, val document: StatementDocument)

    /**
     * يبني الكشف النصّي كما يُشارك.
     *
     * @param statement كشف العضو كما حسبه `StatementEngine`.
     * @param roomTitle عنوان الغرفة كما يراه صاحب الجهاز.
     * @param subject اسم صاحب الكشف؛ وإن كان فارغًا يُوضع عنوان عام.
     * @param appVersion إصدار التطبيق كما يظهر في ترويسة الورق.
     * @param exportedAt وقت الإنشاء (لاسم الملفّ وحده).
     * @param dateText كيف يُكتب التاريخ في السطور (يحقنه المتصل من مُنسّق الشاشة).
     */
    fun build(
        statement: MemberStatement,
        roomTitle: String,
        subject: String,
        appVersion: String = "0",
        exportedAt: Long = System.currentTimeMillis(),
        dateText: (Long) -> String = ::isoDate
    ): Export {
        val document = StatementDocumentBuilder.build(
            statement = statement,
            roomTitle = roomTitle,
            subject = subject,
            appVersion = appVersion,
            fileNameStamp = isoDate(exportedAt),
            dateText = dateText
        )
        return Export(
            fileName = document.fileName + ".txt",
            body = document.text + NEWLINE,
            document = document
        )
    }

    /** اسم ملفّ آمن للمشاركة (بلا امتداد). */
    fun fileName(subject: String, roomTitle: String, exportedAt: Long = System.currentTimeMillis()): String =
        StatementDocumentBuilder.fileName(subject, roomTitle, isoDate(exportedAt))

    /** تاريخ `yyyy-MM-dd` لاسم الملفّ وترويسة الورق — بتوقيت الجهاز، بلا علاقة بالأرقام المالية. */
    fun isoDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))

    /** يُتاح للشاشة وللمشاركة: الملخّص كما هو في المستند (لا صياغة ثانية). */
    fun summaryOf(statement: MemberStatement): String = StatementText.summary(statement)
}
