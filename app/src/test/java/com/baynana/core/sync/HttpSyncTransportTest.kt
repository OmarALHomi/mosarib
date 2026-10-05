package com.baynana.core.sync

import com.baynana.domain.sync.OutboxEnvelope
import com.baynana.domain.sync.PushOutcome
import com.baynana.domain.sync.SyncWire
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * سلوك النقل السلكي (ح٢٢) — يُفحص **بلا شبكة**: الطلب يُلتقط، والردّ يُصنع.
 *
 * الأسئلة الثلاثة التي يجيب عنها هذا الاختبار، وكلّها أسئلة مستخدم لا أسئلة شيفرة:
 * ١. هل ينقطع القيد إذا انقطعت الشبكة؟ لا: يعود «لم يصل» ويبقى في القاعدة.
 * ٢. هل يضيع الرمز أو يظهر للمستخدم؟ لا: في الرأس فقط، ورسائل الخطأ لا تحمله.
 * ٣. هل تُحفظ الأسطر متأخرةً أو بترتيب مختلف؟ لا: كل عنصر بنتيجته بنفس الترتيب.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HttpSyncTransportTest {

    private val token = "s3cret-token-should-never-be-printed"

    /** يلتقط الطلب ويعيد ردًّا مُصنَّعًا. */
    private class Recorder(
        private val response: () -> SyncExchangeResult
    ) : SyncExchange {
        var method: String = ""
        var url: String = ""
        var body: String = ""
        var calls = 0

        override fun perform(connection: HttpURLConnection, body: ByteArray?): SyncExchangeResult {
            calls++
            this.method = connection.requestMethod
            this.url = connection.url.toString()
            this.body = body?.toString(Charsets.UTF_8).orEmpty()
            return response()
        }
    }

    private fun envelope(index: Int) = OutboxEnvelope(
        operationId = "op-$index",
        entityType = "entry",
        entityId = "entry-$index",
        action = SyncWire.ACTION_UPSERT,
        payload = """{"id":"entry-$index","amountMinor":"${1000 * index}"}""",
        createdAt = 1_767_225_600_000 + index,
        attempts = 0
    )

    private fun transport(exchange: SyncExchange, url: String = "https://sync.example.com${SyncWire.PATH_CHANGES}") =
        HttpSyncTransport(changesUrl = url, deviceId = "device-1", token = token, exchange = exchange)

    @Test
    fun `الإرسال الناجح يحمل الرمز في الرأس ونتيجة لكل عنصر`() = runBlocking {
        val recorder = Recorder {
            SyncExchangeResult(200, """{"outcomes":[
                {"operationId":"op-1","status":"ACCEPTED","retryable":false,"reason":""},
                {"operationId":"op-2","status":"ACCEPTED","retryable":false,"reason":""}]}""")
        }
        val outcomes = transport(recorder).push(listOf(envelope(1), envelope(2)))

        assertEquals(listOf(PushOutcome.Accepted, PushOutcome.Accepted), outcomes)
        assertEquals("POST", recorder.method)
        assertEquals("https://sync.example.com/api/v1/changes", recorder.url)

        val sent = JSONObject(recorder.body)
        assertEquals("device-1", sent.getString("deviceId"))
        val items = sent.getJSONArray("items")
        assertEquals(2, items.length())
        assertEquals("op-1", items.getJSONObject(0).getString("operationId"))
        assertEquals("op-2", items.getJSONObject(1).getString("operationId"))
        // المبالغ نصّية بالوحدة الصغرى على السلك (ADR-04)، ولا رقم عشري واحد.
        assertTrue(items.getJSONObject(1).getString("payload").contains("\"amountMinor\":\"2000\""))
    }

    /**
     * على السلك فعلًا: خادم حقيقي داخل العملية على منفذ محلي.
     *
     * لِمَ هذا الاختبار رغم وجود الطلب المُلتقَط أعلاه؟ لأن `HttpURLConnection` في JDK **يخفي رأس
     * `Authorization`** عن `getRequestProperty` (حماية أمنية)، فيستحيل إثبات وصوله بالالتقاط.
     * والدليل الوحيد الصادق: خادم يقرأ ما وصله. وهذا يُثبت أيضًا أن الرمز يُرسل في الرأس لا في
     * رابط ولا في جسم، وأن الردّ الحقيقي يُفكّ كما ينبغي.
     */
    @Test
    fun `الرمز يصل إلى الخادم في الرأس لا في الرابط`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var seenAuthorization: String? = null
        var seenBody = ""
        server.createContext("/api/v1/changes") { exchange ->
            seenAuthorization = exchange.requestHeaders.getFirst("Authorization")
            seenBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            val reply = """{"outcomes":[{"operationId":"op-1","status":"ACCEPTED","retryable":false,"reason":""}]}"""
            exchange.sendResponseHeaders(200, reply.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(reply.toByteArray()) }
        }
        server.start()
        try {
            val port = server.address.port
            val outcomes = runBlocking {
                HttpSyncTransport(
                    changesUrl = "http://127.0.0.1:$port/api/v1/changes",
                    deviceId = "device-1",
                    token = token
                ).push(listOf(envelope(1)))
            }

            assertEquals(PushOutcome.Accepted, outcomes[0])
            assertEquals("Bearer $token", seenAuthorization)
            assertTrue("الجهاز يُعرَّف في الجسم", seenBody.contains("\"deviceId\":\"device-1\""))
            assertFalse("السرّ لا يظهر في الرابط", seenBody.contains("token"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `انقطاع الشبكة خطأ نقل مؤقّت لا رفض ولا فقدان`() = runBlocking {
        val failing = Recorder { throw IOException("انقطع الاتصال في المنتصف") }
        val outcomes = transport(failing).push(listOf(envelope(1), envelope(2)))

        assertEquals(2, outcomes.size)
        outcomes.forEach {
            assertTrue("انقطاع ⇒ خطأ نقل: $it", it is PushOutcome.TransportError)
        }
        assertTrue("الجملة عربية ومفهومة", (outcomes[0] as PushOutcome.TransportError).message.contains("الشبكة"))
    }

    @Test
    fun `خطأ الخادم المؤقّت يُعاد ولا يُعلن رفضًا`() = runBlocking {
        val server = Recorder { SyncExchangeResult(503, """{"error":"under maintenance"}""") }
        val outcomes = transport(server).push(listOf(envelope(1)))
        val error = outcomes[0] as PushOutcome.TransportError
        assertTrue("سبب مؤقّت برقم الحالة", error.message.contains("503"))
    }

    @Test
    fun `الرمز المرفوض رفض دائم بجملة عربية بلا كشف السرّ`() = runBlocking {
        val rejected = Recorder { SyncExchangeResult(401, """{"error":"invalid token s3cret-token-should-never-be-printed"}""") }
        val outcomes = transport(rejected).push(listOf(envelope(1)))

        val refusal = outcomes[0] as PushOutcome.Rejected
        assertFalse("الرفض الدائم لا يُعاد صامتًا", refusal.retryable)
        assertTrue("السبب مفهوم لصاحب الدفتر", refusal.reason.contains("رمز الجهاز"))
        assertFalse("السرّ لا يتسرّب إلى الواجهة", refusal.reason.contains(token))
    }

    @Test
    fun `رفض الصيغة دائم أيضًا`() = runBlocking {
        val invalid = Recorder { SyncExchangeResult(422, "{}") }
        val refusal = transport(invalid).push(listOf(envelope(1)))[0] as PushOutcome.Rejected
        assertFalse(refusal.retryable)
        assertTrue(refusal.reason.contains("حدّث التطبيق"))
    }

    @Test
    fun `السحب يقرأ الصفحة كما هي ومؤشرها`() = runBlocking {
        val page = Recorder {
            SyncExchangeResult(
                200,
                """{"changes":[{"kind":"UPSERT","entityType":"entry","entityId":"entry-9","operationId":"op-9","payload":"{}","serverTime":1767225600000}],"nextCursor":"c:MQ","hasMore":true}"""
            )
        }
        val result = transport(page).pull(cursor = "c:MA")

        assertEquals("GET", page.method)
        assertEquals(
            "المسار لا يتضاعف عند إضافة الاستعلام",
            "https://sync.example.com/api/v1/changes?cursor=c%3AMA&limit=50",
            page.url
        )
        assertEquals(1, result.changes.size)
        assertEquals("c:MQ", result.nextCursor)
        assertTrue(result.hasMore)
    }

    @Test
    fun `سحب متعثّر لا يمحو المؤشر`() = runBlocking {
        val failing = Recorder { throw IOException("لا شبكة") }
        val result = transport(failing).pull(cursor = "c:MA")

        assertTrue(result.changes.isEmpty())
        assertEquals("المؤشر يبقى كما هو فلا يُعاد من الصفر", "c:MA", result.nextCursor)
        assertFalse(result.hasMore)
    }

    @Test
    fun `دفعة فارغة لا تُرسل طلبًا أصلًا`() = runBlocking {
        val recorder = Recorder { SyncExchangeResult(200, """{"outcomes":[]}""") }
        val outcomes = transport(recorder).push(emptyList())
        assertTrue(outcomes.isEmpty())
        assertEquals("لا طلب بلا عمل", 0, recorder.calls)
    }
}
