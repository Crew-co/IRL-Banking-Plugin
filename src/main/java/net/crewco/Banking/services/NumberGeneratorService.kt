// src/main/java/net/crewco/Banking/services/NumberGeneratorService.kt
package net.crewco.Banking.services

import java.security.SecureRandom
import java.util.UUID

class NumberGeneratorService {

    private val random = SecureRandom()

    // Bank routing number (9 digits) - Same for all accounts in this bank
    fun getBankRoutingNumber(): String = "123456789"

    // Generate unique 10-digit account number
    fun generateAccountNumber(): String {
        val sb = StringBuilder()
        repeat(10) {
            sb.append(random.nextInt(10))
        }
        return sb.toString()
    }

    // Generate unique 16-digit card number (Luhn-valid)
    fun generateCardNumber(): String {
        val prefix = "4" // Visa-style prefix
        val partialNumber = prefix + (1..14).map { random.nextInt(10) }.joinToString("")
        val checkDigit = calculateLuhnCheckDigit(partialNumber)
        return partialNumber + checkDigit
    }

    // Generate 3-digit CVV
    fun generateCVV(): String {
        return String.format("%03d", random.nextInt(1000))
    }

    // Generate unique transaction ID
    fun generateTransactionId(): String {
        val timestamp = System.currentTimeMillis().toString(36).uppercase()
        val randomPart = (1..8).map {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"[random.nextInt(36)]
        }.joinToString("")
        return "TXN$timestamp$randomPart"
    }

    // Generate unique loan ID
    fun generateLoanId(): String {
        val timestamp = System.currentTimeMillis().toString(36).uppercase()
        val randomPart = (1..6).map {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"[random.nextInt(36)]
        }.joinToString("")
        return "LN$timestamp$randomPart"
    }

    // Generate unique ATM ID
    fun generateAtmId(): String {
        return "ATM-${UUID.randomUUID().toString().take(8).uppercase()}"
    }

    // Hash PIN for storage
    fun hashPin(pin: String): String {
        // In production, use BCrypt or similar
        return pin.hashCode().toString()
    }

    // Verify PIN
    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        return hashPin(inputPin) == storedHash
    }

    // Luhn algorithm check digit calculation
    private fun calculateLuhnCheckDigit(partialNumber: String): Int {
        var sum = 0
        var alternate = true

        for (i in partialNumber.length - 1 downTo 0) {
            var digit = partialNumber[i].digitToInt()

            if (alternate) {
                digit *= 2
                if (digit > 9) digit -= 9
            }

            sum += digit
            alternate = !alternate
        }

        return (10 - (sum % 10)) % 10
    }

    // Validate card number using Luhn algorithm
    fun isValidCardNumber(cardNumber: String): Boolean {
        if (cardNumber.length != 16 || !cardNumber.all { it.isDigit() }) return false

        var sum = 0
        var alternate = false

        for (i in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[i].digitToInt()

            if (alternate) {
                digit *= 2
                if (digit > 9) digit -= 9
            }

            sum += digit
            alternate = !alternate
        }

        return sum % 10 == 0
    }
}