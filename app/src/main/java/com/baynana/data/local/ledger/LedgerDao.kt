package com.baynana.data.local.ledger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * واجهة القراءة والكتابة لدفتر الغرفة.
 *
 * ملاحظات تصميمية مثبّتة:
 * - `@Upsert` لا `REPLACE` على الصفوف التي لها أبناء: `REPLACE` يحذف الصف الأب فيسقط أبناءه
 *   (وهذا العيب أُصلح سابقًا في جدول العملاء). القيود والإقرارات لا تُحذف عرضًا أبدًا.
 * - [insertEntryIfNew] تُرجع `-1` عند تكرار `operationId`، وهذا هو حاجز منع الازدواج عند
 *   إعادة الإرسال أو إعادة التشغيل.
 * - الإقرار يُحدَّث بقرار واحد لكل عضو لكل قيد عبر `@Upsert` وفهرس فريد.
 */
@Dao
interface LedgerDao {

    // ------------------------------------------------------------- الغرف

    @Upsert
    suspend fun upsertRoom(room: LedgerRoom)

    @Query("SELECT * FROM rooms ORDER BY updatedAt DESC")
    fun observeRooms(): Flow<List<LedgerRoom>>

    @Query("SELECT * FROM rooms WHERE status = :status ORDER BY updatedAt DESC")
    fun observeRoomsByStatus(status: String): Flow<List<LedgerRoom>>

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    suspend fun getRoom(roomId: String): LedgerRoom?

    @Query("SELECT * FROM rooms WHERE linkCode = :linkCode")
    suspend fun getRoomByLinkCode(linkCode: String): LedgerRoom?

    @Query("UPDATE rooms SET status = :status, updatedAt = :updatedAt, closedAt = :closedAt WHERE id = :roomId")
    suspend fun updateRoomStatus(roomId: String, status: String, updatedAt: Long, closedAt: Long?)

    // ------------------------------------------------------------ الأعضاء

    @Upsert
    suspend fun upsertMember(member: RoomMember)

    @Query("SELECT * FROM room_members WHERE roomId = :roomId ORDER BY joinedAt ASC")
    fun observeMembers(roomId: String): Flow<List<RoomMember>>

    @Query("SELECT * FROM room_members WHERE roomId = :roomId AND isMe = 1 LIMIT 1")
    suspend fun getMyMembership(roomId: String): RoomMember?

    @Query("SELECT * FROM room_members WHERE roomId = :roomId AND memberId = :memberId")
    suspend fun getMember(roomId: String, memberId: String): RoomMember?

    // ------------------------------------------------------------- القيود

    /**
     * إدراج قيد جديد فقط. `IGNORE` مع فهرس `operationId` الفريد تعني: إعادة تشغيل نفس العملية
     * تُرجع `-1` ولا تُنشئ صفًا ثانيًا. هذه هي نقطة منع الازدواج في الطبقة المحلية.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryIfNew(entry: LedgerEntry): Long

    /** تحديث قيد موجود (بعد تعديل مشروع)، ولا يحذف إقراراته. */
    @Upsert
    suspend fun upsertEntry(entry: LedgerEntry)

    @Query("SELECT * FROM entries WHERE roomId = :roomId ORDER BY occurredAt DESC, createdAt DESC")
    fun observeEntries(roomId: String): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM entries WHERE roomId = :roomId AND status != :voided ORDER BY occurredAt DESC")
    suspend fun getActiveEntries(roomId: String, voided: String = EntryStatus.VOIDED): List<LedgerEntry>

    @Query("SELECT * FROM entries WHERE id = :entryId")
    suspend fun getEntry(entryId: String): LedgerEntry?

    @Query("SELECT * FROM entries WHERE operationId = :operationId")
    suspend fun getEntryByOperationId(operationId: String): LedgerEntry?

    @Query("UPDATE entries SET status = :status, updatedAt = :updatedAt WHERE id = :entryId")
    suspend fun updateEntryStatus(entryId: String, status: String, updatedAt: Long)

    @Query(
        "SELECT * FROM entries WHERE roomId = :roomId AND type IN (:types) " +
            "AND status != :voided ORDER BY occurredAt ASC, createdAt ASC"
    )
    suspend fun getDebtEntriesOldestFirst(
        roomId: String,
        types: List<String> = listOf(EntryType.WATER_SESSION, EntryType.GOODS_DEBT, EntryType.SETTLEMENT),
        voided: String = EntryStatus.VOIDED
    ): List<LedgerEntry>

    @Query("SELECT * FROM entries WHERE listingId = :listingId AND status != :voided LIMIT 1")
    suspend fun getActiveEntryForListing(listingId: String, voided: String = EntryStatus.VOIDED): LedgerEntry?

    @Query("SELECT COUNT(*) FROM entries WHERE roomId = :roomId AND status = :status")
    suspend fun countEntriesWithStatus(roomId: String, status: String): Int

    // ----------------------------------------------------------- الإسقاطات

    /**
     * كتابة إسقاط بمفتاحه الطبيعي `(paymentEntryId, debtEntryId)`: إعادة الإسقاط تُحدّث الصف
     * نفسه. الأفضل أن ينادي المنادي [allocatePayment] لأنها تتحقق من الحدود أولًا.
     */
    @Upsert
    suspend fun upsertAllocation(allocation: EntryAllocation)

    @Query("SELECT * FROM entry_allocations WHERE paymentEntryId = :paymentEntryId")
    suspend fun getAllocationsForPayment(paymentEntryId: String): List<EntryAllocation>

    @Query("SELECT * FROM entry_allocations WHERE debtEntryId = :debtEntryId")
    suspend fun getAllocationsForDebt(debtEntryId: String): List<EntryAllocation>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM entry_allocations WHERE debtEntryId = :debtEntryId")
    suspend fun allocatedToDebt(debtEntryId: String): Long

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM entry_allocations WHERE paymentEntryId = :paymentEntryId")
    suspend fun allocatedFromPayment(paymentEntryId: String): Long

    // ------------------------------------------------------------- الإقرار

    /** قرار واحد لكل عضو لكل قيد: الإقرار الجديد يستبدل السابق ولا يضيف سجلًا. */
    @Upsert
    suspend fun upsertAcknowledgement(acknowledgement: Acknowledgement)

    @Query("SELECT * FROM acknowledgements WHERE entryId = :entryId ORDER BY decidedAt DESC")
    fun observeAcknowledgements(entryId: String): Flow<List<Acknowledgement>>

    @Query("SELECT * FROM acknowledgements WHERE entryId = :entryId AND memberId = :memberId")
    suspend fun getAcknowledgement(entryId: String, memberId: String): Acknowledgement?

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :entryId")
    suspend fun getEntryWithDetails(entryId: String): EntryWithDetails?

    // ------------------------------------------------------- صندوق الصادر

    /**
     * جدولة عملية سلكية في نفس معاملة حفظ القيد. `IGNORE` تضمن ألا تُجدول نفس العملية مرتين،
     * و`operationId` هو نفسه معرّف القيد فلا تضيع المطابقة بين المحلي والسلكي.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueueOutbox(item: OutboxItem): Long

    @Query("SELECT * FROM outbox WHERE state IN (:states) ORDER BY createdAt ASC LIMIT :limit")
    suspend fun nextOutboxBatch(
        states: List<String> = listOf(OutboxState.PENDING, OutboxState.FAILED),
        limit: Int = 20
    ): List<OutboxItem>

    @Query("UPDATE outbox SET state = :state, attempts = attempts + 1, lastError = :error, updatedAt = :updatedAt WHERE operationId = :operationId")
    suspend fun markOutbox(operationId: String, state: String, error: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM outbox WHERE state IN (:states)")
    fun observeOutboxCount(
        states: List<String> = listOf(OutboxState.PENDING, OutboxState.FAILED)
    ): Flow<Int>

    @Query("DELETE FROM outbox WHERE operationId = :operationId")
    suspend fun deleteOutbox(operationId: String)

    // ------------------------------------------------------ مؤشر المزامنة

    @Upsert
    suspend fun upsertSyncState(state: SyncState)

    @Query("SELECT * FROM sync_state WHERE `key` = :key")
    suspend fun getSyncState(key: String): SyncState?

    // ------------------------------------------------------ عمليات مركّبة

    /**
     * حفظ قيد جديد مع جدولة إرساله: كتابة واحدة ذرّية، فلا يوجد قيد محلي بلا صف في صندوق
     * الصادر، ولا صف صادر بلا قيد. تُرجع false إن كان القيد مكررًا (نفس operationId).
     */
    @Transaction
    suspend fun insertEntryAndEnqueue(entry: LedgerEntry, payload: String): Boolean {
        val rowId = insertEntryIfNew(entry)
        if (rowId == -1L) return false
        enqueueOutbox(
            OutboxItem(
                operationId = entry.operationId,
                entityType = "entry",
                entityId = entry.id,
                action = "UPSERT",
                payload = payload,
                createdAt = entry.createdAt,
                updatedAt = entry.createdAt
            )
        )
        return true
    }

    /**
     * تسجيل قرار عضو على قيد: قرار واحد لكل عضو لكل قيد (المفتاح الطبيعي)، ثم تحديث حالة القيد
     * بحسب القرار. لا يُحذف القيد ولا تُمس أرقامه، ولا يُجمّد دفتر كاتبه.
     */
    @Transaction
    suspend fun recordDecision(
        entryId: String,
        memberId: String,
        decision: String,
        note: String,
        decidedAt: Long,
        resultingStatus: String,
        updatedAt: Long
    ) {
        upsertAcknowledgement(
            Acknowledgement(
                entryId = entryId,
                memberId = memberId,
                decision = decision,
                note = note,
                decidedAt = decidedAt,
                createdAt = decidedAt
            )
        )
        updateEntryStatus(entryId, resultingStatus, updatedAt)
    }

    /**
     * إسقاط سداد على دين محدد بحدود صارمة: لا يتجاوز مبلغ السداد، ولا يتجاوز قيمة الدين، ولا
     * يجوز الخلط بين عملتين (لا مقاصة ولا تحويل تلقائي). ما زاد عن الدين يبقى رصيدًا في السداد
     * غير مسقَط (رصيد في الغرفة)، ولا يُسقَط قهرًا.
     *
     * تُرجع false إن رفضت الحدود الطلب، فالمنادي (ح٤) يقرر: تخصيص جزئي أو إظهار سبب الرفض.
     */
    @Transaction
    suspend fun allocatePayment(
        paymentEntryId: String,
        debtEntryId: String,
        amountMinor: Long,
        createdAt: Long
    ): Boolean {
        if (amountMinor <= 0L) return false
        val payment = getEntry(paymentEntryId) ?: return false
        val debt = getEntry(debtEntryId) ?: return false
        if (payment.currency != debt.currency) return false
        if (payment.type != EntryType.PAYMENT && payment.type != EntryType.GENERAL_RECEIPT) return false
        // المبالغ كلها Long ومحدودة بسقف Money.MAX_MINOR، فالجمع هنا آمن بلا فيض.
        if (allocatedFromPayment(paymentEntryId) + amountMinor > payment.amountMinor) return false
        if (allocatedToDebt(debtEntryId) + amountMinor > debt.amountMinor) return false
        upsertAllocation(
            EntryAllocation(
                paymentEntryId = paymentEntryId,
                debtEntryId = debtEntryId,
                amountMinor = amountMinor,
                currency = payment.currency,
                createdAt = createdAt
            )
        )
        return true
    }
}
