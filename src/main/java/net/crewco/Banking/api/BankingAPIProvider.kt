// src/main/java/net/crewco/Banking/api/BankingAPIProvider.kt
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
import org.bukkit.Location
import java.util.UUID

/**
 * Implementation of the Banking API.
 * This is registered as a service and can be retrieved by other plugins.
 */
class BankingAPIProvider(
    private val accountService: AccountService,
    private val cardService: CardService,
    private val loanService: LoanService,
    private val transactionService: TransactionService,
    private val atmService: ATMService,
    private val numberGenerator: NumberGeneratorService
) : BankingAPI {

    // ==================== Account Operations ====================

    override suspend fun createAccount(
        ownerUuid: UUID,
        type: AccountType,
        initialDeposit: Double,
        accountName: String
    ): BankAccount? = accountService.createAccount(ownerUuid, type, initialDeposit, accountName)

    override suspend fun getAccount(accountNumber: String): BankAccount? =
        accountService.getAccount(accountNumber)

    override suspend fun getAccountsByPlayer(uuid: UUID): List<BankAccount> =
        accountService.getAccountsByPlayer(uuid)

    override suspend fun getWallet(uuid: UUID): BankAccount? =
        accountService.getWallet(uuid)

    override suspend fun getPrimaryAccount(uuid: UUID): BankAccount? =
        accountService.getPrimaryAccount(uuid)

    override suspend fun deposit(
        accountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String
    ): Boolean = accountService.deposit(accountNumber, amount, initiatedBy, description)

    override suspend fun withdraw(
        accountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String
    ): WithdrawalResult = accountService.withdraw(accountNumber, amount, initiatedBy, description)

    override suspend fun transfer(
        fromAccountNumber: String,
        toAccountNumber: String,
        amount: Double,
        initiatedBy: UUID,
        description: String
    ): TransferResult = accountService.transfer(fromAccountNumber, toAccountNumber, amount, initiatedBy, description)

    override suspend fun getBalance(accountNumber: String): Double? =
        accountService.getBalance(accountNumber)

    override suspend fun getTotalBalance(uuid: UUID): Double =
        accountService.getTotalBalance(uuid)

    override suspend fun freezeAccount(accountNumber: String): Boolean =
        accountService.freezeAccount(accountNumber)

    override suspend fun unfreezeAccount(accountNumber: String): Boolean =
        accountService.unfreezeAccount(accountNumber)

    // ==================== Card Operations ====================

    override suspend fun issueCard(
        ownerUuid: UUID,
        linkedAccountNumber: String,
        cardType: CardType,
        pin: String
    ): Card? = cardService.issueCard(ownerUuid, linkedAccountNumber, cardType, pin)

    override suspend fun getCard(cardNumber: String): Card? =
        cardService.getCard(cardNumber)

    override suspend fun getCardsByPlayer(ownerUuid: UUID): List<Card> =
        cardService.getCardsByPlayer(ownerUuid)

    override suspend fun useCard(
        cardNumber: String,
        pin: String,
        amount: Double,
        merchantDescription: String
    ): CardUseResult = cardService.useCard(cardNumber, pin, amount, merchantDescription)

    override suspend fun freezeCard(cardNumber: String): Boolean =
        cardService.freezeCard(cardNumber)

    override suspend fun unfreezeCard(cardNumber: String): Boolean =
        cardService.unfreezeCard(cardNumber)

    override suspend fun cancelCard(cardNumber: String): Boolean =
        cardService.cancelCard(cardNumber)

    // ==================== Loan Operations ====================

    override suspend fun applyForLoan(
        borrowerUuid: UUID,
        linkedAccountNumber: String,
        loanType: LoanType,
        amount: Double,
        termMonths: Int,
        collateral: String?
    ): LoanApplicationResult = loanService.applyForLoan(
        borrowerUuid, linkedAccountNumber, loanType, amount, termMonths, collateral
    )

    override suspend fun getLoan(loanId: String): Loan? =
        loanService.getLoan(loanId)

    override suspend fun getLoansByPlayer(borrowerUuid: UUID): List<Loan> =
        loanService.getLoansByPlayer(borrowerUuid)

    override suspend fun getActiveLoans(borrowerUuid: UUID): List<Loan> =
        loanService.getActiveLoans(borrowerUuid)

    override suspend fun makeLoanPayment(loanId: String, amount: Double?): LoanPaymentResult =
        loanService.makePayment(loanId, amount)

    // ==================== Transaction Operations ====================

    override suspend fun getTransactionHistory(accountNumber: String, limit: Int): List<Transaction> =
        transactionService.getTransactionHistory(accountNumber, limit)

    override suspend fun getTransaction(transactionId: String): Transaction? =
        transactionService.getTransaction(transactionId)

    // ==================== ATM Operations ====================

    override suspend fun getNearbyATMs(location: Location, radius: Double): List<ATM> =
        atmService.getNearbyATMs(location, radius)

    override suspend fun atmWithdraw(
        atmId: String,
        accountNumber: String,
        amount: Double,
        playerUuid: UUID
    ): ATMWithdrawResult = atmService.withdraw(atmId, accountNumber, amount, playerUuid)

    override suspend fun atmDeposit(
        atmId: String,
        accountNumber: String,
        amount: Double,
        playerUuid: UUID
    ): ATMDepositResult = atmService.deposit(atmId, accountNumber, amount, playerUuid)

    // ==================== Utility ====================

    override suspend fun ensureWalletExists(uuid: UUID): BankAccount =
        accountService.ensureWalletExists(uuid)

    override fun getBankRoutingNumber(): String =
        numberGenerator.getBankRoutingNumber()
}