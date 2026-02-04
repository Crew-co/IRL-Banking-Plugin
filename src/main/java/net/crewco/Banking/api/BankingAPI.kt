// src/main/java/net/crewco/Banking/api/BankingAPI.kt
package net.crewco.Banking.api

import net.crewco.Banking.data.cdata.CardType
import net.crewco.Banking.data.cdata.CardUseResult
import net.crewco.Banking.data.edata.WithdrawalResult
import net.crewco.Banking.data.ldata.LoanType
import net.crewco.Banking.data.models.*
import net.crewco.Banking.services.*
import net.crewco.Banking.services.sdata.ATMDepositResult
import net.crewco.Banking.services.sdata.ATMWithdrawResult
import net.crewco.Banking.services.sdata.LoanApplicationResult
import net.crewco.Banking.services.sdata.LoanPaymentResult
import net.crewco.Banking.services.sdata.TransferResult
import java.util.UUID

/**
 * Main API interface for the CrewCo Banking plugin.
 * Other plugins can use this to interact with the banking system.
 */
interface BankingAPI {

    // ==================== Account Operations ====================

    /**
     * Creates a new bank account for a player.
     * @param ownerUuid The UUID of the account owner
     * @param type The type of account to create
     * @param initialDeposit Optional initial deposit amount
     * @param accountName Optional custom name for the account
     * @return The created account, or null if creation failed
     */
    suspend fun createAccount(
        ownerUuid: UUID,
        type: AccountType,
        initialDeposit: Double = 0.0,
        accountName: String = ""
    ): BankAccount?

    /**
     * Gets an account by its account number.
     */
    suspend fun getAccount(accountNumber: String): BankAccount?

    /**
     * Gets all accounts belonging to a player.
     */
    suspend fun getAccountsByPlayer(uuid: UUID): List<BankAccount>

    /**
     * Gets the player's wallet account.
     */
    suspend fun getWallet(uuid: UUID): BankAccount?

    /**
     * Gets the player's primary account (checking or wallet).
     */
    suspend fun getPrimaryAccount(uuid: UUID): BankAccount?

    /**
     * Deposits money into an account.
     * @return true if successful
     */
    suspend fun deposit(
        accountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String = ""
    ): Boolean

    /**
     * Withdraws money from an account.
     * @return Result indicating success or failure reason
     */
    suspend fun withdraw(
        accountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String = ""
    ): WithdrawalResult

    /**
     * Transfers money between accounts.
     * @return Result indicating success or failure reason
     */
    suspend fun transfer(
        fromAccountNumber: String,
        toAccountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String = ""
    ): TransferResult

    /**
     * Gets the balance of an account.
     */
    suspend fun getBalance(accountNumber: String): Double?

    /**
     * Gets the total balance across all of a player's accounts.
     */
    suspend fun getTotalBalance(uuid: UUID): Double

    /**
     * Freezes an account, preventing transactions.
     */
    suspend fun freezeAccount(accountNumber: String): Boolean

    /**
     * Unfreezes a frozen account.
     */
    suspend fun unfreezeAccount(accountNumber: String): Boolean

    // ==================== Card Operations ====================

    /**
     * Issues a new card linked to an account.
     * @param pin 4-digit PIN for the card
     */
    suspend fun issueCard(
        ownerUuid: UUID,
        linkedAccountNumber: String,
        cardType: CardType,
        pin: String
    ): Card?

    /**
     * Gets a card by its card number.
     */
    suspend fun getCard(cardNumber: String): Card?

    /**
     * Gets all cards belonging to a player.
     */
    suspend fun getCardsByPlayer(ownerUuid: UUID): List<Card>

    /**
     * Processes a card payment.
     * @return Result indicating success or failure reason
     */
    suspend fun useCard(
        cardNumber: String,
        pin: String,
        amount: Double,
        merchantDescription: String
    ): CardUseResult

    /**
     * Freezes a card.
     */
    suspend fun freezeCard(cardNumber: String): Boolean

    /**
     * Unfreezes a card.
     */
    suspend fun unfreezeCard(cardNumber: String): Boolean

    /**
     * Cancels a card permanently.
     */
    suspend fun cancelCard(cardNumber: String): Boolean

    // ==================== Loan Operations ====================

    /**
     * Applies for a loan.
     */
    suspend fun applyForLoan(
        borrowerUuid: UUID,
        linkedAccountNumber: String,
        loanType: LoanType,
        amount: Double,
        termMonths: Int,
        collateral: String? = null
    ): LoanApplicationResult

    /**
     * Gets a loan by its loan ID.
     */
    suspend fun getLoan(loanId: String): Loan?

    /**
     * Gets all loans for a player.
     */
    suspend fun getLoansByPlayer(borrowerUuid: UUID): List<Loan>

    /**
     * Gets active loans for a player.
     */
    suspend fun getActiveLoans(borrowerUuid: UUID): List<Loan>

    /**
     * Makes a payment on a loan.
     * @param amount Optional custom amount, defaults to monthly payment
     */
    suspend fun makeLoanPayment(loanId: String, amount: Double? = null): LoanPaymentResult

    // ==================== Transaction Operations ====================

    /**
     * Gets transaction history for an account.
     */
    suspend fun getTransactionHistory(accountNumber: String, limit: Int = 50): List<Transaction>

    /**
     * Gets a specific transaction by ID.
     */
    suspend fun getTransaction(transactionId: String): Transaction?

    // ==================== ATM Operations ====================

    /**
     * Gets the nearest ATMs to a location.
     */
    suspend fun getNearbyATMs(location: org.bukkit.Location, radius: Double = 50.0): List<ATM>

    /**
     * Withdraws cash from an ATM.
     */
    suspend fun atmWithdraw(
        atmId: String,
        accountNumber: String,
        amount: Double,
        playerUuid: UUID
    ): ATMWithdrawResult

    /**
     * Deposits cash into an ATM.
     */
    suspend fun atmDeposit(
        atmId: String,
        accountNumber: String,
        amount: Double,
        playerUuid: UUID
    ): ATMDepositResult

    // ==================== Utility ====================

    /**
     * Ensures a player has a wallet account, creating one if needed.
     */
    suspend fun ensureWalletExists(uuid: UUID): BankAccount

    /**
     * Gets the bank's routing number.
     */
    fun getBankRoutingNumber(): String
}