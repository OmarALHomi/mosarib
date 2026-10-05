package com.baynana.data.local.migration

import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerEntry
import com.baynana.data.local.ledger.OutboxPayloads
import com.baynana.data.local.sync.SyncCoordinator
import com.baynana.domain.migration.LedgerMigration
import com.baynana.domain.sync.PushOutcome
import com.baynana.domain.sync.TransportPort
import org.json.JSONArray
import org.json.JSONObject

/**
 * ترحيل الدفتر من التطبيق (ح٢٣): تصدير الأحداث، والاستيراد عبر **العقد نفسه**، ومطابقة الجرد.
 *
 * **لماذا لا يُكتب الاستيراد مباشرة في القاعدة؟** لأن مسارًا خاصًّا للاستيراد هو أول ما ينحرف عن
 * الفحوص: يكتب قيدًا لم يمرّ بمحرّك الحساب، أو يُسقط إسقاطًا. فالاستيراد هنا = **إرسال الأحداث إلى
 * الخادم عبر `TransportPort`** ثم جولة مزامنة عادية تُطبّقها بـ`RoomSyncStore` — أي نفس المسار
 * الذي يسلكه أي قيد قادم من الطرف الآخر. ومنع التكرار بالمُعرّف يعني أن إعادة الاستيراد لا تُضاعف.
 *
 * **والجرد هو الحاكم**: بعد الاستيراد تُقارَن أرقام الملفّ بأرقام الدفتر المحلي، وكل فرق يُقال
 * بالعربية واحدًا واحدًا. فـ«انتقل الدفتر» عبارة تُقاس لا تُدّعى.
 *
 * **تصدر الأحداث من مصدرين، ولماذا:**
 * 1. **صندوق الصادر**: حمولات الأحداث كما كُتبت (وهي الأدقّ: تحمل الإسقاطات وأسباب التخصيص).
 * 2. **جدول القيود**: يلتقط ما وصل من الطرف الآخر ولم يمرّ بصندوق صادرنا (قيود وإقرارات واردة)،
 *    وحمولته تُبنى من الصف نفسه — وإلا لظهر جرد ناقص لدفتر استُقبل من شريك.
 *
 * والمُعرّف يحكم: قيد موجود في المصدرين يُصدَّر **مرة واحدة**.
 */
class MigrationRepository(
    private val db: AppDatabase,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val dao get() = db.ledgerDao()

    /** نتيجة استيراد: ما قُبل، وما رُفض بسببه، وهل طابق الجرد. */
    data class ImportReport(
        val sent: Int,
        val accepted: Int,
        val rejected: Int,
        val rejections: List<String>,
        val pulled: Int,
        val diff: LedgerMigration.Diff,
        val localInventoryLine: String
    ) {
        val isClean: Boolean get() = diff.isClean

        /** سطر عربي واحد يُعرض للمستخدم: صريح في النجاح والفشل. */
        fun summaryText(): String = buildString {
            append("أُرسل: $accepted من $sent")
            if (rejected > 0) append(" • مرفوض: $rejected")
            append(" • وصل جديد: $pulled")
            append(if (isClean) " • الجرد مطابق" else " • الجرد فيه ${diff.differences.size} فرقًا")
        }
    }

    /** ملفّ تصدير جاهز للكتابة في ملفّ ومشاركته. */
    data class Export(val fileName: String, val text: String, val inventory: LedgerMigration.Inventory) {
        val events: Int get() = text.lineSequence().count { it.isNotBlank() } - 1
    }

    // ------------------------------------------------------------------ التصدير

    /**
     * يبني أحداث الترحيل من صندوق الصادر والقيود معًا، بلا تكرار.
     *
     * والترتيب محسوم بالزمن ثم المعرّف (كما في `LedgerMigration.exportText`) ليكون الملفّ حتميًّا:
     * تصديران لنفس الدفتر يجب أن يعطيا نفس البايتات، وإلا صار كل فحص اختلافًا.
     */
    suspend fun events(): List<LedgerMigration.Event> {
        val seen = HashSet<String>()
        val events = ArrayList<LedgerMigration.Event>()

        dao.getAllOutboxItems().forEach { item ->
            if (item.operationId.isBlank() || !seen.add(item.operationId)) return@forEach
            events += LedgerMigration.Event(
                operationId = item.operationId,
                entityType = item.entityType,
                entityId = item.entityId,
                action = item.action,
                payload = item.payload,
                createdAt = item.createdAt
            )
        }

        dao.getAllEntries().forEach { entry ->
            if (entry.operationId.isBlank() || !seen.add(entry.operationId)) return@forEach
            val action = if (entry.reversesEntryId != null) "VOID" else "UPSERT"
            events += LedgerMigration.Event(
                operationId = entry.operationId,
                entityType = "entry",
                entityId = entry.id,
                action = action,
                payload = payloadFor(entry),
                createdAt = entry.createdAt
            )
        }

        return events
    }

    /**
     * حمولة صفّ وصل من الطرف الآخر (لا صفّ صادر لنا). تُبنى `decodeWire` يوافقها: كائن `entry`
     * والاختصارات، والإسقاطات من جدول الإسقاطات الحقيقي (لا من حساب مُعاد).
     */
    private suspend fun payloadFor(entry: LedgerEntry): String {
        val json = JSONObject(OutboxPayloads.entry(entry)).getJSONObject("entry")
        val allocations = dao.getAllocationsForPayment(entry.id)
        if (allocations.isEmpty()) return OutboxPayloads.entry(entry)
        val rows = JSONArray()
        allocations.forEach { row ->
            rows.put(
                JSONObject()
                    .put("paymentEntryId", row.paymentEntryId)
                    .put("debtEntryId", row.debtEntryId)
                    .put("amountMinor", row.amountMinor.toString())
                    .put("currency", row.currency)
                    .put("createdAt", row.createdAt)
            )
        }
        return JSONObject()
            .put("v", OutboxPayloads.VERSION)
            .put("kind", "RECEIPT")
            .put("entry", json)
            .put("allocations", rows)
            .toString()
    }

    /** جرد الدفتر المحلي من المصدر نفسه (لا حساب ثانٍ يُقارَن بغير ما يُصدَّر). */
    suspend fun localInventory(): LedgerMigration.Inventory = LedgerMigration.inventory(events())

    /** ملفّ الترحيل: ترويسة بالجرد وأحداث مرتبة، باسم يحمل التاريخ ليعرف المالك أيّ ملفّ يرسل. */
    suspend fun export(): Export {
        val events = events()
        val stamp = now()
        return Export(
            fileName = "baynana-migration-$stamp.jsonl",
            text = LedgerMigration.exportText(events, exportedAt = stamp, deviceId = newId().take(8)),
            inventory = LedgerMigration.inventory(events)
        )
    }

    // ------------------------------------------------------------------ الاستيراد

    /**
     * يستورد ملفًّا: يرسل أحداثه إلى الخادم عبر [transport]، ثم يُشغّل **جولة مزامنة عادية** لتُطبَّق
     * بالتطبيق نفسه الذي يُطبّق الوارد من الشريك، ثم يقارن الجرد.
     *
     * والردّ يقول كل شيء بصراحة: كم أُرسل، وكم قُبل، وكم رُفض ولماذا، وكم وصل جديدًا، وهل طابق
     * الجرد. وحتى لو رُفض عنصر، فالمقارنة تكشفه بالأرقام فيظهر في القائمة لا في رسالة نجاح كاذبة.
     */
    suspend fun import(text: String, transport: TransportPort, batchSize: Int = 100): ImportReport {
        val file = LedgerMigration.parse(text)
        require(file.events.isNotEmpty()) { "الملفّ لا يحمل أحداثًا: لا شيء يُستورد" }

        val rejections = mutableListOf<String>()
        var accepted = 0
        var sent = 0
        file.events.chunked(batchSize).forEach { batch ->
            sent += batch.size
            try {
                val outcomes = transport.push(batch.map { it.toEnvelope() })
                outcomes.forEach { outcome ->
                    when (outcome) {
                        is PushOutcome.Accepted -> accepted++
                        is PushOutcome.Rejected -> rejections += outcome.reason.ifBlank { "رُفض بلا سبب مذكور" }
                        is PushOutcome.TransportError -> rejections += outcome.message
                    }
                }
            } catch (error: Exception) {
                // انقطاع في المنتصف: لا نُعلن نجاحًا. ما وصل قُبل، وما لم يصل يُعاد بالجولة التالية.
                rejections += "تعذّر الوصول إلى الخادم في المنتصف — أعد المحاولة"
            }
        }

        val report = runCatching { SyncCoordinator(db, transport).syncOnce(now()) }
        val pulled = report.getOrNull()?.applied ?: 0
        report.getOrNull()?.errors?.forEach { rejections += it }
        // الحكم يجمع شيئين: هل الملفّ متّسق مع ترويسته؟ وهل وصل كل شيء إلى دفترنا؟ فالملفّ المُعدَّل
        // (أو المبتور) لا يُقبل نقله أصلًا، ولو خلت أرقامه من أي فرق ظاهر.
        val diff = LedgerMigration.mergedDiff(file, localInventory())

        return ImportReport(
            sent = sent,
            accepted = accepted,
            rejected = rejections.size,
            rejections = rejections.distinct().take(10),
            pulled = pulled,
            diff = diff,
            localInventoryLine = inventoryLine(localInventory())
        )
    }

    /** مطابقة الملفّ بالدفتر المحلي بلا أي كتابة: للسؤال «هل وصل كل شيء؟» — ومعها اتّساق الملفّ. */
    suspend fun verify(text: String): LedgerMigration.Diff {
        val file = LedgerMigration.parse(text)
        return LedgerMigration.mergedDiff(file, localInventory())
    }

    /** سطر عربي يلخّص الجرد: يُعرض للمالك فيقارنه بما يعرفه بدل أن يثق برقم غامض. */
    fun inventoryLine(inventory: LedgerMigration.Inventory): String = buildString {
        append("${inventory.rooms.size} غرفة")
        append(" • ${inventory.activeTotal} قيدًا نشطًا")
        if (inventory.voidedTotal > 0) append(" • ${inventory.voidedTotal} ملغى")
        if (inventory.reversalsTotal > 0) append(" • ${inventory.reversalsTotal} عكسي")
        if (inventory.drafts > 0) append(" • ${inventory.drafts} مسودة محلية")
        val members = inventory.rooms.sumOf { it.netByMember.size }
        if (members > 0) append(" • $members طرفًا بأرصدة")
    }
}

private fun LedgerMigration.Event.toEnvelope() = com.baynana.domain.sync.OutboxEnvelope(
    operationId = operationId,
    entityType = entityType,
    entityId = entityId,
    action = action,
    payload = payload,
    createdAt = createdAt,
    attempts = 0
)
