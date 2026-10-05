package com.baynana.core.sync

import android.content.Context
import java.util.UUID

/**
 * إعداد قناة المزامنة (ح٢٢): من أين يكلّم التطبيق الخادم، وبأي هوية جهاز.
 *
 * **ثلاثة قرارات مقصودة، كلّ منها يمنع عطلًا حقيقيًّا:**
 *
 * ١. **العنوان من أصل بنائي (`sync_endpoint.txt`) لا من إعداد يحرّره المستخدم.** تغيير العنوان
 *    يعني أن دفاتر الناس تُرسل إلى خادم آخر؛ فمن يمسك الجهاز دقيقةً لا يجوز أن يحوّل بيانات
 *    العائلة إلى خادمه. والعنوان يُغيَّر ببناء جديد، مثل قناة الإصدار (ح٢٠).
 *
 * ٢. **`https` شرط، ولا استثناء.** التطبيق يعلن `usesCleartextTraffic="false"`، فلا يُبنى في هذه
 *    الطبقة بابٌ خلفي يتيح النصّ المكشوف «للتجربة». من أراد تجربة الخادم المحلي وضع أمامه شهادة
 *    TLS، أو جرّب العقد بلا شبكة عبر متجهات العقد (`sync_contract_test.mjs`).
 *
 * ٣. **هوية الجهاز محلية**: `deviceId` عشوائي يُولَّد مرة ويُحفظ (ليس سرًّا، بل اسم يميّز الأجهزة
 *    في السجلّ)، ورمز الجهاز (`token`) سرّ يُخزَّن في ملفّ الإعدادات الخاصّ ولا يُطبع ولا يُسجَّل
 *    أبدًا — ولا يوجد واجهة تعرضه للمستخدم.
 *
 * **الغياب ليس عطلًا:** لا ملفّ أو ملفّ فارغ ⇒ `null` ⇒ لا قناة، ويعمل التطبيق محليًّا كما وعدنا
 * دائمًا، ويقول للمستخدم «لم تُضبط قناة مزامنة بعد» بلا أن يُفسد شيئًا.
 */
object SyncConfig {

    /** أصل بنائي فيه عنوان الخادم، سطر واحد. */
    const val ENDPOINT_ASSET = "sync_endpoint.txt"

    /** مسار العقد في الخادم (نفس المسار في الخادم المرجعي وفي أي تنفيذ لاحق). */
    const val ENDPOINT_PATH = "/api/v1/changes"

    /** ملفّ إعدادات الجهاز: رمز الجهاز ومعرّفه، لا شيء غيرهما. */
    const val PREFS_FILE = "baynana_sync_prefs"
    const val KEY_DEVICE_TOKEN = "sync_device_token"
    const val KEY_DEVICE_ID = "sync_device_id"

    /**
     * يفكّ قيمة الأصل إلى عنوان أساسي بلا شرطة أخيرة، أو `null` إن كانت فارغة أو غير مؤهَّلة.
     *
     * يُقبل https فقط. والقيمة المكرَّرة للمسار (`.../api/v1/changes`) تُقلَّم: فلا ينتهي العنوان
     * بـ`/api/v1/changes/api/v1/changes` إن كتب المالك المسار كاملًا (خطأ سهل وسهل منعه).
     */
    fun baseUrl(assetValue: String?): String? {
        val first = assetValue
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
            ?: return null
        if (!first.startsWith("https://")) return null
        var base = first.trimEnd('/')
        if (base.endsWith(ENDPOINT_PATH)) base = base.removeSuffix(ENDPOINT_PATH).trimEnd('/')
        // مضيف بلا اسم أو عنوان بمسافات: مرفوض، والصمت هنا أهون من طلب يسقط بلا سبب مفهوم.
        val host = base.removePrefix("https://").substringBefore('/').substringBefore(':')
        if (host.isBlank() || host.contains(' ') || !host.contains('.')) return null
        return base
    }

    /** العنوان الكامل للعقد، أو `null` إن لم تُضبط قناة. */
    fun changesUrl(assetValue: String?): String? = baseUrl(assetValue)?.plus(ENDPOINT_PATH)

    /** عنوان الخادم من الأصل البنائي. */
    fun changesUrl(context: Context): String? = changesUrl(readAsset(context, ENDPOINT_ASSET))

    /** معرّف الجهاز: يُولَّد مرة ويبقى. لا يُرسل سرًّا ولا يُعرض في شاشة. */
    fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.getString(KEY_DEVICE_ID, null)?.let { if (it.isNotBlank()) return it }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    /**
     * رمز الجهاز، أو `null` إن لم يُضبط. لا توليد تلقائي: الرمز يأتي من المالك عند تنصيب القناة،
     * ومحاولة الاتصال بلا رمز تُرفض من الخادم فتُقال للمستخدم بصراحة.
     */
    fun deviceToken(context: Context): String? {
        val value = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_DEVICE_TOKEN, null)
            ?.trim()
        return if (value.isNullOrBlank()) null else value
    }

    fun isConfigured(context: Context): Boolean =
        changesUrl(context) != null && deviceToken(context) != null

    /** قراءة أصل نصّي بلا استثناء: الغياب غياب قناة، لا عطل. */
    internal fun readAsset(context: Context, name: String): String? = runCatching {
        context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()
}
