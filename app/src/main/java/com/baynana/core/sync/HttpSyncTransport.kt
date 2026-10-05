package com.baynana.core.sync

import com.baynana.domain.sync.OutboxEnvelope
import com.baynana.domain.sync.PullPage
import com.baynana.domain.sync.PushOutcome
import com.baynana.domain.sync.SyncWire
import com.baynana.domain.sync.TransportPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * منفّذ النقل عبر الشبكة (ح٢٢): ينفّذ [TransportPort] على عقد `v1` نفسه الذي ينفّذه الخادم المرجعي
 * (`server/baynana-sync-server.mjs`).
 *
 * **قواعد صريحة، ومقابلها عطل متوقّع:**
 *
 * ١. **لا يُعلن نجاح لم يُقرأ ردّه**: كل ما ليس `200` يُترجم إلى خطأ نقل مؤقّت لكل عناصر الدفعة،
 *    فيُعاد الإرسال لاحقًا بموعد أسّي. أما الردّ 200 فيُفكّ بـ[SyncWire.parsePushResponse] الذي
 *    يوزّع النتائج بالمكان ويُبقي الناقص مؤقّتًا.
 *
 * ٢. **الرفض الدائم (401/403/409/422) لا يُعاد صامتًا** إلى ما لا نهاية: يُترجم إلى
 *    `Rejected(retryable = false)` برسالة عربية، فيظهر في الشاشة «يحتاج تدخلًا» بسبب مفهوم، بدل
 *    إعادةٍ أبدية تستهلك البطارية وتخفي المشكلة. ودفعة كبيرة (413) مؤقّتة لأن الحلّ تقسيمها.
 *
 * ٣. **انقطاع الشبكة ليس رفضًا**: `IOException` ⇒ كل العناصر خطأ نقل، ولا يُلمس شيء في القاعدة.
 *    وهذا هو الفرق بين «لم يصل» و«رُفض»، وهو فرق يهمّ صاحب الدفتر.
 *
 * ٤. **الرمز سرّ لا يُسجَّل ولا يظهر في رسالة**: يُرسل في الرأس فقط، ورسائل الخطأ لا تُدرج نصّ
 *    الاستثناء ولا الرأس.
 *
 * ٥. **لا إعادة محاولة داخل الطبقة**: الإعادة من العامل بسياسة أسّية (`SyncScheduler`)، فطبقة
 *    واحدة تتولّى الزمن — لا مؤقّتان يتصارعان.
 *
 * ٦. **إعادة الإرسال مأمونة**: الخادم يعرف `operationId`، فإعادة دفعة كاملة بعد انقطاع لا تُنشئ
 *    قيدًا ثانيًا (منع التكرار في العقد، لا في العميل — فلا يضيع شيء إن تعطّل العميل في المنتصف).
 *
 * ٧. **التحويلات لا تُتبع**: `instanceFollowRedirects = false`، فلا يُسلَّم رمز الجهاز لخادم آخر
 *    عبر إعادة توجيه.
 */
class HttpSyncTransport(
    private val changesUrl: String,
    private val deviceId: String,
    private val token: String,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 20_000,
    /** منفّذ الطلب بديل للاختبار: لا شبكة في اختبارات الوحدة. */
    private val exchange: SyncExchange = DefaultSyncExchange,
    private val pullLimit: Int = 50
) : TransportPort {

    override suspend fun push(envelopes: List<OutboxEnvelope>): List<PushOutcome> {
        if (envelopes.isEmpty()) return emptyList()
        val body = SyncWire.pushRequestBody(deviceId, envelopes).toByteArray(Charsets.UTF_8)
        val result = send(
            connection = open("POST").apply { setRequestProperty("Content-Type", "application/json; charset=utf-8") },
            body = body
        )
        return when (result) {
            is SendResult.Failed -> envelopes.map { PushOutcome.TransportError(result.reason) }
            is SendResult.Read -> when {
                result.status == 200 -> SyncWire.parsePushResponse(result.body, envelopes)
                else -> {
                    val refusal = permanentRefusal(result.status)
                    if (refusal == null) {
                        envelopes.map { PushOutcome.TransportError(transientReason(result.status)) }
                    } else {
                        envelopes.map { PushOutcome.Rejected(retryable = false, reason = refusal) }
                    }
                }
            }
        }
    }

    override suspend fun pull(cursor: String?): PullPage {
        val url = SyncWire.pullUrl(changesUrl.removeSuffix(SyncWire.PATH_CHANGES), cursor, pullLimit)
        val result = send(open("GET", url), body = null)
        val unchanged = PullPage(emptyList(), cursor.orEmpty(), hasMore = false)
        return when (result) {
            // تعذّر السحب لا يمحو تقدّمًا: نُعيد المؤشر كما هو، والقاعدة المحلية لم تُلمس، والعامل
            // يعيد المحاولة لاحقًا. ولا ندّعي أننا قرأنا شيئًا.
            is SendResult.Failed -> unchanged
            is SendResult.Read -> if (result.status != 200) unchanged
            else SyncWire.parsePullPage(result.body) ?: unchanged
        }
    }

    private fun open(method: String, url: String = changesUrl): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Baynana-Android")
        return connection
    }

    /** تنفيذ الطلب، وكل استثناء يصير خطأ نقل بجملة عربية واحدة بلا تفاصيل داخلية. */
    private suspend fun send(connection: HttpURLConnection, body: ByteArray?): SendResult =
        withContext(Dispatchers.IO) {
            try {
                val result = exchange.perform(connection, body)
                SendResult.Read(status = result.status, body = result.body)
            } catch (io: IOException) {
                SendResult.Failed("تعذّر الوصول إلى الخادم: تحقّق من الشبكة")
            } catch (other: Exception) {
                // لا يُدرج نصّ الاستثناء: قد يحمل تفاصيل الشبكة أو الرأس، ولا يفهمها المستخدم.
                SendResult.Failed("تعذّر إتمام المزامنة، وسيُعاد المحاولة")
            } finally {
                connection.disconnect()
            }
        }

    /** رفض دائم بجملة عربية، أو `null` إن كان الخطأ مؤقّتًا يُعاد. */
    private fun permanentRefusal(status: Int): String? = when (status) {
        401, 403 -> "رمز الجهاز مرفوض — راجع ضبط قناة المزامنة"
        409 -> "تعارض في المزامنة لا يُحلّ بالإعادة"
        413 -> null // دفعة كبيرة: تُقسَّم وتُعاد، فهو مؤقّت
        422 -> "الخادم رفض صيغة الإرسال — حدّث التطبيق"
        else -> null
    }

    private fun transientReason(status: Int): String = when (status) {
        in 500..599 -> "الخادم غير متاح مؤقتًا (${status})"
        else -> "ردّ غير متوقّع من الخادم (${status})"
    }

    private sealed interface SendResult {
        data class Read(val status: Int, val body: String) : SendResult
        data class Failed(val reason: String) : SendResult
    }
}

/** نتيجة طلب HTTP خام: ما يعود من الشبكة بلا تفسير. */
data class SyncExchangeResult(val status: Int, val body: String)

/** نقطة الاستبدال الواحدة: تُكتب الطلب (إن كان له جسم) وتُقرأ الاستجابة. */
fun interface SyncExchange {
    fun perform(connection: HttpURLConnection, body: ByteArray?): SyncExchangeResult
}

/** التنفيذ الفعلي: كتابة الجسم، ثم قراءة الاستجابة بسقف حجم. */
object DefaultSyncExchange : SyncExchange {
    override fun perform(connection: HttpURLConnection, body: ByteArray?): SyncExchangeResult {
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body) }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val content = CharArray(MAX_BODY_CHARS)
            var filled = 0
            while (filled < content.size) {
                val read = reader.read(content, filled, content.size - filled)
                if (read < 0) break
                filled += read
            }
            String(content, 0, filled)
        }.orEmpty()
        // سقف حجم: صفحة ضخمة لا تُحوَّل إلى استهلاك ذاكرة على هاتف.
        return SyncExchangeResult(status, if (text.length > MAX_BODY_CHARS) text.take(MAX_BODY_CHARS) else text)
    }

    const val MAX_BODY_CHARS = 2 * 1024 * 1024
}
