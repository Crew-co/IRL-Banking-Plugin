// src/main/java/net/crewco/Banking/commands/BankCommand.kt
package net.crewco.Banking.commands

import com.google.inject.Inject
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.Startup.Companion.plugin
import net.crewco.Banking.Startup.Companion.transactionService
import net.crewco.Banking.data.edata.WithdrawalResult
import net.crewco.Banking.data.models.AccountType
import net.crewco.Banking.services.AccountService
import net.crewco.Banking.services.sdata.TransferResult
import net.crewco.Banking.util.Messages
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.*
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import java.util.stream.Stream

class BankCommand {

    // ==================== Main Command ====================

    @Command("bank [action] [args]")
    @CommandDescription("Banking commands - use /bank help for more info")
    @Permission("banking.use")
    suspend fun bank(
        player: Player,
        @Argument("action", suggestions = "bankActions") action: String?,
        @Argument("args", suggestions = "bankArgs") args: Array<String>?
    ) {
        val subcommand = action?.lowercase() ?: "list"
        val arguments = args ?: emptyArray()

        when (subcommand) {
            "list", "accounts" -> listAccounts(player)
            "balance", "bal" -> checkBalance(player, arguments.getOrNull(0))
            "open", "create" -> openAccount(player, arguments.getOrNull(0))
            "deposit" -> deposit(player, arguments.getOrNull(0), arguments.getOrNull(1))
            "withdraw" -> withdraw(player, arguments.getOrNull(0), arguments.getOrNull(1))
            "transfer", "send" -> transfer(player, arguments.getOrNull(0), arguments.getOrNull(1), arguments.getOrNull(2))
            "history", "transactions" -> viewHistory(player, arguments.getOrNull(0))
            "info", "details" -> accountInfo(player, arguments.getOrNull(0))
            "help" -> showHelp(player)
            else -> {
                player.sendMessage(Messages.error("Unknown subcommand: $subcommand"))
                showHelp(player)
            }
        }
    }

    // ==================== Suggestions ====================

    @Suggestions("bankActions")
    fun actionSuggestions(
        context: CommandContext<Player>,
        input: String
    ): Stream<String> {
        val actions = mutableListOf(
            "list",
            "balance",
            "open",
            "deposit",
            "withdraw",
            "transfer",
            "history",
            "info",
            "help"
        )
        return actions.stream().filter { it.startsWith(input.lowercase()) }
    }

    @Suggestions("bankArgs")
    fun argsSuggestions(
        context: CommandContext<Player>,
        input: String
    ): Stream<String> {
        val suggestions = mutableListOf<String>()

        // Get the action from the command context if available
        val action = try {
            context.get<String>("action")?.lowercase()
        } catch (e: Exception) {
            null
        }

        when (action) {
            "open", "create" -> {
                // Suggest account types
                AccountType.entries.forEach { type ->
                    suggestions.add(type.name.lowercase())
                }
            }
            "balance", "bal", "history", "transactions", "info", "details" -> {
                // Suggest account numbers (would need async access to player's accounts)
                suggestions.add("<account_number>")
            }
            "deposit", "withdraw" -> {
                // Suggest amounts
                suggestions.addAll(listOf("100", "500", "1000", "5000", "10000"))
            }
            "transfer", "send" -> {
                // Suggest amounts
                suggestions.addAll(listOf("100", "500", "1000", "5000"))
            }
        }

        return suggestions.stream().filter { it.startsWith(input.lowercase()) }
    }

    @Suggestions("accountTypes")
    fun accountTypeSuggestions(
        context: CommandContext<Player>,
        input: String
    ): Stream<String> {
        val types = AccountType.entries.map { it.name.lowercase() }
        return types.stream().filter { it.startsWith(input.lowercase()) }
    }

    // ==================== Subcommand Handlers ====================

    private suspend fun listAccounts(player: Player) {
        val accounts = accountService.getAccountsByPlayer(player.uniqueId)

        if (accounts.isEmpty()) {
            player.sendMessage(Messages.noAccounts())
            return
        }

        player.sendMessage(Messages.header("Your Bank Accounts"))

        for (account in accounts) {
            player.sendMessage(
                Messages.accountLine(
                    account.accountName,
                    account.accountType.displayName,
                    account.accountNumber,
                    account.balance
                )
            )
        }

        val total = accounts.sumOf { it.balance }
        player.sendMessage(Messages.totalBalance(total))
    }

    private suspend fun checkBalance(player: Player, accountNumber: String?) {
        val account = if (accountNumber != null) {
            accountService.getAccount(accountNumber)
        } else {
            accountService.getPrimaryAccount(player.uniqueId)
        }

        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.accountNotFound())
            return
        }

        player.sendMessage(Messages.balanceInfo(account.accountName, account.balance, account.getAvailableBalance()))
    }

    private suspend fun openAccount(player: Player, typeArg: String?) {
        if (typeArg == null) {
            player.sendMessage(Messages.error("Usage: /bank open <type>"))
            player.sendMessage(Messages.info("Available types: ${AccountType.entries.joinToString(", ") { it.name.lowercase() }}"))
            return
        }

        val type = try {
            AccountType.valueOf(typeArg.uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendMessage(Messages.error("Invalid account type: $typeArg"))
            player.sendMessage(Messages.info("Available types: ${AccountType.entries.joinToString(", ") { it.name.lowercase() }}"))
            return
        }

        val existing = accountService.getAccountsByPlayer(player.uniqueId)
            .firstOrNull { it.accountType == type }

        if (existing != null && type != AccountType.WALLET) {
            player.sendMessage(Messages.error("You already have a ${type.displayName}!"))
            return
        }

        val account = accountService.createAccount(player.uniqueId, type)

        if (account != null) {
            player.sendMessage(Messages.success("Account opened successfully!"))
            player.sendMessage(Messages.accountDetails(account))
        } else {
            player.sendMessage(Messages.error("Failed to open account."))
        }
    }

    private suspend fun deposit(player: Player, amountArg: String?, accountNumber: String?) {
        if (amountArg == null) {
            player.sendMessage(Messages.error("Usage: /bank deposit <amount> [account_number]"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            player.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val account = if (accountNumber != null) {
            accountService.getAccount(accountNumber)
        } else {
            accountService.getPrimaryAccount(player.uniqueId)
        }

        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.accountNotFound())
            return
        }

        val success = accountService.deposit(account.accountNumber, amount, player.uniqueId, "Manual deposit")

        if (success) {
            val newBalance = accountService.getBalance(account.accountNumber) ?: 0.0
            player.sendMessage(Messages.depositSuccess(amount, newBalance))
        } else {
            player.sendMessage(Messages.error("Deposit failed."))
        }
    }

    private suspend fun withdraw(player: Player, amountArg: String?, accountNumber: String?) {
        if (amountArg == null) {
            player.sendMessage(Messages.error("Usage: /bank withdraw <amount> [account_number]"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            player.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val account = if (accountNumber != null) {
            accountService.getAccount(accountNumber)
        } else {
            accountService.getPrimaryAccount(player.uniqueId)
        }

        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.accountNotFound())
            return
        }

        val result = accountService.withdraw(account.accountNumber, amount, player.uniqueId, "Manual withdrawal")

        when (result) {
            WithdrawalResult.SUCCESS -> {
                val newBalance = accountService.getBalance(account.accountNumber) ?: 0.0
                player.sendMessage(Messages.withdrawSuccess(amount, newBalance))
            }
            WithdrawalResult.INSUFFICIENT_FUNDS -> {
                player.sendMessage(Messages.error("Insufficient funds!"))
            }
            WithdrawalResult.DAILY_LIMIT_EXCEEDED -> {
                player.sendMessage(Messages.error("Daily withdrawal limit exceeded!"))
            }
            WithdrawalResult.ACCOUNT_FROZEN -> {
                player.sendMessage(Messages.error("Your account is frozen!"))
            }
        }
    }

    private suspend fun transfer(player: Player, amountArg: String?, toAccountNumber: String?, fromAccountNumber: String?) {
        if (amountArg == null || toAccountNumber == null) {
            player.sendMessage(Messages.error("Usage: /bank transfer <amount> <to_account> [from_account]"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            player.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val fromAccount = if (fromAccountNumber != null) {
            accountService.getAccount(fromAccountNumber)
        } else {
            accountService.getPrimaryAccount(player.uniqueId)
        }

        if (fromAccount == null || fromAccount.uuid != player.uniqueId) {
            player.sendMessage(Messages.error("Source account not found or doesn't belong to you!"))
            return
        }

        val toAccount = accountService.getAccount(toAccountNumber)
        if (toAccount == null) {
            player.sendMessage(Messages.error("Destination account not found!"))
            return
        }

        val result = accountService.transfer(
            fromAccount.accountNumber,
            toAccountNumber,
            amount,
            player.uniqueId,
            "Transfer to $toAccountNumber"
        )

        when (result) {
            TransferResult.SUCCESS -> {
                player.sendMessage(Messages.transferSuccess(amount, toAccountNumber))
            }
            TransferResult.INSUFFICIENT_FUNDS -> {
                player.sendMessage(Messages.error("Insufficient funds!"))
            }
            TransferResult.DAILY_LIMIT_EXCEEDED -> {
                player.sendMessage(Messages.error("Daily limit exceeded!"))
            }
            TransferResult.FROM_ACCOUNT_FROZEN -> {
                player.sendMessage(Messages.error("Your account is frozen!"))
            }
            TransferResult.TO_ACCOUNT_FROZEN -> {
                player.sendMessage(Messages.error("Destination account is frozen!"))
            }
            TransferResult.SAME_ACCOUNT -> {
                player.sendMessage(Messages.error("Cannot transfer to the same account!"))
            }
            else -> {
                player.sendMessage(Messages.error("Transfer failed."))
            }
        }
    }

    private suspend fun viewHistory(player: Player, accountNumber: String?) {
        val account = if (accountNumber != null) {
            accountService.getAccount(accountNumber)
        } else {
            accountService.getPrimaryAccount(player.uniqueId)
        }

        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.accountNotFound())
            return
        }

        val transactions = transactionService.getTransactionHistory(account.accountNumber, 10)

        if (transactions.isEmpty()) {
            player.sendMessage(Messages.info("No transaction history."))
            return
        }

        player.sendMessage(Messages.header("Transaction History"))

        for (tx in transactions) {
            player.sendMessage(Messages.transactionLine(tx))
        }
    }

    private suspend fun accountInfo(player: Player, accountNumber: String?) {
        val account = if (accountNumber != null) {
            accountService.getAccount(accountNumber)
        } else {
            accountService.getPrimaryAccount(player.uniqueId)
        }

        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.accountNotFound())
            return
        }

        player.sendMessage(Messages.header("Account Information"))
        player.sendMessage(Messages.accountDetails(account))
    }

    private fun showHelp(player: Player) {
        player.sendMessage(Messages.header("Bank Commands"))
        player.sendMessage(Messages.info("/bank - List all your accounts"))
        player.sendMessage(Messages.info("/bank balance [account] - Check balance"))
        player.sendMessage(Messages.info("/bank open <type> - Open a new account"))
        player.sendMessage(Messages.info("/bank deposit <amount> [account] - Deposit money"))
        player.sendMessage(Messages.info("/bank withdraw <amount> [account] - Withdraw money"))
        player.sendMessage(Messages.info("/bank transfer <amount> <to> [from] - Transfer money"))
        player.sendMessage(Messages.info("/bank history [account] - View transactions"))
        player.sendMessage(Messages.info("/bank info [account] - View account details"))
        player.sendMessage(Messages.info(""))
        player.sendMessage(Messages.info("Account types: ${AccountType.entries.joinToString(", ") { it.name.lowercase() }}"))
    }
}