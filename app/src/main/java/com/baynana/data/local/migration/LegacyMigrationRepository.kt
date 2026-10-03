package com.baynana.data.local.migration

import androidx.room.withTransaction
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerEntry
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.RoomMember
import com.baynana.data.local.ledger.SyncState
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.migration.CustomerReconciliation
import com.baynana.domain.migration.LegacyCustomerSnapshot
import com.baynana.domain.migration.LegacyMigrationEngine
import com.baynana.domain.migration.LegacyMigrationPlan
import com.baynana.domain.migration.LegacySessionSnapshot
import com.baynana.domain.migration.LegacySnapshot
import com.baynana.domain.migration.LegacyVoucherSnapshot
import com.baynana.domain.migration.PlannedEntry
import com.baynana.domain.migration.PlannedRoom
import com.baynana.features.customers.Customer
import com.baynana.features.sessions.WaterSession
import com.baynana.features.vouchers.Voucher

/**
 * تنفيذ الترحيل من الإرث على القاعدة: قراءة فقط من جداول الإرث، وكتابة في جداول الدفتر داخل
 * معاملة واحدة لكل عميل.
 *
 * المبادئ المنفَّذة:
 * - **الإرث لا يُمَس**: لا حذف ولا تعديل لأي صف في `customers`/`water_sessions`/`vouchers`؛
 *   الترحيل نسخٌ إلى الدفتر الجديد مع ربط المصدر.
 * - **بلا اختراع تاريخ**: تواريخ القيود من السقيات نفسها.
 * - **لا ازدواج**: معرّف العملية مشتق من رقم السقية، وإعادة الترحيل لا تُنشئ صفًا ثانيًا.
 * - **جرد مطابق**: كل عميل يُرحَّل بمقارنة صريحة (قديم = جديد)؛ وأي فرق يوقف ترحيل ذلك العميل
 *   ويُعلن، فلا تدخل أرقام نصف صحيحة إلى دفاتر الناس.
 */
class LegacyMigrationRepository(private val db: AppDatabase) {

    companion object {
        const val MARKER_KEY = "legacy-migration"
    }

    /** يقرأ الإرث كما هو ويبني الخطة — بلا أي كتابة. */
    suspend fun buildPlan(currency: String = "YER_NEW"): LegacyMigrationPlan {
        val dao = db.ledgerDao()
        val customers: List<Customer> = db.customerDao().getAllCustomersForBackup()
        val sessions: List<WaterSession> = db.waterSessionDao().getAllSessionsForBackup()
        val vouchers: List<Voucher> = db.voucherDao().getAllVouchersForBackup()

        val snapshot = LegacySnapshot(
            customers = customers.map {
                LegacyCustomerSnapshot(
                    id = it.id,
                    name = it.name,
                    phone = it.phone,
                    farmName = it.farmName,
                    isArchived = it.isArchived,
                    linkCode = it.linkCode
                )
            },
            sessions = sessions.map {
                LegacySessionSnapshot(
                    id = it.id,
                    customerId = it.customerId,
                    startTime = it.startTime,
                    totalAmount = it.totalAmount,
                    amountPaid = it.amountPaid,
                    remainingDebt = it.remainingDebt,
                    isLive = it.isLive,
                    pumpName = it.pumpName
                )
            },
            vouchers = vouchers.map {
                LegacyVoucherSnapshot(
                    id = it.id,
                    customerId = it.customerId,
                    sessionId = it.sessionId,
                    type = it.type.name,
                    amount = it.amount,
                    date = it.date
                )
            }
        )
        return LegacyMigrationEngine.plan(snapshot, currency)
    }

    /**
     * يطبّق الخطة. النتيجة: عدد الغرف والقيود المكتوبة، وعدد الذي كان مُرحَّلًا سابقًا، وقائمة
     * العملاء الذين أُوقف ترحيلهم بسبب فرق في الجرد مع سببه.
     *
     * @param onlyCustomers إن حُدِّدت، يُرحَّل هؤلاء فقط (للترحيل التدريجي أو لإعادة محاولة عميل).
     * @param now وقت التنفيذ (يُسجَّل في العلامة فقط، ولا يُستعمل كتاريخ أي قيد).
     */
    suspend fun apply(
        plan: LegacyMigrationPlan,
        onlyCustomers: Set<Long>? = null,
        now: Long = System.currentTimeMillis()
    ): LegacyMigrationOutcome {
        val roomsByCustomer = plan.rooms.associateBy { it.customerId }
        val entriesByCustomer = plan.entries.groupBy { entry ->
            // الغرفة تحمل رقم العميل في معرّفها؛ والمطابقة هنا صريحة لا بالتخمين.
            roomCustomerId(entry.roomId)
        }
        val reasons = plan.reconciliations.associate { it.customerId to it }

        var roomsWritten = 0
        var entriesWritten = 0
        var alreadyDone = 0
        val refused = mutableListOf<CustomerReconciliation>()

        for ((customerId, reconciliation) in reasons.toSortedMap()) {
            if (onlyCustomers != null && customerId !in onlyCustomers) continue
            if (!reconciliation.matches) {
                // الجرد لا يطابق: لا كتابة لهذا العميل، ويُعلن السبب.
                refused += reconciliation
                continue
            }
            val room = roomsByCustomer[customerId] ?: continue
            val customerEntries = entriesByCustomer[customerId].orEmpty()

            val result = db.withTransaction {
                val existingRoom = db.ledgerDao().getRoom(room.roomId)
                writeRoom(room, now)
                val written = customerEntries.count { writeEntry(it, room, now) }
                Triple(existingRoom == null, written, customerEntries.size)
            }

            if (result.first) roomsWritten++
            entriesWritten += result.second
            alreadyDone += result.third - result.second
        }

        // العلامة تُخبر بالحقيقة: كم قيدًا من الخطة صار موجودًا فعلًا في الدفتر بعد هذه الجولة
        // (بعد ترحيل جزئي لا تكتب «اكتمل»)، وكم عميلًا أُوقف ولماذا.
        val appliedRooms = plan.rooms
            .filter { onlyCustomers == null || it.customerId in onlyCustomers }
            .map { it.roomId }
        val plannedIds = plan.entries.map { it.id }.toSet()
        val presentNow = appliedRooms.sumOf { roomId ->
            db.ledgerDao().getEntriesIncludingVoided(roomId).count { it.id in plannedIds }
        }

        db.ledgerDao().upsertSyncState(
            SyncState(
                key = MARKER_KEY,
                cursor = presentNow.toString(),
                lastSyncAt = now,
                lastError = refused.joinToString("؛ ") { "${it.customerName}: فرق ${it.differenceMinor}" }
            )
        )

        return LegacyMigrationOutcome(
            roomsWritten = roomsWritten,
            entriesWritten = entriesWritten,
            entriesAlreadyPresent = alreadyDone,
            refusedCustomers = refused,
            plan = plan
        )
    }

    private suspend fun writeRoom(room: PlannedRoom, now: Long) {
        val dao = db.ledgerDao()
        // سلامة إضافية: لو كان الكود محجوزًا لغرفة أخرى قائمة، لا نكتب غرفة بكود مزدوج.
        val taken = dao.getRoomByLinkCode(room.linkCode)
        val linkCode = if (taken != null && taken.id != room.roomId) "legacy-${room.customerId}" else room.linkCode
        dao.upsertRoom(
            LedgerRoom(
                id = room.roomId,
                kind = RoomKind.WATER,
                currency = room.currency,
                title = room.title,
                // عميل مؤرشف: الغرفة للقراءة فقط. وسجلاته باقية كما هي.
                status = if (room.archived) RoomStatus.CLOSED else RoomStatus.ACTIVE,
                linkCode = linkCode,
                counterpartName = room.counterpartName,
                counterpartPhone = room.counterpartPhone,
                createdAt = now,
                updatedAt = now,
                closedAt = if (room.archived) now else null
            )
        )
        dao.upsertMember(
            RoomMember(
                roomId = room.roomId,
                memberId = room.ownerMemberId,
                displayName = "أنا",
                isMe = true,
                joinedAt = now
            )
        )
        dao.upsertMember(
            RoomMember(
                roomId = room.roomId,
                memberId = room.customerMemberId,
                displayName = room.counterpartName,
                phone = room.counterpartPhone,
                role = "legacy-customer",
                joinedAt = now
            )
        )
    }

    /** يكتب قيدًا واحدًا. تُرجع true إن كُتب الآن، وfalse إن كان موجودًا (إعادة ترحيل). */
    private suspend fun writeEntry(entry: PlannedEntry, room: PlannedRoom, now: Long): Boolean {
        val isCredit = entry.debtDirection < 0
        val ledgerEntry = LedgerEntry(
            id = entry.id,
            roomId = entry.roomId,
            operationId = entry.operationId,
            type = entry.type,
            // الاتجاه: الدين على العميل، والرصيد الدائن على صاحب الجهاز (قاعدة المرآة نفسها).
            owedByMemberId = if (isCredit) room.ownerMemberId else room.customerMemberId,
            owedToMemberId = if (isCredit) room.customerMemberId else room.ownerMemberId,
            amountMinor = entry.amountMinor,
            currency = entry.currency,
            occurredAt = entry.occurredAt,
            description = entry.description,
            quantityNote = "",
            // مُرحَّل من الإرث: مُرسل ومقبول ضمنيًا لصاحبه، ولا يُنتظر إقرار طرف لم يكن في التطبيق القديم.
            status = EntryStatus.ACKNOWLEDGED,
            createdByMemberId = room.ownerMemberId,
            sourceTable = entry.sourceTable,
            sourceId = entry.sourceId,
            createdAt = entry.occurredAt,
            updatedAt = now
        )
        return db.ledgerDao().insertEntryIfNew(ledgerEntry) != -1L
    }
}

private fun roomCustomerId(roomId: String): Long =
    roomId.removePrefix("legacy-room-").toLongOrNull() ?: -1L

/** نتيجة التنفيذ: أرقام الجرد قبل/بعد والعملاء المرفوضون. */
data class LegacyMigrationOutcome(
    val roomsWritten: Int,
    val entriesWritten: Int,
    val entriesAlreadyPresent: Int,
    val refusedCustomers: List<CustomerReconciliation>,
    val plan: LegacyMigrationPlan
) {
    val isClean: Boolean get() = refusedCustomers.isEmpty()

    /** صف عربي واحد يُعرض للمستخدم بعد الترحيل. */
    fun summaryText(): String = buildString {
        append("غرف جديدة: $roomsWritten")
        append(" • قيود مُرحَّلة: $entriesWritten")
        if (entriesAlreadyPresent > 0) append(" • كانت مُرحَّلة: $entriesAlreadyPresent")
        append(" • مجموع الديون: ${plan.legacyTotalMinor} فلسًا")
        if (!isClean) append(" • أُوقف ترحيل ${refusedCustomers.size} عميلًا لاختلاف الجرد")
    }
}
