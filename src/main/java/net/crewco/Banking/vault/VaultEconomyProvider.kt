// src/main/java/net/crewco/Banking/vault/VaultEconomyProvider.kt
package net.crewco.Banking.vault

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.crewco.Banking.Startup
import net.crewco.Banking.data.edata.WithdrawalResult
import net.crewco.Banking.data.models.AccountType
import net.crewco.Banking.services.AccountService
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.logging.Level

class VaultEconomyProvider(
    private val plugin: Startup,
    private val accountService: AccountService
) : Economy {

    companion object {
        private const val TIMEOUT_MS = 5000L // 5 second timeout to prevent hangs
    }

    /**
     * Safely executes a suspend function for Vault compatibility.
     * Uses Dispatchers.IO to avoid blocking the main thread and prevent deadlocks.
     */
    private fun <T> runSafe(default: T, block: suspend () -> T): T {
        return try {
            runBlocking(Dispatchers.IO) {
                withTimeout(TIMEOUT_MS) {
                    block()
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Vault economy operation failed: ${e.message}", e)
            default
        }
    }

    /**
     * Safely executes a suspend function that returns an EconomyResponse.
     */
    private fun runSafeEconomy(block: suspend () -> EconomyResponse): EconomyResponse {
        return try {
            runBlocking(Dispatchers.IO) {
                withTimeout(TIMEOUT_MS) {
                    block()
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Vault economy operation failed: ${e.message}", e)
            EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Operation timed out or failed: ${e.message}")
        }
    }

    override fun isEnabled(): Boolean = true

    override fun getName(): String = "CrewCo Banking"

    override fun hasBankSupport(): Boolean = true

    override fun fractionalDigits(): Int = 2

    override fun format(amount: Double): String = "$${String.format("%.2f", amount)}"

    override fun currencyNamePlural(): String =
        plugin.config.getString("economy.currency-plural", "Dollars") ?: "Dollars"

    override fun currencyNameSingular(): String =
        plugin.config.getString("economy.currency-singular", "Dollar") ?: "Dollar"

    // ==================== Player Account Methods ====================

    override fun hasAccount(player: OfflinePlayer): Boolean = runSafe(false) {
        accountService.getWallet(player.uniqueId) != null
    }

    override fun hasAccount(playerName: String): Boolean {
        val player = Bukkit.getOfflinePlayer(playerName)
        return hasAccount(player)
    }

    override fun hasAccount(player: OfflinePlayer, worldName: String): Boolean = hasAccount(player)

    override fun hasAccount(playerName: String, worldName: String): Boolean = hasAccount(playerName)

    override fun getBalance(player: OfflinePlayer): Double = runSafe(0.0) {
        accountService.getWallet(player.uniqueId)?.balance ?: 0.0
    }

    override fun getBalance(playerName: String): Double {
        val player = Bukkit.getOfflinePlayer(playerName)
        return getBalance(player)
    }

    override fun getBalance(player: OfflinePlayer, world: String): Double = getBalance(player)

    override fun getBalance(playerName: String, world: String): Double = getBalance(playerName)

    override fun has(player: OfflinePlayer, amount: Double): Boolean = getBalance(player) >= amount

    override fun has(playerName: String, amount: Double): Boolean {
        val player = Bukkit.getOfflinePlayer(playerName)
        return has(player, amount)
    }

    override fun has(player: OfflinePlayer, world: String, amount: Double): Boolean = has(player, amount)

    override fun has(playerName: String, world: String, amount: Double): Boolean = has(playerName, amount)

    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): EconomyResponse = runSafeEconomy {
        val wallet = accountService.getWallet(player.uniqueId)
            ?: return@runSafeEconomy EconomyResponse(
                0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "No wallet found"
            )

        val result = accountService.withdraw(
            wallet.accountNumber,
            amount,
            player.uniqueId,
            "Vault withdrawal"
        )

        when (result) {
            WithdrawalResult.SUCCESS -> {
                val newBalance = accountService.getBalance(wallet.accountNumber) ?: 0.0
                EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "")
            }
            WithdrawalResult.INSUFFICIENT_FUNDS -> {
                EconomyResponse(0.0, wallet.balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds")
            }
            else -> {
                EconomyResponse(0.0, wallet.balance, EconomyResponse.ResponseType.FAILURE, "Withdrawal failed: $result")
            }
        }
    }

    override fun withdrawPlayer(playerName: String, amount: Double): EconomyResponse {
        val player = Bukkit.getOfflinePlayer(playerName)
        return withdrawPlayer(player, amount)
    }

    override fun withdrawPlayer(player: OfflinePlayer, world: String, amount: Double): EconomyResponse =
        withdrawPlayer(player, amount)

    override fun withdrawPlayer(playerName: String, world: String, amount: Double): EconomyResponse =
        withdrawPlayer(playerName, amount)

    override fun depositPlayer(player: OfflinePlayer, amount: Double): EconomyResponse = runSafeEconomy {
        val wallet = accountService.ensureWalletExists(player.uniqueId)

        val success = accountService.deposit(
            wallet.accountNumber,
            amount,
            player.uniqueId,
            "Vault deposit"
        )

        if (success) {
            val newBalance = accountService.getBalance(wallet.accountNumber) ?: 0.0
            EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "")
        } else {
            EconomyResponse(0.0, wallet.balance, EconomyResponse.ResponseType.FAILURE, "Deposit failed")
        }
    }

    override fun depositPlayer(playerName: String, amount: Double): EconomyResponse {
        val player = Bukkit.getOfflinePlayer(playerName)
        return depositPlayer(player, amount)
    }

    override fun depositPlayer(player: OfflinePlayer, world: String, amount: Double): EconomyResponse =
        depositPlayer(player, amount)

    override fun depositPlayer(playerName: String, world: String, amount: Double): EconomyResponse =
        depositPlayer(playerName, amount)

    override fun createPlayerAccount(player: OfflinePlayer): Boolean = runSafe(false) {
        accountService.ensureWalletExists(player.uniqueId)
        true
    }

    override fun createPlayerAccount(playerName: String): Boolean {
        val player = Bukkit.getOfflinePlayer(playerName)
        return createPlayerAccount(player)
    }

    override fun createPlayerAccount(player: OfflinePlayer, world: String): Boolean =
        createPlayerAccount(player)

    override fun createPlayerAccount(playerName: String, world: String): Boolean =
        createPlayerAccount(playerName)

    // ==================== Bank Account Methods ====================

    override fun createBank(name: String, player: OfflinePlayer): EconomyResponse = runSafeEconomy {
        val account = accountService.createAccount(
            player.uniqueId,
            AccountType.BUSINESS,
            0.0,
            name
        )

        if (account != null) {
            EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "")
        } else {
            EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Could not create bank")
        }
    }

    override fun createBank(name: String, playerName: String): EconomyResponse {
        val player = Bukkit.getOfflinePlayer(playerName)
        return createBank(name, player)
    }

    override fun deleteBank(name: String): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use /bank close")
    }

    override fun bankBalance(name: String): EconomyResponse = runSafeEconomy {
        val balance = accountService.getBalance(name)
        if (balance != null) {
            EconomyResponse(0.0, balance, EconomyResponse.ResponseType.SUCCESS, "")
        } else {
            EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank not found")
        }
    }

    override fun bankHas(name: String, amount: Double): EconomyResponse = runSafeEconomy {
        val balance = accountService.getBalance(name) ?: 0.0
        if (balance >= amount) {
            EconomyResponse(0.0, balance, EconomyResponse.ResponseType.SUCCESS, "")
        } else {
            EconomyResponse(0.0, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds")
        }
    }

    override fun bankWithdraw(name: String, amount: Double): EconomyResponse = runSafeEconomy {
        val account = accountService.getAccount(name)
            ?: return@runSafeEconomy EconomyResponse(
                0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank not found"
            )

        val result = accountService.withdraw(name, amount, account.uuid, "Vault bank withdrawal")
        val newBalance = accountService.getBalance(name) ?: 0.0

        when (result) {
            WithdrawalResult.SUCCESS -> {
                EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "")
            }
            else -> {
                EconomyResponse(0.0, newBalance, EconomyResponse.ResponseType.FAILURE, "Withdrawal failed: $result")
            }
        }
    }

    override fun bankDeposit(name: String, amount: Double): EconomyResponse = runSafeEconomy {
        val account = accountService.getAccount(name)
            ?: return@runSafeEconomy EconomyResponse(
                0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank not found"
            )

        val success = accountService.deposit(name, amount, account.uuid, "Vault bank deposit")
        val newBalance = accountService.getBalance(name) ?: 0.0

        if (success) {
            EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "")
        } else {
            EconomyResponse(0.0, newBalance, EconomyResponse.ResponseType.FAILURE, "Deposit failed")
        }
    }

    override fun isBankOwner(name: String, player: OfflinePlayer): EconomyResponse = runSafeEconomy {
        val account = accountService.getAccount(name)
        if (account != null && account.uuid == player.uniqueId) {
            EconomyResponse(0.0, account.balance, EconomyResponse.ResponseType.SUCCESS, "")
        } else {
            EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Not owner")
        }
    }

    override fun isBankOwner(name: String, playerName: String): EconomyResponse {
        val player = Bukkit.getOfflinePlayer(playerName)
        return isBankOwner(name, player)
    }

    override fun isBankMember(name: String, player: OfflinePlayer): EconomyResponse =
        isBankOwner(name, player)

    override fun isBankMember(name: String, playerName: String): EconomyResponse {
        val player = Bukkit.getOfflinePlayer(playerName)
        return isBankMember(name, player)
    }

    override fun getBanks(): MutableList<String> = runSafe(mutableListOf()) {
        mutableListOf()
    }
}