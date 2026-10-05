package com.baynana.core.sync

/**
 * منطق **إعداد قناة المزامنة من الجهاز** (ح٢٢ب) — منطق نقيّ، والاختبارات تشغّله بلا أندرويد.
 *
 * وهذا هو الجزء الذي كان ناقصًا: القناة كانت تعمل من الطبقة الدنيا (٤ من ٤)، لكن **لا واجهة** تُدخل
 * رمز الجهاز، فجهاز حقيقي لا يستطيع ضبط القناة أصلًا. والمنطق هنا يفصل بين أربعة أشياء يجب ألّا
 * تختلط، لأن الخلط بينها هو ما يجعل الإعداد «ينجح» زورًا:
 *
 * 1. **الرمز لا يُحكم عليه بالشكل**: الرمز يمنحه المالك، ولا نكتب قواعد له ترفض رمزًا صحيحًا. الحكم
 *    الوحيد أن يقبله الخادم.
 * 2. **الشبكة تُفصل عن القبول**: تعذّر الوصول (بلا شبكة/خادم متوقّف) ≠ رمز مرفوض. الأول يُعاد،
 *    والثاني يُصلح. وخلطهما يدفع المستخدم لتبديل رمز صحيح.
 * 3. **الصمت يُسمّى**: ردّ بلا سبب مفهوم ⇒ «رُفض الطلب» بلا اختراع سبب.
 * 4. **لا حفظ بلا إثبات**: الرمز لا يُخزَّن إلا بعد محاولة صادقة تقول «قُبل».
 */
object SyncTokenSetup {

    /** ماذا حدث لمحاولة الإعداد؟ أربع حالات، وكلّها تُعرض للمستخدم بجملة عربية. */
    sealed interface Outcome {
        /** قُبل الرمز: يجوز الحفظ. */
        data class Accepted(val applied: Int) : Outcome

        /** رُفض الرمز أو الصيغة: لا فائدة من إعادة المحاولة بالرمز نفسه. */
        data class Refused(val reason: String) : Outcome

        /** الفشل مؤقّت: الصواب إعادة المحاولة، لا تبديل الرمز. */
        data class Unreachable(val reason: String) : Outcome
    }

    /**
     * يصوغ نتيجة الجولة في حكم واحد.
     *
     * @param errors الأسباب العربية التي أخرجتها جولة المزامنة (فارغة ⇒ نجحت).
     * @param applied عدد ما طُبِّق من تغييرات الطرف الآخر في الجولة.
     */
    fun interpret(errors: List<String>, applied: Int): Outcome {
        if (errors.isEmpty()) return Outcome.Accepted(applied)
        val reason = errors.first().trim()
        if (reason.isBlank()) return Outcome.Refused("رُفض الطلب بلا سبب مكتوب من الخادم")
        return classify(reason)
    }

    /**
     * التصنيف من نصّ السبب — لأن السبب يأتي من طبقة النقل مصوغًا بالعربية أصلًا.
     * والقاعدة: **الفشل المؤقّت يُعاد، والرفض الدائم يُصلَح**.
     */
    fun classify(reasonArabic: String): Outcome {
        val text = reasonArabic.trim()
        val transient = listOf(
            "تعذّر الوصول", "تعذر الوصول", "لا شبكة", "انقطعت", "مهلة",
            "الخادم", "مؤقّت", "أعد المحاولة", "غير متاح", "بطيء"
        )
        if (transient.any { text.contains(it) }) return Outcome.Unreachable(text)
        val permanent = listOf("مرفوض", "غير صالح", "401", "403", "422", "409", "صيغة", "غير مسموح", "رمز")
        if (permanent.any { text.contains(it) }) return Outcome.Refused(text)
        // سبب غير مصنَّف: لا نخترع حكمًا. نعتبره مؤقّتًا حتى يُقال غيره، لأن إعادة المحاولة أهون من
        // إقناع المستخدم بأن رمزه الصحيح خاطئ.
        return Outcome.Unreachable(text)
    }

    /** الشكل المعروض: جملة واحدة + ما العمل، بلا مصطلحات تقنية. */
    fun message(outcome: Outcome): String = when (outcome) {
        is Outcome.Accepted ->
            if (outcome.applied > 0) {
                "الرمز صحيح ✓ — ووصلت ${outcome.applied} حركة من الطرف الآخر"
            } else {
                "الرمز صحيح ✓ — والقناة تعمل"
            }
        is Outcome.Refused -> "الرمز مرفوض: ${outcome.reason} • تحقّق من الرمز مع المالك، فالرمز لا يُخمَّن."
        is Outcome.Unreachable -> "تعذّر الوصول إلى الخادم: ${outcome.reason} • الرمز لم يُرفض؛ أعد المحاولة عند توفّر الشبكة."
    }

    /** هل يصحّ الحفظ؟ (فقط عند القبول — ولا نجاح كاذب.) */
    fun shouldSave(outcome: Outcome): Boolean = outcome is Outcome.Accepted

    /**
     * بوابة إعادة المحاولة: الرفض الدائم لا يُعاد تلقائيًّا، والمؤقّت يُعاد.
     * (نفس قاعدة العقد: ٤٠١/٤٠٣/٤٠٩/٤٢٢ ليست «حاول مرّة أخرى».)
     */
    fun shouldRetry(outcome: Outcome): Boolean = outcome is Outcome.Unreachable
}
