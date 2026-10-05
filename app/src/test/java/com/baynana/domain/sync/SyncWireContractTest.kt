package com.baynana.domain.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * عقد المزامنة `v1` (ح٢٢): **التطبيق مقابل الخادم المرجعي الحقيقي**.
 *
 * المتجهات في [SyncWireVectors] ليست مكتوبة بيد: هي ردّ الخادم نفسه كما وُلّد بـ
 * `node tools/sync_contract_test.mjs --write`، وهذا الملفّ يفكّها بالدوال التي يستعملها الإنتاج.
 * فإن غيّر أحد شكل حقل في الخادم (أو نوع الزمن، أو ترتيب النتائج) سقط هذا الاختبار قبل أن يسقط
 * الاتصال في يد مستخدم.
 *
 * ويُفهم من هذا الاختبار أمر يُسأل عنه كثيرًا: «هل نسخة قديمة تعمل بعد ظهور نسخة جديدة؟» الجواب
 * هنا لا في وعد: الردّ يُفكَّك بمقاس ثابت، والحالة التي لا نعرفها لا تُعلن نجاحًا، والصفحة الفارغة
 * لا تُصفّر تقدّمًا، والردّ الناقص لا يُقبل ناقصه.
 *
 * (يحتاج Robolectric لأن `org.json` هو تنفيذ أندرويد — وهو نفس ما يشتغل في الإنتاج.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncWireContractTest {

    private val wirePayload =
        """{"id":"entry-1","roomId":"room-water-1","operationId":"op-entry-1","type":"WATER_SESSION","owedByMemberId":"counterpart-room-water-1","owedToMemberId":"me","amountMinor":"1500000","currency":"YER_NEW","occurredAt":1767225596000}"""

    private fun envelope(operationId: String = "op-entry-1") = OutboxEnvelope(
        operationId = operationId,
        entityType = "entry",
        entityId = "entry-1",
        action = SyncWire.ACTION_UPSERT,
        payload = if (operationId == "op-entry-1") wirePayload else """{"id":"entry-9"}""",
        createdAt = 1_767_225_596_000,
        attempts = 0
    )

    /**
     * مقارنة **دلالية** لا نصّية: ترتيب المفاتيح في JSONObject ليس جزءًا من العقد، فلا يُبنى فحص
     * على ترتيب قد يتغيّر بين تنفيذ وآخر. المقارنة على: نفس المفاتيح، ونفس القيم، ونفس الطول.
     */
    private fun assertSamePushRequest(expected: String, actual: String) {
        val expectedRoot = JSONObject(expected)
        val actualRoot = JSONObject(actual)
        assertEquals(
            "حقول الطلب تغيّرت",
            expectedRoot.keys().asSequence().toSet(),
            actualRoot.keys().asSequence().toSet()
        )
        assertEquals(expectedRoot.getString("deviceId"), actualRoot.getString("deviceId"))
        val expectedItems = expectedRoot.getJSONArray("items")
        val actualItems = actualRoot.getJSONArray("items")
        assertEquals("عدد العناصر", expectedItems.length(), actualItems.length())
        for (index in 0 until expectedItems.length()) {
            val expectedItem = expectedItems.getJSONObject(index)
            val actualItem = actualItems.getJSONObject(index)
            assertEquals(
                "حقول العنصر رقم $index تغيّرت — أي حقل زائد أو ناقص يكسر خادمًا قديمًا",
                expectedItem.keys().asSequence().toSet(),
                actualItem.keys().asSequence().toSet()
            )
            for (key in expectedItem.keys()) {
                assertEquals("العنصر $index، الحقل $key", expectedItem.get(key).toString(), actualItem.get(key).toString())
            }
        }
    }

    /** الطلب الذي يبنيه التطبيق = الطلب الذي بنى به الخادم متجهه (نفس الحقول ونفس القيم). */
    @Test
    fun `الطلب المولَّد من التطبيق يطابق ما بنى به الخادم متجهه`() {
        assertSamePushRequest(
            SyncWireVectors.PUSH_REQUEST,
            SyncWire.pushRequestBody("device-a", listOf(envelope()))
        )
    }

    @Test
    fun `ردّ الخادم يفكّ بترتيب الطلب ومعه الرفض الدائم بسببه`() {
        val outcomes = SyncWire.parsePushResponse(
            SyncWireVectors.PUSH_RESPONSE,
            listOf(envelope(), envelope(), envelope("op-bad-money"))
        )

        assertEquals(3, outcomes.size)
        assertEquals(PushOutcome.Accepted, outcomes[0])
        assertEquals("المكرر مقبول ولا يُعلن خطأ", PushOutcome.Accepted, outcomes[1])

        val rejected = outcomes[2] as PushOutcome.Rejected
        assertFalse("المبلغ الرقمي رفض دائم لا يُعاد صامتًا", rejected.retryable)
        assertTrue("السبب بالعربية ومفهوم", rejected.reason.contains("الوحدة الصغرى"))
    }

    @Test
    fun `صفحة السحب المحدودة تُقرأ مع مؤشرها وعلم المزيد`() {
        val page = SyncWire.parsePullPage(SyncWireVectors.PULL_PAGE_LIMITED)
        assertNotNull(page)
        assertEquals(2, page!!.changes.size)
        assertTrue(page.hasMore)
        assertEquals("UPSERT", page.changes.first().kind)
        assertEquals("entry-1", page.changes.first().entityId)
        assertEquals("الزمن من الخادم بالمللي ثانية كعدد", 1_767_225_600_000L, page.changes.first().serverTime)
        assertEquals("c:Mg", page.nextCursor)
    }

    @Test
    fun `الصفحة بعد المؤشر لا تُعيد ما قُرئ`() {
        val page = SyncWire.parsePullPage(SyncWireVectors.PULL_PAGE_AFTER_CURSOR)!!
        assertEquals(listOf("entry-3"), page.changes.map { it.entityId })
        assertFalse(page.hasMore)
    }

    @Test
    fun `الصفحة الفارغة تبقى فارغة بلا انفجار ومع مؤشرها`() {
        val page = SyncWire.parsePullPage(SyncWireVectors.PULL_EMPTY)!!
        assertTrue(page.changes.isEmpty())
        assertFalse(page.hasMore)
        assertTrue(page.nextCursor.isNotBlank())
    }

    @Test
    fun `ردّ مشوّه لا يُعلن نجاحًا لأي عنصر`() {
        val outcomes = SyncWire.parsePushResponse(
            SyncWireVectors.PUSH_RESPONSE_GARBAGE,
            listOf(envelope(), envelope("op-2"))
        )
        assertEquals(2, outcomes.size)
        outcomes.forEach { assertTrue("مشوّه ⇒ خطأ نقل مؤقّت: $it", it is PushOutcome.TransportError) }
    }

    @Test
    fun `ردّ ناقص النتائج لا يُعلن نجاح الناقص`() {
        val outcomes = SyncWire.parsePushResponse(
            SyncWireVectors.PUSH_RESPONSE_SHORT,
            listOf(envelope(), envelope("op-2"))
        )
        assertEquals(PushOutcome.Accepted, outcomes[0])
        assertTrue("العنصر الذي لم يُقرأ ردّه لا يُعدّ مُرسلًا", outcomes[1] is PushOutcome.TransportError)
    }

    @Test
    fun `حالة غير معروفة من خادم أحدث لا تُعلن نجاحًا`() {
        val body = """{"outcomes":[{"operationId":"op-1","status":"QUARANTINED","retryable":true,"reason":"حجر"}]}"""
        val outcomes = SyncWire.parsePushResponse(body, listOf(envelope()))
        assertTrue(outcomes[0] is PushOutcome.TransportError)
    }

    @Test
    fun `حالة مرفوضة بلا سبب تُوصف ولا تظهر فارغة`() {
        val body = """{"outcomes":[{"operationId":"op-1","status":"REJECTED","retryable":false}]}"""
        val outcomes = SyncWire.parsePushResponse(body, listOf(envelope()))
        val rejected = outcomes[0] as PushOutcome.Rejected
        assertTrue("لا سبب فارغ في واجهة عربية", rejected.reason.isNotBlank())
    }

    @Test
    fun `عنوان السحب يمرّر المؤشر مُرمَّزًا ومعه الحد`() {
        assertEquals(
            "https://sync.example.com/api/v1/changes?cursor=c%3AMg&limit=50",
            SyncWire.pullUrl("https://sync.example.com", "c:Mg", 50)
        )
        assertEquals(
            "بلا مؤشر لا تُضاف معلمة فارغة",
            "https://sync.example.com/api/v1/changes?limit=20",
            SyncWire.pullUrl("https://sync.example.com/", null, 20)
        )
    }

    @Test
    fun `زمن نصّي يُقرأ احتياطًا لمن يخزّنه نصًّا`() {
        val body =
            """{"changes":[{"kind":"UPSERT","entityType":"entry","entityId":"e1","operationId":"op-1","payload":"{}","serverTime":"1767225600000"}],"nextCursor":"c:MQ","hasMore":false}"""
        assertEquals(1_767_225_600_000L, SyncWire.parsePullPage(body)!!.changes[0].serverTime)
    }

    @Test
    fun `عنصر بلا معرّف يُسقط من الصفحة بدل أن يُطبَّق على كيان مجهول`() {
        val body =
            """{"changes":[{"kind":"UPSERT","entityType":"entry","entityId":"","operationId":"op-1","payload":"{}","serverTime":1}],"nextCursor":"c:MQ","hasMore":false}"""
        assertTrue(SyncWire.parsePullPage(body)!!.changes.isEmpty())
    }

    @Test
    fun `جسم لا JSON يُعطي صفحة غير مقروءة بلا استثناء`() {
        assertNull(SyncWire.parsePullPage("<html>502</html>"))
        assertNull(SyncWire.parsePullPage(""))
    }
}
