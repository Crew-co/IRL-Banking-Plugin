// src/main/java/net/crewco/Banking/listeners/ATMInteractListener.kt
package net.crewco.Banking.listeners

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.atmGUI
import net.crewco.Banking.Startup.Companion.atmService
import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.Startup.Companion.cardService
import net.crewco.Banking.Startup.Companion.plugin
import net.crewco.Banking.gui.ATMGUI
import net.crewco.Banking.services.ATMService
import net.crewco.Banking.services.CardService
import net.crewco.Banking.util.ATMStructure
import net.crewco.Banking.util.BankCardItem
import net.crewco.Banking.util.Messages
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent

class ATMInteractListener: Listener {

    /**
     * Handle ATM sign creation
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onSignChange(event: SignChangeEvent) {
        val line0 = event.line(0) ?: return
        val plainText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line0)

        if (!plainText.equals("[ATM]", ignoreCase = true)) return

        val player = event.player

        // Check permission
        if (!player.hasPermission("banking.admin.atm")) {
            player.sendMessage(Messages.error("You don't have permission to create ATMs!"))
            event.isCancelled = true
            return
        }

        // Check if attached to valid block
        val attachedBlock = ATMStructure.getAttachedBlock(event.block)
        if (attachedBlock == null) {
            player.sendMessage(Messages.error("ATM sign must be placed on a valid block!"))
            player.sendMessage(Messages.info("Valid blocks: Iron Block, Quartz, Polished Stone variants"))
            event.isCancelled = true
            return
        }

        // Create ATM in database
        plugin.launch {
            val atm = atmService.createATM(attachedBlock.location, player.uniqueId)

            if (atm != null) {
                // Format the sign
                plugin.server.scheduler.runTask(plugin, Runnable {
                    val sign = event.block.state as? Sign
                    if (sign != null) {
                        ATMStructure.formatATMSign(sign, atm.atmId)
                    }
                })

                player.sendMessage(Messages.success("ATM created successfully!"))
                player.sendMessage(Messages.info("ATM ID: ${atm.atmId}"))
            } else {
                player.sendMessage(Messages.error("Failed to create ATM. One may already exist here."))
            }
        }
    }

    /**
     * Handle ATM interaction with bank card
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val clickedBlock = event.clickedBlock ?: return
        val player = event.player

        // Check if clicked block is an ATM sign or base block
        val isATMSign = ATMStructure.isATMSign(clickedBlock)
        val isATMBase = ATMStructure.isATMBaseBlock(clickedBlock)

        if (!isATMSign && !isATMBase) return

        event.isCancelled = true

        // Get the base block location for ATM lookup
        val atmLocation = if (isATMSign) {
            ATMStructure.getATMBaseLocation(clickedBlock)
        } else {
            clickedBlock.location
        }

        if (atmLocation == null) {
            player.sendMessage(Messages.error("Invalid ATM structure!"))
            return
        }

        // Check if player is holding a bank card
        val heldItem = player.inventory.itemInMainHand

        if (!BankCardItem.isBankCard(heldItem)) {
            player.sendMessage(Messages.error("You need to hold a bank card to use the ATM!"))
            player.sendMessage(Messages.info("Get a card with /card apply <type> <account> <pin>"))
            return
        }

        // Verify card ownership
        val cardOwnerUuid = BankCardItem.getOwnerUuid(heldItem)
        if (cardOwnerUuid != player.uniqueId.toString()) {
            player.sendMessage(Messages.error("This card doesn't belong to you!"))
            return
        }

        val cardNumber = BankCardItem.getCardNumber(heldItem)
        if (cardNumber == null) {
            player.sendMessage(Messages.error("Invalid card!"))
            return
        }

        // Get ATM and card from database
        plugin.launch {
            val atm = atmService.getATMAtLocation(atmLocation)

            if (atm == null) {
                player.sendMessage(Messages.error("This ATM is not registered in the system."))
                player.sendMessage(Messages.info("An admin needs to recreate the ATM sign."))
                return@launch
            }

            if (!atm.active) {
                player.sendMessage(Messages.error("This ATM is currently offline."))
                return@launch
            }

            val card = cardService.getCard(cardNumber)

            if (card == null) {
                player.sendMessage(Messages.error("Card not found in system. It may have been cancelled."))
                return@launch
            }

            if (card.frozen) {
                player.sendMessage(Messages.error("Your card is frozen!"))
                return@launch
            }

            if (!card.active) {
                player.sendMessage(Messages.error("Your card has been cancelled!"))
                return@launch
            }

            if (card.isExpired()) {
                player.sendMessage(Messages.error("Your card has expired!"))
                return@launch
            }

            // Open ATM GUI with card info
            atmGUI.open(player, atm, card)
        }
    }

    /**
     * Prevent breaking ATM blocks without permission
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player

        // Check if breaking ATM sign
        if (ATMStructure.isATMSign(block)) {
            if (!player.hasPermission("banking.admin.atm")) {
                player.sendMessage(Messages.error("You can't break ATM signs!"))
                event.isCancelled = true
                return
            }

            // Remove ATM from database
            val baseLocation = ATMStructure.getATMBaseLocation(block)
            if (baseLocation != null) {
                plugin.launch {
                    val atm = atmService.getATMAtLocation(baseLocation)
                    if (atm != null) {
                        atmService.removeATM(atm.atmId)
                        player.sendMessage(Messages.info("ATM removed from system."))
                    }
                }
            }
            return
        }

        // Check if breaking ATM base block
        if (ATMStructure.isATMBaseBlock(block)) {
            if (!player.hasPermission("banking.admin.atm")) {
                player.sendMessage(Messages.error("You can't break ATM blocks!"))
                event.isCancelled = true
                return
            }

            // Remove ATM from database
            plugin.launch {
                val atm = atmService.getATMAtLocation(block.location)
                if (atm != null) {
                    atmService.removeATM(atm.atmId)
                    player.sendMessage(Messages.info("ATM removed from system."))
                }
            }
        }
    }

    /**
     * Handle custom amount input for player payments
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val pending = ATMGUI.pendingPlayerPayments[player.uniqueId] ?: return

        event.isCancelled = true
        ATMGUI.pendingPlayerPayments.remove(player.uniqueId)

        val message = event.message.trim().lowercase()

        if (message == "cancel") {
            player.sendMessage(Messages.info("Payment cancelled."))
            return
        }

        val amount = message.replace(",", "").replace("$", "").toDoubleOrNull()

        if (amount == null || amount <= 0) {
            player.sendMessage(Messages.error("Invalid amount! Payment cancelled."))
            return
        }

        // Process the payment
        plugin.launch {
            val result = accountService.transfer(
                pending.sourceAccountNumber,
                pending.targetAccountNumber,
                amount,
                player.uniqueId,
                "ATM Transfer to ${pending.targetName}"
            )

            when (result) {
                net.crewco.Banking.services.sdata.TransferResult.SUCCESS -> {
                    player.sendMessage(Messages.success("Sent ${Messages.formatCurrency(amount)} to ${pending.targetName}!"))
                    val newBalance = accountService.getBalance(pending.sourceAccountNumber) ?: 0.0
                    player.sendMessage(Messages.info("New balance: ${Messages.formatCurrency(newBalance)}"))
                    
                    // Notify target player if online
                    val targetPlayer = org.bukkit.Bukkit.getPlayer(pending.targetUuid)
                    if (targetPlayer != null && targetPlayer.isOnline) {
                        targetPlayer.sendMessage(Messages.success("${player.name} sent you ${Messages.formatCurrency(amount)}!"))
                    }
                }
                net.crewco.Banking.services.sdata.TransferResult.INSUFFICIENT_FUNDS -> {
                    player.sendMessage(Messages.error("Insufficient funds!"))
                }
                net.crewco.Banking.services.sdata.TransferResult.DAILY_LIMIT_EXCEEDED -> {
                    player.sendMessage(Messages.error("Daily limit exceeded!"))
                }
                else -> {
                    player.sendMessage(Messages.error("Transfer failed: ${result.name}"))
                }
            }
        }
    }
}