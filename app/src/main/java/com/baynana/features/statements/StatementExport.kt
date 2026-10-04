package com.baynana.features.statements

import com.baynana.domain.ledger.MemberStatement
import com.baynana.domain.ledger.StatementText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ملفّ كشف الحساب كما يُشارَك فعلًا: نصٌّ واحد يخرج من نفس السطور التي تُرى على الشاشة.
 *
 * القاعدة الحاكمة: **لا يُبنى رقم هنا**. كل سطر يمرّ من `StatementText.line`، والترويسة من
 * `StatementText.summary`. فمهما تغيّر التنسيق أو العملة أو التسمية، تتغيّر الشاشة والملفّ معًا
 * في وقت واحد — وهذا هو شرط البوّابة الذهبية (نصّ الشاشة = نصّ الملفّ).
 *
 * ولا يُشارَك رقم وحده أبدًا: كل كشف يحمل [LOCAL_ONLY_FOOTER] لأن ما بين يدَي المستخدم صورة من
 * دفتره المحلّي، ولا أحد يعلم بمحتواه غيره.
 */
object StatementExport {

    /** ترويسة الشعار: تجعل الملفّ المطبوع معروفًا من سطر واحد. */
    const val HEADER = "بيننا — مستودع حساباتك ومعاملاتك"

    /** يقول بوضوح إنّ هذه نسخة محلّية، فلا يُظنّ أنها وصلت إلى أحد. */
    const val LOCAL_ONLY_FOOTER = "هذا الكشف من دفترك على هذا الهاتف، ولم يُرسل إلى أي خادم."

    /** فاصل الأسطر: ثابت لا يتغيّر بتغيّر النظام، حتى تكون المقارنة الآلية ممكنة. */
    const val NEWLINE = "\n"

    private const val FALLBACK_SUBJECT = "كشف حساب"
    private const val FALLBACK_ROOM = "غرفة"

    /** أقصى طول لاسم الملف: أسماء الملفات الطويلة تفشل عند المشاركة على بعض الأجهزة. */
    private const val MAX_NAME_CHARS = 60

    /** ما يخرج: اسم ملف آمن للمشاركة، ومحتواه النصّي. */
    data class Export(val fileName: String, val body: String)

    /**
     * يبني الكشف بالكامل.
     *
     * @param statement كشف العضو كما حسبه `StatementEngine` — لا يُعاد حساب شيء هنا.
     * @param roomTitle عنوان الغرفة كما يراه صاحب الجهاز.
     * @param subject اسم صاحب الكشف («كشف: أحمد» مثلًا)؛ وإن كان فارغًا يُوضع عنوان عام.
     * @param exportedAt وقت الإنشاء، لأجل اسم الملفّ وحده.
     * @param dateText كيف يُكتب التاريخ في سطور الكشف (يحقنه المتصل من نفس مُنسّق الشاشة).
     */
    fun build(
        statement: MemberStatement,
        roomTitle: String,
        subject: String,
        exportedAt: Long = System.currentTimeMillis(),
        dateText: (Long) -> String = ::isoDate
    ): Export {
        val title = roomTitle.trim().ifBlank { FALLBACK_ROOM }
        val who = subject.trim().ifBlank { FALLBACK_SUBJECT }
        val body = buildString {
            append(HEADER).append(NEWLINE)
            append("$who — $title").append(NEWLINE)
            append(StatementText.summary(statement)).append(NEWLINE)
            if (statement.lines.isEmpty()) {
                append("لا سطور بعد: الدفتر فارغ.").append(NEWLINE)
            } else {
                statement.lines.forEach { line ->
                    append(StatementText.line(line, statement.currency, dateText(line.occurredAt)))
                        .append(NEWLINE)
                }
            }
            append(LOCAL_ONLY_FOOTER)
        }
        return Export(fileName = fileName(who, title, exportedAt), body = body)
    }

    /** اسم ملف آمن: بلا فواصل مسارات ولا رموز محجوزة، ولا يطول حتى يرفضه نظام المشاركة. */
    fun fileName(subject: String, roomTitle: String, exportedAt: Long = System.currentTimeMillis()): String {
        val raw = "بيننا — $subject — $roomTitle — ${isoDate(exportedAt)}"
        val safe = raw.map { ch ->
            when {
                ch.isLetterOrDigit() -> ch
                ch == '-' || ch == '_' -> ch
                else -> '-'
            }
        }.joinToString("").replace(Regex("-{2,}"), "-").trim('-').take(MAX_NAME_CHARS).trim('-')
        return safe.ifBlank { "بيننا-كشف" } + ".txt"
    }

    /**
     * تاريخ `yyyy-MM-dd` لاسم الملفّ. لا يُستعمل في الأرقام المالية، وهو ثابت بتوقيت الجهاز:
     * التاريخ الذي يرى المستخدمه هو التاريخ الذي يُكتب في الاسم.
     */
    fun isoDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))
}
