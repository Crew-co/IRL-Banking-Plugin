// src/main/java/net/crewco/Banking/data/models/BankAccount.kt
package net.crewco.Banking.data.models

import net.crewco.Banking.data.edata.WithdrawalResult
import java.time.LocalDateTime
import java.util.UUID

data class BankAccount(
    val id: Long = 0,
    val uuid: UUID,                          // Owner's UUID
    val accountNumber: String,               // 10-digit account number
    val routingNumber: String,               // 9-digit routing number
    val accountType: AccountType,
    var balance: Double = 0.0,
    var frozen: Boolean = false,
    var overdraftLimit: Double = 500.0,      // Max overdraft amount
    var dailyWithdrawnToday: Double = 0.0,
    var lastWithdrawalDate: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var accountName: String = ""             // Custom name for the account
) {
    fun canWithdraw(amount: Double): WithdrawalResult {
        if (frozen) return WithdrawalResult.ACCOUNT_FROZEN

        // Check daily limit
        val maxDaily = accountType.maxDailyWithdrawal
        if (maxDaily > 0 && dailyWithdrawnToday + amount > maxDaily) {
            return WithdrawalResult.DAILY_LIMIT_EXCEEDED
        }

        // Check balance (with overdraft if allowed)
        val availableBalance = if (accountType.allowsOverdraft) {
            balance + overdraftLimit
        } else {
            balance
        }

        if (amount > availableBalance) {
            return WithdrawalResult.INSUFFICIENT_FUNDS
        }

        return WithdrawalResult.SUCCESS
    }

    fun getAvailableBalance(): Double {
        return if (accountType.allowsOverdraft) {
            balance + overdraftLimit
        } else {
            balance
        }
    }
}
