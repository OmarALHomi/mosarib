package com.baynana.domain.sync

import org.json.JSONArray
import org.json.JSONObject

/**
 * صيغة السلك لعقد `v1` (ح٢٢) — نصًّا صريحًا، بلا شبكة وبلا أندرويد، كما هي عادة كل عقود المشروع.
 *
 * **الأثر الحقيقي لهذا الملفّ:** كل تنفيذ للنقل (Firestore اليوم، خادم المالك غدًا، وربما وسيط
 * آخر) يمرّ من هنا. فمعنى «نسخة قديمة تعمل بلا تعديل» يصير قابلًا للقياس: تغييرات العميل تُبنى
 * بهذه الدوال، وردّ الخادم يُفكّ بها، والاختبار يقارنها بمخرَج الخادم المرجعي الحقيقي.
 *
 * ثوابت تقابل أعطالًا حقيقية:
 * 1. **مقابلة النتائج بالمكان لا بالمعرّف** (ترتيب الطلب = ترتيب الردّ)، فإن قصّر الخادم الردّ
 *    يُكمَل بـ`TransportError` مؤقّت **ولا يُعلَن نجاح** — لا «أُرسل» كاذبًا لعنصر لم يُقرأ ردّه.
 * 2. **`retryable` يُفسَّر كما هو**: `false` تعني «لا تُعِد صامتًا» فيذهب العنصر إلى «يحتاج تدخلًا»
 *    برسالة عربية، و`true` تعني «أعد لاحقًا» فيبقى في الانتظار بموعد أسّي.
 * 3. **`serverTime` بالمللي ثانية كعدد** (نفس وحدة الزمن في التطبيق)، ويُقبل نصًّا احتياطًا فقط
 *    لأن Firestore يخزّن التواريخ نصًّا في بعض المسارات القديمة.
 * 4. **المؤشر نصّ يُنقل كما هو**: العميل لا يفترض أنه رقم ولا تاريخ، فلا ينكسر إن غيّر الخادم شكله.
 * 5. **`hasMore` صريح**: لا استنتاج نهاية الصفحة من عدد العناصر.
 */
object SyncWire {

    const val PATH_CHANGES = "/api/v1/changes"
    const val ACTION_UPSERT = "UPSERT"
    const val ACTION_VOID = "VOID"

    const val STATUS_ACCEPTED = "ACCEPTED"
    const val STATUS_REJECTED = "REJECTED"

    /** جسيم الإرسال: `{"deviceId": "...", "items": [ ... ]}`. */
    fun pushRequestBody(deviceId: String, envelopes: List<OutboxEnvelope>): String {
        val items = JSONArray()
        envelopes.forEach { envelope ->
            items.put(
                JSONObject()
                    .put("operationId", envelope.operationId)
                    .put("entityType", envelope.entityType)
                    .put("entityId", envelope.entityId)
                    .put("action", envelope.action)
                    // الحمولة نصّ JSON كما هي (بمبالغ نصّية بالوحدة الصغرى): الخادم لا يعيد تفسيرها.
                    .put("payload", envelope.payload)
                    .put("createdAt", envelope.createdAt)
            )
        }
        return JSONObject()
            .put("deviceId", deviceId)
            .put("items", items)
            .toString()
    }

    /**
     * يفكّ ردّ الإرسال إلى نتيجة لكل عنصر **بنفس ترتيب الطلب**.
     *
     * إن كان الردّ غير مقروء أصلًا: كل العناصر «خطأ نقل» (مؤقّت) — إعادة المحاولة أهون من إعلان
     * نجاح لم يقع. وإن نقص عدد النتائج: الناقص كذلك، ولا يُقبل ما لم يُقرأ ردّه.
     */
    fun parsePushResponse(body: String, requestOrder: List<OutboxEnvelope>): List<PushOutcome> {
        val outcomes = MutableList(requestOrder.size) {
            PushOutcome.TransportError("لم يُقرأ ردّ الخادم لكل عنصر") as PushOutcome
        }
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return outcomes
        val array = root.optJSONArray("outcomes") ?: return outcomes

        for (index in 0 until minOf(array.length(), requestOrder.size)) {
            val item = array.optJSONObject(index) ?: continue
            val status = item.optString("status", "").uppercase()
            val reason = item.optString("reason", "")
            outcomes[index] = when (status) {
                STATUS_ACCEPTED -> PushOutcome.Accepted
                STATUS_REJECTED -> PushOutcome.Rejected(
                    retryable = item.optBoolean("retryable", false),
                    reason = reason.ifBlank { "رُفض بلا سبب مذكور" }
                )
                // حالة لا نعرفها: لا يُعلن نجاحها، وتُعاد لاحقًا (قد يكون الخادم أحدث من التطبيق).
                else -> PushOutcome.TransportError("حالة غير معروفة من الخادم: ${status.take(16)}")
            }
        }
        return outcomes
    }

    /** جسيم السحب: `{"changes": [...], "nextCursor": "...", "hasMore": bool}`. */
    fun parsePullPage(body: String): PullPage? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val array = root.optJSONArray("changes") ?: return null
        val changes = ArrayList<RemoteChange>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val entityId = item.optString("entityId", "")
            val operationId = item.optString("operationId", "")
            if (entityId.isBlank() || operationId.isBlank()) continue
            changes += RemoteChange(
                kind = item.optString("kind", RemoteChange.UPSERT).uppercase(),
                entityType = item.optString("entityType", ""),
                entityId = entityId,
                operationId = operationId,
                payload = item.optString("payload", ""),
                serverTime = readMillis(item, "serverTime")
            )
        }
        // المؤشر القديم يبقى إن لم يُرسل الخادم جديدًا: لا نُصفّر التقدّم إلى الصفر بلا سبب.
        val nextCursor = root.optString("nextCursor", "")
        return PullPage(
            changes = changes,
            nextCursor = nextCursor,
            hasMore = root.optBoolean("hasMore", false)
        )
    }

    /** عنوان السحب: `cursor` يُمرَّر فقط إن وُجد، و`limit` داخل حدود الخادم. */
    fun pullUrl(baseUrl: String, cursor: String?, limit: Int): String {
        val root = baseUrl.trimEnd('/')
        val params = mutableListOf<String>()
        if (!cursor.isNullOrBlank()) {
            params += "cursor=" + java.net.URLEncoder.encode(cursor, "UTF-8").replace("+", "%20")
        }
        if (limit > 0) params += "limit=$limit"
        val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return "$root$PATH_CHANGES$query"
    }

    /** الزمن: عدد بالمللي ثانية، ويُقبل نصًّا لأن بعض المسارات القديمة تخزّنه نصًّا. */
    private fun readMillis(item: JSONObject, field: String): Long {
        val raw = item.opt(field)
        return when (raw) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull() ?: 0L
            else -> 0L
        }
    }
}
