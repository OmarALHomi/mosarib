package com.baynana.data.local.profile

import com.baynana.features.settings.AppSetting
import com.baynana.features.settings.AppSettingDao

/**
 * اسم صاحب الجهاز: مصدر واحد لكل اسم يخرج من هذا الجهاز إلى طرف آخر.
 *
 * وُلدت هذه القاعدة من عطب حقيقي: كان الاسم المحلي «أنا» — وهو علامة تعني «صاحب هذا الجهاز» —
 * يُخزَّن في صفّ العضو ويُرسل حرفيًّا في دعوة الغرفة، فيقرأ الطرف الآخر «من: أنا» ولا يعرف من
 * يدعوه. العلامة المحلية لا تُرسل ولا تُعرض لغير صاحبها؛ وإن غاب الاسم الصالح فالواجهة تقول
 * صراحةً «طرف لم يُسمِّ نفسه» بدل أن تَكذب على القارئ.
 */
object LocalProfile {
    /** مفتاح الإعداد نفسه الذي يحرّره المستخدم من الشاشة، فلا اسمان لشخص واحد. */
    const val NAME_KEY = "distributor_name"

    /** علامة محلية تعني «أنا» على جهاز صاحبها فقط؛ لا تصلح للإرسال ولا للعرض على غير صاحبها. */
    const val PLACEHOLDER = "أنا"

    /** ما يُعرض لطرف آخر حين لا يكون الاسم معروفًا أو صالحًا. */
    const val UNKNOWN_LABEL = "طرف لم يُسمِّ نفسه"

    /** سقف بسيط يمنع أن يصير الاسم رسالة في وسط كشف حساب. */
    const val MAX_LENGTH = 40

    /** تنظيف واحد للاسم: بلا أسطر جديدة ولا فراغات طرفية. */
    fun clean(raw: String?): String =
        raw?.replace('\n', ' ')?.replace('\r', ' ')?.trim().orEmpty()

    /** اسم يصلح أن يخرج من الجهاز: معرَّف، ليس العلامة المحلية، وفي حدود الطول. */
    fun isSendable(raw: String?): Boolean {
        val name = clean(raw)
        return name.isNotEmpty() && name != PLACEHOLDER && name.length <= MAX_LENGTH
    }

    /** الاسم كما يُرسل: يُفضَّل فراغ صريح على علامة مضلِّلة. */
    fun wireName(raw: String?): String = if (isSendable(raw)) clean(raw) else ""

    /** الاسم كما يُعرض لطرف آخر: لا تظهر العلامة المحلية أبدًا. */
    fun peerLabel(raw: String?): String = wireName(raw).ifBlank { UNKNOWN_LABEL }

    /** اسم صاحب الجهاز إن كان صالحًا؛ وإلا `null` (ندَع الاسم يَغيب بدل أن نُكذّب). */
    suspend fun read(dao: AppSettingDao): String? =
        clean(dao.getSettingValue(NAME_KEY)).takeIf { isSendable(it) }

    /**
     * يُخزَّن الاسم الصالح فقط، والرفض صريح (`false`) ليلتقطه الاختبار بدل أن يمرّ صامتًا.
     * الاسم يُستعمل للإرسال والعرض معًا، فلا يوجد في التطبيق اسمان لصاحب الجهاز.
     */
    suspend fun save(dao: AppSettingDao, raw: String): Boolean {
        val name = clean(raw)
        if (!isSendable(name)) return false
        dao.saveSetting(AppSetting(NAME_KEY, name))
        return true
    }
}
