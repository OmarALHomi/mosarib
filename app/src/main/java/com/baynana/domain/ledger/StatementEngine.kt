package com.baynana.domain.ledger

import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money
import com.baynana.domain.money.MoneyFormat

/**
 * كشف عضو واحد: الأسطر بترتيبها الزمني مع الرصيد الجاري، والأرقام الثلاثة الذهبية.
 *
 * تُبنى من [LedgerSnapshot] نفسه، فلا يمكن أن يختلف كشف الورق عن رصيد الشاشة: كلاهما قراءة من
 * لقطة واحدة. والدوران هنا ترتيب وحساب جارٍ فقط، لا إعادة حساب للأرصدة.
 */
data class MemberStatement(
    val memberId: String,
    val currency: String,
    val lines: List<StatementLine>,
    /** المبلغ: مجموع ديون العضو المؤثرة. */
    val chargedMinor: Long,
    /** المسدَّد: ما أُسقط من ديونه. */
    val paidMinor: Long,
    /** الباقي: charged − paid. */
    val remainingMinor: Long,
    /** ما بيده من مال الطرف الآخر ولم يُخصَّص. */
    val unappliedMinor: Long,
    val netMinor: Long
) {
    val isClear: Boolean get() = remainingMinor == 0L && unappliedMinor == 0L
}

/** سطر كشف: قيد، وما يخصّ العضو منه، والرصيد الجاري بعد ترتيبه. */
data class StatementLine(
    val entryId: String,
    val occurredAt: Long,
    val type: String,
    val description: String,
    val direction: LineDirection,
    val amountMinor: Long,
    val allocatedMinor: Long,
    val remainingMinor: Long,
    val status: String,
    /** صافي العضو بعد هذا السطر (طرح كامل، يُحسب بالجمع لا بإعادة بناء الأرصدة). */
    val runningNetMinor: Long
)

/** اتجاه السطر بالنسبة للعضو صاحب الكشف. */
enum class LineDirection {
    /** على العضو: زاد ما عليه (سقية، دين سلعة، صلح). */
    CHARGE,

    /** للعضو: نقص ما عليه (سداد دفع، قبض استلمه الطرف الآخر). */
    PAYMENT
}

object StatementEngine {

    /**
     * كشف العضو **المدين** (المزارع في قالب الري): ديونه وسداده.
     *
     * الترتيب زمني تصاعدي (الأقدم أولًا) وبفصل التعادل بالمعرّف، فلا يختلف كشف جهاز عن آخر.
     * الرصيد الجاري يُجمع سطرًا سطرًا من نفس أرقام اللقطة، وآخر رقم فيه يساوي `netMinor`.
     */
    fun statementFor(
        debtorMemberId: String,
        snapshot: LedgerSnapshot
    ): MemberStatement {
        val lines = mutableListOf<StatementLine>()
        var running = 0L

        for (line in snapshot.lines) {
            when {
                // دين على العضو: يزيد ما عليه.
                line.isDebt && line.debtorMemberId == debtorMemberId -> {
                    running -= line.amountMinor
                    lines += line.toStatementLine(LineDirection.CHARGE, running)
                }
                // سداد دفعه العضو (منظور الطرف الآخر: هو مستلم المال): ينقص ما عليه.
                line.isCredit && line.creditorMemberId == debtorMemberId -> {
                    running += line.amountMinor
                    lines += line.toStatementLine(LineDirection.PAYMENT, running)
                }
                // قيود أخرى تخصّ العضو من الجهة الأخرى (مثل قبض استلمه هو): تُظهر ولا تُقلب الأرقام.
                line.isDebt && line.creditorMemberId == debtorMemberId -> {
                    running += line.amountMinor
                    lines += line.toStatementLine(LineDirection.PAYMENT, running)
                }
                line.isCredit && line.debtorMemberId == debtorMemberId -> {
                    running -= line.amountMinor
                    lines += line.toStatementLine(LineDirection.CHARGE, running)
                }
            }
        }

        val member = snapshot.member(debtorMemberId)
        val charged = member?.chargedMinor ?: 0L
        val paid = member?.paidMinor ?: 0L
        return MemberStatement(
            memberId = debtorMemberId,
            currency = snapshot.currency,
            lines = lines,
            chargedMinor = charged,
            paidMinor = paid,
            remainingMinor = charged - paid,
            unappliedMinor = member?.unappliedMinor ?: 0L,
            netMinor = member?.netMinor ?: 0L
        )
    }

    private fun SnapshotLine.toStatementLine(direction: LineDirection, running: Long) = StatementLine(
        entryId = entryId,
        occurredAt = occurredAt,
        type = type,
        description = description,
        direction = direction,
        amountMinor = amountMinor,
        allocatedMinor = allocatedMinor,
        remainingMinor = remainingMinor,
        status = status,
        runningNetMinor = running
    )
}

/**
 * **مصدر النص الواحد** لكل ما يُعرض أو يُطبع: الشاشة، وملف PDF، وكشف المزارع، والتصدير النصي.
 *
 * القاعدة (ADR-04): المال يُعرض بالريال ويُخزَّن بالفلس؛ التنسيق من [MoneyFormat] مرة واحدة، ولا
 * `Double` في أي خطوة. وأي سطح عرض جديد يستدعي هذه الدوال، فلا يوجد «تنسيق ثانٍ» ينحرف يومًا.
 */
object StatementText {

    /** ترويسة الأرقام الثلاثة: «المبلغ … • المسدَّد … • الباقي …». */
    fun summary(statement: MemberStatement): String = summaryOf(
        chargedMinor = statement.chargedMinor,
        paidMinor = statement.paidMinor,
        remainingMinor = statement.remainingMinor,
        currency = statement.currency
    )

    fun summaryOf(chargedMinor: Long, paidMinor: Long, remainingMinor: Long, currency: String): String {
        val code = requireCurrency(currency)
        return buildString {
            append("المبلغ: ").append(money(chargedMinor, code))
            append(" • المسدَّد: ").append(money(paidMinor, code))
            append(" • الباقي: ").append(money(remainingMinor, code))
        }
    }

    /** سطر كشف واحد: «2026-10-03 • سقية 5 ساعات • 10,000 ر.ي • المتبقي 5,000 ر.ي». */
    fun line(line: StatementLine, currency: String, dateText: String): String {
        val code = requireCurrency(currency)
        val kind = if (line.direction == LineDirection.CHARGE) "على العضو" else "سدَّد"
        val remaining = money(line.remainingMinor, code)
        return "$dateText • ${line.description.ifBlank { line.type }} • ${money(line.amountMinor, code)} • $kind • المتبقي $remaining"
    }

    /**
     * الأرقام وحدها بنصّ ثابت يفصله «|» — للتصدير النصي (CSV/JSONL) وللمقارنة الآلية بين
     * الشاشة والورق في الاختبارات الذهبية.
     */
    fun canonicalNumbers(statement: MemberStatement): String =
        "${statement.chargedMinor}|${statement.paidMinor}|${statement.remainingMinor}|${statement.currency}"

    /** سطر إجمالي الغرفة: الرصيد المعلّق والدين المفتوح مفصولين لا مجموعين (§4.1). */
    fun roomSummary(snapshot: LedgerSnapshot): String {
        val code = requireCurrency(snapshot.currency)
        return buildString {
            append("الديون المفتوحة: ").append(money(snapshot.openDebtMinor, code))
            append(" • أرصدة دائنة معلّقة: ").append(money(snapshot.unappliedReceiptMinor, code))
            if (snapshot.draftCount > 0) {
                append(" • مسودات لم تُرسل: ").append(snapshot.draftCount)
            }
            if (snapshot.awaitingAcknowledgement > 0) {
                append(" • في انتظار الإقرار: ").append(snapshot.awaitingAcknowledgement)
            }
        }
    }

    /** عناوين جدول الكشف: نفسها في الشاشة وفي الورق (شرط البوابة الذهبية). */
    val COLUMN_TITLES: List<String> = listOf("التاريخ", "البيان", "المبلغ", "الرصيد الجاري")

    /** مبلغ مفرد نصًّا — للخلايا وللترويسات، من `MoneyFormat` وحده. */
    fun amount(minor: Long, currency: String): String = money(minor, requireCurrency(currency))

    /** مبلغ السطر بإشارته: الدين موجب، والسداد بعلامة ناقص ظاهرة (لا يختفي الفرق في لون). */
    fun signedAmount(minor: Long, currency: String, isCharge: Boolean): String =
        (if (isCharge) "" else "− ") + amount(minor, currency)

    /**
     * الرصيد الجاري بعد السطر: «عليك …» أو «لك …» أو «مصفّى حتى هنا».
     * الكلمة تحمل الإشارة، فمن لا يفرّق الألوان يقرأ الاتجاه من النصّ.
     */
    fun running(runningNetMinor: Long, currency: String): String = when {
        runningNetMinor == 0L -> "مصفّى حتى هنا"
        // الكلمة تحمل الإشارة، والرقم يُكتب مقدارًا مجرّدًا: «عليك 5,000 ر.ي» لا «عليك -5,000».
        runningNetMinor < 0L -> "عليك ${amount(-runningNetMinor, currency)}"
        else -> "لك ${amount(runningNetMinor, currency)}"
    }

    /**
     * خلايا سطر الكشف الأربع. **المصدر الوحيد** الذي تُبنى منه صفوف الشاشة وصفوف الورق، فلا
     * يمكن أن تختلف ورقة عن شاشة: كلاهما يقرأ هذه القائمة نفسها.
     */
    fun rowCells(line: StatementLine, currency: String, dateText: String): List<String> {
        val code = requireCurrency(currency)
        return listOf(
            dateText,
            line.description.ifBlank { line.type },
            signedAmount(line.amountMinor, currency, line.direction == LineDirection.CHARGE),
            running(line.runningNetMinor, currency)
        )
    }

    private fun money(minor: Long, currency: Currency): String =
        MoneyFormat.format(Money.ofMinor(minor, currency))

    private fun requireCurrency(code: String): Currency =
        Currency.fromCode(code) ?: error("عملة غير معروفة: $code")
}
