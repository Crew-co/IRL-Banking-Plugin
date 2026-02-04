// src/main/java/net/crewco/Banking/commands/AdminBankCommand.kt
package net.crewco.Banking.commands

import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.Startup.Companion.atmService
import net.crewco.Banking.Startup.Companion.loanService
import net.crewco.Banking.util.Messages
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.*
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import java.util.stream.Stream

class AdminBankCommand {

    // ==================== Main Command ====================

    @Command("bankadmin [category] [action] [args]")
    @CommandDescription("Bank administration commands")
    @Permission("banking.admin")
    suspend fun bankAdmin(
        sender: CommandSender,
        @Argument("category", suggestions = "adminCategories") category: String?,
        @Argument("action", suggestions = "adminActions") action: String?,
        @Argument("args", suggestions = "adminArgs") args: Array<String>?
    ) {
        val cat = category?.lowercase() ?: "help"
        val act = action?.lowercase() ?: "help"
        val arguments = args ?: emptyArray()

        when (cat) {
            "account", "acc" -> handleAccountCommands(sender, act, arguments)
            "wallet", "eco", "economy" -> handleWalletCommands(sender, act, arguments)
            "loan", "loans" -> handleLoanCommands(sender, act, arguments)
            "atm", "atms" -> handleAtmCommands(sender, act, arguments)
            "help" -> showHelp(sender)
            else -> {
                sender.sendMessage(Messages.error("Unknown category: $cat"))
                showHelp(sender)
            }
        }
    }

    // ==================== Suggestions ====================

    @Suggestions("adminCategories")
    fun categorySuggestions(
        context: CommandContext<CommandSender>,
        input: String
    ): Stream<String> {
        val categories = mutableListOf(
            "account",
            "wallet",
            "loan",
            "atm",
            "help"
        )
        return categories.stream().filter { it.startsWith(input.lowercase()) }
    }

    @Suggestions("adminActions")
    fun actionSuggestions(
        context: CommandContext<CommandSender>,
        input: String
    ): Stream<String> {
        val actions = mutableListOf<String>()

        val category = try {
            context.get<String>("category")?.lowercase()
        } catch (e: Exception) {
            null
        }

        when (category) {
            "account", "acc" -> {
                actions.addAll(listOf("freeze", "unfreeze", "info", "setbalance", "deposit", "withdraw"))
            }
            "wallet", "eco", "economy" -> {
                actions.addAll(listOf("info", "set", "give", "take", "reset"))
            }
            "loan", "loans" -> {
                actions.addAll(listOf("pending", "approve", "reject", "disburse", "overdue"))
            }
            "atm", "atms" -> {
                actions.addAll(listOf("create", "remove", "refill", "list", "toggle"))
            }
        }

        return actions.stream().filter { it.startsWith(input.lowercase()) }
    }

    @Suggestions("adminArgs")
    fun argsSuggestions(
        context: CommandContext<CommandSender>,
        input: String
    ): Stream<String> {
        val suggestions = mutableListOf<String>()

        val category = try {
            context.get<String>("category")?.lowercase()
        } catch (e: Exception) {
            null
        }

        val action = try {
            context.get<String>("action")?.lowercase()
        } catch (e: Exception) {
            null
        }

        when (category) {
            "account", "acc" -> {
                when (action) {
                    "freeze", "unfreeze" -> suggestions.add("<account_number>")
                    "info" -> {
                        suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
                        suggestions.add("<account_number>")
                    }
                    "setbalance", "deposit", "withdraw" -> suggestions.addAll(listOf("<account_number>", "<amount>"))
                }
            }
            "wallet", "eco", "economy" -> {
                when (action) {
                    "info" -> suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
                    "set", "give", "take" -> {
                        suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
                        suggestions.add("<amount>")
                    }
                    "reset" -> suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
                }
            }
            "loan", "loans" -> {
                when (action) {
                    "approve", "disburse" -> suggestions.add("<loan_id>")
                    "reject" -> suggestions.addAll(listOf("<loan_id>", "<reason>"))
                }
            }
            "atm", "atms" -> {
                when (action) {
                    "remove", "toggle" -> suggestions.add("<atm_id>")
                    "refill" -> suggestions.addAll(listOf("<atm_id>", "<amount>"))
                }
            }
        }

        return suggestions.stream().filter { it.startsWith(input.lowercase()) }
    }

    // ==================== Wallet Commands (Player-based) ====================

    private suspend fun handleWalletCommands(sender: CommandSender, action: String, args: Array<String>) {
        when (action) {
            "info" -> walletInfo(sender, args.getOrNull(0))
            "set" -> walletSet(sender, args.getOrNull(0), args.getOrNull(1))
            "give", "deposit", "add" -> walletGive(sender, args.getOrNull(0), args.getOrNull(1))
            "take", "withdraw", "remove" -> walletTake(sender, args.getOrNull(0), args.getOrNull(1))
            "reset" -> walletReset(sender, args.getOrNull(0))
            "help" -> showWalletHelp(sender)
            else -> {
                sender.sendMessage(Messages.error("Unknown wallet action: $action"))
                showWalletHelp(sender)
            }
        }
    }

    private suspend fun walletInfo(sender: CommandSender, playerName: String?) {
        if (playerName == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin wallet info <player>"))
            return
        }

        val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline) {
            sender.sendMessage(Messages.error("Player '$playerName' has never played on this server."))
            return
        }

        val uuid = offlinePlayer.uniqueId
        val accounts = accountService.getAccountsByPlayer(uuid)

        if (accounts.isEmpty()) {
            sender.sendMessage(Messages.error("Player '$playerName' has no accounts."))
            return
        }

        sender.sendMessage(Messages.header("Accounts for ${offlinePlayer.name}"))
        sender.sendMessage(Messages.infoLabeled("UUID", uuid.toString()))
        sender.sendMessage(Component.empty())

        for (account in accounts) {
            sender.sendMessage(Messages.accountTypeHeader(account.accountType.name))
            sender.sendMessage(Messages.adminAccountDetails(account))
            sender.sendMessage(Component.empty())
        }

        // Show wallet specifically
        val wallet = accountService.getWallet(uuid)
        if (wallet != null) {
            sender.sendMessage(Messages.walletInfo(wallet.accountNumber, wallet.balance))
        }
    }

    private suspend fun walletSet(sender: CommandSender, playerName: String?, amountArg: String?) {
        if (playerName == null || amountArg == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin wallet set <player> <amount>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount < 0) {
            sender.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline) {
            sender.sendMessage(Messages.error("Player '$playerName' has never played on this server."))
            return
        }

        val uuid = offlinePlayer.uniqueId
        val wallet = accountService.ensureWalletExists(uuid)

        val currentBalance = wallet.balance
        val diff = amount - currentBalance

        val success = if (diff > 0) {
            accountService.deposit(wallet.accountNumber, diff, uuid, "Admin set balance")
        } else if (diff < 0) {
            accountService.withdraw(wallet.accountNumber, -diff, uuid, "Admin set balance")
            true
        } else {
            true
        }

        if (success) {
            sender.sendMessage(Messages.success("Set ${offlinePlayer.name}'s wallet balance to ${Messages.formatCurrency(amount)}"))
            sender.sendMessage(Messages.infoLabeled("Previous balance", Messages.formatCurrency(currentBalance)))
        } else {
            sender.sendMessage(Messages.error("Failed to set wallet balance."))
        }
    }

    private suspend fun walletGive(sender: CommandSender, playerName: String?, amountArg: String?) {
        if (playerName == null || amountArg == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin wallet give <player> <amount>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage(Messages.error("Invalid amount: $amountArg (must be positive)"))
            return
        }

        val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline) {
            sender.sendMessage(Messages.error("Player '$playerName' has never played on this server."))
            return
        }

        val uuid = offlinePlayer.uniqueId
        val wallet = accountService.ensureWalletExists(uuid)

        val success = accountService.deposit(wallet.accountNumber, amount, uuid, "Admin deposit")

        if (success) {
            val newBalance = accountService.getBalance(wallet.accountNumber) ?: 0.0
            sender.sendMessage(Messages.success("Gave ${Messages.formatCurrency(amount)} to ${offlinePlayer.name}"))
            sender.sendMessage(Messages.infoLabeled("New balance", Messages.formatCurrency(newBalance)))
        } else {
            sender.sendMessage(Messages.error("Failed to deposit to wallet."))
        }
    }

    private suspend fun walletTake(sender: CommandSender, playerName: String?, amountArg: String?) {
        if (playerName == null || amountArg == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin wallet take <player> <amount>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage(Messages.error("Invalid amount: $amountArg (must be positive)"))
            return
        }

        val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline) {
            sender.sendMessage(Messages.error("Player '$playerName' has never played on this server."))
            return
        }

        val uuid = offlinePlayer.uniqueId
        val wallet = accountService.getWallet(uuid)

        if (wallet == null) {
            sender.sendMessage(Messages.error("Player '$playerName' has no wallet."))
            return
        }

        val result = accountService.withdraw(wallet.accountNumber, amount, uuid, "Admin withdrawal")

        val newBalance = accountService.getBalance(wallet.accountNumber) ?: 0.0
        when (result) {
            net.crewco.Banking.data.edata.WithdrawalResult.SUCCESS -> {
                sender.sendMessage(Messages.success("Took ${Messages.formatCurrency(amount)} from ${offlinePlayer.name}"))
                sender.sendMessage(Messages.infoLabeled("New balance", Messages.formatCurrency(newBalance)))
            }
            net.crewco.Banking.data.edata.WithdrawalResult.INSUFFICIENT_FUNDS -> {
                sender.sendMessage(Messages.error("Insufficient funds. Current balance: ${Messages.formatCurrency(wallet.balance)}"))
            }
            else -> {
                sender.sendMessage(Messages.error("Failed to withdraw from wallet: $result"))
            }
        }
    }

    private suspend fun walletReset(sender: CommandSender, playerName: String?) {
        if (playerName == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin wallet reset <player>"))
            return
        }

        val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline) {
            sender.sendMessage(Messages.error("Player '$playerName' has never played on this server."))
            return
        }

        val uuid = offlinePlayer.uniqueId
        val wallet = accountService.getWallet(uuid)

        if (wallet == null) {
            sender.sendMessage(Messages.error("Player '$playerName' has no wallet."))
            return
        }

        val currentBalance = wallet.balance

        if (currentBalance > 0) {
            accountService.withdraw(wallet.accountNumber, currentBalance, uuid, "Admin reset")
        }

        sender.sendMessage(Messages.success("Reset ${offlinePlayer.name}'s wallet to ${Messages.formatCurrency(0.0)}"))
        sender.sendMessage(Messages.infoLabeled("Previous balance", Messages.formatCurrency(currentBalance)))
    }

    private fun showWalletHelp(sender: CommandSender) {
        sender.sendMessage(Messages.header("Wallet Admin Commands"))
        sender.sendMessage(Messages.info("/bankadmin wallet info <player> - View player's accounts"))
        sender.sendMessage(Messages.info("/bankadmin wallet set <player> <amount> - Set wallet balance"))
        sender.sendMessage(Messages.info("/bankadmin wallet give <player> <amount> - Add to wallet"))
        sender.sendMessage(Messages.info("/bankadmin wallet take <player> <amount> - Remove from wallet"))
        sender.sendMessage(Messages.info("/bankadmin wallet reset <player> - Reset wallet to 0"))
    }

    // ==================== Account Commands ====================

    private suspend fun handleAccountCommands(sender: CommandSender, action: String, args: Array<String>) {
        when (action) {
            "freeze" -> freezeAccount(sender, args.getOrNull(0))
            "unfreeze" -> unfreezeAccount(sender, args.getOrNull(0))
            "info" -> accountInfo(sender, args.getOrNull(0))
            "setbalance" -> setBalance(sender, args.getOrNull(0), args.getOrNull(1))
            "deposit" -> accountDeposit(sender, args.getOrNull(0), args.getOrNull(1))
            "withdraw" -> accountWithdraw(sender, args.getOrNull(0), args.getOrNull(1))
            "help" -> showAccountHelp(sender)
            else -> {
                sender.sendMessage(Messages.error("Unknown account action: $action"))
                showAccountHelp(sender)
            }
        }
    }

    private suspend fun freezeAccount(sender: CommandSender, accountNumber: String?) {
        if (accountNumber == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin account freeze <account_number>"))
            return
        }

        val success = accountService.freezeAccount(accountNumber)

        if (success) {
            sender.sendMessage(Messages.success("Account $accountNumber has been frozen."))
        } else {
            sender.sendMessage(Messages.error("Failed to freeze account."))
        }
    }

    private suspend fun unfreezeAccount(sender: CommandSender, accountNumber: String?) {
        if (accountNumber == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin account unfreeze <account_number>"))
            return
        }

        val success = accountService.unfreezeAccount(accountNumber)

        if (success) {
            sender.sendMessage(Messages.success("Account $accountNumber has been unfrozen."))
        } else {
            sender.sendMessage(Messages.error("Failed to unfreeze account."))
        }
    }

    private suspend fun accountInfo(sender: CommandSender, identifier: String?) {
        if (identifier == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin account info <account_number|player>"))
            return
        }

        val accountByNumber = accountService.getAccount(identifier)
        if (accountByNumber != null) {
            sender.sendMessage(Messages.header("Account Information (Admin)"))
            sender.sendMessage(Messages.adminAccountDetails(accountByNumber))
            return
        }

        val offlinePlayer = Bukkit.getOfflinePlayer(identifier)
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline) {
            sender.sendMessage(Messages.error("Account or player '$identifier' not found."))
            return
        }

        walletInfo(sender, identifier)
    }

    private suspend fun setBalance(sender: CommandSender, accountNumber: String?, amountArg: String?) {
        if (accountNumber == null || amountArg == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin account setbalance <account_number> <amount>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount < 0) {
            sender.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val account = accountService.getAccount(accountNumber)

        if (account == null) {
            sender.sendMessage(Messages.error("Account not found."))
            return
        }

        val diff = amount - account.balance

        if (diff > 0) {
            accountService.deposit(accountNumber, diff, account.uuid, "Admin adjustment")
        } else if (diff < 0) {
            accountService.withdraw(accountNumber, -diff, account.uuid, "Admin adjustment")
        }

        sender.sendMessage(Messages.success("Balance set to ${Messages.formatCurrency(amount)}"))
    }

    private suspend fun accountDeposit(sender: CommandSender, accountNumber: String?, amountArg: String?) {
        if (accountNumber == null || amountArg == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin account deposit <account_number> <amount>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val account = accountService.getAccount(accountNumber)

        if (account == null) {
            sender.sendMessage(Messages.error("Account not found."))
            return
        }

        val success = accountService.deposit(accountNumber, amount, account.uuid, "Admin deposit")

        if (success) {
            val newBalance = accountService.getBalance(accountNumber) ?: 0.0
            sender.sendMessage(Messages.success("Deposited ${Messages.formatCurrency(amount)} to $accountNumber"))
            sender.sendMessage(Messages.infoLabeled("New balance", Messages.formatCurrency(newBalance)))
        } else {
            sender.sendMessage(Messages.error("Failed to deposit."))
        }
    }

    private suspend fun accountWithdraw(sender: CommandSender, accountNumber: String?, amountArg: String?) {
        if (accountNumber == null || amountArg == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin account withdraw <account_number> <amount>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val account = accountService.getAccount(accountNumber)

        if (account == null) {
            sender.sendMessage(Messages.error("Account not found."))
            return
        }

        val result = accountService.withdraw(accountNumber, amount, account.uuid, "Admin withdrawal")

        when (result) {
            net.crewco.Banking.data.edata.WithdrawalResult.SUCCESS -> {
                val newBalance = accountService.getBalance(accountNumber) ?: 0.0
                sender.sendMessage(Messages.success("Withdrew ${Messages.formatCurrency(amount)} from $accountNumber"))
                sender.sendMessage(Messages.infoLabeled("New balance", Messages.formatCurrency(newBalance)))
            }
            net.crewco.Banking.data.edata.WithdrawalResult.INSUFFICIENT_FUNDS -> {
                sender.sendMessage(Messages.error("Insufficient funds. Current balance: ${Messages.formatCurrency(account.balance)}"))
            }
            else -> {
                sender.sendMessage(Messages.error("Failed to withdraw: $result"))
            }
        }
    }

    private fun showAccountHelp(sender: CommandSender) {
        sender.sendMessage(Messages.header("Account Admin Commands"))
        sender.sendMessage(Messages.info("/bankadmin account freeze <account> - Freeze account"))
        sender.sendMessage(Messages.info("/bankadmin account unfreeze <account> - Unfreeze account"))
        sender.sendMessage(Messages.info("/bankadmin account info <account|player> - View account/player info"))
        sender.sendMessage(Messages.info("/bankadmin account setbalance <account> <amount> - Set balance"))
        sender.sendMessage(Messages.info("/bankadmin account deposit <account> <amount> - Deposit to account"))
        sender.sendMessage(Messages.info("/bankadmin account withdraw <account> <amount> - Withdraw from account"))
    }

    // ==================== Loan Commands ====================

    private suspend fun handleLoanCommands(sender: CommandSender, action: String, args: Array<String>) {
        when (action) {
            "pending" -> pendingLoans(sender)
            "approve" -> approveLoan(sender, args.getOrNull(0))
            "reject" -> rejectLoan(sender, args.getOrNull(0), args.drop(1).joinToString(" "))
            "disburse" -> disburseLoan(sender, args.getOrNull(0))
            "overdue" -> overdueLoans(sender)
            "help" -> showLoanHelp(sender)
            else -> {
                sender.sendMessage(Messages.error("Unknown loan action: $action"))
                showLoanHelp(sender)
            }
        }
    }

    private suspend fun pendingLoans(sender: CommandSender) {
        val loans = loanService.getPendingLoans()

        if (loans.isEmpty()) {
            sender.sendMessage(Messages.info("No pending loan applications."))
            return
        }

        sender.sendMessage(Messages.header("Pending Loan Applications"))

        for (loan in loans) {
            sender.sendMessage(Messages.adminLoanLine(loan))
        }
    }

    private suspend fun approveLoan(sender: CommandSender, loanId: String?) {
        if (loanId == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin loan approve <loan_id>"))
            return
        }

        val success = loanService.approveLoan(loanId)

        if (success) {
            sender.sendMessage(Messages.success("Loan $loanId approved."))
            sender.sendMessage(Messages.info("Use /bankadmin loan disburse $loanId to disburse funds."))
        } else {
            sender.sendMessage(Messages.error("Failed to approve loan."))
        }
    }

    private suspend fun rejectLoan(sender: CommandSender, loanId: String?, reason: String) {
        if (loanId == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin loan reject <loan_id> <reason>"))
            return
        }

        val rejectReason = reason.ifBlank { "No reason provided" }
        val success = loanService.rejectLoan(loanId, rejectReason)

        if (success) {
            sender.sendMessage(Messages.success("Loan $loanId rejected."))
        } else {
            sender.sendMessage(Messages.error("Failed to reject loan."))
        }
    }

    private suspend fun disburseLoan(sender: CommandSender, loanId: String?) {
        if (loanId == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin loan disburse <loan_id>"))
            return
        }

        val success = loanService.disburseLoan(loanId)

        if (success) {
            sender.sendMessage(Messages.success("Loan $loanId disbursed."))
        } else {
            sender.sendMessage(Messages.error("Failed to disburse loan. Is it approved?"))
        }
    }

    private suspend fun overdueLoans(sender: CommandSender) {
        val loans = loanService.getOverdueLoans()

        if (loans.isEmpty()) {
            sender.sendMessage(Messages.info("No overdue loans."))
            return
        }

        sender.sendMessage(Messages.header("Overdue Loans"))

        for (loan in loans) {
            sender.sendMessage(Messages.adminLoanLine(loan))
        }
    }

    private fun showLoanHelp(sender: CommandSender) {
        sender.sendMessage(Messages.header("Loan Admin Commands"))
        sender.sendMessage(Messages.info("/bankadmin loan pending - View pending applications"))
        sender.sendMessage(Messages.info("/bankadmin loan approve <loan_id> - Approve loan"))
        sender.sendMessage(Messages.info("/bankadmin loan reject <loan_id> <reason> - Reject loan"))
        sender.sendMessage(Messages.info("/bankadmin loan disburse <loan_id> - Disburse funds"))
        sender.sendMessage(Messages.info("/bankadmin loan overdue - View overdue loans"))
    }

    // ==================== ATM Commands ====================

    private suspend fun handleAtmCommands(sender: CommandSender, action: String, args: Array<String>) {
        when (action) {
            "create" -> createATM(sender)
            "remove" -> removeATM(sender, args.getOrNull(0))
            "refill" -> refillATM(sender, args.getOrNull(0), args.getOrNull(1))
            "list" -> listATMs(sender)
            "toggle" -> toggleATM(sender, args.getOrNull(0))
            "help" -> showAtmHelp(sender)
            else -> {
                sender.sendMessage(Messages.error("Unknown ATM action: $action"))
                showAtmHelp(sender)
            }
        }
    }

    private suspend fun createATM(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Messages.error("This command can only be used by players."))
            return
        }

        val atm = atmService.createATM(sender.location, sender.uniqueId)

        if (atm != null) {
            sender.sendMessage(Messages.success("ATM created! ID: ${atm.atmId}"))
        } else {
            sender.sendMessage(Messages.error("Failed to create ATM. One may already exist here."))
        }
    }

    private suspend fun removeATM(sender: CommandSender, atmId: String?) {
        if (atmId == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin atm remove <atm_id>"))
            return
        }

        val success = atmService.removeATM(atmId)

        if (success) {
            sender.sendMessage(Messages.success("ATM removed."))
        } else {
            sender.sendMessage(Messages.error("Failed to remove ATM."))
        }
    }

    private suspend fun refillATM(sender: CommandSender, atmId: String?, amountArg: String?) {
        if (atmId == null || amountArg == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin atm refill <atm_id> <amount>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val success = atmService.refillATM(atmId, amount)

        if (success) {
            sender.sendMessage(Messages.success("ATM refilled with ${Messages.formatCurrency(amount)}"))
        } else {
            sender.sendMessage(Messages.error("Failed to refill ATM."))
        }
    }

    private suspend fun listATMs(sender: CommandSender) {
        val atms = atmService.getAllActiveATMs()

        if (atms.isEmpty()) {
            sender.sendMessage(Messages.info("No ATMs found."))
            return
        }

        sender.sendMessage(Messages.header("All ATMs"))

        for (atm in atms) {
            sender.sendMessage(Messages.adminAtmLine(atm))
        }
    }

    private suspend fun toggleATM(sender: CommandSender, atmId: String?) {
        if (atmId == null) {
            sender.sendMessage(Messages.error("Usage: /bankadmin atm toggle <atm_id>"))
            return
        }

        val atm = atmService.getATM(atmId)

        if (atm == null) {
            sender.sendMessage(Messages.error("ATM not found."))
            return
        }

        val newStatus = !atm.active
        val success = atmService.setATMActive(atmId, newStatus)

        if (success) {
            val status = if (newStatus) "activated" else "deactivated"
            sender.sendMessage(Messages.success("ATM $status."))
        } else {
            sender.sendMessage(Messages.error("Failed to toggle ATM."))
        }
    }

    private fun showAtmHelp(sender: CommandSender) {
        sender.sendMessage(Messages.header("ATM Admin Commands"))
        sender.sendMessage(Messages.info("/bankadmin atm create - Create ATM at your location"))
        sender.sendMessage(Messages.info("/bankadmin atm remove <atm_id> - Remove ATM"))
        sender.sendMessage(Messages.info("/bankadmin atm refill <atm_id> <amount> - Refill ATM"))
        sender.sendMessage(Messages.info("/bankadmin atm list - List all ATMs"))
        sender.sendMessage(Messages.info("/bankadmin atm toggle <atm_id> - Toggle active status"))
    }

    // ==================== Main Help ====================

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Messages.header("Bank Admin Commands"))
        sender.sendMessage(Messages.info("/bankadmin account - Account management (by account number)"))
        sender.sendMessage(Messages.info("/bankadmin wallet - Wallet management (by player name)"))
        sender.sendMessage(Messages.info("/bankadmin loan - Loan management"))
        sender.sendMessage(Messages.info("/bankadmin atm - ATM management"))
        sender.sendMessage(Component.empty())
        sender.sendMessage(Messages.info("Use /bankadmin <category> help for more details"))
    }
}