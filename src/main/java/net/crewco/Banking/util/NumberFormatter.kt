// src/main/java/net/crewco/Banking/util/NumberFormatter.kt
package net.crewco.Banking.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object NumberFormatter {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val decimalFormat = DecimalFormat("#,##0.00")
    private val compactFormat = DecimalFormat("#,##0.##")

    fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

    fun formatDecimal(amount: Double): String = decimalFormat.format(amount)

    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${compactFormat.format(amount / 1_000_000_000)}B"
            amount >= 1_000_000 -> "${compactFormat.format(amount / 1_000_000)}M"
            amount >= 1_000 -> "${compactFormat.format(amount / 1_000)}K"
            else -> compactFormat.format(amount)
        }
    }

    fun formatAccountNumber(number: String): String {
        return number.chunked(4).joinToString("-")
    }

    fun formatCardNumber(number: String): String {
        return number.chunked(4).joinToString(" ")
    }

    fun maskCardNumber(number: String): String {
        return "**** **** **** ${number.takeLast(4)}"
    }
}