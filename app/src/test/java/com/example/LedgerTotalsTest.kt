package com.example

import com.example.features.customers.AccountFilter
import com.example.features.customers.Customer
import com.example.features.customers.CustomerWithBalance
import com.example.features.customers.computeLedgerTotals
import com.example.features.customers.ledgerTotalsFor
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerTotalsTest {

    @Test
    fun `empty list gives 0, 0`() {
        val (owedBy, owedTo) = ledgerTotalsFor(emptyList())
        assertEquals(0.0, owedBy, 0.0001)
        assertEquals(0.0, owedTo, 0.0001)
    }

    @Test
    fun `mixed owners and customers ledger totals calculation`() {
        // Farmer with debt: owes distributor 5,000 -> owedTo (لك) = 5,000
        val debtorFarmer = CustomerWithBalance(
            customer = Customer(id = 1L, name = "مزارع مدين", isWellOwner = false),
            receivableBalance = 5000.0,
            payableBalance = 0.0
        )

        // Farmer with credit (prepaid/overpaid): distributor owes farmer 1,200 -> owedBy (عليك) = 1,200
        val creditorFarmer = CustomerWithBalance(
            customer = Customer(id = 2L, name = "مزارع دائن", isWellOwner = false),
            receivableBalance = -1200.0,
            payableBalance = 0.0
        )

        // Settled farmer: 0 balances
        val settledFarmer = CustomerWithBalance(
            customer = Customer(id = 3L, name = "مزارع خالص", isWellOwner = false),
            receivableBalance = 0.0,
            payableBalance = 0.0
        )

        // Well owner: distributor owes owner 8,000 for water purchases -> owedBy (عليك) = 8,000
        val pureWellOwner = CustomerWithBalance(
            customer = Customer(id = 4L, name = "صاحب بئر", isWellOwner = true),
            receivableBalance = 0.0,
            payableBalance = 8000.0
        )

        // Dual owner (supplier + customer): distributor owes owner 4,000 for water purchases,
        // but owner also owes distributor 1,500 for a personal session -> owedBy = 4,000, owedTo = 1,500
        val dualOwner = CustomerWithBalance(
            customer = Customer(id = 5L, name = "صاحب بئر مزدوج", isWellOwner = true),
            receivableBalance = 1500.0,
            payableBalance = 4000.0
        )

        val fullList = listOf(debtorFarmer, creditorFarmer, settledFarmer, pureWellOwner, dualOwner)

        // Expected:
        // owedBy (عليك) = 1200 + 8000 + 4000 = 13200.0
        // owedTo (لك)   = 5000 + 1500 = 6500.0
        val (owedBy, owedTo) = ledgerTotalsFor(fullList)
        assertEquals(13200.0, owedBy, 0.0001)
        assertEquals(6500.0, owedTo, 0.0001)
    }

    @Test
    fun `the totals for a filtered sublist equal the sum of only those items`() {
        val farmer1 = CustomerWithBalance(
            customer = Customer(id = 1L, name = "أحمد", isWellOwner = false, customGroup = "الغيل"),
            receivableBalance = 3000.0
        )
        val farmer2 = CustomerWithBalance(
            customer = Customer(id = 2L, name = "سعيد", isWellOwner = false, customGroup = "الحصن"),
            receivableBalance = 2000.0
        )
        val owner1 = CustomerWithBalance(
            customer = Customer(id = 3L, name = "صالح", isWellOwner = true, customGroup = "الغيل"),
            payableBalance = 5000.0
        )
        val all = listOf(farmer1, farmer2, owner1)

        // Filter by group "الغيل": includes farmer1 and owner1
        val groupGhail = all.filter { it.customer.customGroup == "الغيل" }
        val (groupOwedBy, groupOwedTo) = ledgerTotalsFor(groupGhail)
        assertEquals(5000.0, groupOwedBy, 0.0001)
        assertEquals(3000.0, groupOwedTo, 0.0001)

        // Filter by farmers only: includes farmer1 and farmer2
        val farmersOnly = all.filter { !it.customer.isWellOwner }
        val (farmersOwedBy, farmersOwedTo) = ledgerTotalsFor(farmersOnly)
        assertEquals(0.0, farmersOwedBy, 0.0001)
        assertEquals(5000.0, farmersOwedTo, 0.0001)

        // Filter by owners only: includes owner1
        val ownersOnly = all.filter { it.customer.isWellOwner }
        val (ownersOwedBy, ownersOwedTo) = ledgerTotalsFor(ownersOnly)
        assertEquals(5000.0, ownersOwedBy, 0.0001)
        assertEquals(0.0, ownersOwedTo, 0.0001)
    }

    @Test
    fun `the full list equals the old computeLedgerTotals result for AccountFilter ALL`() {
        val list = listOf(
            CustomerWithBalance(
                customer = Customer(id = 10L, name = "مزارع 1", isWellOwner = false),
                receivableBalance = 4500.0,
                payableBalance = 0.0
            ),
            CustomerWithBalance(
                customer = Customer(id = 20L, name = "مزارع 2", isWellOwner = false),
                receivableBalance = -800.0,
                payableBalance = 0.0
            ),
            CustomerWithBalance(
                customer = Customer(id = 30L, name = "مالك بئر 1", isWellOwner = true),
                receivableBalance = 600.0,
                payableBalance = 9000.0
            )
        )

        val newTotals = ledgerTotalsFor(list)
        val oldTotals = computeLedgerTotals(list, AccountFilter.ALL)

        assertEquals(oldTotals.first, newTotals.first, 0.0001)
        assertEquals(oldTotals.second, newTotals.second, 0.0001)
    }

    @Test
    fun `filtered list equals computeLedgerTotals for specific AccountFilter`() {
        val list = listOf(
            CustomerWithBalance(
                customer = Customer(id = 10L, name = "مزارع 1", isWellOwner = false),
                receivableBalance = 4500.0,
                payableBalance = 0.0
            ),
            CustomerWithBalance(
                customer = Customer(id = 20L, name = "مزارع 2", isWellOwner = false),
                receivableBalance = -800.0,
                payableBalance = 0.0
            ),
            CustomerWithBalance(
                customer = Customer(id = 30L, name = "مالك بئر 1", isWellOwner = true),
                receivableBalance = 600.0,
                payableBalance = 9000.0
            )
        )

        // FARMERS filter
        val farmers = list.filter { !it.customer.isWellOwner }
        assertEquals(computeLedgerTotals(list, AccountFilter.FARMERS), ledgerTotalsFor(farmers))

        // WELL_OWNERS filter
        val owners = list.filter { it.customer.isWellOwner }
        assertEquals(computeLedgerTotals(list, AccountFilter.WELL_OWNERS), ledgerTotalsFor(owners))
    }
}
