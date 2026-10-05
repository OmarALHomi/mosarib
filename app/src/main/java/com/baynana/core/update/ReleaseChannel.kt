package com.baynana.core.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * قناة الإصدار (ح٢٠): من أين يُقرأ ملفّ الإصدار، وما يُقال حين لا يُقرأ.
 *
 * **الرابط مثبَّت في التطبيق لا في إعداد يحرّره المستخدم.** لو صار الرابط قابلًا للتحرير من
 * الشاشة، لأمكن إقناع صاحب الجهاز بأن «قناة التحديث الرسمية» صارت عنوانًا آخر، فينزل ملفًّا
 * موقّعًا بمفتاح غيره — والتوقيع لا يحمي من اختيار مصدر مختلف. فيُقرأ العنوان من أصل بنائي
 * (`release_endpoint.txt`) يُغيَّر ببناء جديد، ويبقى المسار `/api/v1/app-release` نفسه: من ملفّ
 * ساكن على استضافة مجانية الآن، وإلى الخادم الخاص لاحقًا (ح٢٢) **بلا تعديل في التطبيق**.
 *
 * **ولا شيء يبدأ وحده:** هذه الطبقة تقرأ فقط. التنزيل قرار مستخدم في `ReleaseRepository`.
 */
object ReleaseChannel {

    /** العنوان الأساسي، سطر واحد في أصل بنائي (قالب فارغ في المستودع). */
    const val ENDPOINT_ASSET = "release_endpoint.txt"

    /** المسار الثابت للعقد: هو نفسه على الملفّ الساكن وعلى الخادم الخاص. */
    const val ENDPOINT_PATH = "/api/v1/app-release"

    /** سقف حجم ملفّ الإصدار: نصّ صغير؛ تجاوزه يعني أن المستضيف ردّ صفحة خطأ. */
    const val MAX_BODY_BYTES = 64 * 1024

    /** يبني العنوان الكامل من العنوان الأساسي، ويرفض ما ليس https. */
    fun endpoint(assetValue: String?): String? {
        val base = assetValue
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
            ?: return null
        if (!base.startsWith("https://")) return null
        val trimmed = base.trimEnd('/')
        return if (trimmed.endsWith(ENDPOINT_PATH)) trimmed else trimmed + ENDPOINT_PATH
    }

    fun endpoint(context: Context): String? = endpoint(ReleaseSignatures.readAsset(context, ENDPOINT_ASSET))
}

/** نتيجة قراءة واحدة. الفصل بين النجاح والتعذّر مقصود: لكل تعذّر رسالة عربية تُفهم. */
sealed interface ReleaseFetch {
    data class Body(val text: String) : ReleaseFetch
    data class Failed(val reasonArabic: String) : ReleaseFetch
}

/** مصدر ملفّ الإصدار. واجهة، لأن الاختبارات تُمرّر مصدرًا محليًّا بلا شبكة. */
fun interface ReleaseSource {
    suspend fun fetch(): ReleaseFetch
}

/**
 * القراءة عبر الشبكة. ترفض ما ليس https، وتقرأ بسقف حجم، وتُحوّل كل فشل إلى جملة عربية واحدة —
 * لأن المستخدم لا يفهم `SSLHandshakeException`، ولا يجب أن يراها.
 */
class HttpReleaseSource(
    private val url: String,
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 8_000,
    private val maxBytes: Int = ReleaseChannel.MAX_BODY_BYTES,
    private val online: () -> Boolean = { true }
) : ReleaseSource {

    override suspend fun fetch(): ReleaseFetch = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) return@withContext ReleaseFetch.Failed("عنوان قناة التحديث ليس مشفّرًا (https)")
        if (!online()) return@withContext ReleaseFetch.Failed("لا إنترنت الآن")

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Accept", "text/plain")
                instanceFollowRedirects = false
            }
            val code = connection.responseCode
            if (code != 200) {
                return@withContext ReleaseFetch.Failed("قناة التحديث ردّت بحالة $code")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(4096)
                val text = StringBuilder()
                while (true) {
                    val read = reader.read(buffer)
                    if (read <= 0) break
                    text.append(buffer, 0, read)
                    if (text.length > maxBytes) return@withContext ReleaseFetch.Failed("ملفّ الإصدار أكبر من المتوقع")
                }
                text.toString()
            }
            ReleaseFetch.Body(body)
        } catch (error: Exception) {
            ReleaseFetch.Failed("تعذّر الوصول إلى قناة التحديث: ${error.javaClass.simpleName}")
        } finally {
            runCatching { connection?.disconnect() }
        }
    }
}

/** مصدر محلي من الأصل `release_manifest.txt` — للمعاينة على جهاز بلا شبكة، ولاختبار الشاشة. */
class AssetReleaseSource(private val context: Context) : ReleaseSource {
    override suspend fun fetch(): ReleaseFetch {
        val text = ReleaseSignatures.readAsset(context, "release_manifest.txt")
        return if (text.isNullOrBlank()) {
            ReleaseFetch.Failed("لا ملفّ إصدار محلي في هذه النسخة")
        } else {
            ReleaseFetch.Body(text)
        }
    }
}

/** هل هناك شبكة الآن؟ السؤال يجعل الشاشة تقول «لا إنترنت» فورًا بدل انتظار مهلة زمنية. */
object NetworkStatus {
    fun isOnline(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@runCatching false
        val network = manager.activeNetwork ?: return@runCatching false
        val capabilities = manager.getNetworkCapabilities(network) ?: return@runCatching false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)
}
