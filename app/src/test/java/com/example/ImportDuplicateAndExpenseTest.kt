package com.example

import com.example.features.customers.Customer
import com.example.features.customers.DuplicateReason
import com.example.features.customers.ImportDuplicateFinder
import com.example.features.customers.ImportedCustomerDraft
import com.example.features.customers.ImportedOperationItem
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ImportDuplicateAndExpenseTest {

    private fun timeOnDay(year: Int, month: Int, day: Int, hour: Int = 10): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, day, hour, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ─────────────────────────────────────────────────────────────────
    // Part A Tests: Re-import Duplicate Prevention
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun testReimportSameDraftsForExistingCustomerInsertsZeroOperations() {
        val customerId = 42L
        val dayTime = timeOnDay(2026, Calendar.OCTOBER, 15, 10)

        val draft = ImportedCustomerDraft(
            name = "حساب تجريبي",
            attachedOperations = listOf(
                ImportedOperationItem(type = "DEBIT", amount = 1200.0, description = "سقي نخيل", date = dayTime),
                ImportedOperationItem(type = "CREDIT", amount = 500.0, description = "دفعة نقدية", date = dayTime)
            )
        )

        // Previous import created these records in DB:
        val existingSessions = listOf(
            WaterSession(
                id = 101L,
                customerId = customerId,
                pumpName = "سقي / قيد مستورد",
                totalAmount = 1200.0,
                notes = "سقي نخيل",
                startTime = dayTime
            )
        )
        val existingVouchers = listOf(
            Voucher(
                id = 201L,
                customerId = customerId,
                type = VoucherType.RECEIPT,
                amount = 500.0,
                notes = "دفعة نقدية",
                paymentMethod = "رصيد سابق",
                date = dayTime
            )
        )

        val result = ImportDuplicateFinder.filterOperationsToImport(
            targetCustomerId = customerId,
            draft = draft,
            existingSessions = existingSessions,
            existingVouchers = existingVouchers
        )

        assertEquals("Should insert 0 operations on re-import", 0, result.operationsToInsert.size)
        assertEquals("Should report 2 skipped operations", 2, result.skippedCount)
        assertFalse(result.shouldInsertInitialBalance)
    }

    @Test
    fun testReimportInitialBalanceForExistingCustomerSkipped() {
        val customerId = 55L
        val dayTime = timeOnDay(2026, Calendar.OCTOBER, 15, 9)

        val draft = ImportedCustomerDraft(
            name = "عميل رصيد سابق",
            initialBalance = 2500.0,
            isDebit = true,
            notes = "دفتر قديم"
        )

        val existingSessions = listOf(
            WaterSession(
                id = 102L,
                customerId = customerId,
                pumpName = "رصيد افتتاحي",
                totalAmount = 2500.0,
                notes = "رصيد سابق: دفتر قديم",
                startTime = dayTime
            )
        )

        val result = ImportDuplicateFinder.filterOperationsToImport(
            targetCustomerId = customerId,
            draft = draft,
            existingSessions = existingSessions,
            existingVouchers = emptyList()
        )

        assertFalse("Initial balance should not be inserted again", result.shouldInsertInitialBalance)
        assertEquals("Should report 1 skipped operation", 1, result.skippedCount)
    }

    @Test
    fun testReimportDebitMatchesExpenseVoucherOrSession() {
        val customerId = 60L
        val dayTime = timeOnDay(2026, Calendar.OCTOBER, 15, 14)

        val draft = ImportedCustomerDraft(
            name = "عميل سند صرف",
            initialBalance = 800.0,
            isDebit = true,
            notes = "صرف بذور",
            attachedOperations = listOf(
                ImportedOperationItem(type = "DEBIT", amount = 800.0, description = "صرف بذور", date = dayTime)
            )
        )

        // Previous import created an EXPENSE voucher rather than a session
        val existingVouchers = listOf(
            Voucher(
                id = 301L,
                customerId = customerId,
                type = VoucherType.EXPENSE,
                category = "رصيد افتتاحي",
                amount = 800.0,
                notes = "رصيد سابق: صرف بذور",
                paymentMethod = "رصيد سابق",
                date = dayTime
            )
        )

        val result = ImportDuplicateFinder.filterOperationsToImport(
            targetCustomerId = customerId,
            draft = draft,
            existingSessions = emptyList(),
            existingVouchers = existingVouchers
        )

        assertEquals("DEBIT matching an existing EXPENSE voucher should be skipped", 0, result.operationsToInsert.size)
        assertEquals(1, result.skippedCount)
        assertFalse(result.shouldInsertInitialBalance)
    }

    @Test
    fun testExistingCustomerSingleOpDraftEarlierOpeningBalanceGetsZeroInserts() {
        val customerId = 70L
        val dayTime = timeOnDay(2026, Calendar.OCTOBER, 10, 9)

        val draft = ImportedCustomerDraft(
            name = "عميل سابق بسجل وحيد",
            initialBalance = 1500.0,
            isDebit = true,
            notes = "دفتر قديم",
            attachedOperations = listOf(
                ImportedOperationItem(type = "DEBIT", amount = 1500.0, description = "دفتر قديم", date = dayTime)
            )
        )

        val existingSessions = listOf(
            WaterSession(
                id = 501L,
                customerId = customerId,
                pumpName = "رصيد افتتاحي",
                totalAmount = 1500.0,
                notes = "رصيد سابق: دفتر قديم",
                startTime = dayTime
            )
        )

        val result = ImportDuplicateFinder.filterOperationsToImport(
            targetCustomerId = customerId,
            draft = draft,
            existingSessions = existingSessions,
            existingVouchers = emptyList()
        )

        assertEquals("Existing customer with single-op draft gets 0 inserts", 0, result.operationsToInsert.size)
        assertFalse("shouldInsertInitialBalance must be false", result.shouldInsertInitialBalance)
        assertEquals("Skipped count must be 1", 1, result.skippedCount)
    }

    @Test
    fun testNewSingleOpCustomerGetsOneOpeningBalanceRecord() {
        val draft = ImportedCustomerDraft(
            name = "عميل جديد بسجل وحيد",
            initialBalance = 2000.0,
            isDebit = true,
            notes = "رصيد افتتاحي للعميل الجديد",
            attachedOperations = listOf(
                ImportedOperationItem(type = "DEBIT", amount = 2000.0, description = "رصيد افتتاحي للعميل الجديد", date = 1000L)
            )
        )

        // For a new customer, existing == null
        val filterResult = com.example.features.customers.FilteredImportResult(
            operationsToInsert = if (draft.attachedOperations.size > 1) draft.attachedOperations else emptyList(),
            shouldInsertInitialBalance = true,
            skippedCount = 0
        )

        assertEquals("No attachedOperations should be inserted individually", 0, filterResult.operationsToInsert.size)
        assertTrue("shouldInsertInitialBalance must be true so 1 opening-balance record is inserted", filterResult.shouldInsertInitialBalance)
        assertFalse("draft.attachedOperations.size > 1 must be false for single-op customer", draft.attachedOperations.size > 1)
        assertTrue("draft.initialBalance > 0.0 must be true", draft.initialBalance > 0.0)
    }

    // ─────────────────────────────────────────────────────────────────
    // Part B Tests: ImportDuplicateFinder
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun testTypeMismatchTwinsDetection() {
        val customerId = 10L
        val dayTime = timeOnDay(2026, Calendar.OCTOBER, 10, 8)

        // Session created before Task 002 (0 minutes)
        val session = WaterSession(
            id = 1L,
            customerId = customerId,
            pumpName = "سقي / قيد مستورد",
            durationMinutes = 0,
            totalAmount = 1800.0,
            notes = "قيد مستورد",
            startTime = dayTime
        )

        // Voucher created after Task 002 for same customer, amount, notes
        val voucher = Voucher(
            id = 2L,
            customerId = customerId,
            type = VoucherType.EXPENSE,
            category = "صرف مستورد",
            paymentMethod = "رصيد سابق",
            amount = 1800.0,
            notes = "قيد مستورد",
            date = dayTime
        )

        val candidates = ImportDuplicateFinder.find(listOf(session), listOf(voucher))

        assertEquals("Should identify exactly 1 duplicate candidate", 1, candidates.size)
        val duplicate = candidates.first()
        assertEquals("The duplicate must be the session", 1L, duplicate.id)
        assertTrue("isSession must be true", duplicate.isSession)
        assertEquals(DuplicateReason.TYPE_MISMATCH_TWIN, duplicate.reason)
        assertEquals(1800.0, duplicate.amount, 0.001)
    }

    @Test
    fun testExactTwinsDetectionForSessionsAndVouchers() {
        val customerId = 20L
        val otherCustomer = 99L
        val day1 = timeOnDay(2026, Calendar.OCTOBER, 12, 10)

        // Session Run 1 (id 10), then intervening customer (id 15), then Session Run 2 (id 20)
        val s1 = WaterSession(id = 10L, customerId = customerId, pumpName = "سقي / قيد مستورد", totalAmount = 300.0, notes = "بيان", startTime = day1)
        val sOther = WaterSession(id = 15L, customerId = otherCustomer, pumpName = "سقي / قيد مستورد", totalAmount = 100.0, notes = "عميل آخر", startTime = day1)
        val s2 = WaterSession(id = 20L, customerId = customerId, pumpName = "سقي / قيد مستورد", totalAmount = 300.0, notes = "بيان", startTime = day1)

        // Voucher Run 1 (id 30), then intervening customer (id 35), then Voucher Run 2 (id 40)
        val v1 = Voucher(id = 30L, customerId = customerId, type = VoucherType.RECEIPT, category = "دفعة مستوردة", paymentMethod = "رصيد سابق", amount = 400.0, notes = "سداد", date = day1)
        val vOther = Voucher(id = 35L, customerId = otherCustomer, type = VoucherType.RECEIPT, category = "دفعة مستوردة", paymentMethod = "رصيد سابق", amount = 100.0, notes = "عميل آخر", date = day1)
        val v2 = Voucher(id = 40L, customerId = customerId, type = VoucherType.RECEIPT, category = "دفعة مستوردة", paymentMethod = "رصيد سابق", amount = 400.0, notes = "سداد", date = day1)

        val candidates = ImportDuplicateFinder.find(listOf(s1, sOther, s2), listOf(v1, vOther, v2))

        assertEquals("Should detect 2 exact twin duplicates (one session, one voucher)", 2, candidates.size)

        val dupSession = candidates.find { it.isSession }
        val dupVoucher = candidates.find { !it.isSession }

        assertEquals("Keep lowest session id (10), duplicate is 20", 20L, dupSession?.id)
        assertEquals(DuplicateReason.EXACT_TWIN, dupSession?.reason)

        assertEquals("Keep lowest voucher id (30), duplicate is 40", 40L, dupVoucher?.id)
        assertEquals(DuplicateReason.EXACT_TWIN, dupVoucher?.reason)
    }

    @Test
    fun testTwoIdenticalInRun1AndTwoCopiesInRun2GivesTwoDuplicates() {
        val custA = 10L
        val custB = 20L
        val day = timeOnDay(2026, Calendar.OCTOBER, 14, 10)

        // Run 1 for custA: two identical records (real repeat in run 1)
        val v1 = Voucher(id = 908L, customerId = custA, type = VoucherType.EXPENSE, category = "صرف مستورد", paymentMethod = "رصيد سابق", amount = 250.0, notes = "بنزين", date = day)
        val v2 = Voucher(id = 909L, customerId = custA, type = VoucherType.EXPENSE, category = "صرف مستورد", paymentMethod = "رصيد سابق", amount = 250.0, notes = "بنزين", date = day)

        // Intervening customer record
        val vIntervening = Voucher(id = 1500L, customerId = custB, type = VoucherType.EXPENSE, category = "صرف مستورد", paymentMethod = "رصيد سابق", amount = 500.0, notes = "زيت", date = day)

        // Run 2 for custA: copy of the two records
        val v3 = Voucher(id = 2566L, customerId = custA, type = VoucherType.EXPENSE, category = "صرف مستورد", paymentMethod = "رصيد سابق", amount = 250.0, notes = "بنزين", date = day)
        val v4 = Voucher(id = 2567L, customerId = custA, type = VoucherType.EXPENSE, category = "صرف مستورد", paymentMethod = "رصيد سابق", amount = 250.0, notes = "بنزين", date = day)

        val candidates = ImportDuplicateFinder.find(emptyList(), listOf(v1, v2, vIntervening, v3, v4))

        assertEquals("2 identical in run 1 + 2 copies in run 2 must give 2 duplicates", 2, candidates.size)
        val dupIds = candidates.map { it.id }.toSet()
        assertEquals("Duplicates must be the copies from run 2 (2566, 2567)", setOf(2566L, 2567L), dupIds)
    }

    @Test
    fun testThreeIdenticalInOneRunGivesZeroDuplicates() {
        val custA = 10L
        val day = timeOnDay(2026, Calendar.OCTOBER, 14, 10)

        // Real repeated sessions inside the only session import run (e.g. 81, 82, 83)
        val s1 = WaterSession(id = 81L, customerId = custA, pumpName = "سقي / قيد مستورد", totalAmount = 150.0, notes = "سقي حوض", startTime = day)
        val s2 = WaterSession(id = 82L, customerId = custA, pumpName = "سقي / قيد مستورد", totalAmount = 150.0, notes = "سقي حوض", startTime = day)
        val s3 = WaterSession(id = 83L, customerId = custA, pumpName = "سقي / قيد مستورد", totalAmount = 150.0, notes = "سقي حوض", startTime = day)

        val candidates = ImportDuplicateFinder.find(listOf(s1, s2, s3), emptyList())

        assertTrue("Three identical records in a single run must give 0 duplicates", candidates.isEmpty())
    }

    @Test
    fun testSingleCustomerFileImportedTwiceGivesZeroDuplicatesSafeUnderDetection() {
        val custA = 10L
        val day = timeOnDay(2026, Calendar.OCTOBER, 14, 10)

        // If the file only has customer A, there is no intervening customer, so all records form one run
        val v1 = Voucher(id = 101L, customerId = custA, type = VoucherType.RECEIPT, category = "دفعة مستوردة", paymentMethod = "رصيد سابق", amount = 500.0, notes = "سداد", date = day)
        val v2 = Voucher(id = 102L, customerId = custA, type = VoucherType.RECEIPT, category = "دفعة مستوردة", paymentMethod = "رصيد سابق", amount = 500.0, notes = "سداد", date = day)

        val candidates = ImportDuplicateFinder.find(emptyList(), listOf(v1, v2))

        assertTrue("Single-customer file imported twice gives 0 duplicates (safe under-detection)", candidates.isEmpty())
    }

    @Test
    fun testSingleNonDuplicateRecord() {
        val session = WaterSession(
            id = 1L,
            customerId = 10L,
            pumpName = "سقي / قيد مستورد",
            durationMinutes = 0,
            totalAmount = 500.0,
            notes = "عملية يتيمة",
            startTime = timeOnDay(2026, Calendar.OCTOBER, 1, 10)
        )
        val voucher = Voucher(
            id = 2L,
            customerId = 20L, // different customer
            type = VoucherType.RECEIPT,
            category = "دفعة مستوردة",
            paymentMethod = "رصيد سابق",
            amount = 500.0,
            notes = "دفعة يتيمة",
            date = timeOnDay(2026, Calendar.OCTOBER, 2, 10)
        )

        val candidates = ImportDuplicateFinder.find(listOf(session), listOf(voucher))
        assertTrue("Single non-duplicate records should return empty duplicate list", candidates.isEmpty())
    }

    @Test
    fun testPairingEachRecordOnlyOnce() {
        val customerId = 30L
        val day1 = timeOnDay(2026, Calendar.OCTOBER, 5, 10)
        val day2 = timeOnDay(2026, Calendar.OCTOBER, 6, 10)

        // 1 session, but 2 vouchers on different days (so vouchers are not exact twins)
        val s1 = WaterSession(
            id = 100L,
            customerId = customerId,
            pumpName = "سقي / قيد مستورد",
            durationMinutes = 0,
            totalAmount = 750.0,
            notes = "بيان مشترك",
            startTime = day1
        )
        val v1 = Voucher(
            id = 200L,
            customerId = customerId,
            type = VoucherType.EXPENSE,
            category = "صرف مستورد",
            paymentMethod = "رصيد سابق",
            amount = 750.0,
            notes = "بيان مشترك",
            date = day1
        )
        val v2 = Voucher(
            id = 201L,
            customerId = customerId,
            type = VoucherType.EXPENSE,
            category = "صرف مستورد",
            paymentMethod = "رصيد سابق",
            amount = 750.0,
            notes = "بيان مشترك",
            date = day2
        )

        val candidates = ImportDuplicateFinder.find(listOf(s1), listOf(v1, v2))

        assertEquals("S1 should be paired at most once with V1; V2 remains unpaired", 1, candidates.size)
        assertEquals(100L, candidates.first().id)
        assertTrue(candidates.first().isSession)
        assertEquals(DuplicateReason.TYPE_MISMATCH_TWIN, candidates.first().reason)
    }

    // ─────────────────────────────────────────────────────────────────
    // Part C Tests: Expense Split Verification
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun testExpenseSplitLogic() {
        val normalCustomer = Customer(id = 1L, name = "عميل عادي", isWellOwner = false)
        val wellOwner = Customer(id = 2L, name = "صاحب بئر", isWellOwner = true)
        val customersById = mapOf(1L to normalCustomer, 2L to wellOwner)

        val vOperating = Voucher(id = 10L, type = VoucherType.EXPENSE, customerId = null, amount = 1000.0)
        val vDisbursement = Voucher(id = 20L, type = VoucherType.EXPENSE, customerId = 1L, amount = 2500.0)
        val vOwnerPayment = Voucher(id = 30L, type = VoucherType.EXPENSE, customerId = 2L, amount = 4000.0)

        val allVouchers = listOf(vOperating, vDisbursement, vOwnerPayment)

        val operatingExpenses = allVouchers
            .filter { it.type == VoucherType.EXPENSE && it.customerId == null }
            .sumOf { it.amount.coerceAtLeast(0.0) }

        val customerDisbursements = allVouchers
            .filter { it.type == VoucherType.EXPENSE && it.customerId != null && customersById[it.customerId]?.isWellOwner != true }
            .sumOf { it.amount.coerceAtLeast(0.0) }

        val ownerPayments = allVouchers
            .filter { it.type == VoucherType.EXPENSE && it.customerId?.let { id -> customersById[id]?.isWellOwner == true } == true }
            .sumOf { it.amount.coerceAtLeast(0.0) }

        assertEquals("Operating expenses should only include customerId == null", 1000.0, operatingExpenses, 0.001)
        assertEquals("Customer disbursements should sum non-owner customer EXPENSE", 2500.0, customerDisbursements, 0.001)
        assertEquals("Owner payments should sum well owner EXPENSE", 4000.0, ownerPayments, 0.001)

        // Verify VouchersScreen header logic
        val wellOwnerIds = setOf(2L)
        val vouchersScreenExpenses = allVouchers.filter { voucher ->
            voucher.type == VoucherType.EXPENSE && (voucher.customerId == null || wellOwnerIds.contains(voucher.customerId))
        }.sumOf { it.amount }

        assertEquals(
            "VouchersScreen header expense must sum operating (1000) + owner payments (4000) = 5000",
            5000.0,
            vouchersScreenExpenses,
            0.001
        )
    }
}

