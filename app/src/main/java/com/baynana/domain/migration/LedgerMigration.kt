package com.baynana.domain.migration

import org.json.JSONArray
import org.json.JSONObject
import java.util.TreeMap

/**
 * ملفّ الترحيل وجرده (ح٢٣): تصدير أحداث، واستيراد، و**مطابقة جرد بصفر فرق**.
 *
 * **المشكلة التي يحلّها هذا الملفّ:** نقل دفتر عائلة من جهاز إلى الخادم الخاص (أو من خادم إلى خادم)
 * لا يُقاس بعدد الملفّات، بل بأن يكون **كل رقم في الدفتر كما كان**. فالعبارة «انتقل الدفتر» بلا
 * جرد تعني أن يكتشف صاحب الدفتر بعد شهر أن دَينًا سقط في الطريق. ولهذا الصيغة هنا تحمل مع الأحداث
 * **جردًا كاملًا** يُقارَن بعده، والفرق يُقال بالعربية واحدًا واحدًا بدل «فشل الترحيل».
 *
 * **نفس قواعد الحساب**: الجرد يُبنى بنفس تصفية التطبيق ([ledgerEffectiveRule]): الملغى يُستثنى
 * (أثره سقط)، والقيد العكسي يُستثنى (وإلا حُسب الإلغاء مرتين)، والمسودة تُعدّ وحدها ولا تُنقل
 * ولا تُقارن (لم تُشارك). فلا يوجد محرّك ثانٍ يختلف يومًا عن شاشة الأرصدة.
 *
 * **الصيغة** (سطر واحد لكل سجل، JSONL): سطر ترويسة فيه الجرد، ثم سطر لكل حدث:
 * ```
 * {"kind":"header","format":"BAYNANA-MIGRATION-1","exportedAt":…,"deviceId":"…","inventory":{…}}
 * {"kind":"event","operationId":"…","entityType":"entry","entityId":"…","action":"UPSERT","payload":"…","createdAt":…}
 * ```
 * والترتيب ثابت (الزمن ثم المعرّف) لأن ملفًّا غير حتمي يجعل كل مقارنة تختلف بلا سبب حقيقي.
 *
 * **والاستيراد يمرّ من العقد نفسه** (`/api/v1/changes`): لا مسار استيراد خاص يكتب في القاعدة
 * مباشرة، لأن المسار الخاص هو أول ما ينحرف عن الفحوص ويمرّر قيدًا مشوّهًا. ومنع التكرار بالمُعرّف
 * يعني أن إعادة استيراد الملفّ نفسه لا تُضاعف شيئًا.
 */
object LedgerMigration {

    const val INVENTORY_FORMAT = "BAYNANA-INVENTORY-1"
    const val MIGRATION_FORMAT = "BAYNANA-MIGRATION-1"

    /** حدث ترحيل واحد: نفس شكل عنصر الإرسال في العقد `v1`. */
    data class Event(
        val operationId: String,
        val entityType: String,
        val entityId: String,
        val action: String,
        val payload: String,
        val createdAt: Long
    )

    /** قيد مقروء من الحمولة مع ترتيب تحديثه (للتفضيل عند تكرار المعرّف). */
    private data class Seen(val entry: JSONObject, val operationId: String, val updatedAt: Long)

    /** حصيلة نوع واحد داخل غرفة. */
    data class Tally(val count: Int, val sumMinor: Long)

    /** جرد غرفة: ما يُقارَن بعد الاستيراد. */
    data class RoomInventory(
        val roomId: String,
        val currency: String,
        val active: Int,
        val voided: Int,
        val reversals: Int,
        val byType: Map<String, Tally>,
        val netByMember: Map<String, Long>,
        val minOccurredAt: Long,
        val maxOccurredAt: Long,
        /** كل معرّفات العمليات في الغرفة (بما فيها الملغى): بها يُكتشف المفقود والزائد. */
        val operationIds: List<String>
    )

    /** جرد الدفتر كاملًا. [drafts] معلوماتي: مسودات محلية لا تُنقل ولا تُقارن. */
    data class Inventory(
        val rooms: List<RoomInventory>,
        val activeTotal: Int,
        val voidedTotal: Int,
        val reversalsTotal: Int,
        val drafts: Int,
        val operationIds: List<String>
    )

    /** نتيجة المقارنة: [differences] تفشّل الترحيل، و[notes] تُعلن ولا تفشّل. */
    data class Diff(val differences: List<String>, val notes: List<String>) {
        val isClean: Boolean get() = differences.isEmpty()
    }

    /** ملفّ مقروء: الترويسة وجرد المصدر وأحداثه. */
    data class File(
        val exportedAt: Long,
        val deviceId: String,
        val inventory: Inventory,
        val events: List<Event>
    )

    // ------------------------------------------------------------------ الجرد

    /**
     * يبني الجرد من الأحداث.
     *
     * - يُفكّ من كل حمولة كائن `entry` (وهو ما تحمله حمولات ENTRY وRECEIPT وVOID).
     * - **يُوحَّد بالمعرّف**: القيد نفسه قد يُصدَّر من صندوق الصادر ومن جدول القيود، فالمعرّف واحد
     *   والحمولة الأحدث هي المعتمدة (بـ`updatedAt`) — فلا يُحسب قيد مرتين.
     * - حمولات الخطط (`ALLOCATE`) بلا كائن `entry` فتُسقط من الجرد: الإسقاطات **مشتقّة** يُعيد كل
     *   جهاز حسابها بنفسه من القيود، ولا تُنقل كأرقام.
     */
    fun inventory(events: List<Event>): Inventory {
        val byOperation = LinkedHashMap<String, Seen>()

        events.forEach { event ->
            val payload = runCatching { JSONObject(event.payload) }.getOrNull() ?: return@forEach
            val entry = payload.optJSONObject("entry") ?: return@forEach
            val operationId = entry.optString("operationId", "").ifBlank { event.operationId }
            if (operationId.isBlank()) return@forEach
            val updatedAt = entry.optLong("updatedAt", 0L)
            val previous = byOperation[operationId]
            if (previous == null || updatedAt >= previous.updatedAt) {
                byOperation[operationId] = Seen(entry, operationId, updatedAt)
            }
        }

        val rooms = TreeMap<String, MutableList<Seen>>()
        byOperation.values.forEach { seen ->
            val roomId = seen.entry.optString("roomId", "")
            if (roomId.isBlank()) return@forEach
            rooms.getOrPut(roomId) { mutableListOf() }.add(seen)
        }

        val roomInventory = rooms.map { (roomId, entries) ->
            var active = 0
            var voided = 0
            var reversals = 0
            val byType = TreeMap<String, Tally>()
            val net = TreeMap<String, Long>()
            var minAt = Long.MAX_VALUE
            var maxAt = Long.MIN_VALUE
            var currency = ""

            entries.forEach { seen ->
                val entry = seen.entry
                val amount = seen.entry.optString("amountMinor", "").toLongOrNull() ?: 0L
                val status = entry.optString("status", "")
                val isReversal = entry.optString("reversesEntryId", "").isNotBlank()
                if (currency.isBlank()) currency = entry.optString("currency", "")
                val occurredAt = entry.optLong("occurredAt", 0L)
                if (occurredAt in 1 until minAt) minAt = occurredAt
                if (occurredAt > maxAt) maxAt = occurredAt

                when {
                    // المسودة لا تُشارك: تُعدّ وحدها ولا تدخل أي رقم يُقارَن.
                    status == STATUS_DRAFT -> Unit
                    status == STATUS_VOIDED -> voided++
                    isReversal -> reversals++
                    else -> {
                        active++
                        val type = entry.optString("type", "غير معروف")
                        val previous = byType[type] ?: Tally(0, 0L)
                        byType[type] = Tally(previous.count + 1, previous.sumMinor + amount)
                        val owedBy = entry.optString("owedByMemberId", "")
                        val owedTo = entry.optString("owedToMemberId", "")
                        // نفس قاعدة المحرّك: على الطرف الأوّل يُخصم، وللثاني يُضاف.
                        if (owedBy.isNotBlank()) net[owedBy] = (net[owedBy] ?: 0L) - amount
                        if (owedTo.isNotBlank()) net[owedTo] = (net[owedTo] ?: 0L) + amount
                    }
                }
            }

            RoomInventory(
                roomId = roomId,
                currency = currency,
                active = active,
                voided = voided,
                reversals = reversals,
                byType = byType,
                netByMember = net,
                minOccurredAt = if (minAt == Long.MAX_VALUE) 0L else minAt,
                maxOccurredAt = if (maxAt == Long.MIN_VALUE) 0L else maxAt,
                operationIds = entries.map { it.operationId }.sorted()
            )
        }.sortedBy { it.roomId }

        val totalDrafts = byOperation.values.count { it.entry.optString("status", "") == STATUS_DRAFT }
        return Inventory(
            rooms = roomInventory,
            activeTotal = roomInventory.sumOf { it.active },
            voidedTotal = roomInventory.sumOf { it.voided },
            reversalsTotal = roomInventory.sumOf { it.reversals },
            drafts = totalDrafts,
            operationIds = byOperation.keys.sorted()
        )
    }

    // ------------------------------------------------------------------ المقارنة

    /**
     * يقارن جردين ويردّ **كل فرق بجملة عربية واحدة**، بلا توقّف عند أول فرق: من ينقل دفترًا يريد
     * أن يعرف كل ما اختلّ في جلسة واحدة، لا أن يصلح فرقًا ليظهر التالي.
     */
    fun compare(source: Inventory, target: Inventory): Diff {
        val differences = mutableListOf<String>()
        val notes = mutableListOf<String>()

        val sourceRooms = source.rooms.associateBy { it.roomId }
        val targetRooms = target.rooms.associateBy { it.roomId }
        (targetRooms.keys - sourceRooms.keys).sorted().forEach {
            differences += "غرفة زائدة عند الوجهة: $it"
        }
        (sourceRooms.keys - targetRooms.keys).sorted().forEach {
            differences += "غرفة مفقودة من الوجهة: $it"
        }

        (sourceRooms.keys intersect targetRooms.keys).sorted().forEach { roomId ->
            val a = sourceRooms.getValue(roomId)
            val b = targetRooms.getValue(roomId)
            if (a.currency != b.currency) differences += "غرفة $roomId: العملة مختلفة: ${a.currency} مقابل ${b.currency}"
            if (a.active != b.active) differences += "غرفة $roomId: القيود النشطة ${a.active} عند المصدر و${b.active} عند الوجهة"
            if (a.voided != b.voided) differences += "غرفة $roomId: الملغى ${a.voided} عند المصدر و${b.voided} عند الوجهة"
            if (a.reversals != b.reversals) differences += "غرفة $roomId: القيود العكسية ${a.reversals} عند المصدر و${b.reversals} عند الوجهة"
            if (a.minOccurredAt != b.minOccurredAt) differences += "غرفة $roomId: أقدم قيد ${a.minOccurredAt} مقابل ${b.minOccurredAt}"
            if (a.maxOccurredAt != b.maxOccurredAt) differences += "غرفة $roomId: أحدث قيد ${a.maxOccurredAt} مقابل ${b.maxOccurredAt}"

            (a.byType.keys + b.byType.keys).sorted().forEach { type ->
                val ta = a.byType[type]
                val tb = b.byType[type]
                if (ta == null || tb == null) {
                    differences += "غرفة $roomId: النوع $type موجود عند ${if (ta == null) "الوجهة" else "المصدر"} فقط"
                } else {
                    if (ta.count != tb.count) differences += "غرفة $roomId: النوع $type — العدد ${ta.count} مقابل ${tb.count}"
                    if (ta.sumMinor != tb.sumMinor) differences += "غرفة $roomId: النوع $type — المجموع ${ta.sumMinor} مقابل ${tb.sumMinor}"
                }
            }

            (a.netByMember.keys + b.netByMember.keys).sorted().forEach { member ->
                val na = a.netByMember[member]
                val nb = b.netByMember[member]
                if (na == null) differences += "غرفة $roomId: العضو $member يظهر عند الوجهة فقط بصافي $nb"
                else if (nb == null) differences += "غرفة $roomId: العضو $member مفقود من الوجهة (صافيه $na)"
                else if (na != nb) differences += "غرفة $roomId: صافي العضو $member ${na} مقابل ${nb}"
            }

            val missing = a.operationIds - b.operationIds.toSet()
            val extra = b.operationIds - a.operationIds.toSet()
            if (missing.isNotEmpty()) differences += "غرفة $roomId: قيود ناقصة عند الوجهة: ${missing.joinToString("، ")}"
            if (extra.isNotEmpty()) differences += "غرفة $roomId: قيود زائدة عند الوجهة: ${extra.joinToString("، ")}"
        }

        if (source.activeTotal != target.activeTotal) {
            differences += "إجمالي القيود النشطة ${source.activeTotal} عند المصدر و${target.activeTotal} عند الوجهة"
        }
        if (source.voidedTotal != target.voidedTotal) {
            differences += "إجمالي الملغى ${source.voidedTotal} عند المصدر و${target.voidedTotal} عند الوجهة"
        }
        if (source.operationIds != target.operationIds) {
            val missing = source.operationIds - target.operationIds.toSet()
            val extra = target.operationIds - source.operationIds.toSet()
            if (missing.isNotEmpty()) differences += "قيود ناقصة عند الوجهة: ${missing.joinToString("، ")}"
            if (extra.isNotEmpty()) differences += "قيود زائدة عند الوجهة: ${extra.joinToString("، ")}"
        }
        // المسودات لا تُنقل بحكم أنها لم تُشارك: تُعلن للمستخدم ولا تُفشل الترحيل.
        if (source.drafts > 0) notes += "مسودات محلية لم تُنقل ولم تُقارن: ${source.drafts} (تبقى على جهاز صاحبها حتى يقرّر)"

        return Diff(differences, notes)
    }

    /**
     * اتّساق الملفّ مع ترويسته: هل الأحداث الموجودة فعلًا تعطي الجرد المكتوب في الترويسة؟
     *
     * **لماذا هذا الفحص ضروري؟** لأن ملفًّا مُعدَّلًا أو مبتورًا قد تبقى ترويسته تحمل أرقام الأصل،
     * فيمرّ «التحقق» وهو لم يقرأ إلا الترويسة — أي يصدّق ادّعاء الملفّ عن نفسه. هنا يُقارَن الادّعاء
     * بالواقع، فحذف حدث واحد يكفي لظهور فرق باسم القيد المفقود.
     */
    fun verifyFileIntegrity(file: File): Diff = compare(file.inventory, inventory(file.events))

    /** يدمج فروق الاتّساق مع فروق الوجهة في حكم واحد، بوسم صريح لا يُخفى. */
    fun mergedDiff(file: File, target: Inventory): Diff {
        val integrity = verifyFileIntegrity(file)
        val againstTarget = compare(file.inventory, target)
        return Diff(
            differences = integrity.differences.map { "الملفّ غير متّسق مع ترويسته — $it" } + againstTarget.differences,
            notes = (integrity.notes + againstTarget.notes).distinct()
        )
    }

    // ------------------------------------------------------------------ الصيغة

    /** نصّ ملفّ الترحيل: ترويسة بالجرد، ثم الأحداث مرتبة ترتيبًا ثابتًا. */
    fun exportText(events: List<Event>, exportedAt: Long, deviceId: String): String {
        val ordered = events.sortedWith(compareBy({ it.createdAt }, { it.operationId }))
        val header = JSONObject()
            .put("kind", "header")
            .put("format", MIGRATION_FORMAT)
            .put("exportedAt", exportedAt)
            .put("deviceId", deviceId)
            .put("inventory", inventoryJson(inventory(events)))
        val builder = StringBuilder(header.toString()).append('\n')
        ordered.forEach { event ->
            builder.append(
                JSONObject()
                    .put("kind", "event")
                    .put("operationId", event.operationId)
                    .put("entityType", event.entityType)
                    .put("entityId", event.entityId)
                    .put("action", event.action)
                    .put("payload", event.payload)
                    .put("createdAt", event.createdAt)
                    .toString()
            ).append('\n')
        }
        return builder.toString()
    }

    /**
     * يقرأ ملفّ الترحيل. كل خطأ له جملة عربية تُفهم، لأن ملفًّا مقطوعًا في واتساب — مثلًا — يجب أن
     * يُقال للمستخدم ما فيه، لا أن يُرمى استثناء JSON.
     */
    fun parse(text: String): File {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty()) { "الملفّ فارغ: لا ترويسة ولا أحداث" }
        val header = runCatching { JSONObject(lines.first()) }
            .getOrElse { error("الترويسة ليست JSON صحيحًا — الملفّ تالف أو مبتور") }
        require(header.optString("kind", "") == "header") { "أول سطر ليس ترويسة ملفّ ترحيل" }
        require(header.optString("format", "") == MIGRATION_FORMAT) {
            "صيغة الملفّ غير معروفة: ${header.optString("format", "بلا صيغة")}"
        }
        val events = ArrayList<Event>(lines.size)
        lines.drop(1).forEachIndexed { index, line ->
            val json = runCatching { JSONObject(line) }
                .getOrElse { error("السطر ${index + 2} تالف — الملفّ مبتور أو معدَّل") }
            if (json.optString("kind", "") != "event") return@forEachIndexed
            events += Event(
                operationId = json.optString("operationId", ""),
                entityType = json.optString("entityType", ""),
                entityId = json.optString("entityId", ""),
                action = json.optString("action", "UPSERT"),
                payload = json.optString("payload", ""),
                createdAt = json.optLong("createdAt", 0L)
            )
        }
        return File(
            exportedAt = header.optLong("exportedAt", 0L),
            deviceId = header.optString("deviceId", ""),
            inventory = inventoryFromJson(header.optJSONObject("inventory") ?: JSONObject()),
            events = events
        )
    }

    // ------------------------------------------------------------------ JSON

    fun inventoryJson(inventory: Inventory): JSONObject {
        val rooms = JSONArray()
        inventory.rooms.forEach { room ->
            val byType = JSONObject()
            room.byType.forEach { (type, tally) ->
                byType.put(type, JSONObject().put("count", tally.count).put("sumMinor", tally.sumMinor.toString()))
            }
            val net = JSONObject()
            room.netByMember.forEach { (member, value) -> net.put(member, value.toString()) }
            val roomOperations = JSONArray()
            room.operationIds.forEach { roomOperations.put(it) }
            rooms.put(
                JSONObject()
                    .put("roomId", room.roomId)
                    .put("currency", room.currency)
                    .put("active", room.active)
                    .put("voided", room.voided)
                    .put("reversals", room.reversals)
                    .put("byType", byType)
                    .put("netByMember", net)
                    .put("minOccurredAt", room.minOccurredAt)
                    .put("maxOccurredAt", room.maxOccurredAt)
                    .put("operationIds", roomOperations)
            )
        }
        val allOperations = JSONArray()
        inventory.operationIds.forEach { allOperations.put(it) }
        return JSONObject()
            .put("format", INVENTORY_FORMAT)
            .put("rooms", rooms)
            .put("activeTotal", inventory.activeTotal)
            .put("voidedTotal", inventory.voidedTotal)
            .put("reversalsTotal", inventory.reversalsTotal)
            .put("drafts", inventory.drafts)
            .put("operationIds", allOperations)
    }

    fun inventoryFromJson(json: JSONObject): Inventory {
        val rooms = ArrayList<RoomInventory>()
        val roomsJson = json.optJSONArray("rooms")
        for (index in 0 until (roomsJson?.length() ?: 0)) {
            val room = roomsJson!!.optJSONObject(index) ?: continue
            val byType = TreeMap<String, Tally>()
            room.optJSONObject("byType")?.let { types ->
                types.keys().forEach { key ->
                    val tally = types.optJSONObject(key) ?: return@forEach
                    byType[key] = Tally(
                        count = tally.optInt("count", 0),
                        sumMinor = tally.optString("sumMinor", "0").toLongOrNull() ?: 0L
                    )
                }
            }
            val net = TreeMap<String, Long>()
            room.optJSONObject("netByMember")?.let { members ->
                members.keys().forEach { key ->
                    net[key] = members.optString(key, "0").toLongOrNull() ?: 0L
                }
            }
            rooms += RoomInventory(
                roomId = room.optString("roomId", ""),
                currency = room.optString("currency", ""),
                active = room.optInt("active", 0),
                voided = room.optInt("voided", 0),
                reversals = room.optInt("reversals", 0),
                byType = byType,
                netByMember = net,
                minOccurredAt = room.optLong("minOccurredAt", 0L),
                maxOccurredAt = room.optLong("maxOccurredAt", 0L),
                operationIds = readStrings(room.optJSONArray("operationIds"))
            )
        }
        return Inventory(
            rooms = rooms.sortedBy { it.roomId },
            activeTotal = json.optInt("activeTotal", 0),
            voidedTotal = json.optInt("voidedTotal", 0),
            reversalsTotal = json.optInt("reversalsTotal", 0),
            drafts = json.optInt("drafts", 0),
            operationIds = readStrings(json.optJSONArray("operationIds"))
        )
    }

    private fun readStrings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val result = ArrayList<String>(array.length())
        for (index in 0 until array.length()) result += array.optString(index, "")
        return result.filter { it.isNotBlank() }
    }

    const val STATUS_DRAFT = "DRAFT"
    const val STATUS_VOIDED = "VOIDED"

    /**
     * قاعدة الحساب الواحدة كما في [com.baynana.domain.ledger.LedgerViews.effectiveEntries]، مكتوبة
     * هنا نصًّا لأن أداة الترحيل في Node تحتاج معرفتها أيضًا:
     * الملغى يُستثنى، والمسودة تُستثنى، والقيد العكسي يُستثنى.
     */
    const val ledgerEffectiveRule =
        "الملغى يُستثنى (أثره سقط)، والعكسي يُستثنى (وإلا حُسب الإلغاء مرتين)، والمسودة تُعدّ ولا تُقارن (لم تُشارك)"
}
