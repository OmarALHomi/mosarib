package com.baynana.domain.money

/**
 * تمثيل المال في العقد السلكي (`/api/v1`) **نصًّا** لا رقمًا.
 *
 * السبب: أي `JSON number` يتحول في الطرفين إلى `double`، و`double` لا يمثّل كل الأعداد
 * الصحيحة بدقة فوق 2^53، ثم تُضاف كسور عائمة في العمليات. القاعدة المعتمدة: **المال لا
 * يسافر كرقم أبدًا**.
 *
 * الشكل المعتمد — حقلان، والوحدة الصغرى نصًّا:
 * ```json
 * { "amountMinor": "1500000", "currency": "YER_NEW" }
 * ```
 * و`amountMinor` عدد صحيح بلا فاصلة ولا علامة، يمثّل الفلس/السنت. لا كسور عشرية في السلك،
 * ولا تحويل إلى ريال قبل العرض. أي قيمة لا تطابق هذا القالب تُرفض ولا تُقدَّر.
 *
 * القيد نفسه ينطبق على ملفات التصدير المتنقل (`events.jsonl`)، وعلى أي تخزين نصي مثل
 * SharedPreferences أو إعداد الأدمن.
 */
object MoneyWire {

    /** مفاتيح العقد. ثابتة لأنها تُكتب في ملفات التصدير وفي المستودع. */
    const val KEY_AMOUNT_MINOR = "amountMinor"
    const val KEY_CURRENCY = "currency"

    /** تحويل إلى حقول السلك: الوحدة الصغرى نصًّا ورمز العملة. */
    fun encode(money: Money): Map<String, String> = mapOf(
        KEY_AMOUNT_MINOR to money.minor.toString(),
        KEY_CURRENCY to money.currency.code
    )

    /** تحويل إلى نص مركّب مفرد، للاستخدام في المسارات والسجلات: `1500000:YER_NEW`. */
    fun encodeCompact(money: Money): String = "${money.minor}:${money.currency.code}"

    /**
     * قراءة من حقول السلك. يرفض: غياب الحقل، نصًّا غير رقمي، كسرًا عشريًا، قيمة سالبة،
     * عملة غير معروفة، أو تجاوز الحد. كل رفض برسالة عربية.
     */
    fun decode(amountMinor: String?, currencyCode: String?): MoneyParse {
        if (amountMinor == null || amountMinor.isBlank()) {
            return MoneyParse.Error("حقل المبلغ مفقود في البيانات الواردة")
        }
        if (currencyCode == null || currencyCode.isBlank()) {
            return MoneyParse.Error("حقل العملة مفقود في البيانات الواردة")
        }
        val currency = Currency.fromCode(currencyCode.trim())
            ?: return MoneyParse.Error("عملة غير معروفة: $currencyCode")

        val text = amountMinor.trim()
        if (text.startsWith("-")) return MoneyParse.Error("لا يُقبل مبلغ سالب في البيانات الواردة")
        if (text.startsWith("+")) return MoneyParse.Error("صيغة المبلغ غير صحيحة: علامة زائدة")
        if (text.any { !it.isDigit() }) {
            return MoneyParse.Error("الوحدة الصغرى يجب أن تكون عددًا صحيحًا بلا كسور: $text")
        }

        val minor = text.toLongOrNull() ?: return MoneyParse.Error("المبلغ أكبر من الحد المسموح")
        return try {
            MoneyParse.Ok(Money.ofMinor(minor, currency))
        } catch (error: MoneyException) {
            MoneyParse.Error(error.message ?: "مبلغ غير صالح")
        }
    }

    /** قراءة من النص المركّب `1500000:YER_NEW`. */
    fun decodeCompact(text: String?): MoneyParse {
        if (text.isNullOrBlank()) return MoneyParse.Error("قيمة مالية فارغة")
        val parts = text.trim().split(':')
        if (parts.size != 2) return MoneyParse.Error("صيغة القيمة المالية غير صحيحة")
        return decode(parts[0], parts[1])
    }

    /**
     * قراءة متسامحة مع صيغة قديمة كانت تخزّن المبلغ بالريال كنص عشري.
     * تُستخدم مرة واحدة في الترحيل (ح٥) وتُسجَّل في سجل المطابقة، لا في المسار العادي.
     */
    fun decodeLegacyMajor(majorText: String?, currencyCode: String?): MoneyParse {
        if (majorText == null) return MoneyParse.Error("حقل المبلغ مفقود")
        val currency = currencyCode?.let { Currency.fromCode(it.trim()) }
            ?: return MoneyParse.Error("حقل العملة مفقود أو غير معروف")
        return MoneyParser.parse(majorText, currency)
    }
}
