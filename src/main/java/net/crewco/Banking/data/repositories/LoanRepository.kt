// src/main/java/net/crewco/Banking/data/repositories/LoanRepository.kt
package net.crewco.Banking.data.repositories

import net.crewco.Banking.data.database.DatabaseManager
import net.crewco.Banking.data.ldata.LoanStatus
import net.crewco.Banking.data.ldata.LoanType
import net.crewco.Banking.data.models.Loan
import java.time.LocalDateTime
import java.util.UUID

class LoanRepository(private val db: DatabaseManager) {

    suspend fun create(loan: Loan): Boolean {
        val affected = db.execute(
            """
            INSERT INTO bank_loans 
            (loan_id, borrower_uuid, linked_account_number, loan_type, principal_amount,
             interest_rate, remaining_balance, monthly_payment, total_paid, missed_payments,
             term_months, months_remaining, status, collateral, next_payment_due)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            loan.loanId,
            loan.borrowerUuid.toString(),
            loan.linkedAccountNumber,
            loan.loanType.name,
            loan.principalAmount,
            loan.interestRate,
            loan.remainingBalance,
            loan.monthlyPayment,
            loan.totalPaid,
            loan.missedPayments,
            loan.termMonths,
            loan.monthsRemaining,
            loan.status.name,
            loan.collateral,
            loan.nextPaymentDue?.toString()
        )
        return affected > 0
    }

    suspend fun findByLoanId(loanId: String): Loan? {
        val results = db.query(
            "SELECT * FROM bank_loans WHERE loan_id = ?",
            loanId
        )
        return results.firstOrNull()?.toLoan()
    }

    suspend fun findByBorrower(borrowerUuid: UUID): List<Loan> {
        val results = db.query(
            "SELECT * FROM bank_loans WHERE borrower_uuid = ?",
            borrowerUuid.toString()
        )
        return results.map { it.toLoan() }
    }

    suspend fun findActiveByBorrower(borrowerUuid: UUID): List<Loan> {
        val results = db.query(
            "SELECT * FROM bank_loans WHERE borrower_uuid = ? AND status = ?",
            borrowerUuid.toString(),
            LoanStatus.ACTIVE.name
        )
        return results.map { it.toLoan() }
    }

    suspend fun findByStatus(status: LoanStatus): List<Loan> {
        val results = db.query(
            "SELECT * FROM bank_loans WHERE status = ?",
            status.name
        )
        return results.map { it.toLoan() }
    }

    suspend fun findOverdueLoans(): List<Loan> {
        val results = db.query(
            """
            SELECT * FROM bank_loans 
            WHERE status = ? AND datetime(next_payment_due) < datetime('now')
            """.trimIndent(),
            LoanStatus.ACTIVE.name
        )
        return results.map { it.toLoan() }
    }

    suspend fun updateStatus(loanId: String, status: LoanStatus): Boolean {
        val affected = db.execute(
            "UPDATE bank_loans SET status = ? WHERE loan_id = ?",
            status.name,
            loanId
        )
        return affected > 0
    }

    suspend fun approve(loanId: String): Boolean {
        val affected = db.execute(
            """
            UPDATE bank_loans 
            SET status = ?, approved_at = ? 
            WHERE loan_id = ?
            """.trimIndent(),
            LoanStatus.APPROVED.name,
            LocalDateTime.now().toString(),
            loanId
        )
        return affected > 0
    }

    suspend fun makePayment(loanId: String, amount: Double): Boolean {
        val loan = findByLoanId(loanId) ?: return false
        val newRemaining = loan.remainingBalance - amount
        val newTotalPaid = loan.totalPaid + amount
        val newMonthsRemaining = loan.monthsRemaining - 1
        val nextPaymentDue = LocalDateTime.now().plusMonths(1)

        val newStatus = if (newRemaining <= 0) LoanStatus.PAID_OFF else LoanStatus.ACTIVE

        val affected = db.execute(
            """
            UPDATE bank_loans 
            SET remaining_balance = ?, total_paid = ?, months_remaining = ?,
                last_payment_date = ?, next_payment_due = ?, status = ?
            WHERE loan_id = ?
            """.trimIndent(),
            maxOf(0.0, newRemaining),
            newTotalPaid,
            maxOf(0, newMonthsRemaining),
            LocalDateTime.now().toString(),
            if (newStatus == LoanStatus.ACTIVE) nextPaymentDue.toString() else null,
            newStatus.name,
            loanId
        )
        return affected > 0
    }

    suspend fun incrementMissedPayments(loanId: String): Boolean {
        val affected = db.execute(
            """
            UPDATE bank_loans 
            SET missed_payments = missed_payments + 1 
            WHERE loan_id = ?
            """.trimIndent(),
            loanId
        )
        return affected > 0
    }

    private fun Map<String, Any?>.toLoan(): Loan {
        return Loan(
            id = (this["id"] as Number).toLong(),
            loanId = this["loan_id"] as String,
            borrowerUuid = UUID.fromString(this["borrower_uuid"] as String),
            linkedAccountNumber = this["linked_account_number"] as String,
            loanType = LoanType.valueOf(this["loan_type"] as String),
            principalAmount = (this["principal_amount"] as Number).toDouble(),
            interestRate = (this["interest_rate"] as Number).toDouble(),
            remainingBalance = (this["remaining_balance"] as Number).toDouble(),
            monthlyPayment = (this["monthly_payment"] as Number).toDouble(),
            totalPaid = (this["total_paid"] as Number).toDouble(),
            missedPayments = (this["missed_payments"] as Number).toInt(),
            termMonths = (this["term_months"] as Number).toInt(),
            monthsRemaining = (this["months_remaining"] as Number).toInt(),
            status = LoanStatus.valueOf(this["status"] as String),
            collateral = this["collateral"] as? String,
            createdAt = parseDateTime(this["created_at"] as? String),
            approvedAt = (this["approved_at"] as? String)?.let { parseDateTime(it) },
            nextPaymentDue = (this["next_payment_due"] as? String)?.let { parseDateTime(it) },
            lastPaymentDate = (this["last_payment_date"] as? String)?.let { parseDateTime(it) }
        )
    }

    private fun parseDateTime(value: String?): LocalDateTime {
        return try {
            if (value != null) LocalDateTime.parse(value) else LocalDateTime.now()
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}