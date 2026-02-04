// src/main/java/net/crewco/Banking/services/TransactionService.kt
package net.crewco.Banking.services

import net.crewco.Banking.api.events.TransactionEvent
import net.crewco.Banking.data.edata.TransactionStatus
import net.crewco.Banking.data.edata.TransactionType
import net.crewco.Banking.data.models.Transaction
import net.crewco.Banking.data.repositories.TransactionRepository
import org.bukkit.Bukkit
import java.time.LocalDateTime
import java.util.UUID

class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val numberGenerator: NumberGeneratorService
) {

    suspend fun recordDeposit(
        accountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String = ""
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = null,
            toAccountNumber = accountNumber,
            amount = amount,
            type = TransactionType.DEPOSIT,
            status = TransactionStatus.COMPLETED,
            description = description.ifEmpty { "Deposit" },
            initiatedBy = initiatedBy,
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)
        Bukkit.getPluginManager().callEvent(TransactionEvent(transaction))

        return transaction
    }

    suspend fun recordWithdrawal(
        accountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String = ""
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = accountNumber,
            toAccountNumber = null,
            amount = amount,
            type = TransactionType.WITHDRAWAL,
            status = TransactionStatus.COMPLETED,
            description = description.ifEmpty { "Withdrawal" },
            initiatedBy = initiatedBy,
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)
        Bukkit.getPluginManager().callEvent(TransactionEvent(transaction))

        return transaction
    }

    suspend fun recordTransfer(
        fromAccountNumber: String,
        toAccountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String = ""
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = fromAccountNumber,
            toAccountNumber = toAccountNumber,
            amount = amount,
            type = TransactionType.TRANSFER,
            status = TransactionStatus.COMPLETED,
            description = description.ifEmpty { "Transfer" },
            initiatedBy = initiatedBy,
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)
        Bukkit.getPluginManager().callEvent(TransactionEvent(transaction))

        return transaction
    }

    suspend fun recordATMWithdrawal(
        accountNumber: String,
        amount: Double,
        fee: Double,
        initiatedBy: UUID,
        atmId: String
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = accountNumber,
            toAccountNumber = null,
            amount = amount,
            type = TransactionType.ATM_WITHDRAWAL,
            status = TransactionStatus.COMPLETED,
            description = "ATM Withdrawal ($atmId)",
            fee = fee,
            initiatedBy = initiatedBy,
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)
        Bukkit.getPluginManager().callEvent(TransactionEvent(transaction))

        return transaction
    }

    suspend fun recordATMDeposit(
        accountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        atmId: String
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = null,
            toAccountNumber = accountNumber,
            amount = amount,
            type = TransactionType.ATM_DEPOSIT,
            status = TransactionStatus.COMPLETED,
            description = "ATM Deposit ($atmId)",
            initiatedBy = initiatedBy,
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)
        Bukkit.getPluginManager().callEvent(TransactionEvent(transaction))

        return transaction
    }

    suspend fun recordCardPurchase(
        accountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        merchantDescription: String
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = accountNumber,
            toAccountNumber = null,
            amount = amount,
            type = TransactionType.CARD_PURCHASE,
            status = TransactionStatus.COMPLETED,
            description = merchantDescription,
            initiatedBy = initiatedBy,
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)
        Bukkit.getPluginManager().callEvent(TransactionEvent(transaction))

        return transaction
    }

    suspend fun recordLoanDisbursement(
        accountNumber: String,
        amount: Double,
        loanId: String,
        initiatedBy: UUID
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = null,
            toAccountNumber = accountNumber,
            amount = amount,
            type = TransactionType.LOAN_DISBURSEMENT,
            status = TransactionStatus.COMPLETED,
            description = "Loan Disbursement ($loanId)",
            initiatedBy = initiatedBy,
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)
        Bukkit.getPluginManager().callEvent(TransactionEvent(transaction))

        return transaction
    }

    suspend fun recordLoanPayment(
        accountNumber: String,
        amount: Double,
        loanId: String,
        initiatedBy: UUID
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = accountNumber,
            toAccountNumber = null,
            amount = amount,
            type = TransactionType.LOAN_PAYMENT,
            status = TransactionStatus.COMPLETED,
            description = "Loan Payment ($loanId)",
            initiatedBy = initiatedBy,
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)
        Bukkit.getPluginManager().callEvent(TransactionEvent(transaction))

        return transaction
    }

    suspend fun recordInterestCredit(
        accountNumber: String,
        amount: Double
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = null,
            toAccountNumber = accountNumber,
            amount = amount,
            type = TransactionType.INTEREST_CREDIT,
            status = TransactionStatus.COMPLETED,
            description = "Interest Credit",
            initiatedBy = UUID(0, 0), // System
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)

        return transaction
    }

    suspend fun recordFeeDeduction(
        accountNumber: String,
        amount: Double,
        feeDescription: String
    ): Transaction {
        val transaction = Transaction(
            transactionId = numberGenerator.generateTransactionId(),
            fromAccountNumber = accountNumber,
            toAccountNumber = null,
            amount = amount,
            type = TransactionType.FEE_DEDUCTION,
            status = TransactionStatus.COMPLETED,
            description = feeDescription,
            initiatedBy = UUID(0, 0), // System
            processedAt = LocalDateTime.now()
        )

        transactionRepository.create(transaction)

        return transaction
    }

    suspend fun getTransactionHistory(accountNumber: String, limit: Int = 50): List<Transaction> {
        return transactionRepository.findByAccount(accountNumber, limit)
    }

    suspend fun getTransaction(transactionId: String): Transaction? {
        return transactionRepository.findByTransactionId(transactionId)
    }
}