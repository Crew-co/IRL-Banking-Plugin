// src/main/java/net/crewco/Banking/services/InterestService.kt
package net.crewco.Banking.services

import net.crewco.Banking.Startup
import net.crewco.Banking.data.repositories.AccountRepository
import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class InterestService(
    private val plugin: Startup,
    private val accountRepository: AccountRepository,
    private val transactionService: TransactionService
) {

    private var lastInterestRun: LocalDateTime = LocalDateTime.now()

    fun startScheduler() {
        // Run daily tasks
        plugin.launch {
            while (true) {
                delay(Duration.ofHours(1).toMillis()) // Check every hour

                val now = LocalDateTime.now()

                // Daily reset at midnight
                if (now.hour == 0 && ChronoUnit.HOURS.between(lastInterestRun, now) >= 23) {
                    performDailyTasks()
                    lastInterestRun = now
                }

                // Monthly interest on the 1st
                if (now.dayOfMonth == 1 && now.hour == 1) {
                    applyMonthlyInterest()
                    applyMonthlyFees()
                }
            }
        }
    }

    private suspend fun performDailyTasks() {
        // Reset daily withdrawal limits
        accountRepository.resetDailyWithdrawals()
        plugin.logger.info("Daily withdrawal limits reset")
    }

    private suspend fun applyMonthlyInterest() {
        val accounts = accountRepository.getAllAccounts()
        var totalInterestPaid = 0.0
        var accountsProcessed = 0

        for (account in accounts) {
            if (account.accountType.interestRate > 0 && account.balance > 0) {
                // Monthly interest = (Annual Rate / 12) * Balance
                val monthlyRate = account.accountType.interestRate / 100 / 12
                val interest = account.balance * monthlyRate

                if (interest > 0.01) { // Only apply if interest is more than 1 cent
                    accountRepository.updateBalance(account.accountNumber, account.balance + interest)
                    transactionService.recordInterestCredit(account.accountNumber, interest)
                    totalInterestPaid += interest
                    accountsProcessed++
                }
            }
        }

        plugin.logger.info("Applied interest to $accountsProcessed accounts. Total: $${"%.2f".format(totalInterestPaid)}")
    }

    private suspend fun applyMonthlyFees() {
        val accounts = accountRepository.getAllAccounts()
        var totalFeesCollected = 0.0
        var accountsCharged = 0

        for (account in accounts) {
            val fee = account.accountType.monthlyFee
            if (fee > 0) {
                if (account.balance >= fee) {
                    accountRepository.updateBalance(account.accountNumber, account.balance - fee)
                    transactionService.recordFeeDeduction(account.accountNumber, fee, "Monthly maintenance fee")
                    totalFeesCollected += fee
                    accountsCharged++
                } else if (account.balance > 0) {
                    // Charge what's available
                    transactionService.recordFeeDeduction(account.accountNumber, account.balance, "Monthly maintenance fee (partial)")
                    totalFeesCollected += account.balance
                    accountRepository.updateBalance(account.accountNumber, 0.0)
                    accountsCharged++
                }
            }
        }

        plugin.logger.info("Collected monthly fees from $accountsCharged accounts. Total: $${"%.2f".format(totalFeesCollected)}")
    }
}