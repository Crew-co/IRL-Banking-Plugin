// src/main/java/net/crewco/Banking/gui/CardGUI.kt
package net.crewco.Banking.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.cardService
import net.crewco.Banking.Startup.Companion.plugin
import net.crewco.Banking.data.cdata.CardType
import net.crewco.Banking.data.models.Card
import net.crewco.Banking.util.BankCardItem
import net.crewco.Banking.util.Messages
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class CardGUI(private val plugin: Startup) {

    fun openMainMenu(player: Player) {
        plugin.launch {
            val cards = cardService.getCardsByPlayer(player.uniqueId)
            showCardsMenu(player, cards)
        }
    }

    private fun showCardsMenu(player: Player, cards: List<Card>) {
        val gui = ChestGui(6, "§1§lCrewCo Bank §8- §fYour Cards")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
        val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        // Header
        val headerPane = StaticPane(0, 0, 9, 1)
        headerPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§6§l💳 Your Cards",
            listOf(
                "",
                "§7Total Cards: §f${cards.size}",
                "§7Active: §a${cards.count { it.active && !it.frozen }}"
            )
        )), 4, 0)
        gui.addPane(headerPane)

        if (cards.isNotEmpty()) {
            val paginatedPane = PaginatedPane(1, 1, 7, 4)

            val cardItems = cards.map { card ->
                val material = when {
                    !card.active -> Material.GRAY_DYE
                    card.frozen -> Material.ICE
                    card.isExpired() -> Material.RED_DYE
                    card.cardType.name.contains("CREDIT") -> Material.GOLD_INGOT
                    card.cardType.name.contains("PREMIUM") -> Material.DIAMOND
                    else -> Material.PAPER
                }

                val statusColor = when {
                    !card.active -> "§8"
                    card.frozen -> "§b"
                    card.isExpired() -> "§c"
                    else -> "§a"
                }

                val status = when {
                    !card.active -> "Cancelled"
                    card.frozen -> "Frozen"
                    card.isExpired() -> "Expired"
                    else -> "Active"
                }

                GuiItem(createItem(
                    material,
                    "§f${card.cardType.displayName}",
                    listOf(
                        "",
                        "§7Card: §e${card.getMaskedNumber()}",
                        "§7Expires: §f${card.expirationDate}",
                        "§7Linked Account: §f${card.linkedAccountNumber}",
                        "",
                        "§7Daily Limit: §e${Messages.formatCurrency(card.dailyLimit)}",
                        "§7Spent Today: §c${Messages.formatCurrency(card.spentToday)}",
                        "§7Remaining: §a${Messages.formatCurrency(card.dailyLimit - card.spentToday)}",
                        "",
                        "§7Status: $statusColor$status",
                        "",
                        "§eClick for options"
                    )
                )) {
                    showCardOptions(player, card)
                }
            }

            paginatedPane.populateWithGuiItems(cardItems)
            gui.addPane(paginatedPane)

            // Navigation
            val navPane = StaticPane(0, 5, 9, 1)
            navPane.addItem(GuiItem(createItem(Material.ARROW, "§7Previous", emptyList())) {
                if (paginatedPane.page > 0) {
                    paginatedPane.page = paginatedPane.page - 1
                    gui.update()
                }
            }, 0, 0)

            navPane.addItem(GuiItem(createItem(Material.ARROW, "§7Next", emptyList())) {
                if (paginatedPane.page < paginatedPane.pages - 1) {
                    paginatedPane.page = paginatedPane.page + 1
                    gui.update()
                }
            }, 8, 0)
            gui.addPane(navPane)
        } else {
            val emptyPane = StaticPane(0, 2, 9, 2)
            emptyPane.addItem(GuiItem(createItem(
                Material.BARRIER,
                "§cNo Cards",
                listOf(
                    "",
                    "§7You don't have any cards.",
                    "§7Use §e/card apply §7to get one!"
                )
            )), 4, 0)
            gui.addPane(emptyPane)
        }

        // Close button
        val bottomPane = StaticPane(0, 5, 9, 1)
        bottomPane.addItem(GuiItem(createItem(
            Material.BARRIER,
            "§c§lClose",
            emptyList()
        )) {
            player.closeInventory()
        }, 4, 0)
        gui.addPane(bottomPane)

        gui.show(player)
    }

    private fun showCardOptions(player: Player, card: Card) {
        val gui = ChestGui(4, "§1§lCard §8- §f${card.getMaskedNumber()}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.GRAY_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Card info
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§6§l${card.cardType.displayName}",
            listOf(
                "",
                "§7Card Number:",
                "§e${card.cardNumber}",
                "",
                "§7CVV: §f${card.cvv}",
                "§7Expires: §f${card.expirationDate}"
            )
        )), 4, 0)

        // Get physical card
        if (card.active && !card.frozen) {
            contentPane.addItem(GuiItem(createItem(
                Material.PAPER,
                "§a§lGet Physical Card",
                listOf(
                    "",
                    "§7Receive a card item to use at ATMs",
                    "",
                    "§aClick to receive"
                )
            )) {
                val cardItem = BankCardItem.createCardItem(card)
                player.inventory.addItem(cardItem)
                player.sendMessage(Messages.success("Card added to your inventory!"))
                player.closeInventory()
            }, 1, 2)
        }

        // Freeze/Unfreeze
        if (card.active) {
            if (card.frozen) {
                contentPane.addItem(GuiItem(createItem(
                    Material.CAMPFIRE,
                    "§6§lUnfreeze Card",
                    listOf(
                        "",
                        "§7Unfreeze this card to use it again",
                        "",
                        "§aClick to unfreeze"
                    )
                )) {
                    plugin.launch {
                        val success = cardService.unfreezeCard(card.cardNumber)
                        if (success) {
                            player.sendMessage(Messages.success("Card unfrozen!"))
                        } else {
                            player.sendMessage(Messages.error("Failed to unfreeze card."))
                        }
                        openMainMenu(player)
                    }
                }, 3, 2)
            } else {
                contentPane.addItem(GuiItem(createItem(
                    Material.ICE,
                    "§b§lFreeze Card",
                    listOf(
                        "",
                        "§7Temporarily disable this card",
                        "§7You can unfreeze it later",
                        "",
                        "§eClick to freeze"
                    )
                )) {
                    plugin.launch {
                        val success = cardService.freezeCard(card.cardNumber)
                        if (success) {
                            player.sendMessage(Messages.success("Card frozen!"))
                        } else {
                            player.sendMessage(Messages.error("Failed to freeze card."))
                        }
                        openMainMenu(player)
                    }
                }, 3, 2)
            }
        }

        // Cancel card
        if (card.active) {
            contentPane.addItem(GuiItem(createItem(
                Material.BARRIER,
                "§c§lCancel Card",
                listOf(
                    "",
                    "§cPermanently deactivate this card",
                    "§cThis cannot be undone!",
                    "",
                    "§cClick to cancel"
                )
            )) {
                showCancelConfirmation(player, card)
            }, 5, 2)
        }

        // Change PIN info
        contentPane.addItem(GuiItem(createItem(
            Material.TRIPWIRE_HOOK,
            "§e§lChange PIN",
            listOf(
                "",
                "§7Change your card PIN",
                "",
                "§eUse: §f/card changepin <card> <old> <new>"
            )
        )), 7, 2)

        // Back button
        contentPane.addItem(GuiItem(createItem(
            Material.ARROW,
            "§7Back",
            emptyList()
        )) {
            openMainMenu(player)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    private fun showCancelConfirmation(player: Player, card: Card) {
        val gui = ChestGui(3, "§4§lConfirm Cancel Card")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
        val background = OutlinePane(0, 0, 9, 3, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.RED_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 3)

        // Warning
        contentPane.addItem(GuiItem(createItem(
            Material.BARRIER,
            "§c§lCancel Card?",
            listOf(
                "",
                "§cThis will permanently deactivate",
                "§cyour card ${card.getMaskedNumber()}",
                "",
                "§cThis action cannot be undone!"
            )
        )), 4, 0)

        // Confirm
        contentPane.addItem(GuiItem(createItem(
            Material.LIME_WOOL,
            "§a§lYes, Cancel Card",
            emptyList()
        )) {
            plugin.launch {
                val success = cardService.cancelCard(card.cardNumber)
                if (success) {
                    player.sendMessage(Messages.success("Card cancelled permanently."))
                } else {
                    player.sendMessage(Messages.error("Failed to cancel card."))
                }
                player.closeInventory()
            }
        }, 2, 1)

        // Cancel
        contentPane.addItem(GuiItem(createItem(
            Material.RED_WOOL,
            "§c§lNo, Go Back",
            emptyList()
        )) {
            showCardOptions(player, card)
        }, 6, 1)

        gui.addPane(contentPane)
        gui.show(player)
    }

    private fun createItem(material: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.setDisplayName(name)
        if (lore.isNotEmpty()) {
            meta.lore = lore
        }
        item.itemMeta = meta
        return item
    }
}