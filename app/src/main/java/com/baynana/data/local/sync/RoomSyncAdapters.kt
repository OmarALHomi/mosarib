package com.baynana.data.local.sync

import androidx.room.withTransaction
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerEntry
import com.baynana.data.local.ledger.OutboxItem
import com.baynana.data.local.ledger.Tombstone
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.sync.ApplyOutcome
import com.baynana.domain.sync.OutboxEnvelope
import com.baynana.domain.sync.RemoteChange
import com.baynana.domain.sync.SyncOutbox
import com.baynana.domain.sync.SyncStore
import com.baynana.domain.sync.TransportPort
import org.json.JSONObject

/**
 * ربط محرّك المزامنة (النقي في `domain`) بقاعدة Room. كل الكتابات هنا في معاملة واحدة، فلا تبقى
 * حالة نصفية لو انقطع التيار في منتصف التطبيق.
 */
class RoomSyncOutbox(private val db: AppDatabase) : SyncOutbox {

    private val dao get() = db.ledgerDao()

    override suspend fun pending(limit: Int, now: Long): List<OutboxEnvelope> =
        dao.dueOutboxBatch(now = now, limit = limit).map { it.toEnvelope() }

    override suspend fun markSent(operationId: String, at: Long) {
        dao.markOutboxFinal(operationId, com.baynana.domain.ledger.OutboxState.SENT, "", at)
    }

    override suspend fun markFailed(operationId: String, error: String, nextAttemptAt: Long, at: Long) {
        dao.markOutboxForRetry(
            operationId = operationId,
            state = com.baynana.domain.ledger.OutboxState.FAILED,
            error = error,
            nextAttemptAt = nextAttemptAt,
            updatedAt = at
        )
    }

    override suspend fun markDead(operationId: String, error: String, at: Long) {
        dao.markOutboxFinal(operationId, com.baynana.domain.ledger.OutboxState.DEAD, error, at)
    }
}

/**
 * تطبيق التغييرات القادمة. القواعد المطبَّقة:
 * - **لا حذف لصف قيد أبدًا**: الإغلاق عن بعد يجعل القيد `VOIDED` ويحرّر إسقاطاته، ويُسجَّل حجر قبر.
 * - **إعادة التسليم لا تُضاعف**: الإدراج يعتمد `operationId` الفريد فيرجع DUPLICATE بلا كتابة.
 * - **الحجر يسبق كل شيء**: كيان له حجر قبر لا يُنعش من ذاكرة قديمة.
 */
class RoomSyncStore(private val db: AppDatabase, private val cursorKey: String = "ledger") : SyncStore {

    private val dao get() = db.ledgerDao()

    override suspend fun isTombstoned(entityId: String): Boolean = dao.countTombstones(entityId) > 0

    override suspend fun recordTombstone(change: RemoteChange) {
        db.withTransaction {
            dao.upsertTombstone(
                Tombstone(
                    entityId = change.entityId,
                    entityType = change.entityType,
                    operationId = change.operationId,
                    reason = change.payload,
                    deletedAt = change.serverTime,
                    recordedAt = change.serverTime
                )
            )
            closeLocally(change.entityId, change.serverTime)
        }
    }

    override suspend fun apply(change: RemoteChange): ApplyOutcome = db.withTransaction {
        if (dao.countTombstones(change.entityId) > 0) return@withTransaction ApplyOutcome.SKIPPED_TOMBSTONED

        when (change.kind) {
            RemoteChange.UPSERT -> applyUpsert(change)
            RemoteChange.VOID -> applyVoid(change)
            RemoteChange.DELETE -> {
                recordTombstone(change)
                ApplyOutcome.APPLIED
            }
            else -> ApplyOutcome.UNSUPPORTED
        }
    }

    private suspend fun applyUpsert(change: RemoteChange): ApplyOutcome {
        val wire = try {
            decodeWire(change.payload)
        } catch (_: Exception) {
            return ApplyOutcome.UNSUPPORTED
        }
        // الإسقاطات تُشترط بوجود قيودها محليًا: الترتيب الزمني يضمن وصول الدين قبل سداده، وإن
        // وصل إسقاط بلا دينه فالأفضل عدم التطبيق وإظهار الحالة للمراجعة بدل أرصدة ناقصة.
        val missingDebt = wire.allocations.map { it.first }.filter { dao.getEntry(it) == null }
        if (missingDebt.isNotEmpty()) return ApplyOutcome.UNSUPPORTED

        val inserted = dao.insertEntryIfNew(wire.entry)
        wire.allocations.forEach { (debtEntryId, amountMinor) ->
            dao.upsertAllocation(
                com.baynana.data.local.ledger.EntryAllocation(
                    paymentEntryId = wire.entry.id,
                    debtEntryId = debtEntryId,
                    amountMinor = amountMinor,
                    currency = wire.entry.currency,
                    createdAt = wire.entry.occurredAt
                )
            )
        }
        return if (inserted == -1L) ApplyOutcome.DUPLICATE else ApplyOutcome.APPLIED
    }

    private suspend fun applyVoid(change: RemoteChange): ApplyOutcome {
        val reversal = try {
            decodeWire(change.payload).entry
        } catch (_: Exception) {
            return ApplyOutcome.UNSUPPORTED
        }
        val original = reversal.reversesEntryId
            ?: return if (dao.insertEntryIfNew(reversal) == -1L) ApplyOutcome.DUPLICATE else ApplyOutcome.APPLIED

        val inserted = dao.insertEntryIfNew(reversal)
        dao.updateEntryStatus(original, EntryStatus.VOIDED, change.serverTime)
        dao.releaseAllocationsForEntry(original)
        return if (inserted == -1L) ApplyOutcome.DUPLICATE else ApplyOutcome.APPLIED
    }

    /** إغلاق محلي بلا حذف: القيد يبقى بحالة ملغى، وإسقاطاته تُحرَّر. */
    private suspend fun closeLocally(entityId: String, at: Long) {
        val entry = dao.getEntry(entityId) ?: return
        if (entry.status != EntryStatus.VOIDED) {
            dao.updateEntryStatus(entryId = entityId, status = EntryStatus.VOIDED, updatedAt = at)
            dao.releaseAllocationsForEntry(entityId)
        }
    }

    override suspend fun cursor(): String = dao.getSyncState(cursorKey)?.cursor ?: ""

    override suspend fun setCursor(cursor: String, syncedAt: Long) {
        dao.upsertSyncState(
            com.baynana.data.local.ledger.SyncState(
                key = cursorKey,
                cursor = cursor,
                lastSyncAt = syncedAt,
                lastError = ""
            )
        )
    }

    override suspend fun recordError(message: String, at: Long) {
        val current = dao.getSyncState(cursorKey)
        dao.upsertSyncState(
            com.baynana.data.local.ledger.SyncState(
                key = cursorKey,
                cursor = current?.cursor ?: "",
                lastSyncAt = current?.lastSyncAt ?: 0L,
                lastError = message
            )
        )
    }
}

private fun OutboxItem.toEnvelope() = OutboxEnvelope(
    operationId = operationId,
    entityType = entityType,
    entityId = entityId,
    action = action,
    payload = payload,
    createdAt = createdAt,
    attempts = attempts
)

/**
 * فكّ حمولة السلك. الصيغة هي **نفس ما يُرسل** بالضبط (`OutboxPayloads`): غلاف فيه `entry`
 * وإسقاطات اختيارية، والمبالغ **نصّية بالوحدة الصغرى** (ADR-04).
 *
 * هذا التماثل شرطٌ لا تحسين: ما نُرسله يُخزَّن في الطرف الآخر ويعود إلينا في السحب، فلو اختلف
 * الفكّ عن البناء لصار كل ما نستقبله «غير مفهوم» — وهذا ما كشفه CI فعلًا.
 *
 * الرفض هنا مقصود ومحدود: أي مبلغ ليس عددًا صحيحًا نصًّا، أو نوع قيد غير معروف، يمنع التطبيق
 * ويُسجَّل للمراجعة، بدل أن يدخل رقم فاسد إلى أرصدة الناس.
 */
internal data class DecodedWire(
    val entry: LedgerEntry,
    /** إسقاطات السداد كما وردت: (debtEntryId, amountMinor). */
    val allocations: List<Pair<String, Long>>,
    val isVoid: Boolean
)

internal fun decodeWire(payload: String): DecodedWire {
    val root = JSONObject(payload)
    val body = if (root.has("entry")) root.getJSONObject("entry") else root
    val kind = root.optString("kind", "")

    val amountText = body.getString("amountMinor")
    require(amountText.all { it.isDigit() || it == '-' }) { "المبلغ ليس عددًا صحيحًا نصًّا" }
    val amount = amountText.toLong()
    require(amount > 0L) { "مبلغ غير موجب" }

    val type = body.getString("type")
    require(type in EntryType.all || type == EntryType.ADJUSTMENT) { "نوع قيد غير معروف: $type" }

    val entry = LedgerEntry(
        id = body.getString("id"),
        roomId = body.getString("roomId"),
        operationId = body.getString("operationId"),
        type = type,
        owedByMemberId = body.getString("owedByMemberId"),
        owedToMemberId = body.getString("owedToMemberId"),
        amountMinor = amount,
        currency = body.getString("currency"),
        occurredAt = body.getLong("occurredAt"),
        description = body.optString("description", ""),
        quantityNote = body.optString("quantityNote", ""),
        status = body.optString("status", EntryStatus.SENT),
        createdByMemberId = body.optString("createdByMemberId", ""),
        sourceTable = if (body.has("sourceTable")) body.getString("sourceTable") else null,
        sourceId = if (body.has("sourceId")) body.getString("sourceId") else null,
        listingId = if (body.has("listingId")) body.getString("listingId") else null,
        reversesEntryId = if (body.has("reversesEntryId")) body.getString("reversesEntryId") else null,
        createdAt = body.optLong("createdAt", body.getLong("occurredAt")),
        updatedAt = body.optLong("updatedAt", body.getLong("occurredAt"))
    )

    val allocations = mutableListOf<Pair<String, Long>>()
    root.optJSONArray("allocations")?.let { array ->
        for (index in 0 until array.length()) {
            val row = array.getJSONObject(index)
            val text = row.getString("amountMinor")
            require(text.all { it.isDigit() }) { "مبلغ الإسقاط ليس عددًا صحيحًا نصًّا" }
            allocations += row.getString("debtEntryId") to text.toLong()
        }
    }

    return DecodedWire(entry = entry, allocations = allocations, isVoid = kind == "VOID")
}

/** توافق للخلف: نداءات قديمة تريد القيد وحده. */
internal fun decodeEntry(payload: String): LedgerEntry = decodeWire(payload).entry

/**
 * ملخّص محلي لِما بقي بانتظار الإرسال — يُعرض للمستخدم بدل «نجح سحابيًا» الكاذبة.
 */
data class LocalSyncStatus(
    val pending: Int,
    val failed: Int,
    val dead: Int,
    val nextAttemptAt: Long,
    val lastError: String
)

/** حالة المزامنة المحلية: عدد المنتظرين، والفاشلين، وأقرب موعد محاولة، وآخر خطأ ظاهر. */
class SyncStatusReader(private val db: AppDatabase) {
    suspend fun status(now: Long): LocalSyncStatus {
        val dao = db.ledgerDao()
        val pending = dao.nextOutboxBatch(
            states = listOf(com.baynana.domain.ledger.OutboxState.PENDING)
        ).size
        val deferred = dao.deferredOutbox(now = now)
        val dead = dao.nextOutboxBatch(states = listOf(com.baynana.domain.ledger.OutboxState.DEAD)).size
        val next = deferred.minOfOrNull { it.nextAttemptAt } ?: 0L
        // السبب الظاهر للمستخدم: خطأ السحب إن وُجد، وإلا سبب آخر فشل إرسال — فلا يُخفى الفشل.
        val syncError = dao.getSyncState("ledger")?.lastError.orEmpty()
        val pushError = deferred.firstOrNull()?.lastError.orEmpty()
        return LocalSyncStatus(
            pending = pending,
            failed = deferred.size,
            dead = dead,
            nextAttemptAt = next,
            lastError = syncError.ifBlank { pushError }
        )
    }
}

/**
 * منفّذ جولة مزامنة واحدة: يجمع المحرّك مع المخزن والقناة.
 *
 * ملاحظة صريحة: **لا توجد قناة حقيقية بعد**. ترتيب العمل في خطة v6 يضع النقل السلكي الفعلي في
 * ح١٩ (وسيط ملفات/QR) وح٢٢ (الخادم الخاص)، وحتى ذلك الحين تُمرَّر قناة حقيقية من خارج هذه الطبقة.
 * ووجود القناة كواجهة يعني أن المحرّك والعامل قابلان للاختبار اليوم كاملَين بلا شبكة.
 */
class SyncCoordinator(
    private val db: AppDatabase,
    private val transport: TransportPort,
    private val batchSize: Int = 20,
    private val cursorKey: String = "ledger"
) {
    suspend fun syncOnce(now: Long = System.currentTimeMillis()): com.baynana.domain.sync.SyncReport {
        val engine = com.baynana.domain.sync.SyncEngine(
            outbox = RoomSyncOutbox(db),
            store = RoomSyncStore(db, cursorKey),
            batchSize = batchSize
        )
        return engine.run(transport, now)
    }

    /**
     * القيد بصيغة السلك كما يُرسل ويُستقبل: JSON بمبالغ **نصّية** بالوحدة الصغرى (ADR-04).
     * يستخدمه النقل الفعلي (ح١٩/ح٢٢) واختبارات المزامنة، والصيغة هي نفسها التي يفكّها [decodeEntry].
     */
    suspend fun entryPayload(entryId: String): String {
        val entry = db.ledgerDao().getEntry(entryId) ?: error("قيد غير موجود: $entryId")
        return entry.toWireJson().toString()
    }
}

private fun LedgerEntry.toWireJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("roomId", roomId)
    .put("operationId", operationId)
    .put("type", type)
    .put("owedByMemberId", owedByMemberId)
    .put("owedToMemberId", owedToMemberId)
    .put("amountMinor", amountMinor.toString())
    .put("currency", currency)
    .put("occurredAt", occurredAt)
    .put("description", description)
    .put("quantityNote", quantityNote)
    .put("status", status)
    .put("createdByMemberId", createdByMemberId)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)
