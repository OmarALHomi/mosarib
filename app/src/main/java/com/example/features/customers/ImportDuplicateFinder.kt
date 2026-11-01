package com.example.features.customers

import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

enum class DuplicateReason(val displayLabel: String) {
    TYPE_MISMATCH_TWIN("سقي مستورد ↔ سند صرف"),
    EXACT_TWIN("مكرر")
}

data class DuplicateCandidate(
    val id: Long,
    val isSession: Boolean,
    val customerId: Long,
    val amount: Double,
    val notes: String,
    val date: Long,
    val reason: DuplicateReason
)

data class FilteredImportResult(
    val operationsToInsert: List<ImportedOperationItem>,
    val shouldInsertInitialBalance: Boolean,
    val skippedCount: Int
)

object ImportDuplicateFinder {

    fun isSessionImported(session: WaterSession): Boolean {
        return session.pumpName in setOf("سقي / قيد مستورد", "رصيد افتتاحي")
    }

    fun isVoucherImported(voucher: Voucher): Boolean {
        return voucher.paymentMethod == "رصيد سابق"
    }

    fun isSameCalendarDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getCalendarDayKey(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.YEAR).toLong() * 1000L + cal.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Scan imported sessions and vouchers to find duplicate candidates.
     * 1. Exact twins within imported sessions and within imported vouchers (same customer, type, amount, notes, same day).
     * 2. Type mismatch twins between imported sessions (pumpName "سقي / قيد مستورد", duration 0) and imported EXPENSE vouchers.
     * Pairs each record at most once.
     */
    fun find(
        sessions: List<WaterSession>,
        vouchers: List<Voucher>
    ): List<DuplicateCandidate> {
        val duplicates = mutableListOf<DuplicateCandidate>()
        val pairedSessionIds = mutableSetOf<Long>()
        val pairedVoucherIds = mutableSetOf<Long>()

        // 1. Exact twins within imported sessions
        val importedSessions = sessions.filter { isSessionImported(it) }
        var currentSessionCust: Long? = null
        val sessionCustRunCount = mutableMapOf<Long, Int>()
        val sessionRunMap = mutableMapOf<Long, Int>()
        for (s in importedSessions.sortedBy { it.id }) {
            if (s.customerId != currentSessionCust) {
                currentSessionCust = s.customerId
                sessionCustRunCount[s.customerId] = (sessionCustRunCount[s.customerId] ?: -1) + 1
            }
            sessionRunMap[s.id] = sessionCustRunCount[s.customerId]!!
        }

        val sessionGroups = importedSessions.groupBy { s ->
            "${s.customerId}|${s.pumpName}|${s.notes.trim()}|${(s.totalAmount * 100.0).roundToLong()}|${getCalendarDayKey(s.startTime)}"
        }

        val keptSessions = mutableListOf<WaterSession>()
        for ((_, group) in sessionGroups) {
            val runsForGroup = group.groupBy { sessionRunMap[it.id] ?: 0 }
            if (runsForGroup.size <= 1) {
                // When the whole group is in one run, it has 0 duplicates
                keptSessions.addAll(group)
            } else {
                val maxCountPerRun = runsForGroup.values.maxOf { it.size }
                val sorted = group.sortedBy { it.id }
                val toKeep = sorted.take(maxCountPerRun)
                val toDuplicate = sorted.drop(maxCountPerRun)
                keptSessions.addAll(toKeep)
                for (dup in toDuplicate) {
                    duplicates.add(
                        DuplicateCandidate(
                            id = dup.id,
                            isSession = true,
                            customerId = dup.customerId,
                            amount = dup.totalAmount,
                            notes = dup.notes,
                            date = dup.startTime,
                            reason = DuplicateReason.EXACT_TWIN
                        )
                    )
                    pairedSessionIds.add(dup.id)
                }
            }
        }

        // 2. Exact twins within imported vouchers
        val importedVouchers = vouchers.filter { isVoucherImported(it) }
        var currentVoucherCust: Long? = null
        val voucherCustRunCount = mutableMapOf<Long, Int>()
        val voucherRunMap = mutableMapOf<Long, Int>()
        for (v in importedVouchers.sortedBy { it.id }) {
            val custId = v.customerId ?: -1L
            if (custId != currentVoucherCust) {
                currentVoucherCust = custId
                voucherCustRunCount[custId] = (voucherCustRunCount[custId] ?: -1) + 1
            }
            voucherRunMap[v.id] = voucherCustRunCount[custId]!!
        }

        val voucherGroups = importedVouchers.groupBy { v ->
            "${v.customerId ?: -1L}|${v.type}|${v.category}|${v.notes.trim()}|${(v.amount * 100.0).roundToLong()}|${getCalendarDayKey(v.date)}"
        }

        val keptVouchers = mutableListOf<Voucher>()
        for ((_, group) in voucherGroups) {
            val runsForGroup = group.groupBy { voucherRunMap[it.id] ?: 0 }
            if (runsForGroup.size <= 1) {
                // When the whole group is in one run, it has 0 duplicates
                keptVouchers.addAll(group)
            } else {
                val maxCountPerRun = runsForGroup.values.maxOf { it.size }
                val sorted = group.sortedBy { it.id }
                val toKeep = sorted.take(maxCountPerRun)
                val toDuplicate = sorted.drop(maxCountPerRun)
                keptVouchers.addAll(toKeep)
                for (dup in toDuplicate) {
                    duplicates.add(
                        DuplicateCandidate(
                            id = dup.id,
                            isSession = false,
                            customerId = dup.customerId ?: -1L,
                            amount = dup.amount,
                            notes = dup.notes,
                            date = dup.date,
                            reason = DuplicateReason.EXACT_TWIN
                        )
                    )
                    pairedVoucherIds.add(dup.id)
                }
            }
        }

        // 3. Type mismatch twins between remaining kept sessions and kept vouchers
        // Session: pumpName == "سقي / قيد مستورد" && durationMinutes == 0
        // Voucher: category == "صرف مستورد" && type == VoucherType.EXPENSE
        val typeMismatchSessions = keptSessions.filter {
            it.pumpName == "سقي / قيد مستورد" && it.durationMinutes == 0 && !pairedSessionIds.contains(it.id)
        }
        val candidateVouchers = keptVouchers.filter {
            it.type == VoucherType.EXPENSE && it.category == "صرف مستورد" && !pairedVoucherIds.contains(it.id)
        }.toMutableList()

        for (session in typeMismatchSessions) {
            val matchIdx = candidateVouchers.indexOfFirst { v ->
                v.customerId == session.customerId &&
                    abs(session.totalAmount - v.amount) < 0.01 &&
                    session.notes.trim() == v.notes.trim()
            }
            if (matchIdx != -1) {
                val matchedVoucher = candidateVouchers.removeAt(matchIdx)
                pairedSessionIds.add(session.id)
                pairedVoucherIds.add(matchedVoucher.id)
                duplicates.add(
                    DuplicateCandidate(
                        id = session.id,
                        isSession = true,
                        customerId = session.customerId,
                        amount = session.totalAmount,
                        notes = session.notes,
                        date = session.startTime,
                        reason = DuplicateReason.TYPE_MISMATCH_TWIN
                    )
                )
            }
        }

        return duplicates
    }

    /**
     * Determines whether an imported operation item already exists for this customer as an imported record.
     */
    fun isOperationAlreadyImported(
        customerId: Long,
        op: ImportedOperationItem,
        existingSessions: List<WaterSession>,
        existingVouchers: List<Voucher>,
        matchedSessionIds: MutableSet<Long> = mutableSetOf(),
        matchedVoucherIds: MutableSet<Long> = mutableSetOf()
    ): Boolean {
        val opTrimmedNotes = op.description.trim()
        val opDate = op.date

        if (op.type == "DEBIT") {
            val defaultSessionNotes = "عملية مستوردة من سجل خارجي"
            val defaultExpenseNotes = "سند صرف مستورد من سجل خارجي"

            // 1. Try matching an existing imported session
            val sessionMatch = existingSessions.find { s ->
                s.customerId == customerId &&
                    isSessionImported(s) &&
                    !matchedSessionIds.contains(s.id) &&
                    abs(s.totalAmount - op.amount) < 0.01 &&
                    (s.notes.trim() == opTrimmedNotes || (opTrimmedNotes.isEmpty() && s.notes.trim() == defaultSessionNotes)) &&
                    (opDate == null || isSameCalendarDay(s.startTime, opDate))
            }
            if (sessionMatch != null) {
                matchedSessionIds.add(sessionMatch.id)
                return true
            }

            // 2. Try matching an existing imported EXPENSE voucher
            val voucherMatch = existingVouchers.find { v ->
                v.customerId == customerId &&
                    isVoucherImported(v) &&
                    v.type == VoucherType.EXPENSE &&
                    !matchedVoucherIds.contains(v.id) &&
                    abs(v.amount - op.amount) < 0.01 &&
                    (v.notes.trim() == opTrimmedNotes || (opTrimmedNotes.isEmpty() && v.notes.trim() == defaultExpenseNotes)) &&
                    (opDate == null || isSameCalendarDay(v.date, opDate))
            }
            if (voucherMatch != null) {
                matchedVoucherIds.add(voucherMatch.id)
                return true
            }

            return false
        } else {
            // A CREDIT matches an imported RECEIPT voucher
            val defaultReceiptNotes = "سند قبض مستورد من سجل خارجي"
            val voucherMatch = existingVouchers.find { v ->
                v.customerId == customerId &&
                    isVoucherImported(v) &&
                    v.type == VoucherType.RECEIPT &&
                    !matchedVoucherIds.contains(v.id) &&
                    abs(v.amount - op.amount) < 0.01 &&
                    (v.notes.trim() == opTrimmedNotes || (opTrimmedNotes.isEmpty() && v.notes.trim() == defaultReceiptNotes)) &&
                    (opDate == null || isSameCalendarDay(v.date, opDate))
            }
            if (voucherMatch != null) {
                matchedVoucherIds.add(voucherMatch.id)
                return true
            }

            return false
        }
    }

    /**
     * Determines whether an opening balance already exists as an imported record.
     */
    fun isInitialBalanceAlreadyImported(
        customerId: Long,
        initialBalance: Double,
        isDebit: Boolean,
        rawNotes: String,
        date: Long?,
        existingSessions: List<WaterSession>,
        existingVouchers: List<Voucher>,
        matchedSessionIds: MutableSet<Long> = mutableSetOf(),
        matchedVoucherIds: MutableSet<Long> = mutableSetOf()
    ): Boolean {
        if (initialBalance <= 0.0) return false
        val trimmedNotes = rawNotes.trim()

        if (isDebit) {
            val expectedNotes = if (trimmedNotes.isNotBlank()) "رصيد سابق: $trimmedNotes" else "رصيد ودين سابق مستورد من دفتر الحسابات"

            // 1. Session match
            val sessionMatch = existingSessions.find { s ->
                s.customerId == customerId &&
                    isSessionImported(s) &&
                    !matchedSessionIds.contains(s.id) &&
                    abs(s.totalAmount - initialBalance) < 0.01 &&
                    (s.notes.trim() == expectedNotes || s.pumpName == "رصيد افتتاحي" || s.notes.trim().startsWith("رصيد سابق:")) &&
                    (date == null || isSameCalendarDay(s.startTime, date) || s.pumpName == "رصيد افتتاحي")
            }
            if (sessionMatch != null) {
                matchedSessionIds.add(sessionMatch.id)
                return true
            }

            // 2. Voucher match
            val voucherMatch = existingVouchers.find { v ->
                v.customerId == customerId &&
                    isVoucherImported(v) &&
                    v.type == VoucherType.EXPENSE &&
                    !matchedVoucherIds.contains(v.id) &&
                    abs(v.amount - initialBalance) < 0.01 &&
                    (v.notes.trim() == expectedNotes || v.category == "رصيد افتتاحي" || v.notes.trim().startsWith("رصيد سابق:")) &&
                    (date == null || isSameCalendarDay(v.date, date) || v.category == "رصيد افتتاحي")
            }
            if (voucherMatch != null) {
                matchedVoucherIds.add(voucherMatch.id)
                return true
            }

            return false
        } else {
            val expectedNotes = if (trimmedNotes.isNotBlank()) "رصيد دائن: $trimmedNotes" else "رصيد دائن سابق مستورد من دفتر الحسابات"
            val voucherMatch = existingVouchers.find { v ->
                v.customerId == customerId &&
                    isVoucherImported(v) &&
                    v.type == VoucherType.RECEIPT &&
                    !matchedVoucherIds.contains(v.id) &&
                    abs(v.amount - initialBalance) < 0.01 &&
                    (v.notes.trim() == expectedNotes || v.category == "رصيد افتتاحي" || v.notes.trim().startsWith("رصيد دائن:")) &&
                    (date == null || isSameCalendarDay(v.date, date) || v.category == "رصيد افتتاحي")
            }
            if (voucherMatch != null) {
                matchedVoucherIds.add(voucherMatch.id)
                return true
            }

            return false
        }
    }

    /**
     * Filters draft operations and initial balance to prevent re-importing duplicate records.
     */
    fun filterOperationsToImport(
        targetCustomerId: Long,
        draft: ImportedCustomerDraft,
        existingSessions: List<WaterSession>,
        existingVouchers: List<Voucher>,
        matchedSessionIds: MutableSet<Long> = mutableSetOf(),
        matchedVoucherIds: MutableSet<Long> = mutableSetOf()
    ): FilteredImportResult {
        var skippedCount = 0

        if (draft.attachedOperations.size > 1) {
            val toInsert = mutableListOf<ImportedOperationItem>()
            for (op in draft.attachedOperations) {
                val already = isOperationAlreadyImported(
                    customerId = targetCustomerId,
                    op = op,
                    existingSessions = existingSessions,
                    existingVouchers = existingVouchers,
                    matchedSessionIds = matchedSessionIds,
                    matchedVoucherIds = matchedVoucherIds
                )
                if (already) {
                    skippedCount++
                } else {
                    toInsert.add(op)
                }
            }
            return FilteredImportResult(
                operationsToInsert = toInsert,
                shouldInsertInitialBalance = false,
                skippedCount = skippedCount
            )
        } else if (draft.initialBalance > 0.0) {
            val opDate = draft.attachedOperations.firstOrNull()?.date
            val already = isInitialBalanceAlreadyImported(
                customerId = targetCustomerId,
                initialBalance = draft.initialBalance,
                isDebit = draft.isDebit,
                rawNotes = draft.notes,
                date = opDate,
                existingSessions = existingSessions,
                existingVouchers = existingVouchers,
                matchedSessionIds = matchedSessionIds,
                matchedVoucherIds = matchedVoucherIds
            )
            return if (already) {
                FilteredImportResult(
                    operationsToInsert = emptyList(),
                    shouldInsertInitialBalance = false,
                    skippedCount = 1
                )
            } else {
                FilteredImportResult(
                    operationsToInsert = emptyList(),
                    shouldInsertInitialBalance = true,
                    skippedCount = 0
                )
            }
        } else {
            return FilteredImportResult(
                operationsToInsert = emptyList(),
                shouldInsertInitialBalance = false,
                skippedCount = 0
            )
        }
    }
}
