// src/main/java/net/crewco/Banking/commands/ATMCommand.kt
package net.crewco.Banking.commands

import com.google.inject.Inject
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.Startup.Companion.atmService
import net.crewco.Banking.services.ATMService
import net.crewco.Banking.services.AccountService
import net.crewco.Banking.util.Messages
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.*
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import java.util.stream.Stream

class ATMCommand{

    // ==================== Main Command ====================

    @Command("atm [action] [args]")
    @CommandDescription("ATM commands - use /atm help for more info")
    @Permission("banking.atm.use")
    suspend fun atm(
        player: Player,
        @Argument("action", suggestions = "atmActions") action: String?,
        @Argument("args", suggestions = "atmArgs") args: Array<String>?
    ) {
        val subcommand = action?.lowercase() ?: "find"
        val arguments = args ?: emptyArray()

        when (subcommand) {
            "find", "nearby", "locate" -> findATM(player)
            "withdraw", "w" -> withdraw(player, arguments.getOrNull(0), arguments.getOrNull(1))
            "deposit", "d" -> deposit(player, arguments.getOrNull(0), arguments.getOrNull(1))
            "balance", "bal" -> checkBalance(player, arguments.getOrNull(0))
            "help" -> showHelp(player)
            else -> {
                player.sendMessage(Messages.error("Unknown subcommand: $subcommand"))
                showHelp(player)
            }
        }
    }

    // ==================== Suggestions ====================

    @Suggestions("atmActions")
    fun actionSuggestions(
        context: CommandContext<Player>,
        input: String
    ): Stream<String> {
        val actions = mutableListOf(
            "find",
            "withdraw",
            "deposit",
            "balance",
            "help"
        )
        return actions.stream().filter { it.startsWith(input.lowercase()) }
    }

    @Suggestions("atmArgs")
    fun argsSuggestions(
        context: CommandContext<Player>,
        input: String
    ): Stream<String> {
        val suggestions = mutableListOf<String>()

        val action = try {
            context.get<String>("action")?.lowercase()
        } catch (e: Exception) {
            null
        }

        when (action) {
            "withdraw", "w", "deposit", "d" -> {
                suggestions.addAll(listOf("100", "500", "1000", "5000"))
            }
            "balance", "bal" -> {
                suggestions.add("<account_number>")
            }
        }

        return suggestions.stream().filter { it.startsWith(input.lowercase()) }
    }

    // ==================== Subcommand Handlers ====================

    private suspend fun findATM(player: Player) {
        val nearbyATMs = atmService.getNearbyATMs(player.location, 100.0)

        if (nearbyATMs.isEmpty()) {
            player.sendMessage(Messages.info("No ATMs found nearby."))
            return
        }

        player.sendMessage(Messages.header("Nearby ATMs"))

        for (atm in nearbyATMs) {
            val distance = player.location.distance(atm.location)
            player.sendMessage(Messages.atmLine(atm, distance))
        }
    }

    private suspend fun withdraw(player: Player, amountArg: String?, accountNumber: String?) {
        if (amountArg == null || accountNumber == null) {
            player.sendMessage(Messages.error("Usage: /atm withdraw <amount> <account_number>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            player.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        // Find nearest ATM
        val nearbyATMs = atmService.getNearbyATMs(player.location, 5.0)
        val atm = nearbyATMs.firstOrNull()

        if (atm == null) {
            player.sendMessage(Messages.error("You must be near an ATM to withdraw!"))
            return
        }

        val account = accountService.getAccount(accountNumber)
        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.error("Account not found or doesn't belong to you!"))
            return
        }

        val result = atmService.withdraw(atm.atmId, accountNumber, amount, player.uniqueId)

        if (result.success) {
            player.sendMessage(Messages.success("Withdrew ${Messages.formatCurrency(result.amount)}"))
            if (result.fee > 0) {
                player.sendMessage(Messages.info("ATM fee: ${Messages.formatCurrency(result.fee)}"))
            }
        } else {
            player.sendMessage(Messages.error(result.message))
        }
    }

    private suspend fun deposit(player: Player, amountArg: String?, accountNumber: String?) {
        if (amountArg == null || accountNumber == null) {
            player.sendMessage(Messages.error("Usage: /atm deposit <amount> <account_number>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            player.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        // Find nearest ATM
        val nearbyATMs = atmService.getNearbyATMs(player.location, 5.0)
        val atm = nearbyATMs.firstOrNull()

        if (atm == null) {
            player.sendMessage(Messages.error("You must be near an ATM to deposit!"))
            return
        }

        val account = accountService.getAccount(accountNumber)
        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.error("Account not found or doesn't belong to you!"))
            return
        }

        val result = atmService.deposit(atm.atmId, accountNumber, amount, player.uniqueId)

        if (result.success) {
            player.sendMessage(Messages.success("Deposited ${Messages.formatCurrency(result.amount)}"))
        } else {
            player.sendMessage(Messages.error(result.message))
        }
    }

    private suspend fun checkBalance(player: Player, accountNumber: String?) {
        if (accountNumber == null) {
            player.sendMessage(Messages.error("Usage: /atm balance <account_number>"))
            return
        }

        // Find nearest ATM
        val nearbyATMs = atmService.getNearbyATMs(player.location, 5.0)

        if (nearbyATMs.isEmpty()) {
            player.sendMessage(Messages.error("You must be near an ATM!"))
            return
        }

        val balance = atmService.checkBalance(accountNumber, player.uniqueId)

        if (balance != null) {
            player.sendMessage(Messages.info("Account Balance: ${Messages.formatCurrency(balance)}"))
        } else {
            player.sendMessage(Messages.error("Account not found or doesn't belong to you!"))
        }
    }

    private fun showHelp(player: Player) {
        player.sendMessage(Messages.header("ATM Commands"))
        player.sendMessage(Messages.info("/atm - Find nearby ATMs"))
        player.sendMessage(Messages.info("/atm withdraw <amount> <account> - Withdraw cash"))
        player.sendMessage(Messages.info("/atm deposit <amount> <account> - Deposit cash"))
        player.sendMessage(Messages.info("/atm balance <account> - Check balance"))
        player.sendMessage(Messages.info(""))
        player.sendMessage(Messages.warning("You must be within 5 blocks of an ATM to use it!"))
    }
}