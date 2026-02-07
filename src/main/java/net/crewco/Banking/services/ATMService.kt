// src/main/java/net/crewco/Banking/services/ATMService.kt
package net.crewco.Banking.services

import net.crewco.Banking.data.ATMResult
import net.crewco.Banking.data.edata.WithdrawalResult
import net.crewco.Banking.data.models.ATM
import net.crewco.Banking.data.repositories.AccountRepository
import net.crewco.Banking.data.repositories.ATMRepository
import net.crewco.Banking.data.repositories.BankRepository
import net.crewco.Banking.services.sdata.ATMDepositResult
import net.crewco.Banking.services.sdata.ATMWithdrawResult
import org.bukkit.Location
import java.util.UUID

class ATMService(
    private val atmRepository: ATMRepository,
    private val accountRepository: AccountRepository,
    private val bankRepository: BankRepository,
    private val numberGenerator: NumberGeneratorService,
    private val transactionService: TransactionService
) {

    /**
     * Create a system-owned ATM (admin only)
     */
    suspend fun createATM(location: Location, placedBy: UUID): ATM? {
        return createATMForBank(location, placedBy, "SYSTEM")
    }

    /**
     * Create an ATM owned by a specific bank
     */
    suspend fun createATMForBank(location: Location, placedBy: UUID, bankId: String): ATM? {
        // Check if ATM already exists at this location
        val existing = atmRepository.findByLocation(location)
        if (existing != null) return null

        val atm = ATM(
            atmId = numberGenerator.generateAtmId(),
            location = location,
            bankId = bankId,
            placedBy = placedBy
        )

        val success = atmRepository.create(atm)
        return if (success) atm else null
    }

    /**
     * Check if player is a member of the ATM's bank
     */
    suspend fun isPlayerMember(playerUuid: UUID, bankId: String): Boolean {
        if (bankId == "SYSTEM") return true // System ATMs treat everyone as members
        val membership = bankRepository.getMembership(playerUuid,bankId)
        return membership != null
    }

    /**
     * Get the total fee for using this ATM
     */
    suspend fun getATMFee(atm: ATM, playerUuid: UUID): Double {
        val isMember = isPlayerMember(playerUuid, atm.bankId)
        return atm.getTotalFee(isMember)
    }

    suspend fun getATM(atmId: String): ATM? {
        return atmRepository.findByAtmId(atmId)
    }

    suspend fun getATMAtLocation(location: Location): ATM? {
        return atmRepository.findByLocation(location)
    }

    suspend fun getNearbyATMs(location: Location, radius: Double = 50.0): List<ATM> {
        return atmRepository.findNearby(location, radius)
    }

    suspend fun getATMsByBank(bankId: String): List<ATM> {
        return atmRepository.findByBankId(bankId)
    }

    suspend fun countATMsByBank(bankId: String): Int {
        return atmRepository.countByBankId(bankId)
    }

    suspend fun withdraw(
        atmId: String,
        accountNumber: String,
        amount: Double,
        playerUuid: UUID
    ): ATMWithdrawResult {
        val atm = atmRepository.findByAtmId(atmId) ?: return ATMWithdrawResult(false, "ATM not found")

        // Check ATM status
        val atmCheck = atm.canDispense(amount)
        if (atmCheck != ATMResult.SUCCESS) {
            return when (atmCheck) {
                ATMResult.ATM_OFFLINE -> ATMWithdrawResult(false, "ATM is offline")
                ATMResult.EXCEEDS_LIMIT -> ATMWithdrawResult(false, "Amount exceeds ATM limit")
                ATMResult.INSUFFICIENT_CASH -> ATMWithdrawResult(false, "ATM has insufficient cash")
                else -> ATMWithdrawResult(false, "ATM error")
            }
        }

        // Check account
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: return ATMWithdrawResult(false, "Account not found")

        if (account.uuid != playerUuid) {
            return ATMWithdrawResult(false, "Account does not belong to you")
        }

        // Calculate fee based on membership
        val isMember = isPlayerMember(playerUuid, atm.bankId)
        val fee = atm.getTotalFee(isMember)
        
        val totalAmount = amount + fee
        val withdrawCheck = account.canWithdraw(totalAmount)
        if (withdrawCheck != WithdrawalResult.SUCCESS) {
            return when (withdrawCheck) {
                WithdrawalResult.INSUFFICIENT_FUNDS -> ATMWithdrawResult(false, "Insufficient funds (need ${totalAmount} including fee)")
                WithdrawalResult.DAILY_LIMIT_EXCEEDED -> ATMWithdrawResult(false, "Daily limit exceeded")
                WithdrawalResult.ACCOUNT_FROZEN -> ATMWithdrawResult(false, "Account is frozen")
                else -> ATMWithdrawResult(false, "Cannot withdraw")
            }
        }

        // Process withdrawal
        accountRepository.updateBalance(accountNumber, account.balance - totalAmount)
        atmRepository.updateCash(atmId, atm.cash - amount)

        // If bank-owned ATM, add fee to bank reserves
        if (atm.bankId != "SYSTEM") {
            val bank = bankRepository.getBank(atm.bankId)
            if (bank != null) {
                bankRepository.updateBank(bank.copy(reserves = bank.reserves + fee))
            }
        }

        // Record transaction
        transactionService.recordATMWithdrawal(accountNumber, amount, fee, playerUuid, atmId)

        return ATMWithdrawResult(true, "Withdrawal successful", amount, fee)
    }

    suspend fun deposit(
        atmId: String,
        accountNumber: String,
        amount: Double,
        playerUuid: UUID
    ): ATMDepositResult {
        val atm = atmRepository.findByAtmId(atmId) ?: return ATMDepositResult(false, "ATM not found")

        if (!atm.active) {
            return ATMDepositResult(false, "ATM is offline")
        }

        // Check account
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: return ATMDepositResult(false, "Account not found")

        if (account.uuid != playerUuid) {
            return ATMDepositResult(false, "Account does not belong to you")
        }

        if (account.frozen) {
            return ATMDepositResult(false, "Account is frozen")
        }

        // Process deposit (no fee for deposits usually)
        accountRepository.updateBalance(accountNumber, account.balance + amount)
        atmRepository.updateCash(atmId, atm.cash + amount)

        // Record transaction
        transactionService.recordATMDeposit(accountNumber, amount, playerUuid, atmId)

        return ATMDepositResult(true, "Deposit successful", amount)
    }

    suspend fun checkBalance(accountNumber: String, playerUuid: UUID): Double? {
        val account = accountRepository.findByAccountNumber(accountNumber) ?: return null
        if (account.uuid != playerUuid) return null
        return account.balance
    }

    suspend fun refillATM(atmId: String, amount: Double): Boolean {
        val atm = atmRepository.findByAtmId(atmId) ?: return false
        return atmRepository.updateCash(atmId, atm.cash + amount)
    }

    suspend fun setATMActive(atmId: String, active: Boolean): Boolean {
        return atmRepository.setActive(atmId, active)
    }

    suspend fun updateATMFees(atmId: String, transactionFee: Double, outOfNetworkFee: Double): Boolean {
        return atmRepository.updateFees(atmId, transactionFee, outOfNetworkFee)
    }

    suspend fun removeATM(atmId: String): Boolean {
        return atmRepository.delete(atmId)
    }

    suspend fun getAllActiveATMs(): List<ATM> {
        return atmRepository.getAllActive()
    }
}