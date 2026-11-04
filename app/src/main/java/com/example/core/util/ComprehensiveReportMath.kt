package com.example.core.util

/**
 * Pure accounting math and formatting helpers for Comprehensive Report.
 */
object ComprehensiveReportMath {

    enum class OwnerBalanceType {
        PAYABLE,
        RECEIVABLE,
        SETTLED
    }

    data class OwnerBalanceCell(
        val text: String,
        val type: OwnerBalanceType
    )

    /**
     * Determines the display text and debt state for the "المتبقي بذمته" cell on Page 1:
     * - Well owner with net payable > 0: "له <amount>" in green (isDebt = false)
     * - Otherwise: receivable balance, red if > 0 (isDebt = true), green otherwise (isDebt = false)
     *
     * Never uses billed - paid and never uses row.balance.
     */
    fun page1BalanceCell(row: ComprehensiveCustomerRow): Pair<String, Boolean /* isDebt */> {
        return if (row.isWellOwner && row.payableBalance > 0.0) {
            Pair("له ${Formatters.formatNumber(row.payableBalance)}", false)
        } else {
            val debt = row.receivableBalance
            Pair(Formatters.formatNumber(debt), debt > 0.0)
        }
    }

    /**
     * Determines the display text and type for the "الصافي" cell in the well-owners table:
     * - payableBalance > 0: "له X" in green (PAYABLE)
     * - receivableBalance > 0: "عليه X" in red (RECEIVABLE)
     * - otherwise: "مسدد" in grey (SETTLED)
     *
     * Uses the app's established netting model (calculateCustomerBalance; never billed - paid).
     */
    fun ownerNetBalanceCell(row: ComprehensiveCustomerRow): OwnerBalanceCell {
        return when {
            row.payableBalance > 0.0 -> OwnerBalanceCell(
                text = "له ${Formatters.formatNumber(row.payableBalance)}",
                type = OwnerBalanceType.PAYABLE
            )
            row.receivableBalance > 0.0 -> OwnerBalanceCell(
                text = "عليه ${Formatters.formatNumber(row.receivableBalance)}",
                type = OwnerBalanceType.RECEIVABLE
            )
            else -> OwnerBalanceCell(
                text = "مسدد",
                type = OwnerBalanceType.SETTLED
            )
        }
    }
}
