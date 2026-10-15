package com.example.util

import java.util.Locale

/**
 * Utility functions for currency and amount formatting.
 */
object CurrencyFormatter {

    fun format(amount: Double, symbol: String = "$"): String {
        return String.format(Locale.US, "%s%.2f", symbol, amount)
    }

    fun formatCompact(amount: Double, symbol: String = "$"): String {
        return if (amount >= 1000) {
            String.format(Locale.US, "%s%.1fk", symbol, amount / 1000)
        } else {
            String.format(Locale.US, "%s%.0f", symbol, amount)
        }
    }

    fun formatPercentage(ratio: Float): String {
        val pct = (ratio * 100).coerceAtLeast(0f)
        return String.format(Locale.US, "%.1f%%", pct)
    }
}
