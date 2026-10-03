package com.baynana.data.local.ledger

import androidx.room.withTransaction
import com.baynana.core.database.AppDatabase
import com.baynana.domain.ledger.AllocationEngine
import com.baynana.domain.ledger.AllocationMode
import com.baynana.domain.ledger.AllocationPlan
import com.baynana.domain.ledger.AllocationView
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.EntryView
import com.baynana.domain.ledger.NewDebtSpec
import com.baynana.domain.ledger.ReceiptSpec
import com.baynana.domain.ledger.ReversalCheck
import com.baynana.domain.ledger.ReversalEngine
import com.baynana.domain.ledger.RoomBalance
import com.baynana.domain.ledger.LedgerSnapshot
import com.baynana.domain.ledger.MemberStatement
import com.baynana.domain.ledger.SnapshotEngine
import com.baynana.domain.ledger.StatementEngine
import com.baynana.domain.ledger.allocationsByEntry
import java.util.UUID

/**
 * منفّذ عمليات الدفتر: يربط محرّك `domain` النقي بقاعدة Room.
 *
 * القواعد المنفَّذة هنا (وليس في الشاشة):
 * - **الحفظ محلي أولًا وفي معاملة واحدة**: القيد + إسقاطاته + صف صندوق الصادر معًا، فإما تمّ
 *   كل شيء أو لا شيء. لا قيد محلي بلا إرسال مجدول، ولا إسقاط بلا قيد.
 * - **إعادة التشغيل لا تُضاعف**: المعرّف الثابت `operationId` يعود بلا كتابة ثانية، ولا يُخصَّص
 *   السداد مرتين. هذه بوابة «replays must not double-deduct».
 * - **القبض العام لا يُغلق دينًا**: يُسجَّل رصيدًا دائنًا في الغرفة، ويُخصَّص لاحقًا بقرار ظاهر
 *   بلا إعادة تأريخ تاريخ الدفع الأصلي.
 * - **الإلغاء بقيد عكسي** لا بالحذف، وتُحرَّر إسقاطات الأصل في المعاملة نفسها.
 */
class LedgerRepository(
    private val db: AppDatabase,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val dao get() = db.ledgerDao()

    /** نتيجة حفظ قيد: [created] = false تعني أن العملية وصلت سابقًا فلم يُكتب صف ثانٍ. */
    data class EntryResult(val entry: LedgerEntry, val created: Boolean)

    /**
     * نتيجة تسجيل سداد/قبض: ما خُصِّص، وما بقي رصيدًا دائنًا، والأسطر التي لم تُقبل.
     *
     * عند [created] = false (إعادة نداء بنفس `operationId`) تصف الخطة **حالة القيد كما هي الآن**:
     * الإسقاطات السابقة قائمة وما أُضيف شيء، فلا يتضاعف الخصم.
     */
    data class ReceiptResult(
        val entry: LedgerEntry,
        val created: Boolean,
        val plan: AllocationPlan
    ) {
        val unappliedMinor: Long get() = plan.unappliedMinor
    }

    /** نتيجة الإلغاء: القيد العكسي، وهل أُنشئ الآن أم كان موجودًا. */
    data class ReversalResult(val reversal: LedgerEntry, val created: Boolean, val releasedAllocations: Int)

    // ------------------------------------------------------------------ الأرصدة

    suspend fun balance(roomId: String): RoomBalance = snapshot(roomId).toRoomBalance()

    /**
     * اللقطة الموحّدة للغرفة (ح٥): كل رقم يظهر في أي شاشة أو ملف يأتي من هنا.
     *
     * القيود الملغاة تُقرأ ** أيضًا** لأن اللقطة تحتاج معرفة أن القيد العكسي يقابل أصلًا ملغى؛
     * واللقطة هي التي تُسقط الملغى والعكسي من الحساب (انظر `effectiveEntries`).
     */
    suspend fun snapshot(roomId: String): LedgerSnapshot {
        val room = dao.getRoom(roomId) ?: error("غرفة غير موجودة: $roomId")
        val entries = dao.getEntriesIncludingVoided(roomId).map { it.toView() }
        val allocations = dao.getAllocationsInRoom(roomId).map { it.toView() }
        return SnapshotEngine.build(roomId, room.currency, entries, allocations)
    }

    /** كشف عضو واحد (المزارع في قالب الري): نفس أرقام اللقطة، مرتبة زمنيًا برصيد جارٍ. */
    suspend fun statementFor(roomId: String, memberId: String): MemberStatement =
        StatementEngine.statementFor(memberId, snapshot(roomId))

    /** الديون المفتوحة على عضو، الأقدم أولًا — نفس الترتيب الذي سيستخدمه FIFO. */
    suspend fun openDebts(roomId: String, memberId: String): List<LedgerEntry> {
        val entries = dao.getEntriesIncludingVoided(roomId).map { it.toView() }
        val allocations = dao.getAllocationsInRoom(roomId).map { it.toView() }
        val ids = SnapshotEngine.openDebtsFor(memberId, entries, allocations).map { it.id }.toSet()
        return dao.getEntriesIncludingVoided(roomId).filter { it.id in ids }
            .sortedWith(compareBy({ it.occurredAt }, { it.id }))
    }

    // ------------------------------------------------------------- إدخال القيود

    /**
     * يسجّل سقية أو دين سلعة أو صلحًا في دفتر الكاتب. المعرّف الثابت [spec].[operationId] يمنع
     * الازدواج: إعادة النداء بنفس المعرّف تُرجع القيد القائم بلا كتابة.
     */
    suspend fun recordDebt(spec: NewDebtSpec): EntryResult = db.withTransaction {
        val existing = dao.getEntryByOperationId(spec.operationId)
        if (existing != null) return@withTransaction EntryResult(existing, created = false)

        val entry = LedgerEntry(
            id = spec.id,
            roomId = spec.roomId,
            operationId = spec.operationId,
            type = spec.type,
            owedByMemberId = spec.debtorMemberId,
            owedToMemberId = spec.creditorMemberId,
            amountMinor = spec.amountMinor,
            currency = spec.currency,
            occurredAt = spec.occurredAt,
            description = spec.description,
            quantityNote = spec.quantityNote,
            status = EntryStatus.SENT,
            createdByMemberId = spec.createdByMemberId,
            listingId = spec.listingId,
            createdAt = spec.occurredAt,
            updatedAt = spec.occurredAt
        )
        val inserted = dao.insertEntryWithAllocationsAndEnqueue(
            entry = entry,
            payload = OutboxPayloads.entry(entry),
            allocations = emptyList()
        )
        if (!inserted) {
            // سباق نادر: نفس operationId كُتب بين القراءة والكتابة.
            return@withTransaction EntryResult(dao.getEntryByOperationId(spec.operationId) ?: entry, created = false)
        }
        EntryResult(entry, created = true)
    }

    /**
     * يسجّل سدادًا أو قبضًا عامًّا ويخصّصه حسب [spec].[mode]:
     * - [AllocationMode.None]: قبض عام لا يُغلق أي دين.
     * - [AllocationMode.OldestFirst]: الأقدم فالأقدم داخل الغرفة والعملة والاتجاه.
     * - [AllocationMode.Selected]: الأسطر المختارة فقط.
     *
     * الفائض يبقى رصيدًا دائنًا في الغرفة. الاتجاه مقلوب في القيد كما تقتضي قاعدة المرآة:
     * من استلم المال صار عليه، ومن دفعه صار له.
     */
    suspend fun recordReceipt(spec: ReceiptSpec): ReceiptResult = db.withTransaction {
        val existing = dao.getEntryByOperationId(spec.operationId)
        if (existing != null) {
            return@withTransaction ReceiptResult(existing, created = false, plan = planFor(existing, spec.mode))
        }

        val entry = LedgerEntry(
            id = spec.id,
            roomId = spec.roomId,
            operationId = spec.operationId,
            type = if (spec.mode is AllocationMode.None) EntryType.GENERAL_RECEIPT else EntryType.PAYMENT,
            owedByMemberId = spec.creditorMemberId,
            owedToMemberId = spec.debtorMemberId,
            amountMinor = spec.amountMinor,
            currency = spec.currency,
            occurredAt = spec.occurredAt,
            description = spec.description,
            status = EntryStatus.SENT,
            createdByMemberId = spec.createdByMemberId,
            createdAt = spec.occurredAt,
            updatedAt = spec.occurredAt
        )

        val view = entry.toView()
        val roomEntries = dao.getActiveEntries(spec.roomId).map { it.toView() }
        val existingAllocations = dao.getAllocationsInRoom(spec.roomId).map { it.toView() }
        val plan = AllocationEngine.plan(view, roomEntries, allocationsByEntry(existingAllocations), spec.mode)

        val rows = plan.allocations.map { planned ->
            EntryAllocation(
                paymentEntryId = entry.id,
                debtEntryId = planned.debtEntryId,
                amountMinor = planned.amountMinor,
                currency = entry.currency,
                createdAt = spec.occurredAt
            )
        }

        val inserted = dao.insertEntryWithAllocationsAndEnqueue(
            entry = entry,
            payload = OutboxPayloads.receipt(entry, rows, spec.mode, plan.unappliedMinor),
            allocations = rows
        )
        if (!inserted) {
            val winner = dao.getEntryByOperationId(spec.operationId) ?: entry
            return@withTransaction ReceiptResult(winner, created = false, plan = planFor(winner, spec.mode))
        }
        ReceiptResult(entry, created = true, plan = plan)
    }

    /**
     * تخصيص قبض عام **لاحقًا**: يُضاف الإسقاط بتاريخ القرار ([decidedAt])، ولا يُعاد تأريخ
     * تاريخ الدفع الأصلي، ويُسجَّل الحدث في صندوق الصادر بسبب معلن.
     */
    suspend fun allocateExistingReceipt(
        receiptEntryId: String,
        mode: AllocationMode,
        operationId: String,
        decidedAt: Long = now(),
        reason: String = ""
    ): AllocationPlan = db.withTransaction {
        val receipt = dao.getEntry(receiptEntryId) ?: error("قيد غير موجود: $receiptEntryId")
        val view = receipt.toView()
        val roomEntries = dao.getActiveEntries(receipt.roomId).map { it.toView() }
        val existing = dao.getAllocationsInRoom(receipt.roomId).map { it.toView() }
        val byEntry = allocationsByEntry(existing)
        val plan = AllocationEngine.plan(view, roomEntries, byEntry, mode)

        // العملية نفسها لا تُكرَّر: صف الصادر بمفتاح العملية يمنع التخصيص المزدوج عند إعادة النداء.
        val enqueued = dao.enqueueOutbox(
            OutboxItem(
                operationId = operationId,
                entityType = "entry",
                entityId = receipt.id,
                action = "ALLOCATE",
                payload = OutboxPayloads.allocation(receipt, plan, reason, decidedAt),
                createdAt = decidedAt,
                updatedAt = decidedAt
            )
        )
        if (enqueued == -1L) return@withTransaction plan

        plan.allocations.forEach { planned ->
            dao.upsertAllocation(
                EntryAllocation(
                    paymentEntryId = receipt.id,
                    debtEntryId = planned.debtEntryId,
                    amountMinor = planned.amountMinor,
                    currency = receipt.currency,
                    createdAt = decidedAt
                )
            )
        }
        plan
    }

    /**
     * إلغاء قيد بقيد عكسي. لا حذف: القيد الأصلي يبقى للتاريخ بحالة [EntryStatus.VOIDED]، ويُدرج
     * قيد عكسي ظاهر للطرفين، وتُحرَّر إسقاطات الأصل في المعاملة نفسها.
     */
    suspend fun reverseEntry(
        entryId: String,
        reason: String,
        operationId: String,
        occurredAt: Long = now()
    ): ReversalResult = db.withTransaction {
        val existingReversal = dao.getReversalOf(entryId)
        if (existingReversal != null) {
            return@withTransaction ReversalResult(existingReversal, created = false, releasedAllocations = 0)
        }
        val original = dao.getEntry(entryId) ?: error("قيد غير موجود: $entryId")
        val originalView = original.toView()
        when (val check = ReversalEngine.check(originalView, null)) {
            is ReversalCheck.Refused -> error(check.reason)
            ReversalCheck.Allowed -> Unit
        }
        val before = dao.getAllocationsForEntry(entryId).size

        val mirrorView = ReversalEngine.mirror(
            original = originalView,
            reversalId = newId(),
            operationId = operationId,
            occurredAt = occurredAt,
            reason = reason
        )
        val reversal = mirrorView.toEntity(createdByMemberId = original.createdByMemberId)
        val inserted = dao.insertReversalAndVoid(
            reversal = reversal,
            originalEntryId = entryId,
            payload = OutboxPayloads.reversal(original, reversal, reason, operationId),
            updatedAt = occurredAt
        )
        if (!inserted) {
            return@withTransaction ReversalResult(
                dao.getReversalOf(entryId) ?: reversal,
                created = false,
                releasedAllocations = 0
            )
        }
        ReversalResult(reversal, created = true, releasedAllocations = before)
    }

    /**
     * حذف مسودة لم تُشارك بعد. الحذف ممنوع بعد المشاركة (§4.4)؛ هنا فقط حيث لا طرف آخر ولا
     * صف صادر مرسل. يُرجع false إن لم تكن مسودة أو كان لها إرسال.
     */
    suspend fun discardDraft(entryId: String): Boolean = db.withTransaction {
        val entry = dao.getEntry(entryId) ?: return@withTransaction false
        if (entry.status != EntryStatus.DRAFT) return@withTransaction false
        if (dao.countOutboxForEntity(entryId) > 0) return@withTransaction false
        dao.releaseAllocationsForEntry(entryId)
        dao.deleteEntry(entryId)
        true
    }

    private suspend fun planFor(receipt: LedgerEntry, mode: AllocationMode): AllocationPlan {
        val entries = dao.getActiveEntries(receipt.roomId).map { it.toView() }
        val allocations = dao.getAllocationsInRoom(receipt.roomId).map { it.toView() }
        return AllocationEngine.plan(receipt.toView(), entries, allocationsByEntry(allocations), mode)
    }
}

/** تحويل صف Room إلى صورة نقية يفهمها المحرّك. */
fun LedgerEntry.toView(): EntryView = EntryView(
    id = id,
    operationId = operationId,
    roomId = roomId,
    type = type,
    owedByMemberId = owedByMemberId,
    owedToMemberId = owedToMemberId,
    amountMinor = amountMinor,
    currency = currency,
    occurredAt = occurredAt,
    status = status,
    description = description,
    reversesEntryId = reversesEntryId
)

/** تحويل صورة نقية إلى صف Room (يُستخدم في بناء القيد العكسي). */
fun EntryView.toEntity(createdByMemberId: String): LedgerEntry = LedgerEntry(
    id = id,
    roomId = roomId,
    operationId = operationId,
    type = type,
    owedByMemberId = owedByMemberId,
    owedToMemberId = owedToMemberId,
    amountMinor = amountMinor,
    currency = currency,
    occurredAt = occurredAt,
    description = description,
    status = status,
    createdByMemberId = createdByMemberId,
    reversesEntryId = reversesEntryId,
    createdAt = occurredAt,
    updatedAt = occurredAt
)

fun EntryAllocation.toView(): AllocationView =
    AllocationView(paymentEntryId = paymentEntryId, debtEntryId = debtEntryId, amountMinor = amountMinor)
