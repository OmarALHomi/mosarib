package com.baynana.data.local.ledger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.OutboxState
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

    /** قراءة واحدة لكل الغرف (بلا تدفّق): تستعملها خلاصة الرئيسية التي تُبنى عند الطلب. */
    @Query("SELECT * FROM rooms ORDER BY updatedAt DESC")
    suspend fun getAllRooms(): List<LedgerRoom>

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

    /** كل أعضاء غرفة: بها تُبنى لقطة الغرفة في حزمة التسليم (ح١٩). */
    @Query("SELECT * FROM room_members WHERE roomId = :roomId ORDER BY joinedAt ASC")
    suspend fun getMembers(roomId: String): List<RoomMember>

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

    /** القيود التي تنتظر إقرار الطرف الآخر في كل الغرف (لبادج المعلّق). */
    @Query(
        "SELECT COUNT(*) FROM entries WHERE status = :sent AND type IN (:types)"
    )
    suspend fun countAwaitingAcknowledgement(
        sent: String = EntryStatus.SENT,
        types: List<String> = EntryType.debts + EntryType.credits
    ): Int

    /**
     * كل قيود الغرفة بما فيها الملغى: اللقطة الموحّدة تحتاج الأصل الملغى لتعرف أن القيد العكسي
     * يقابله فتستثنيه من الحساب. أما [getActiveEntries] فتُستخدم حيث لا يظهر الملغى للمستخدم.
     */
    @Query("SELECT * FROM entries WHERE roomId = :roomId ORDER BY occurredAt ASC, createdAt ASC")
    suspend fun getEntriesIncludingVoided(roomId: String): List<LedgerEntry>

    @Query("SELECT * FROM entries WHERE id = :entryId")
    suspend fun getEntry(entryId: String): LedgerEntry?

    @Query("SELECT * FROM entries WHERE operationId = :operationId")
    suspend fun getEntryByOperationId(operationId: String): LedgerEntry?

    @Query("UPDATE entries SET status = :status, updatedAt = :updatedAt WHERE id = :entryId")
    suspend fun updateEntryStatus(entryId: String, status: String, updatedAt: Long)

    /**
     * حذف صف قيد. مسموح فقط لمسودة لم تُشارك بعد (§4.4)؛ الحذف بعد المشاركة ممنوع والمسار
     * الصحيح هو القيد العكسي. النداء يأتي من مستودع الدفتر بعد التحقق، لا من الشاشة.
     */
    @Query("DELETE FROM entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: String)

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

    /** القيد العكسي الذي ألغى هذا القيد، إن وُجد (الفهرس الفريد يضمن واحدًا على الأكثر). */
    @Query("SELECT * FROM entries WHERE reversesEntryId = :entryId LIMIT 1")
    suspend fun getReversalOf(entryId: String): LedgerEntry?

    @Query("SELECT * FROM entries WHERE roomId = :roomId AND type = :type ORDER BY occurredAt ASC")
    suspend fun getEntriesOfType(roomId: String, type: String): List<LedgerEntry>

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

    /** كل الإسقاطات التي دخل فيها القيد، من جهة السداد أو من جهة الدين. */
    @Query("SELECT * FROM entry_allocations WHERE paymentEntryId = :entryId OR debtEntryId = :entryId ORDER BY createdAt ASC")
    suspend fun getAllocationsForEntry(entryId: String): List<EntryAllocation>

    /** كل إسقاطات غرفة واحدة، كلها بمفتاح الغرفة على القيدين، فتُبنى منها الأرصدة. */
    @Query(
        "SELECT a.* FROM entry_allocations a JOIN entries e ON e.id = a.paymentEntryId " +
            "WHERE e.roomId = :roomId ORDER BY a.createdAt ASC"
    )
    suspend fun getAllocationsInRoom(roomId: String): List<EntryAllocation>

    /**
     * تحرير إسقاطات قيد أُلغي: السداد يعود رصيدًا متاحًا، والدين الذي سُدّ جزئيًا يعود مفتوحًا
     * بمقدار ما تحرّر. لا يحذف هذا أي قيد، بل يسقط التخصيص فقط، ويُسجَّل الحدث في صندوق الصادر.
     */
    @Query("DELETE FROM entry_allocations WHERE paymentEntryId = :entryId OR debtEntryId = :entryId")
    suspend fun releaseAllocationsForEntry(entryId: String): Int

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

    /**
     * عناصر جاهزة للإرسال الآن فقط: حالتها قابلة للإرسال ولم يحن موعدها بعد فشل سابق.
     * الترتيب بالإنشاء (الأقدم أولًا) فيُحفظ ترتيب الكتابة حتى مع الفشل.
     */
    @Query(
        "SELECT * FROM outbox WHERE state IN (:states) AND nextAttemptAt <= :now " +
            "ORDER BY createdAt ASC LIMIT :limit"
    )
    suspend fun dueOutboxBatch(
        now: Long,
        limit: Int = 20,
        states: List<String> = listOf(OutboxState.PENDING, OutboxState.FAILED)
    ): List<OutboxItem>

    @Query(
        "SELECT * FROM outbox WHERE state IN (:states) AND nextAttemptAt > :now " +
            "ORDER BY nextAttemptAt ASC"
    )
    suspend fun deferredOutbox(
        now: Long,
        states: List<String> = listOf(OutboxState.PENDING, OutboxState.FAILED)
    ): List<OutboxItem>

    @Query("UPDATE outbox SET state = :state, attempts = attempts + 1, lastError = :error, updatedAt = :updatedAt WHERE operationId = :operationId")
    suspend fun markOutbox(operationId: String, state: String, error: String, updatedAt: Long)

    @Query(
        "UPDATE outbox SET state = :state, attempts = attempts + 1, lastError = :error, " +
            "nextAttemptAt = :nextAttemptAt, updatedAt = :updatedAt WHERE operationId = :operationId"
    )
    suspend fun markOutboxForRetry(
        operationId: String,
        state: String,
        error: String,
        nextAttemptAt: Long,
        updatedAt: Long
    )

    @Query("UPDATE outbox SET state = :state, lastError = :error, updatedAt = :updatedAt WHERE operationId = :operationId")
    suspend fun markOutboxFinal(operationId: String, state: String, error: String, updatedAt: Long)

    /** هل جُدول لهذا القيد إرسال؟ (يمنع حذف مسودة سبق أن غادرت الجهاز) */
    @Query("SELECT COUNT(*) FROM outbox WHERE entityId = :entityId")
    suspend fun countOutboxForEntity(entityId: String): Int

    @Query("SELECT COUNT(*) FROM outbox WHERE state IN (:states)")
    fun observeOutboxCount(
        states: List<String> = listOf(OutboxState.PENDING, OutboxState.FAILED)
    ): Flow<Int>

    @Query("DELETE FROM outbox WHERE operationId = :operationId")
    suspend fun deleteOutbox(operationId: String)

    // ------------------------------------------------ شاشة حالة المزامنة (د٤)

    /**
     * أحدث قيود الدفتر كلها (لا غرفة واحدة) لعرض «ما حال كل حركة؟» في شاشة واحدة.
     * ترتيبها بالأحدث: المستخدم يبحث عمّا كتبه للتوّ، لا عن أول سطر في التاريخ.
     */
    @Query("SELECT * FROM entries ORDER BY occurredAt DESC, id DESC LIMIT :limit")
    fun observeRecentEntries(limit: Int = 80): Flow<List<LedgerEntry>>

    /** صندوق الصادر كما هو: منه وحده تُعرف حقيقة «أُرسل أم لا» بلا تخمين. */
    @Query("SELECT * FROM outbox ORDER BY createdAt DESC LIMIT :limit")
    fun observeOutboxItems(limit: Int = 120): Flow<List<OutboxItem>>

    /** إقرارات الطرفين بأسبابها: الاعتراض يُعرض بنصّ صاحبه لا بوصف من عندنا. */
    @Query("SELECT * FROM acknowledgements ORDER BY decidedAt DESC LIMIT :limit")
    fun observeRecentAcknowledgements(limit: Int = 200): Flow<List<Acknowledgement>>

    /** عضو «أنا» في كل غرفة: لتمييز إقراري من إقرار الطرف. */
    @Query("SELECT * FROM room_members WHERE isMe = 1")
    fun observeMyMemberships(): Flow<List<RoomMember>>

    /** أعضاء غرفة: للاختيار بين «كشفي» و«كشف الطرف» في شاشة الكشف. */
    @Query("SELECT * FROM room_members WHERE roomId = :roomId")
    suspend fun getMembers(roomId: String): List<RoomMember>

    /**
     * إعادة محاولة إرسال فشل نهائيًا: قرار بشري صريح من شاشة حالة المزامنة.
     * تُصفَّر المحاولات والخطأ لأن السبب القديم قد زال (اتصال، رقم مُصحّح)، ولا تُحذف العملية.
     */
    @Query(
        "UPDATE outbox SET state = :pending, attempts = 0, lastError = '', nextAttemptAt = 0, " +
            "updatedAt = :now WHERE operationId = :operationId"
    )
    suspend fun requeueOutbox(
        operationId: String,
        now: Long,
        pending: String = OutboxState.PENDING
    )

    // ------------------------------------------------------ مؤشر المزامنة

    @Upsert
    suspend fun upsertSyncState(state: SyncState)

    @Query("SELECT * FROM sync_state WHERE `key` = :key")
    suspend fun getSyncState(key: String): SyncState?

    // ----------------------------------------------------------- حجر القبر

    @Upsert
    suspend fun upsertTombstone(tombstone: Tombstone)

    @Query("SELECT COUNT(*) FROM tombstones WHERE entityId = :entityId")
    suspend fun countTombstones(entityId: String): Int

    // ------------------------------------------- التسليم اليدوي بلا خادم (ح١٩)

    /**
     * كل ما لم يُقَرّ بعد، مرتّبًا بالزمن — للتسليم اليدوي عبر ملف/رمز.
     *
     * يُدرج الفاشل والميت أيضًا عن قصد: التسليم اليدوي طريق مشروع لما استعصى على الآلة، وإخفاؤه
     * يعني أن قيدًا معلّقًا لا يصل أبدًا. أما SENT فيُستثنى احترامًا لمعنى الكلمة: ما أُرسل لا
     * يُعاد إرساله في كل حزمة.
     */
    @Query("SELECT * FROM outbox WHERE state != :sentState ORDER BY createdAt ASC LIMIT :limit")
    suspend fun pendingForHandover(limit: Int, sentState: String = OutboxState.SENT): List<OutboxItem>

    @Query("SELECT COUNT(*) FROM outbox WHERE state != :sentState")
    suspend fun pendingHandoverCount(sentState: String = OutboxState.SENT): Int

    @Query("SELECT * FROM tombstones ORDER BY deletedAt DESC")
    fun observeTombstones(): Flow<List<Tombstone>>

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
     * حفظ قيد جديد مع إسقاطاته وجدولة إرساله في معاملة واحدة.
     *
     * الإسقاطات تُحسب خارج هذه الدالة (في محرّك `domain`) ثم تُطبَّق هنا ذرّيًا مع القيد:
     * فلا يوجد قيد محلي مخصَّص جزئيًا، ولا إسقاط بلا قيد، ولا قيد بلا صف في صندوق الصادر.
     * تكرار `operationId` يُرجع false بلا أي كتابة إضافية — وهذا حاجز منع الازدواج عند إعادة
     * التشغيل أو إعادة الإرسال.
     */
    @Transaction
    suspend fun insertEntryWithAllocationsAndEnqueue(
        entry: LedgerEntry,
        payload: String,
        allocations: List<EntryAllocation>
    ): Boolean {
        val rowId = insertEntryIfNew(entry)
        if (rowId == -1L) return false
        allocations.forEach { upsertAllocation(it) }
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
     * إلغاء قيد بقيد عكسي في معاملة واحدة: يُدرج القيد العكسي، يُعلَّم الأصل [EntryStatus.VOIDED]،
     * وتُحرَّر إسقاطات الأصل، ويُجدول الإرسال. لا حذف لصف القيد الأصلي أبدًا.
     */
    @Transaction
    suspend fun insertReversalAndVoid(
        reversal: LedgerEntry,
        originalEntryId: String,
        payload: String,
        updatedAt: Long
    ): Boolean {
        if (insertEntryIfNew(reversal) == -1L) return false
        updateEntryStatus(originalEntryId, EntryStatus.VOIDED, updatedAt)
        releaseAllocationsForEntry(originalEntryId)
        enqueueOutbox(
            OutboxItem(
                operationId = reversal.operationId,
                entityType = "entry",
                entityId = reversal.id,
                action = "VOID",
                payload = payload,
                createdAt = reversal.createdAt,
                updatedAt = reversal.createdAt
            )
        )
        return true
    }

    /**
     * تسجيل قرار عضو على قيد **مع جدولة إرساله** في المعاملة نفسها: القرار الذي لا يُرسل لا يعلم
     * به الطرف الآخر، فيبقى القيد معلّقًا في نظر الجهازين — وهذا بالضبط ما يجعل الإقرار ينتقل
     * في حزمة التسليم (ح١٩) أو في أي قناة لاحقة بلا مسار خاص.
     */
    @Transaction
    suspend fun recordDecisionAndEnqueue(
        entryId: String,
        memberId: String,
        decision: String,
        note: String,
        decidedAt: Long,
        resultingStatus: String,
        updatedAt: Long,
        operationId: String,
        payload: String
    ) {
        recordDecision(
            entryId = entryId,
            memberId = memberId,
            decision = decision,
            note = note,
            decidedAt = decidedAt,
            resultingStatus = resultingStatus,
            updatedAt = updatedAt
        )
        enqueueOutbox(
            OutboxItem(
                operationId = operationId,
                entityType = "ack",
                entityId = entryId,
                action = "UPSERT",
                payload = payload,
                createdAt = decidedAt,
                updatedAt = decidedAt
            )
        )
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
        if (payment.roomId != debt.roomId) return false
        if (payment.type !in EntryType.credits) return false
        if (debt.type !in EntryType.debts) return false
        // الاتجاه: القيد مرآة الدين. لا يُسقَط سداد على دين لشخص آخر في الغرفة.
        if (payment.owedByMemberId != debt.owedToMemberId) return false
        if (payment.owedToMemberId != debt.owedByMemberId) return false
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
