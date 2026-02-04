// src/main/java/net/crewco/Banking/data/models/Loan.kt
package net.crewco.Banking.data.models

import net.crewco.Banking.data.ldata.LoanStatus
import net.crewco.Banking.data.ldata.LoanType
import java.time.LocalDateTime
import java.util.UUID

data class Loan(
    val id: Long = 0,
    val loanId: String,                      // Unique loan reference
    val borrowerUuid: UUID,
    val linkedAccountNumber: String,         // Account for disbursement/payments
    val loanType: LoanType,
    val principalAmount: Double,
    val interestRate: Double,                // Annual interest rate
    var remainingBalance: Double,
    var monthlyPayment: Double,
    var totalPaid: Double = 0.0,
    var missedPayments: Int = 0,
    val termMonths: Int,                     // Loan term in months
    var monthsRemaining: Int,
    val status: LoanStatus = LoanStatus.PENDING,
    val collateral: String? = null,          // Description of collateral if secured
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var approvedAt: LocalDateTime? = null,
    var nextPaymentDue: LocalDateTime? = null,
    var lastPaymentDate: LocalDateTime? = null
) {
    fun calculateTotalInterest(): Double {
        val monthlyRate = interestRate / 100 / 12
        val totalPayments = monthlyPayment * termMonths
        return totalPayments - principalAmount
    }

    fun isOverdue(): Boolean {
        return nextPaymentDue?.isBefore(LocalDateTime.now()) ?: false
    }
}


