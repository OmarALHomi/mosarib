package com.example.core.util

import kotlin.math.round

/**
 * Strict centralized financial arithmetic helper to prevent floating-point representation errors.
 * Ensures consistent half-up rounding to the nearest currency unit across ledger balances,
 * water rates, hourly costs, and statements.
 */
object MoneyPrecision {
    fun roundToCurrency(amount: Double): Double {
        if (amount.isNaN() || amount.isInfinite()) return 0.0
        return round(amount)
    }

    fun roundToCurrencyLong(amount: Double): Long {
        if (amount.isNaN() || amount.isInfinite()) return 0L
        return round(amount).toLong()
    }

    fun safeSubtract(a: Double, b: Double): Double {
        return roundToCurrency(a - b)
    }

    fun safeAdd(a: Double, b: Double): Double {
        return roundToCurrency(a + b)
    }

    fun safeMultiply(rate: Double, units: Double): Double {
        return roundToCurrency(rate * units)
    }
}
