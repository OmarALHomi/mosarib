package com.baynana.domain.money

/**
 * مبلغ مالي بوحدات صغرى صحيحة ([minor]) مع عملته.
 *
 * لماذا لا `Double`: التخزين العشري يُنتج فروقًا صغيرة تتراكم في الأرصدة، وهذا أخطر عيب في
 * دفتر حسابات. كل حساب جديد في «بيننا» يمر من هنا (الخطة v6 §4.1)، والتحويل الكامل للشاشات
 * والمزامنة يتم في الحزم التالية (ح٤ وح٥).
 *
 * قواعد صارمة:
 * - لا جمع ولا طرح ولا مقارنة بين عملتين مختلفتين.
 * - لا تجاوز صامت: أي خروج عن [MAX_MINOR] يرفع [MoneyException].
 * - التقريب عند الضرب فقط، بسياسة واحدة معلنة: نصف لأعلى (HALF_UP) وبإشارة صحيحة.
 * - لا مبالغ سالبة: يُستخدم [negate] فقط للإلغاء العكسي الصريح في دفتر القيود.
 * - الوحدة الصغرى [MinorUnits] هي نوع كل تخزين وحساب، والإرسال في الـAPI نصّي عبر [MoneyWire].
 */
data class Money(
    val minor: MinorUnits,
    val currency: Currency
) : Comparable<Money> {

    init {
        if (minor < 0) throw MoneyException("لا يُقبل مبلغ سالب؛ استخدم الإلغاء العكسي")
        if (minor > MAX_MINOR) throw MoneyException("المبلغ أكبر من الحد المسموح")
    }

    // ------------------------------------------------------------ العمليات

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(checkedAdd(minor, other.minor), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        val result = minor - other.minor
        if (result < 0) throw MoneyException("الطرح يعطي مبلغًا سالبًا؛ راجع القيود")
        return Money(result, currency)
    }

    /** ضرب في كمية صحيحة (عدد أكياس، عدد حصص…) بلا تقريب. */
    operator fun times(quantity: Long): Money {
        if (quantity < 0) throw MoneyException("لا يُقبل عدد سالب")
        val result = try {
            Math.multiplyExact(minor, quantity)
        } catch (_: ArithmeticException) {
            throw MoneyException("المبلغ أكبر من الحد المسموح")
        }
        return Money(result, currency)
    }

    /**
     * ضرب في كسر مثل حسابات الري: سعر الساعة × الدقائق ÷ 60.
     * التقريب HALF_UP، والنتيجة موجبة؛ أي كسر يقرّب لأقرب وحدة صغرى في العملة نفسه.
     */
    fun times(numerator: Long, denominator: Long): Money {
        if (numerator < 0) throw MoneyException("لا يُقبل بسط سالب")
        if (denominator <= 0) throw MoneyException("مقام غير صالح")
        return Money(roundedMultiply(minor, numerator, denominator), currency)
    }

    /** الإلغاء العكسي الصريح: يقلب المبلغ لمقابلته في دفتر القيود (لا يُستعمل للإدخال). */
    fun negate(): MinorUnits = -minor

    // ------------------------------------------------------------ الاستعلام

    val isZero: Boolean get() = minor == 0L

    val isPositive: Boolean get() = minor > 0L

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minor.compareTo(other.minor)
    }

    /** العرض بالريال مع رمز العملة (الكسر يُحذف إن كان صفرًا). */
    fun format(): String = MoneyFormat.format(this)

    /** الوحدة الصغرى نصًّا: شكل التخزين والسلك. */
    fun toMinorString(): String = MoneyFormat.toMinorString(this)

    /** الريال بلا فواصل آلاف، للطباعة والاستيراد. */
    fun toPlainMajorString(): String = MoneyFormat.toPlainMajorString(this)

    override fun toString(): String = "${toMinorString()} ${currency.code}"

    // ------------------------------------------------------------- داخلي

    private fun requireSameCurrency(other: Money) {
        if (other.currency != currency) {
            throw MoneyException("لا يمكن خلط عملتين: ${currency.code} و${other.currency.code}")
        }
    }

    companion object {
        /** حد أعلى وقائي: تريليون وحدة، يمنع تجاوزًا أو قيمة شاذة تُفسد الأرصدة. */
        const val MAX_MINOR: MinorUnits = 1_000_000_000_000L

        /** مبلغ بالوحدة الكاملة (مثل 15000 ريال). */
        fun ofMajor(amount: Long, currency: Currency): Money {
            if (amount < 0) throw MoneyException("لا يُقبل مبلغ سالب")
            val minor = try {
                Math.multiplyExact(amount, currency.minorPerUnit)
            } catch (_: ArithmeticException) {
                throw MoneyException("المبلغ أكبر من الحد المسموح")
            }
            return Money(minor, currency)
        }

        fun ofMinor(minor: MinorUnits, currency: Currency): Money = Money(minor, currency)

        fun zero(currency: Currency): Money = Money(0L, currency)

        /** يقرأ مبلغًا كتبه المستخدم؛ انظر [MoneyParser] لتفاصيل ما يُقبل وما يُرفض. */
        fun parse(input: String, currency: Currency): MoneyParse = MoneyParser.parse(input, currency)

        private fun checkedAdd(left: MinorUnits, right: MinorUnits): MinorUnits = try {
            Math.addExact(left, right)
        } catch (_: ArithmeticException) {
            throw MoneyException("المبلغ أكبر من الحد المسموح")
        }

        /** ضرب مع تقريب HALF_UP بلا كسور عائمة: كل الحساب على صحيح 64-بت. */
        internal fun roundedMultiply(value: MinorUnits, numerator: Long, denominator: Long): MinorUnits {
            if (value == 0L || numerator == 0L) return 0L
            val product = try {
                Math.multiplyExact(value, numerator)
            } catch (_: ArithmeticException) {
                throw MoneyException("المبلغ أكبر من الحد المسموح")
            }
            val negative = product < 0
            val magnitude = if (negative) -product else product
            val quotient = magnitude / denominator
            val remainder = magnitude % denominator
            val rounded = if (remainder * 2 >= denominator) quotient + 1 else quotient
            val result = if (negative) -rounded else rounded
            if (result > MAX_MINOR) throw MoneyException("المبلغ أكبر من الحد المسموح")
            return result
        }
    }
}

/** نتيجة قراءة مبلغ: نجاح بمبلغ، أو رفض برسالة عربية واضحة. */
sealed interface MoneyParse {
    data class Ok(val money: Money) : MoneyParse

    data class Error(val message: String) : MoneyParse

    fun moneyOrNull(): Money? = (this as? Ok)?.money
}
