// src/main/java/net/crewco/Banking/services/AccountService.kt
package net.crewco.Banking.services

import net.crewco.Banking.api.events.AccountCreatedEvent
import net.crewco.Banking.data.edata.WithdrawalResult
import net.crewco.Banking.data.models.AccountType
import net.crewco.Banking.data.models.BankAccount
import net.crewco.Banking.data.repositories.AccountRepository
import net.crewco.Banking.services.sdata.TransferResult
import org.bukkit.Bukkit
import java.util.UUID

class AccountService(
    private val accountRepository: AccountRepository,
    private val numberGenerator: NumberGeneratorService,
    private val transactionService: TransactionService
) {

    suspend fun createAccount(
        ownerUuid: UUID,
        type: AccountType,
        initialDeposit: Double = 0.0,
        accountName: String = ""
    ): BankAccount? {
        // Check if player already has this account type (except wallet - can have multiple)
        if (type != AccountType.WALLET) {
            val existing = accountRepository.findByUuidAndType(ownerUuid, type)
            if (existing != null) return null
        }

        val account = BankAccount(
            uuid = ownerUuid,
            accountNumber = numberGenerator.generateAccountNumber(),
            routingNumber = numberGenerator.getBankRoutingNumber(),
            accountType = type,
            balance = initialDeposit,
            accountName = accountName.ifEmpty { type.displayName }
        )

        val success = accountRepository.create(account)

        if (success) {
            // Fire event
            Bukkit.getPluginManager().callEvent(AccountCreatedEvent(account))

            // Record initial deposit as transaction if applicable
            if (initialDeposit > 0) {
                transactionService.recordDeposit(
                    account.accountNumber,
                    initialDeposit,
                    ownerUuid,
                    "Initial deposit"
                )
            }

            return account
        }

        return null
    }

    suspend fun getAccount(accountNumber: String): BankAccount? {
        return accountRepository.findByAccountNumber(accountNumber)
    }

    suspend fun getAccountsByPlayer(uuid: UUID): List<BankAccount> {
        return accountRepository.findByUuid(uuid)
    }

    suspend fun getWallet(uuid: UUID): BankAccount? {
        return accountRepository.findByUuidAndType(uuid, AccountType.WALLET)
    }

    suspend fun getPrimaryAccount(uuid: UUID): BankAccount? {
        // Priority: Checking > Wallet > First available
        return accountRepository.findByUuidAndType(uuid, AccountType.CHECKING)
            ?: accountRepository.findByUuidAndType(uuid, AccountType.WALLET)
            ?: accountRepository.findByUuid(uuid).firstOrNull()
    }

    suspend fun deposit(accountNumber: String, amount: Double, initiatedBy: UUID, description: String = ""): Boolean {
        if (amount <= 0) return false

        val account = accountRepository.findByAccountNumber(accountNumber) ?: return false
        if (account.frozen) return false

        val newBalance = account.balance + amount
        val success = accountRepository.updateBalance(accountNumber, newBalance)

        if (success) {
            transactionService.recordDeposit(accountNumber, amount, initiatedBy, description)
        }

        return success
    }

    suspend fun withdraw(accountNumber: String, amount: Double, initiatedBy: UUID, description: String = ""): WithdrawalResult {
        if (amount <= 0) return WithdrawalResult.INSUFFICIENT_FUNDS

        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: return WithdrawalResult.INSUFFICIENT_FUNDS

        // Use canWithdraw for standard checks
        val result = account.canWithdraw(amount)
        if (result != WithdrawalResult.SUCCESS) return result

        // Calculate new balance
        val newBalance = account.balance - amount

        // Determine minimum allowed balance based on overdraft
        val minAllowedBalance = if (account.accountType.allowsOverdraft) {
            -account.overdraftLimit
        } else {
            0.0
        }

        // Final safety check to prevent going below minimum
        if (newBalance < minAllowedBalance) {
            return WithdrawalResult.INSUFFICIENT_FUNDS
        }

        accountRepository.updateBalance(accountNumber, newBalance)
        accountRepository.updateDailyWithdrawal(accountNumber, account.dailyWithdrawnToday + amount)

        transactionService.recordWithdrawal(accountNumber, amount, initiatedBy, description)

        return WithdrawalResult.SUCCESS
    }

    suspend fun transfer(
        fromAccountNumber: String,
        toAccountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String = ""
    ): TransferResult {
        if (amount <= 0) return TransferResult.INVALID_AMOUNT
        if (fromAccountNumber == toAccountNumber) return TransferResult.SAME_ACCOUNT

        // Fetch fresh account data
        val fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
            ?: return TransferResult.FROM_ACCOUNT_NOT_FOUND

        val toAccount = accountRepository.findByAccountNumber(toAccountNumber)
            ?: return TransferResult.TO_ACCOUNT_NOT_FOUND

        // Check frozen status
        if (fromAccount.frozen) return TransferResult.FROM_ACCOUNT_FROZEN
        if (toAccount.frozen) return TransferResult.TO_ACCOUNT_FROZEN

        // Check daily withdrawal limit (transfers count towards daily limit)
        val maxDaily = fromAccount.accountType.maxDailyWithdrawal
        if (maxDaily > 0 && fromAccount.dailyWithdrawnToday + amount > maxDaily) {
            return TransferResult.DAILY_LIMIT_EXCEEDED
        }

        // Calculate the minimum balance allowed (considering overdraft if applicable)
        val minAllowedBalance = if (fromAccount.accountType.allowsOverdraft) {
            -fromAccount.overdraftLimit
        } else {
            0.0
        }

        // Calculate new balance after transfer
        val newFromBalance = fromAccount.balance - amount

        // Check if the transfer would put the account below minimum allowed balance
        if (newFromBalance < minAllowedBalance) {
            return TransferResult.INSUFFICIENT_FUNDS
        }

        // Perform transfer with rollback capability
        val withdrawSuccess = accountRepository.updateBalance(fromAccountNumber, newFromBalance)
        if (!withdrawSuccess) {
            return TransferResult.FAILED
        }

        val newToBalance = toAccount.balance + amount
        val depositSuccess = accountRepository.updateBalance(toAccountNumber, newToBalance)
        if (!depositSuccess) {
            // Rollback the withdrawal if deposit failed
            accountRepository.updateBalance(fromAccountNumber, fromAccount.balance)
            return TransferResult.FAILED
        }

        // Update daily withdrawal tracking for the source account
        accountRepository.updateDailyWithdrawal(fromAccountNumber, fromAccount.dailyWithdrawnToday + amount)

        // Record transaction
        transactionService.recordTransfer(
            fromAccountNumber,
            toAccountNumber,
            amount,
            initiatedBy,
            description
        )

        return TransferResult.SUCCESS
    }

    suspend fun freezeAccount(accountNumber: String): Boolean {
        return accountRepository.setFrozen(accountNumber, true)
    }

    suspend fun unfreezeAccount(accountNumber: String): Boolean {
        return accountRepository.setFrozen(accountNumber, false)
    }

    suspend fun getBalance(accountNumber: String): Double? {
        return accountRepository.findByAccountNumber(accountNumber)?.balance
    }

    suspend fun getTotalBalance(uuid: UUID): Double {
        return accountRepository.findByUuid(uuid).sumOf { it.balance }
    }

    suspend fun closeAccount(accountNumber: String): Boolean {
        val account = accountRepository.findByAccountNumber(accountNumber) ?: return false
        if (account.balance != 0.0) return false // Must have zero balance to close
        return accountRepository.delete(accountNumber)
    }

    // Create default wallet for new players
    suspend fun ensureWalletExists(uuid: UUID): BankAccount {
        return getWallet(uuid) ?: createAccount(uuid, AccountType.WALLET, 0.0, "Personal Wallet")!!
    }
}