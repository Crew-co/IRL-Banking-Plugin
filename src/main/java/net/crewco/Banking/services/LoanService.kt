// src/main/java/net/crewco/Banking/services/LoanService.kt
package net.crewco.Banking.services

import net.crewco.Banking.api.events.LoanEvent
import net.crewco.Banking.data.ldata.LoanStatus
import net.crewco.Banking.data.ldata.LoanType
import net.crewco.Banking.data.models.*
import net.crewco.Banking.data.repositories.AccountRepository
import net.crewco.Banking.data.repositories.LoanRepository
import net.crewco.Banking.services.sdata.LoanApplicationResult
import net.crewco.Banking.services.sdata.LoanPaymentResult
import org.bukkit.Bukkit
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.pow

class LoanService(
    private val loanRepository: LoanRepository,
    private val accountRepository: AccountRepository,
    private val numberGenerator: NumberGeneratorService,
    private val transactionService: TransactionService
) {

    suspend fun applyForLoan(
        borrowerUuid: UUID,
        linkedAccountNumber: String,
        loanType: LoanType,
        amount: Double,
        termMonths: Int,
        collateral: String? = null
    ): LoanApplicationResult {
        // Validate account
        val account = accountRepository.findByAccountNumber(linkedAccountNumber)
            ?: return LoanApplicationResult(false, null, "Account not found")

        if (account.uuid != borrowerUuid) {
            return LoanApplicationResult(false, null, "Account does not belong to applicant")
        }

        // Validate loan parameters
        if (amount <= 0 || amount > loanType.maxAmount) {
            return LoanApplicationResult(false, null, "Invalid loan amount")
        }

        if (termMonths <= 0 || termMonths > loanType.maxTermMonths) {
            return LoanApplicationResult(false, null, "Invalid loan term")
        }

        if (loanType.requiresCollateral && collateral.isNullOrBlank()) {
            return LoanApplicationResult(false, null, "Collateral required for this loan type")
        }

        // Check existing loans
        val existingLoans = loanRepository.findActiveByBorrower(borrowerUuid)
        val totalOutstanding = existingLoans.sumOf { it.remainingBalance }

        // Simple credit check - can't have more than 3x their total account balance in loans
        val totalBalance = accountRepository.findByUuid(borrowerUuid).sumOf { it.balance }
        if (totalOutstanding + amount > totalBalance * 3) {
            return LoanApplicationResult(false, null, "Credit limit exceeded")
        }

        // Calculate monthly payment using amortization formula
        val monthlyRate = loanType.baseInterestRate / 100 / 12
        val monthlyPayment = if (monthlyRate > 0) {
            amount * (monthlyRate * (1 + monthlyRate).pow(termMonths)) /
                    ((1 + monthlyRate).pow(termMonths) - 1)
        } else {
            amount / termMonths
        }

        val loan = Loan(
            loanId = numberGenerator.generateLoanId(),
            borrowerUuid = borrowerUuid,
            linkedAccountNumber = linkedAccountNumber,
            loanType = loanType,
            principalAmount = amount,
            interestRate = loanType.baseInterestRate,
            remainingBalance = amount,
            monthlyPayment = monthlyPayment,
            termMonths = termMonths,
            monthsRemaining = termMonths,
            status = LoanStatus.PENDING,
            collateral = collateral,
            nextPaymentDue = LocalDateTime.now().plusMonths(1)
        )

        val success = loanRepository.create(loan)

        if (success) {
            Bukkit.getPluginManager().callEvent(LoanEvent(loan, LoanEvent.LoanAction.APPLIED))
            return LoanApplicationResult(true, loan, "Application submitted successfully")
        }

        return LoanApplicationResult(false, null, "Failed to submit application")
    }

    suspend fun approveLoan(loanId: String): Boolean {
        val loan = loanRepository.findByLoanId(loanId) ?: return false
        if (loan.status != LoanStatus.PENDING) return false

        val success = loanRepository.approve(loanId)
        if (success) {
            val updatedLoan = loanRepository.findByLoanId(loanId)!!
            Bukkit.getPluginManager().callEvent(LoanEvent(updatedLoan, LoanEvent.LoanAction.APPROVED))
        }
        return success
    }

    suspend fun disburseLoan(loanId: String): Boolean {
        val loan = loanRepository.findByLoanId(loanId) ?: return false
        if (loan.status != LoanStatus.APPROVED) return false

        // Deposit funds to linked account
        val account = accountRepository.findByAccountNumber(loan.linkedAccountNumber) ?: return false
        accountRepository.updateBalance(loan.linkedAccountNumber, account.balance + loan.principalAmount)

        // Update loan status
        loanRepository.updateStatus(loanId, LoanStatus.ACTIVE)

        // Record transaction
        transactionService.recordLoanDisbursement(
            loan.linkedAccountNumber,
            loan.principalAmount,
            loanId,
            loan.borrowerUuid
        )

        val updatedLoan = loanRepository.findByLoanId(loanId)!!
        Bukkit.getPluginManager().callEvent(LoanEvent(updatedLoan, LoanEvent.LoanAction.DISBURSED))

        return true
    }

    suspend fun makePayment(loanId: String, amount: Double? = null): LoanPaymentResult {
        val loan = loanRepository.findByLoanId(loanId) ?: return LoanPaymentResult(false, "Loan not found")
        if (loan.status != LoanStatus.ACTIVE) return LoanPaymentResult(false, "Loan is not active")

        val paymentAmount = amount ?: loan.monthlyPayment

        // Check account balance
        val account = accountRepository.findByAccountNumber(loan.linkedAccountNumber)
            ?: return LoanPaymentResult(false, "Account not found")

        if (account.balance < paymentAmount) {
            return LoanPaymentResult(false, "Insufficient funds")
        }

        // Deduct payment
        accountRepository.updateBalance(loan.linkedAccountNumber, account.balance - paymentAmount)

        // Update loan
        loanRepository.makePayment(loanId, paymentAmount)

        // Record transaction
        transactionService.recordLoanPayment(
            loan.linkedAccountNumber,
            paymentAmount,
            loanId,
            loan.borrowerUuid
        )

        val updatedLoan = loanRepository.findByLoanId(loanId)!!
        val action = if (updatedLoan.status == LoanStatus.PAID_OFF) {
            LoanEvent.LoanAction.PAID_OFF
        } else {
            LoanEvent.LoanAction.PAYMENT_MADE
        }
        Bukkit.getPluginManager().callEvent(LoanEvent(updatedLoan, action))

        return LoanPaymentResult(true, "Payment successful", updatedLoan)
    }

    suspend fun getLoan(loanId: String): Loan? {
        return loanRepository.findByLoanId(loanId)
    }

    suspend fun getLoansByPlayer(borrowerUuid: UUID): List<Loan> {
        return loanRepository.findByBorrower(borrowerUuid)
    }

    suspend fun getActiveLoans(borrowerUuid: UUID): List<Loan> {
        return loanRepository.findActiveByBorrower(borrowerUuid)
    }

    suspend fun getPendingLoans(): List<Loan> {
        return loanRepository.findByStatus(LoanStatus.PENDING)
    }

    suspend fun getOverdueLoans(): List<Loan> {
        return loanRepository.findOverdueLoans()
    }

    suspend fun processOverdueLoans() {
        val overdueLoans = getOverdueLoans()
        for (loan in overdueLoans) {
            loanRepository.incrementMissedPayments(loan.loanId)

            // Default after 3 missed payments
            if (loan.missedPayments >= 2) { // Will be 3 after increment
                loanRepository.updateStatus(loan.loanId, LoanStatus.DEFAULTED)
                val updatedLoan = loanRepository.findByLoanId(loan.loanId)!!
                Bukkit.getPluginManager().callEvent(LoanEvent(updatedLoan, LoanEvent.LoanAction.DEFAULTED))
            }
        }
    }

    suspend fun rejectLoan(loanId: String, reason: String): Boolean {
        val loan = loanRepository.findByLoanId(loanId) ?: return false
        if (loan.status != LoanStatus.PENDING) return false

        loanRepository.updateStatus(loanId, LoanStatus.REJECTED)
        val updatedLoan = loanRepository.findByLoanId(loanId)!!
        Bukkit.getPluginManager().callEvent(LoanEvent(updatedLoan, LoanEvent.LoanAction.REJECTED))

        return true
    }
}