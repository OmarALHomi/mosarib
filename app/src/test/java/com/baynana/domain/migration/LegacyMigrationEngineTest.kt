package com.baynana.domain.migration

import com.baynana.domain.ledger.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٧: الترحيل من الإرث بجرد مطابق، وبلا اختراع تاريخ، وبلا ازدواج عند إعادة الترحيل.
 * الأرقام القديمة بالريال العشري، والجديدة بالفلس الصحيح.
 */
class LegacyMigrationEngineTest {

    private val day1 = 1_600_000_000_000L
    private val day2 = 1_600_100_000_000L
    private val day3 = 1_600_200_000_000L

    private fun customer(id: Long, name: String = "عميل $id", archived: Boolean = false) =
        LegacyCustomerSnapshot(id = id, name = name, phone = "777$id", isArchived = archived, linkCode = "LNK$id")

    private fun session(id: Long, customerId: Long, at: Long, remaining: Double, paid: Double = 0.0, total: Double = remaining, live: Boolean = false) =
        LegacySessionSnapshot(
            id = id, customerId = customerId, startTime = at,
            totalAmount = total, amountPaid = paid, remainingDebt = remaining, isLive = live, pumpName = "البئر"
        )

    @Test
    fun `a legacy debt is migrated with its real session date, never the migration date`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(
                customers = listOf(customer(1)),
                sessions = listOf(session(10, 1, day1, remaining = 15_000.0)),
                vouchers = emptyList()
            )
        )

        val entry = plan.entries.single()
        assertEquals("legacy:session:10", entry.operationId)
        assertEquals(day1, entry.occurredAt)
        assertEquals(EntryType.WATER_SESSION, entry.type)
        assertEquals("الريال العشري صار فلسًا: 15,000 ريال = 1,500,000 فلس", 1_500_000L, entry.amountMinor)
        assertEquals("water_sessions", entry.sourceTable)
        assertEquals("10", entry.sourceId)
    }

    @Test
    fun `the ledger total equals the legacy total exactly`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(
                customers = listOf(customer(1), customer(2)),
                sessions = listOf(
                    session(10, 1, day1, remaining = 15_000.0),
                    session(11, 1, day2, remaining = 2_500.50),
                    session(20, 2, day3, remaining = 700.25)
                ),
                vouchers = emptyList()
            )
        )

        assertTrue("الجرد مطابق", plan.allReconcile)
        assertEquals(1_500_000L + 250_050L + 70_025L, plan.ledgerTotal())
        assertEquals(plan.ledgerTotal(), plan.legacyTotalMinor)
        assertEquals(0L, plan.reconciliations.sumOf { it.differenceMinor })
    }

    private fun LegacyMigrationPlan.ledgerTotal(): Long = entries.filter { it.debtDirection > 0 }.sumOf { it.amountMinor }

    @Test
    fun `sessions already settled migrate nothing but are counted in the audit`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(
                customers = listOf(customer(1)),
                sessions = listOf(
                    session(10, 1, day1, remaining = 0.0, paid = 1_000.0, total = 1_000.0),
                    session(11, 1, day2, remaining = 500.0, paid = 500.0, total = 1_000.0)
                ),
                vouchers = emptyList()
            )
        )

        val audit = plan.reconciliations.single()
        assertEquals(2, audit.sessionCount)
        assertEquals(1, audit.migratedSessions)
        assertEquals(1, audit.skippedSettledSessions)
        assertEquals(1, plan.entries.size)
        assertTrue(plan.allReconcile)
    }

    @Test
    fun `a live session is skipped and announced, not frozen into the ledger`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(
                customers = listOf(customer(1)),
                sessions = listOf(session(10, 1, day1, remaining = 900.0, live = true)),
                vouchers = emptyList()
            )
        )

        assertTrue(plan.entries.isEmpty())
        val audit = plan.reconciliations.single()
        assertEquals(1, audit.skippedLiveSessions)
        assertTrue(audit.warnings.any { it.contains("سقية جارية") })
        assertTrue("لا قيد لسقية لم تُقفل", plan.allReconcile)
    }

    @Test
    fun `an old overpayment becomes a credit in the room and is not netted against the debt`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(
                customers = listOf(customer(1)),
                sessions = listOf(
                    session(10, 1, day1, remaining = 1_000.0),
                    session(11, 1, day2, remaining = -300.0, paid = 1_300.0, total = 1_000.0)
                ),
                vouchers = emptyList()
            )
        )

        val audit = plan.reconciliations.single()
        assertEquals(1_000_000L, audit.legacyMinor)
        assertEquals(1_000_000L, audit.plannedMinor)
        assertEquals(300_000L, audit.creditMinor)
        assertTrue("الجرد مطابق", audit.matches)

        val credit = plan.entries.single { it.debtDirection < 0 }
        assertEquals(EntryType.GENERAL_RECEIPT, credit.type)
        assertEquals(300_000L, credit.amountMinor)
        assertEquals("legacy:credit:11", credit.operationId)
    }

    @Test
    fun `vouchers are audited but never migrated as separate entries, so nothing is deducted twice`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(
                customers = listOf(customer(1)),
                sessions = listOf(session(10, 1, day1, remaining = 700.0)),
                vouchers = listOf(
                    LegacyVoucherSnapshot(1, 1, 10, "RECEIPT", 300.0, day1),
                    LegacyVoucherSnapshot(2, 1, null, "RECEIPT", 200.0, day2),
                    LegacyVoucherSnapshot(3, 1, null, "EXPENSE", 50.0, day3)
                )
            )
        )

        assertEquals("قيد واحد فقط: السقية", 1, plan.entries.size)
        val audit = plan.reconciliations.single()
        assertEquals(3, audit.voucherCount)
        assertEquals("مجموع سندات القبض مجرود", 500_000L, audit.voucherTotalMinor)
        assertTrue(audit.warnings.any { it.contains("لم تُرحَّل كقيود") })
        assertTrue(plan.allReconcile)
    }

    @Test
    fun `re-running the migration produces identical identifiers, so it cannot duplicate`() {
        val snapshot = LegacySnapshot(
            customers = listOf(customer(1)),
            sessions = listOf(session(10, 1, day1, remaining = 1_000.0)),
            vouchers = emptyList()
        )
        val first = LegacyMigrationEngine.plan(snapshot)
        val second = LegacyMigrationEngine.plan(snapshot)

        assertEquals(first.entries.map { it.operationId }, second.entries.map { it.operationId })
        assertEquals(first.rooms.map { it.roomId }, second.rooms.map { it.roomId })
        assertEquals("legacy-room-1", first.rooms.single().roomId)
    }

    @Test
    fun `an archived customer keeps a read-only room and is never dropped`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(
                customers = listOf(customer(1, archived = true)),
                sessions = listOf(session(10, 1, day1, remaining = 400.0)),
                vouchers = emptyList()
            )
        )

        val room = plan.rooms.single()
        assertTrue(room.archived)
        assertEquals(1, plan.entries.size)
        assertTrue(plan.reconciliations.single().warnings.any { it.contains("أرشيف") })
    }

    @Test
    fun `a customer with no history still gets a room to link later`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(customers = listOf(customer(7)), sessions = emptyList(), vouchers = emptyList())
        )

        assertEquals(1, plan.rooms.size)
        assertEquals("legacy-room-7", plan.rooms.single().roomId)
        assertTrue(plan.entries.isEmpty())
        assertTrue(plan.reconciliations.single().warnings.any { it.contains("بلا سقيات") })
    }

    @Test
    fun `legacy decimal riyals convert to fils without binary floating point drift`() {
        assertEquals(1_500_000L, LegacyMigrationEngine.toMinor(15_000.0, "YER_NEW"))
        assertEquals(250_050L, LegacyMigrationEngine.toMinor(2_500.50, "YER_NEW"))
        // 0.1 + 0.2 في Double = 0.30000000000000004: التحويل من النص يمنع دخول هذا الشبح.
        assertEquals(30L, LegacyMigrationEngine.toMinor(0.1 + 0.2, "YER_NEW"))
        assertEquals(70_025L, LegacyMigrationEngine.toMinor(700.25, "YER_NEW"))
    }

    @Test
    fun `an unreadable legacy amount is refused loudly instead of becoming zero`() {
        var refused = false
        try {
            LegacyMigrationEngine.toMinor(1_000_000_000_000.5, "YER_NEW")
        } catch (error: IllegalArgumentException) {
            refused = true
            assertTrue(error.message!!.contains("مبلغ قديم غير صالح"))
        }
        assertFalse("المبلغ الضخم غير الصالح لا يمرّ صامتًا", !refused)
    }

    @Test
    fun `the plan explains its own rules so the owner can audit the decision`() {
        val plan = LegacyMigrationEngine.plan(LegacySnapshot(emptyList(), emptyList(), emptyList()))
        assertTrue(plan.notes.any { it.contains("لا اختراع تاريخ") })
        assertTrue(plan.notes.any { it.contains("لا حذف ولا تعديل للإرث") })
        assertTrue(plan.notes.any { it.contains("العملة المستهدفة") })
        assertTrue(LegacyMigrationEngine.statusText(plan).contains("عملاء: 0"))
    }

    @Test
    fun `a session without a pump name still gets a readable description`() {
        val plan = LegacyMigrationEngine.plan(
            LegacySnapshot(
                customers = listOf(customer(1)),
                sessions = listOf(
                    LegacySessionSnapshot(10, 1, day1, totalAmount = 100.0, amountPaid = 0.0, remainingDebt = 100.0)
                ),
                vouchers = emptyList()
            )
        )
        assertTrue(plan.entries.single().description.contains("مُرحَّلة من الدفتر القديم"))
    }
}
