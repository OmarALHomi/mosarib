package com.example.core.util

/**
 * Pure accounting math and formatting helpers for Comprehensive Report.
 */
object ComprehensiveReportMath {

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
}
