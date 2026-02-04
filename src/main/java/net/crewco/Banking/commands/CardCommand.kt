// src/main/java/net/crewco/Banking/commands/CardCommand.kt
package net.crewco.Banking.commands

import com.google.inject.Inject
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.Startup.Companion.cardService
import net.crewco.Banking.data.cdata.CardType
import net.crewco.Banking.data.cdata.CardUseResult
import net.crewco.Banking.services.AccountService
import net.crewco.Banking.services.CardService
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.*
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import java.util.stream.Stream
import net.crewco.Banking.util.Messages

class CardCommand  {

    // ==================== Main Command ====================

    @Command("card [action] [args]")
    @CommandDescription("Card commands - use /card help for more info")
    @Permission("banking.card.use")
    suspend fun card(
        player: Player,
        @Argument("action", suggestions = "cardActions") action: String?,
        @Argument("args", suggestions = "cardArgs") args: Array<String>?
    ) {
        val subcommand = action?.lowercase() ?: "list"
        val arguments = args ?: emptyArray()

        when (subcommand) {
            "list", "cards" -> listCards(player)
            "apply", "new" -> applyCard(player, arguments.getOrNull(0), arguments.getOrNull(1), arguments.getOrNull(2))
            "info", "details" -> cardInfo(player, arguments.getOrNull(0))
            "freeze" -> freezeCard(player, arguments.getOrNull(0))
            "unfreeze" -> unfreezeCard(player, arguments.getOrNull(0))
            "cancel" -> cancelCard(player, arguments.getOrNull(0))
            "changepin", "pin" -> changePin(player, arguments.getOrNull(0), arguments.getOrNull(1), arguments.getOrNull(2))
            "pay", "purchase" -> pay(player, arguments.getOrNull(0), arguments.getOrNull(1), arguments.getOrNull(2), arguments.drop(3).joinToString(" "))
            "help" -> showHelp(player)
            else -> {
                player.sendMessage(Messages.error("Unknown subcommand: $subcommand"))
                showHelp(player)
            }
        }
    }

    // ==================== Suggestions ====================

    @Suggestions("cardActions")
    fun actionSuggestions(
        context: CommandContext<Player>,
        input: String
    ): Stream<String> {
        val actions = mutableListOf(
            "list",
            "apply",
            "info",
            "freeze",
            "unfreeze",
            "cancel",
            "changepin",
            "pay",
            "help"
        )
        return actions.stream().filter { it.startsWith(input.lowercase()) }
    }

    @Suggestions("cardArgs")
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
            "apply", "new" -> {
                CardType.entries.forEach { type ->
                    suggestions.add(type.name.lowercase())
                }
            }
            "info", "details", "freeze", "unfreeze", "cancel", "changepin", "pin", "pay", "purchase" -> {
                suggestions.add("<card_number>")
            }
        }

        return suggestions.stream().filter { it.startsWith(input.lowercase()) }
    }

    // ==================== Subcommand Handlers ====================

    private suspend fun listCards(player: Player) {
        val cards = cardService.getCardsByPlayer(player.uniqueId)

        if (cards.isEmpty()) {
            player.sendMessage(Messages.info("You don't have any cards. Use /card apply to get one!"))
            return
        }

        player.sendMessage(Messages.header("Your Cards"))

        for (card in cards) {
            player.sendMessage(Messages.cardLine(card))
        }
    }

    private suspend fun applyCard(player: Player, cardTypeArg: String?, accountNumber: String?, pin: String?) {
        if (cardTypeArg == null || accountNumber == null || pin == null) {
            player.sendMessage(Messages.error("Usage: /card apply <card_type> <account_number> <pin>"))
            player.sendMessage(Messages.info("Card types: ${CardType.entries.joinToString(", ") { it.name.lowercase() }}"))
            return
        }

        // Validate PIN
        if (!pin.matches(kotlin.text.Regex("^\\d{4}$"))) {
            player.sendMessage(Messages.error("PIN must be exactly 4 digits!"))
            return
        }

        val cardType = try {
            CardType.valueOf(cardTypeArg.uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendMessage(Messages.error("Invalid card type: $cardTypeArg"))
            player.sendMessage(Messages.info("Card types: ${CardType.entries.joinToString(", ") { it.name.lowercase() }}"))
            return
        }

        val account = accountService.getAccount(accountNumber)
        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.error("Account not found or doesn't belong to you!"))
            return
        }

        val card = cardService.issueCard(player.uniqueId, accountNumber, cardType, pin)

        if (card != null) {
            player.sendMessage(Messages.success("Card issued successfully!"))
            player.sendMessage(Messages.cardDetails(card))
            player.sendMessage(Messages.warning("Keep your card number and CVV secure!"))
        } else {
            player.sendMessage(Messages.error("Failed to issue card. Check account type compatibility."))
        }
    }

    private suspend fun cardInfo(player: Player, cardNumber: String?) {
        if (cardNumber == null) {
            player.sendMessage(Messages.error("Usage: /card info <card_number>"))
            return
        }

        val card = cardService.getCard(cardNumber)

        if (card == null || card.ownerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Card not found or doesn't belong to you!"))
            return
        }

        player.sendMessage(Messages.header("Card Information"))
        player.sendMessage(Messages.cardDetails(card))
    }

    private suspend fun freezeCard(player: Player, cardNumber: String?) {
        if (cardNumber == null) {
            player.sendMessage(Messages.error("Usage: /card freeze <card_number>"))
            return
        }

        val card = cardService.getCard(cardNumber)

        if (card == null || card.ownerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Card not found or doesn't belong to you!"))
            return
        }

        val success = cardService.freezeCard(cardNumber)

        if (success) {
            player.sendMessage(Messages.success("Card has been frozen."))
        } else {
            player.sendMessage(Messages.error("Failed to freeze card."))
        }
    }

    private suspend fun unfreezeCard(player: Player, cardNumber: String?) {
        if (cardNumber == null) {
            player.sendMessage(Messages.error("Usage: /card unfreeze <card_number>"))
            return
        }

        val card = cardService.getCard(cardNumber)

        if (card == null || card.ownerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Card not found or doesn't belong to you!"))
            return
        }

        val success = cardService.unfreezeCard(cardNumber)

        if (success) {
            player.sendMessage(Messages.success("Card has been unfrozen."))
        } else {
            player.sendMessage(Messages.error("Failed to unfreeze card."))
        }
    }

    private suspend fun cancelCard(player: Player, cardNumber: String?) {
        if (cardNumber == null) {
            player.sendMessage(Messages.error("Usage: /card cancel <card_number>"))
            return
        }

        val card = cardService.getCard(cardNumber)

        if (card == null || card.ownerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Card not found or doesn't belong to you!"))
            return
        }

        val success = cardService.cancelCard(cardNumber)

        if (success) {
            player.sendMessage(Messages.success("Card has been cancelled permanently."))
        } else {
            player.sendMessage(Messages.error("Failed to cancel card."))
        }
    }

    private suspend fun changePin(player: Player, cardNumber: String?, oldPin: String?, newPin: String?) {
        if (cardNumber == null || oldPin == null || newPin == null) {
            player.sendMessage(Messages.error("Usage: /card changepin <card_number> <old_pin> <new_pin>"))
            return
        }

        val card = cardService.getCard(cardNumber)

        if (card == null || card.ownerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Card not found or doesn't belong to you!"))
            return
        }

        if (!newPin.matches(kotlin.text.Regex("^\\d{4}$"))) {
            player.sendMessage(Messages.error("PIN must be exactly 4 digits!"))
            return
        }

        val success = cardService.changePin(cardNumber, oldPin, newPin)

        if (success) {
            player.sendMessage(Messages.success("PIN changed successfully."))
        } else {
            player.sendMessage(Messages.error("Failed to change PIN. Check your old PIN."))
        }
    }

    private suspend fun pay(player: Player, cardNumber: String?, amountArg: String?, pin: String?, merchant: String?) {
        if (cardNumber == null || amountArg == null || pin == null) {
            player.sendMessage(Messages.error("Usage: /card pay <card_number> <amount> <pin> <merchant>"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            player.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val card = cardService.getCard(cardNumber)

        if (card == null || card.ownerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Card not found or doesn't belong to you!"))
            return
        }

        val merchantName = if (merchant.isNullOrBlank()) "Unknown Merchant" else merchant

        val result = cardService.useCard(cardNumber, pin, amount, merchantName)

        when (result) {
            CardUseResult.SUCCESS -> {
                player.sendMessage(Messages.success("Payment of ${Messages.formatCurrency(amount)} to $merchantName successful!"))
            }
            CardUseResult.INVALID_PIN -> {
                player.sendMessage(Messages.error("Invalid PIN!"))
            }
            CardUseResult.INSUFFICIENT_FUNDS -> {
                player.sendMessage(Messages.error("Insufficient funds!"))
            }
            CardUseResult.DAILY_LIMIT_EXCEEDED -> {
                player.sendMessage(Messages.error("Daily spending limit exceeded!"))
            }
            CardUseResult.CARD_FROZEN -> {
                player.sendMessage(Messages.error("Card is frozen!"))
            }
            CardUseResult.CARD_EXPIRED -> {
                player.sendMessage(Messages.error("Card has expired!"))
            }
            CardUseResult.CARD_INACTIVE -> {
                player.sendMessage(Messages.error("Card is inactive!"))
            }
        }
    }

    private fun showHelp(player: Player) {
        player.sendMessage(Messages.header("Card Commands"))
        player.sendMessage(Messages.info("/card - List all your cards"))
        player.sendMessage(Messages.info("/card apply <type> <account> <pin> - Apply for a new card"))
        player.sendMessage(Messages.info("/card info <card_number> - View card details"))
        player.sendMessage(Messages.info("/card freeze <card_number> - Freeze a card"))
        player.sendMessage(Messages.info("/card unfreeze <card_number> - Unfreeze a card"))
        player.sendMessage(Messages.info("/card cancel <card_number> - Cancel a card"))
        player.sendMessage(Messages.info("/card changepin <card> <old> <new> - Change PIN"))
        player.sendMessage(Messages.info("/card pay <card> <amount> <pin> <merchant> - Make payment"))
        player.sendMessage(Messages.info(""))
        player.sendMessage(Messages.info("Card types: ${CardType.entries.joinToString(", ") { it.name.lowercase() }}"))
    }
}