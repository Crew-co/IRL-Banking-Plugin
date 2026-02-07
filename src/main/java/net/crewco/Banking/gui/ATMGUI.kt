// src/main/java/net/crewco/Banking/gui/ATMGUI.kt
package net.crewco.Banking.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.Startup.Companion.atmService
import net.crewco.Banking.Startup.Companion.cardService
import net.crewco.Banking.Startup.Companion.loanService
import net.crewco.Banking.Startup.Companion.plugin
import net.crewco.Banking.Startup.Companion.transactionService
import net.crewco.Banking.Startup.Companion.moneyService
import net.crewco.Banking.data.models.*
import net.crewco.Banking.data.cdata.CardType
import net.crewco.Banking.data.ldata.LoanStatus
import net.crewco.Banking.data.ldata.LoanType
import net.crewco.Banking.util.BankCardItem
import net.crewco.Banking.util.Messages
import net.milkbowl.vault.chat.Chat
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ATMGUI(private val plugin: Startup) {

    // Store active sessions for navigation
    private val sessions = mutableMapOf<Player, ATMSession>()

    data class ATMSession(
        val atm: ATM,
        val card: Card
    )

    fun open(player: Player, atm: ATM, card: Card) {
        sessions[player] = ATMSession(atm, card)
        showMainMenu(player, atm, card)
    }

    // ==================== MAIN MENU ====================

    private fun showMainMenu(player: Player, atm: ATM, card: Card) {
        val gui = ChestGui(5, ChatColor.translateAlternateColorCodes('&',"&1&lCrewCo ATM &8- &7${card.getMaskedNumber()}"))
        gui.setOnGlobalClick { event -> event.isCancelled = true }
        gui.setOnClose { sessions.remove(player) }

        // Background
        val background = OutlinePane(0, 0, 9, 5, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.BLUE_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 5)

        // Header - Card Info
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "&6&l💳 ${card.cardType.displayName}",
            listOf(
                "",
                "&7Card: &f${card.getMaskedNumber()}",
                "&7Account: &f${card.linkedAccountNumber}",
                "&7Expires: &f${card.expirationDate}",
                "",
                "&7Daily Limit: &e${Messages.formatCurrency(card.dailyLimit)}",
                "&7Spent Today: &c${Messages.formatCurrency(card.spentToday)}"
            )
        )), 4, 0)

        // ===== ROW 1: Quick Actions =====

        // Check Balance
        contentPane.addItem(GuiItem(createItem(
            Material.GOLD_INGOT,
            "&e&lCheck Balance",
            listOf(
                "",
                "&7View your account balance",
                "",
                "&aClick to check"
            )
        )) {
            plugin.launch {
                val balance = accountService.getBalance(card.linkedAccountNumber)
                val account = accountService.getAccount(card.linkedAccountNumber)
                if (balance != null && account != null) {
                    player.sendMessage(Messages.header("Account Balance"))
                    player.sendMessage(Messages.info("Current: ${Messages.formatCurrency(balance)}"))
                    player.sendMessage(Messages.info("Available: ${Messages.formatCurrency(account.getAvailableBalance())}"))
                } else {
                    player.sendMessage(Messages.error("Could not retrieve balance."))
                }
            }
        }, 1, 1)

        // Withdraw
        contentPane.addItem(GuiItem(createItem(
            Material.RED_WOOL,
            "&c&lWithdraw Cash",
            listOf(
                "",
                "&7Withdraw money from your account",
                "&6ATM Fee: ${Messages.formatCurrency(atm.transactionFee)}",
                "",
                "&aClick to withdraw"
            )
        )) {
            showWithdrawMenu(player, atm, card)
        }, 3, 1)

        // Deposit
        contentPane.addItem(GuiItem(createItem(
            Material.LIME_WOOL,
            "&a&lDeposit Cash",
            listOf(
                "",
                "&7Deposit money to your account",
                "",
                "&aClick to deposit"
            )
        )) {
            showDepositMenu(player, atm, card)
        }, 5, 1)

        // Transfer / Send Money
        contentPane.addItem(GuiItem(createItem(
            Material.ENDER_PEARL,
            "&d&lSend Money",
            listOf(
                "",
                "&7Transfer to your accounts",
                "&7or send to other players",
                "",
                "&aClick to send"
            )
        )) {
            showSendMoneyMenu(player, atm, card)
        }, 7, 1)

        // ===== ROW 2: Banking Services =====

        // My Accounts
        contentPane.addItem(GuiItem(createItem(
            Material.CHEST,
            "&6&lMy Accounts",
            listOf(
                "",
                "&7View and manage your",
                "&7bank accounts",
                "",
                "&aClick to view"
            )
        )) {
            showAccountsMenu(player, atm, card)
        }, 1, 2)

        // My Cards
        contentPane.addItem(GuiItem(createItem(
            Material.MAP,
            "&b&lMy Cards",
            listOf(
                "",
                "&7View and manage your",
                "&7debit/credit cards",
                "",
                "&aClick to view"
            )
        )) {
            showCardsMenu(player, atm, card)
        }, 3, 2)

        // My Loans
        contentPane.addItem(GuiItem(createItem(
            Material.BOOK,
            "&e&lMy Loans",
            listOf(
                "",
                "&7View your loans and",
                "&7make payments",
                "",
                "&aClick to view"
            )
        )) {
            showLoansMenu(player, atm, card)
        }, 5, 2)

        // Transaction History
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "&f&lTransaction History",
            listOf(
                "",
                "&7View recent transactions",
                "&7on this account",
                "",
                "&aClick to view"
            )
        )) {
            showTransactionHistory(player, atm, card)
        }, 7, 2)

        // ===== ROW 3: Info & Exit =====

        // ATM Info
        contentPane.addItem(GuiItem(createItem(
            Material.OAK_SIGN,
            "&b&lATM Information",
            listOf(
                "",
                "&7ATM ID: &f${atm.atmId}",
                "&7Max Withdrawal: &f${Messages.formatCurrency(atm.maxWithdrawal)}",
                "&7Available Cash: &f${Messages.formatCurrency(atm.cash)}",
                "&7Transaction Fee: &f${Messages.formatCurrency(atm.transactionFee)}"
            )
        )), 3, 3)

        // Open New Account
        contentPane.addItem(GuiItem(createItem(
            Material.LIME_DYE,
            "&a&lOpen New Account",
            listOf(
                "",
                "&7Open a new bank account",
                "",
                "&aClick to view options"
            )
        )) {
            showOpenAccountMenu(player, atm, card)
        }, 5, 3)

        // Exit
        contentPane.addItem(GuiItem(createItem(
            Material.BARRIER,
            "&c&lExit ATM",
            listOf(
                "",
                "&7Return your card and exit"
            )
        )) {
            player.closeInventory()
            sessions.remove(player)
            player.sendMessage(Messages.info("Please take your card. Thank you for using CrewCo Bank!"))
        }, 8, 4)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== WITHDRAW MENU ====================

    private fun showWithdrawMenu(player: Player, atm: ATM, card: Card) {
        val gui = ChestGui(4, "&1&lATM &8- &cWithdraw")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.RED_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Header
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "&c&lSelect Withdrawal Amount",
            listOf(
                "",
                "&7Card: &f${card.getMaskedNumber()}",
                "&7Max per transaction: &f${Messages.formatCurrency(atm.maxWithdrawal)}",
                "&7ATM Fee: &6${Messages.formatCurrency(atm.transactionFee)}"
            )
        )), 4, 0)

        // Amount buttons
        val amounts = listOf(
            Triple(1, 1, 100.0),
            Triple(2, 1, 200.0),
            Triple(3, 1, 500.0),
            Triple(5, 1, 1000.0),
            Triple(6, 1, 2000.0),
            Triple(7, 1, 5000.0)
        )

        for ((x, y, amount) in amounts) {
            val canWithdraw = amount <= atm.maxWithdrawal && amount <= atm.cash
            val material = if (canWithdraw) Material.EMERALD else Material.REDSTONE
            val nameColor = if (canWithdraw) "&a" else "&c"

            val lore = mutableListOf(
                "",
                "&6Fee: ${Messages.formatCurrency(atm.transactionFee)}",
                "&eTotal Deducted: ${Messages.formatCurrency(amount + atm.transactionFee)}",
                ""
            )
            lore.add(if (canWithdraw) "&aClick to withdraw" else "&cUnavailable")

            contentPane.addItem(GuiItem(createItem(material, "$nameColor&l${Messages.formatCurrency(amount)}", lore)) {
                if (canWithdraw) {
                    processWithdrawal(player, atm, card, amount)
                } else {
                    player.sendMessage(Messages.error("This amount is not available."))
                }
            }, x, y)
        }

        // Larger amounts row
        val largeAmounts = listOf(
            Triple(2, 2, 10000.0),
            Triple(4, 2, 25000.0),
            Triple(6, 2, 50000.0)
        )

        for ((x, y, amount) in largeAmounts) {
            val canWithdraw = amount <= atm.maxWithdrawal && amount <= atm.cash
            val material = if (canWithdraw) Material.EMERALD_BLOCK else Material.REDSTONE_BLOCK
            val nameColor = if (canWithdraw) "&a" else "&c"

            contentPane.addItem(GuiItem(createItem(
                material,
                "$nameColor&l${Messages.formatCurrency(amount)}",
                listOf("", if (canWithdraw) "&aClick to withdraw" else "&cUnavailable")
            )) {
                if (canWithdraw) {
                    processWithdrawal(player, atm, card, amount)
                }
            }, x, y)
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", listOf("&8Return to main menu"))) {
            showMainMenu(player, atm, card)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== DEPOSIT MENU ====================

    private fun showDepositMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val wallet = accountService.getPrimaryAccount(player.uniqueId)
            val walletBalance = wallet?.balance ?: 0.0
            
            val gui = ChestGui(4, "&1&lATM &8- &aDeposit Cash")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.LIME_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            val contentPane = StaticPane(0, 0, 9, 4)

            // Check if this card is linked to wallet
            val isWalletCard = card.linkedAccountNumber == wallet?.accountNumber
            
            if (isWalletCard) {
                // Can't deposit from wallet to wallet
                contentPane.addItem(GuiItem(createItem(
                    Material.BARRIER,
                    "&c&lCannot Deposit",
                    listOf(
                        "",
                        "&7This card is linked to your wallet.",
                        "&7Your wallet IS your cash on hand!",
                        "",
                        "&7Use a card linked to another account",
                        "&7(checking, savings, etc.)"
                    )
                )), 4, 1)
            } else {
                // Header - Show wallet balance (cash on hand)
                contentPane.addItem(GuiItem(createItem(
                    Material.GOLD_INGOT,
                    "&a&l💵 Cash on Hand (Wallet)",
                    listOf(
                        "",
                        "&7Card: &f${card.getMaskedNumber()}",
                        "&7Depositing to: &f${card.linkedAccountNumber}",
                        "",
                        "&7Wallet balance: &a${Messages.formatCurrency(walletBalance)}"
                    )
                )), 4, 0)

                // Deposit All button
                val depositAllMaterial = if (walletBalance > 0) Material.EMERALD_BLOCK else Material.REDSTONE_BLOCK
                val depositAllColor = if (walletBalance > 0) "&a" else "&c"
                contentPane.addItem(GuiItem(createItem(
                    depositAllMaterial,
                    "$depositAllColor&lDeposit All Cash",
                    listOf(
                        "",
                        "&7Amount: &a${Messages.formatCurrency(walletBalance)}",
                        "",
                        if (walletBalance > 0) "&aClick to deposit all" else "&cNo cash to deposit"
                    )
                )) {
                    if (walletBalance > 0) {
                        processDepositAll(player, atm, card)
                    } else {
                        player.sendMessage(Messages.error("You have no cash to deposit!"))
                    }
                }, 4, 1)

                // Quick deposit amounts
                val amounts = listOf(100.0, 500.0, 1000.0, 5000.0, 10000.0)
                val slots = listOf(1 to 2, 2 to 2, 4 to 2, 6 to 2, 7 to 2)

                for ((index, amount) in amounts.withIndex()) {
                    if (index < slots.size) {
                        val (x, y) = slots[index]
                        val hasEnough = walletBalance >= amount
                        val material = if (hasEnough) Material.GOLD_INGOT else Material.IRON_INGOT
                        val nameColor = if (hasEnough) "&6" else "&7"

                        contentPane.addItem(GuiItem(createItem(
                            material,
                            "$nameColor&l${Messages.formatCurrency(amount)}",
                            listOf(
                                "",
                                if (hasEnough) "&aClick to deposit" else "&cNot enough cash"
                            )
                        )) {
                            if (hasEnough) {
                                processDeposit(player, atm, card, amount)
                            } else {
                                player.sendMessage(Messages.error("You don't have ${Messages.formatCurrency(amount)} in your wallet!"))
                            }
                        }, x, y)
                    }
                }
            }

            // Back button
            contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", listOf("&8Return to main menu"))) {
                showMainMenu(player, atm, card)
            }, 0, 3)

            gui.addPane(contentPane)
            gui.show(player)
        }
    }

    // ==================== CASH BREAKDOWN VIEW ====================

    // ==================== SEND MONEY MENU ====================

    private fun showSendMoneyMenu(player: Player, atm: ATM, card: Card) {
        val gui = ChestGui(4, ChatColor.translateAlternateColorCodes('&',"&1&lATM &8- &dSend Money"))
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.PURPLE_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Header
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "&d&lChoose Send Type",
            listOf(
                "",
                "&7From: &f${card.linkedAccountNumber}",
                "",
                "&7Choose how to send money"
            )
        )), 4, 0)

        // Transfer to own accounts
        contentPane.addItem(GuiItem(createItem(
            Material.ENDER_CHEST,
            "&6&lMy Accounts",
            listOf(
                "",
                "&7Transfer money between",
                "&7your own accounts",
                "",
                "&aClick to select"
            )
        )) {
            showTransferMenu(player, atm, card)
        }, 2, 1)

        // Send to another player
        contentPane.addItem(GuiItem(createItem(
            Material.PLAYER_HEAD,
            "&b&lSend to Player",
            listOf(
                "",
                "&7Send money to another",
                "&7player's bank account",
                "",
                "&aClick to select"
            )
        )) {
            showSendToPlayerMenu(player, atm, card)
        }, 6, 1)

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
            showMainMenu(player, atm, card)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== SEND TO PLAYER MENU ====================

    private fun showSendToPlayerMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            // Get online players (excluding self)
            val onlinePlayers = org.bukkit.Bukkit.getOnlinePlayers()
                .filter { it.uniqueId != player.uniqueId }
                .take(21) // Max 21 players to fit in 3 rows

            val gui = ChestGui(5, "&1&lATM &8- &bSend to Player")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 5, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.CYAN_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            val contentPane = StaticPane(0, 0, 9, 5)

            // Header
            contentPane.addItem(GuiItem(createItem(
                Material.PAPER,
                "&b&lSelect Player",
                listOf(
                    "",
                    "&7From: &f${card.linkedAccountNumber}",
                    "",
                    "&7Select an online player to send to"
                )
            )), 4, 0)

            if (onlinePlayers.isNotEmpty()) {
                var slotIndex = 0
                val slots = mutableListOf<Pair<Int, Int>>()
                for (y in 1..3) {
                    for (x in 1..7) {
                        slots.add(x to y)
                    }
                }

                for (targetPlayer in onlinePlayers) {
                    if (slotIndex >= slots.size) break
                    val (x, y) = slots[slotIndex]

                    // Check if target has a bank account
                    val targetAccount = accountService.getPrimaryAccount(targetPlayer.uniqueId)
                    val hasAccount = targetAccount != null

                    val skull = ItemStack(Material.PLAYER_HEAD)
                    val skullMeta = skull.itemMeta as org.bukkit.inventory.meta.SkullMeta
                    skullMeta.owningPlayer = targetPlayer
                    skullMeta.setDisplayName("&f${targetPlayer.name}")
                    
                    val lore = if (hasAccount) {
                        listOf(
                            "",
                            "&7Has bank account",
                            "",
                            "&aClick to send money"
                        )
                    } else {
                        listOf(
                            "",
                            "&cNo bank account",
                            "",
                            "&7Cannot receive transfers"
                        )
                    }
                    skullMeta.lore = lore
                    skull.itemMeta = skullMeta

                    contentPane.addItem(GuiItem(skull) {
                        if (hasAccount) {
                            showSendToPlayerAmountMenu(player, atm, card, targetPlayer, targetAccount!!)
                        } else {
                            player.sendMessage(Messages.error("${targetPlayer.name} doesn't have a bank account!"))
                        }
                    }, x, y)

                    slotIndex++
                }
            } else {
                contentPane.addItem(GuiItem(createItem(
                    Material.BARRIER,
                    "&cNo Players Online",
                    listOf("", "&7No other players are online")
                )), 4, 2)
            }

            // Back button
            contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
                showSendMoneyMenu(player, atm, card)
            }, 0, 4)

            gui.addPane(contentPane)
            gui.show(player)
        }
    }

    private fun showSendToPlayerAmountMenu(player: Player, atm: ATM, card: Card, targetPlayer: Player, targetAccount: BankAccount) {
        plugin.launch {
            val sourceAccount = accountService.getAccount(card.linkedAccountNumber)
            val sourceBalance = sourceAccount?.balance ?: 0.0

            val gui = ChestGui(4, "&1&lSend to &f${targetPlayer.name}")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.CYAN_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            val contentPane = StaticPane(0, 0, 9, 4)

            // Header with player skull
            val skull = ItemStack(Material.PLAYER_HEAD)
            val skullMeta = skull.itemMeta as org.bukkit.inventory.meta.SkullMeta
            skullMeta.owningPlayer = targetPlayer
            skullMeta.setDisplayName("&b&lSending to ${targetPlayer.name}")
            skullMeta.lore = listOf(
                "",
                "&7Your Balance: &a${Messages.formatCurrency(sourceBalance)}",
                "",
                "&7Select amount to send"
            )
            skull.itemMeta = skullMeta
            contentPane.addItem(GuiItem(skull), 4, 0)

            // Amount buttons
            val amounts = listOf(50.0, 100.0, 500.0, 1000.0, 5000.0, 10000.0)
            val slots = listOf(1 to 1, 2 to 1, 3 to 1, 5 to 1, 6 to 1, 7 to 1)

            for ((index, amount) in amounts.withIndex()) {
                if (index >= slots.size) break
                val (x, y) = slots[index]
                
                val canAfford = sourceBalance >= amount
                val material = if (canAfford) Material.EMERALD else Material.REDSTONE
                val color = if (canAfford) "&a" else "&c"

                contentPane.addItem(GuiItem(createItem(
                    material,
                    "$color&l${Messages.formatCurrency(amount)}",
                    listOf(
                        "",
                        if (canAfford) "&aClick to send" else "&cInsufficient funds"
                    )
                )) {
                    if (canAfford) {
                        processSendToPlayer(player, atm, card, targetPlayer, targetAccount, amount)
                    } else {
                        player.sendMessage(Messages.error("Insufficient funds!"))
                    }
                }, x, y)
            }

            // Custom amount option
            contentPane.addItem(GuiItem(createItem(
                Material.NAME_TAG,
                "&e&lCustom Amount",
                listOf(
                    "",
                    "&7Type the amount in chat",
                    "",
                    "&eClick to enter amount"
                )
            )) {
                player.closeInventory()
                player.sendMessage(Messages.header("Send to ${targetPlayer.name}"))
                player.sendMessage(Messages.info("Type the amount you want to send in chat:"))
                player.sendMessage(Messages.info("(Type 'cancel' to cancel)"))
                
                // Store pending transaction info
                pendingPlayerPayments[player.uniqueId] = PendingPayment(
                    targetPlayer.uniqueId,
                    targetPlayer.name,
                    targetAccount.accountNumber,
                    card.linkedAccountNumber,
                    atm
                )
            }, 4, 2)

            // Back button
            contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
                showSendToPlayerMenu(player, atm, card)
            }, 0, 3)

            gui.addPane(contentPane)
            gui.show(player)
        }
    }

    // Data class for pending custom amount payments
    data class PendingPayment(
        val targetUuid: java.util.UUID,
        val targetName: String,
        val targetAccountNumber: String,
        val sourceAccountNumber: String,
        val atm: ATM
    )

    companion object {
        val pendingPlayerPayments = mutableMapOf<java.util.UUID, PendingPayment>()
    }

    private fun processSendToPlayer(player: Player, atm: ATM, card: Card, targetPlayer: Player, targetAccount: BankAccount, amount: Double) {
        plugin.launch {
            val result = accountService.transfer(
                card.linkedAccountNumber,
                targetAccount.accountNumber,
                amount,
                player.uniqueId,
                "ATM Transfer to ${targetPlayer.name}"
            )

            when (result) {
                net.crewco.Banking.services.sdata.TransferResult.SUCCESS -> {
                    player.sendMessage(Messages.success("Sent ${Messages.formatCurrency(amount)} to ${targetPlayer.name}!"))
                    val newBalance = accountService.getBalance(card.linkedAccountNumber) ?: 0.0
                    player.sendMessage(Messages.info("New balance: ${Messages.formatCurrency(newBalance)}"))
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                    
                    // Notify target player
                    if (targetPlayer.isOnline) {
                        targetPlayer.sendMessage(Messages.success("${player.name} sent you ${Messages.formatCurrency(amount)}!"))
                        targetPlayer.playSound(targetPlayer.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f)
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

            showMainMenu(player, atm, card)
        }
    }

    // ==================== TRANSFER MENU ====================

    private fun showTransferMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val accounts = accountService.getAccountsByPlayer(player.uniqueId)
                .filter { it.accountNumber != card.linkedAccountNumber }

            val gui = ChestGui(4, ChatColor.translateAlternateColorCodes('&',"&1&lATM &8- &dTransfer"))
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.PURPLE_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            val contentPane = StaticPane(0, 0, 9, 4)

            // Header
            contentPane.addItem(GuiItem(createItem(
                Material.PAPER,
                "&d&lSelect Destination Account",
                listOf(
                    "",
                    "&7Transfer FROM: &f${card.linkedAccountNumber}",
                    "",
                    "&7Select an account to transfer to"
                )
            )), 4, 0)

            if (accounts.isNotEmpty()) {
                accounts.take(7).forEachIndexed { index, account ->
                    val x = 1 + index
                    val material = when (account.accountType) {
                        AccountType.WALLET -> Material.LEATHER
                        AccountType.CHECKING -> Material.GOLD_INGOT
                        AccountType.SAVINGS -> Material.DIAMOND
                        AccountType.BUSINESS -> Material.EMERALD_BLOCK
                        AccountType.STOCK -> Material.AMETHYST_SHARD
                    }

                    contentPane.addItem(GuiItem(createItem(
                        material,
                        "&f${account.accountName}",
                        listOf(
                            "&7Type: &f${account.accountType.displayName}",
                            "&7Account: &e${account.accountNumber}",
                            "&7Balance: &a${Messages.formatCurrency(account.balance)}",
                            "",
                            "&aClick to select"
                        )
                    )) {
                        showTransferAmountMenu(player, atm, card, account)
                    }, x, 1)
                }
            } else {
                contentPane.addItem(GuiItem(createItem(
                    Material.BARRIER,
                    "&cNo Other Accounts",
                    listOf("", "&7You need another account to transfer to")
                )), 4, 1)
            }

            // Back button
            contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
                showMainMenu(player, atm, card)
            }, 0, 3)

            gui.addPane(contentPane)
            gui.show(player)
        }
    }

    private fun showTransferAmountMenu(player: Player, atm: ATM, card: Card, toAccount: BankAccount) {
        val gui = ChestGui(4, "&1&lTransfer to &f${toAccount.accountNumber}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.PURPLE_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Header
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "&d&lSelect Transfer Amount",
            listOf(
                "",
                "&7From: &f${card.linkedAccountNumber}",
                "&7To: &f${toAccount.accountNumber} (${toAccount.accountType.displayName})"
            )
        )), 4, 0)

        // Amount buttons
        val amounts = listOf(100.0, 500.0, 1000.0, 5000.0, 10000.0, 50000.0)
        val slots = listOf(1 to 1, 2 to 1, 3 to 1, 5 to 1, 6 to 1, 7 to 1)

        for ((index, amount) in amounts.withIndex()) {
            val (x, y) = slots[index]
            contentPane.addItem(GuiItem(createItem(
                Material.ENDER_PEARL,
                "&d&l${Messages.formatCurrency(amount)}",
                listOf("", "&aClick to transfer")
            )) {
                processTransfer(player, atm, card, toAccount, amount)
            }, x, y)
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
            showTransferMenu(player, atm, card)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== ACCOUNTS MENU ====================

    private fun showAccountsMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val accounts = accountService.getAccountsByPlayer(player.uniqueId)

            val gui = ChestGui(6, ChatColor.translateAlternateColorCodes('&',"&1&lMy Accounts"))
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            // Header
            val headerPane = StaticPane(0, 0, 9, 1)
            headerPane.addItem(GuiItem(createItem(
                Material.GOLD_BLOCK,
                "&6&lYour Bank Accounts",
                listOf(
                    "",
                    "&7Total Accounts: &f${accounts.size}",
                    "&7Total Balance: &a${Messages.formatCurrency(accounts.sumOf { it.balance })}"
                )
            )), 4, 0)
            gui.addPane(headerPane)

            if (accounts.isNotEmpty()) {
                val paginatedPane = PaginatedPane(1, 1, 7, 4)

                val accountItems = accounts.map { account ->
                    val material = when (account.accountType) {
                        AccountType.WALLET -> Material.LEATHER
                        AccountType.CHECKING -> Material.GOLD_INGOT
                        AccountType.SAVINGS -> Material.DIAMOND
                        AccountType.BUSINESS -> Material.EMERALD_BLOCK
                        AccountType.STOCK -> Material.AMETHYST_SHARD
                    }

                    val isLinked = account.accountNumber == card.linkedAccountNumber
                    val statusColor = if (account.frozen) "&c" else "&a"

                    GuiItem(createItem(
                        material,
                        "&f${account.accountName}${if (isLinked) " &7(This Card)" else ""}",
                        listOf(
                            "&7Type: &f${account.accountType.displayName}",
                            "",
                            "&7Account #: &e${account.accountNumber}",
                            "&7Routing #: &e${account.routingNumber}",
                            "",
                            "&7Balance: &a${Messages.formatCurrency(account.balance)}",
                            "&7Available: &a${Messages.formatCurrency(account.getAvailableBalance())}",
                            "",
                            "&7Status: $statusColor${if (account.frozen) "FROZEN" else "Active"}"
                        )
                    ))
                }

                paginatedPane.populateWithGuiItems(accountItems)
                gui.addPane(paginatedPane)

                // Navigation
                val navPane = StaticPane(0, 5, 9, 1)
                navPane.addItem(GuiItem(createItem(Material.ARROW, "&7Previous", emptyList())) {
                    if (paginatedPane.page > 0) {
                        paginatedPane.page -= 1
                        gui.update()
                    }
                }, 0, 0)
                navPane.addItem(GuiItem(createItem(Material.ARROW, "&7Next", emptyList())) {
                    if (paginatedPane.page < paginatedPane.pages - 1) {
                        paginatedPane.page += 1
                        gui.update()
                    }
                }, 8, 0)
                gui.addPane(navPane)
            }

            // Back button
            val bottomPane = StaticPane(0, 5, 9, 1)
            bottomPane.addItem(GuiItem(createItem(Material.DARK_OAK_DOOR, "&7Back to ATM", emptyList())) {
                showMainMenu(player, atm, card)
            }, 4, 0)
            gui.addPane(bottomPane)

            gui.show(player)
        }
    }

    // ==================== CARDS MENU ====================

    private fun showCardsMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val cards = cardService.getCardsByPlayer(player.uniqueId)

            val gui = ChestGui(6, "&1&lMy Cards")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            // Header
            val headerPane = StaticPane(0, 0, 9, 1)
            headerPane.addItem(GuiItem(createItem(
                Material.PAPER,
                "&b&l💳 Your Cards",
                listOf(
                    "",
                    "&7Total Cards: &f${cards.size}",
                    "&7Active: &a${cards.count { it.active && !it.frozen }}"
                )
            )), 4, 0)
            gui.addPane(headerPane)

            if (cards.isNotEmpty()) {
                val paginatedPane = PaginatedPane(1, 1, 7, 4)

                val cardItems = cards.map { c ->
                    val isCurrentCard = c.cardNumber == card.cardNumber
                    val material = when {
                        !c.active -> Material.GRAY_DYE
                        c.frozen -> Material.ICE
                        c.isExpired() -> Material.RED_DYE
                        c.cardType.name.contains("CREDIT") -> Material.GOLD_INGOT
                        c.cardType.name.contains("PREMIUM") -> Material.DIAMOND
                        else -> Material.PAPER
                    }

                    val statusColor = when {
                        !c.active -> "&8"
                        c.frozen -> "&b"
                        c.isExpired() -> "&c"
                        else -> "&a"
                    }

                    val status = when {
                        !c.active -> "Cancelled"
                        c.frozen -> "Frozen"
                        c.isExpired() -> "Expired"
                        else -> "Active"
                    }

                    GuiItem(createItem(
                        material,
                        "&f${c.cardType.displayName}${if (isCurrentCard) " &a(In Use)" else ""}",
                        listOf(
                            "",
                            "&7Card: &e${c.getMaskedNumber()}",
                            "&7Expires: &f${c.expirationDate}",
                            "&7Linked Account: &f${c.linkedAccountNumber}",
                            "",
                            "&7Daily Limit: &e${Messages.formatCurrency(c.dailyLimit)}",
                            "&7Spent Today: &c${Messages.formatCurrency(c.spentToday)}",
                            "",
                            "&7Status: $statusColor$status",
                            "",
                            if (c.active && !c.frozen) "&eClick for options" else ""
                        ).filter { it.isNotEmpty() }
                    )) {
                        if (c.active && !c.frozen && !c.isExpired()) {
                            showCardOptions(player, atm, card, c)
                        }
                    }
                }

                paginatedPane.populateWithGuiItems(cardItems)
                gui.addPane(paginatedPane)
            }

            // Back button
            val bottomPane = StaticPane(0, 5, 9, 1)
            bottomPane.addItem(GuiItem(createItem(Material.DARK_OAK_DOOR, "&7Back to ATM", emptyList())) {
                showMainMenu(player, atm, card)
            }, 4, 0)
            gui.addPane(bottomPane)

            gui.show(player)
        }
    }

    private fun showCardOptions(player: Player, atm: ATM, sessionCard: Card, selectedCard: Card) {
        val gui = ChestGui(4, "&1&lCard &8- &f${selectedCard.getMaskedNumber()}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.CYAN_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Card info
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "&6&l${selectedCard.cardType.displayName}",
            listOf(
                "",
                "&7Card Number:",
                "&e${selectedCard.cardNumber}",
                "",
                "&7CVV: &f${selectedCard.cvv}",
                "&7Expires: &f${selectedCard.expirationDate}"
            )
        )), 4, 0)

        // Get physical card
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "&a&lGet Physical Card",
            listOf(
                "",
                "&7Receive a card item",
                "&7to use at other ATMs",
                "",
                "&aClick to receive"
            )
        )) {
            val cardItem = BankCardItem.createCardItem(selectedCard)
            player.inventory.addItem(cardItem)
            player.sendMessage(Messages.success("Card added to your inventory!"))
        }, 2, 2)

        // Freeze/Unfreeze
        if (selectedCard.frozen) {
            contentPane.addItem(GuiItem(createItem(
                Material.CAMPFIRE,
                "&6&lUnfreeze Card",
                listOf("", "&7Unfreeze this card", "", "&aClick to unfreeze")
            )) {
                plugin.launch {
                    cardService.unfreezeCard(selectedCard.cardNumber)
                    player.sendMessage(Messages.success("Card unfrozen!"))
                    showCardsMenu(player, atm, sessionCard)
                }
            }, 4, 2)
        } else {
            contentPane.addItem(GuiItem(createItem(
                Material.ICE,
                "&b&lFreeze Card",
                listOf("", "&7Temporarily disable card", "", "&eClick to freeze")
            )) {
                plugin.launch {
                    cardService.freezeCard(selectedCard.cardNumber)
                    player.sendMessage(Messages.success("Card frozen!"))
                    showCardsMenu(player, atm, sessionCard)
                }
            }, 4, 2)
        }

        // Cancel card
        contentPane.addItem(GuiItem(createItem(
            Material.BARRIER,
            "&c&lCancel Card",
            listOf("", "&cPermanently deactivate", "&cThis cannot be undone!")
        )) {
            plugin.launch {
                cardService.cancelCard(selectedCard.cardNumber)
                player.sendMessage(Messages.warning("Card cancelled permanently."))
                showCardsMenu(player, atm, sessionCard)
            }
        }, 6, 2)

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
            showCardsMenu(player, atm, sessionCard)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== LOANS MENU ====================

    private fun showLoansMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val loans = loanService.getLoansByPlayer(player.uniqueId)

            val gui = ChestGui(6, ChatColor.translateAlternateColorCodes('&',"&1&lMy Loans"))
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            // Header
            val headerPane = StaticPane(0, 0, 9, 1)
            val activeLoans = loans.filter { it.status == LoanStatus.ACTIVE }
            val totalOwed = activeLoans.sumOf { it.remainingBalance }

            headerPane.addItem(GuiItem(createItem(
                Material.BOOK,
                "&e&l📋 Your Loans",
                listOf(
                    "",
                    "&7Total Loans: &f${loans.size}",
                    "&7Active Loans: &e${activeLoans.size}",
                    "&7Total Owed: &c${Messages.formatCurrency(totalOwed)}"
                )
            )), 4, 0)
            gui.addPane(headerPane)

            if (loans.isNotEmpty()) {
                val paginatedPane = PaginatedPane(1, 1, 7, 4)

                val loanItems = loans.map { loan ->
                    val material = when (loan.status) {
                        LoanStatus.ACTIVE -> Material.GOLD_INGOT
                        LoanStatus.PENDING -> Material.CLOCK
                        LoanStatus.APPROVED -> Material.LIME_DYE
                        LoanStatus.PAID_OFF -> Material.EMERALD
                        LoanStatus.DEFAULTED -> Material.REDSTONE
                        LoanStatus.REJECTED -> Material.BARRIER
                        LoanStatus.CANCELLED -> Material.GRAY_DYE
                    }

                    val statusColor = when (loan.status) {
                        LoanStatus.ACTIVE -> "&a"
                        LoanStatus.PENDING -> "&e"
                        LoanStatus.APPROVED -> "&b"
                        LoanStatus.PAID_OFF -> "&2"
                        LoanStatus.DEFAULTED -> "&c"
                        LoanStatus.REJECTED -> "&4"
                        LoanStatus.CANCELLED -> "&8"
                    }

                    val lore = mutableListOf(
                        "",
                        "&7Loan ID: &e${loan.loanId}",
                        "",
                        "&7Principal: &f${Messages.formatCurrency(loan.principalAmount)}",
                        "&7Remaining: &c${Messages.formatCurrency(loan.remainingBalance)}",
                        "&7Monthly Payment: &e${Messages.formatCurrency(loan.monthlyPayment)}",
                        "",
                        "&7Status: $statusColor${loan.status.name}"
                    )

                    if (loan.status == LoanStatus.ACTIVE) {
                        lore.add("")
                        lore.add("&eClick to make payment")
                    }

                    GuiItem(createItem(material, "&f${loan.loanType.displayName}", lore)) {
                        if (loan.status == LoanStatus.ACTIVE) {
                            showLoanPaymentMenu(player, atm, card, loan)
                        }
                    }
                }

                paginatedPane.populateWithGuiItems(loanItems)
                gui.addPane(paginatedPane)
            } else {
                val emptyPane = StaticPane(0, 2, 9, 2)
                emptyPane.addItem(GuiItem(createItem(
                    Material.BARRIER,
                    "&cNo Loans",
                    listOf("", "&7Use &e/loan apply &7to apply")
                )), 4, 0)
                gui.addPane(emptyPane)
            }

            // Bottom buttons
            val bottomPane = StaticPane(0, 5, 9, 1)
            bottomPane.addItem(GuiItem(createItem(Material.BOOK, "&e&lLoan Types Info", emptyList())) {
                showLoanTypesInfo(player, atm, card)
            }, 2, 0)
            bottomPane.addItem(GuiItem(createItem(Material.DARK_OAK_DOOR, "&7Back to ATM", emptyList())) {
                showMainMenu(player, atm, card)
            }, 4, 0)
            gui.addPane(bottomPane)

            gui.show(player)
        }
    }

    private fun showLoanPaymentMenu(player: Player, atm: ATM, card: Card, loan: Loan) {
        val gui = ChestGui(4, "&1&lLoan Payment &8- &f${loan.loanId}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.YELLOW_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Loan info
        contentPane.addItem(GuiItem(createItem(
            Material.BOOK,
            "&6&l${loan.loanType.displayName}",
            listOf(
                "",
                "&7Loan ID: &e${loan.loanId}",
                "&7Principal: &f${Messages.formatCurrency(loan.principalAmount)}",
                "&7Remaining: &c${Messages.formatCurrency(loan.remainingBalance)}",
                "",
                "&7Monthly Payment: &e${Messages.formatCurrency(loan.monthlyPayment)}",
                "&7Months Left: &f${loan.monthsRemaining}/${loan.termMonths}"
            )
        )), 4, 0)

        // Make monthly payment
        contentPane.addItem(GuiItem(createItem(
            Material.GOLD_INGOT,
            "&a&lMake Monthly Payment",
            listOf(
                "",
                "&7Pay: &e${Messages.formatCurrency(loan.monthlyPayment)}",
                "",
                "&aClick to pay"
            )
        )) {
            plugin.launch {
                val result = loanService.makePayment(loan.loanId)
                if (result.success) {
                    player.sendMessage(Messages.success(result.message))
                    if (result.loan?.status == LoanStatus.PAID_OFF) {
                        player.sendMessage(Messages.success("🎉 Congratulations! Loan paid off!"))
                    }
                } else {
                    player.sendMessage(Messages.error(result.message))
                }
                showLoansMenu(player, atm, card)
            }
        }, 2, 2)

        // Pay off entire loan
        contentPane.addItem(GuiItem(createItem(
            Material.EMERALD_BLOCK,
            "&2&lPay Off Entire Loan",
            listOf(
                "",
                "&7Pay remaining: &c${Messages.formatCurrency(loan.remainingBalance)}",
                "",
                "&aClick to pay off"
            )
        )) {
            plugin.launch {
                val result = loanService.makePayment(loan.loanId, loan.remainingBalance)
                if (result.success) {
                    player.sendMessage(Messages.success("🎉 Loan paid off in full!"))
                } else {
                    player.sendMessage(Messages.error(result.message))
                }
                showLoansMenu(player, atm, card)
            }
        }, 6, 2)

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
            showLoansMenu(player, atm, card)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    private fun showLoanTypesInfo(player: Player, atm: ATM, card: Card) {
        val gui = ChestGui(4, "&1&lLoan Types")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.GRAY_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        val types = listOf(
            Triple(1, 1, LoanType.PERSONAL),
            Triple(3, 1, LoanType.BUSINESS),
            Triple(5, 1, LoanType.MORTGAGE),
            Triple(2, 2, LoanType.EMERGENCY),
            Triple(6, 2, LoanType.STUDENT)
        )

        for ((x, y, type) in types) {
            val material = when (type) {
                LoanType.PERSONAL -> Material.GOLD_INGOT
                LoanType.BUSINESS -> Material.EMERALD
                LoanType.MORTGAGE -> Material.BRICKS
                LoanType.EMERGENCY -> Material.REDSTONE
                LoanType.STUDENT -> Material.BOOK
            }

            contentPane.addItem(GuiItem(createItem(
                material,
                "&6&l${type.displayName}",
                listOf(
                    "",
                    "&7Base Rate: &e${type.baseInterestRate}%",
                    "&7Max Amount: &f${Messages.formatCurrency(type.maxAmount)}",
                    "&7Max Term: &f${type.maxTermMonths} months",
                    "&7Collateral: ${if (type.requiresCollateral) "&cRequired" else "&aNot Required"}",
                    "",
                    "&eUse: &f/loan apply ${type.name.lowercase()} ..."
                )
            )), x, y)
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
            showLoansMenu(player, atm, card)
        }, 4, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== TRANSACTION HISTORY ====================

    private fun showTransactionHistory(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val transactions = transactionService.getTransactionHistory(card.linkedAccountNumber, 28)

            val gui = ChestGui(6, ChatColor.translateAlternateColorCodes('&',"&1&lTransaction History"))
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            // Header
            val headerPane = StaticPane(0, 0, 9, 1)
            headerPane.addItem(GuiItem(createItem(
                Material.PAPER,
                "&f&lTransaction History",
                listOf(
                    "",
                    "&7Account: &f${card.linkedAccountNumber}",
                    "&7Showing last ${transactions.size} transactions"
                )
            )), 4, 0)
            gui.addPane(headerPane)

            if (transactions.isNotEmpty()) {
                val paginatedPane = PaginatedPane(1, 1, 7, 4)

                val txItems = transactions.map { tx ->
                    val isIncoming = tx.toAccountNumber == card.linkedAccountNumber
                    val material = if (isIncoming) Material.LIME_CONCRETE else Material.RED_CONCRETE
                    val arrow = if (isIncoming) "&a↓" else "&c↑"
                    val amountColor = if (isIncoming) "&a+" else "&c-"

                    GuiItem(createItem(
                        material,
                        "$arrow &f${tx.type.name.replace("_", " ")}",
                        listOf(
                            "",
                            "&7Amount: $amountColor${Messages.formatCurrency(tx.amount)}",
                            if (tx.fee > 0) "&7Fee: &c${Messages.formatCurrency(tx.fee)}" else "",
                            "",
                            "&7Transaction ID:",
                            "&8${tx.transactionId}",
                            "",
                            "&7Date: &f${tx.createdAt.toLocalDate()}",
                            "&7Time: &f${tx.createdAt.toLocalTime().toString().substringBefore(".")}"
                        ).filter { it.isNotEmpty() }
                    ))
                }

                paginatedPane.populateWithGuiItems(txItems)
                gui.addPane(paginatedPane)

                // Navigation
                val navPane = StaticPane(0, 5, 9, 1)
                navPane.addItem(GuiItem(createItem(Material.ARROW, "&7Previous", emptyList())) {
                    if (paginatedPane.page > 0) {
                        paginatedPane.page = paginatedPane.page - 1
                        gui.update()
                    }
                }, 0, 0)
                navPane.addItem(GuiItem(createItem(Material.ARROW, "&7Next", emptyList())) {
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
                    "&cNo Transactions",
                    listOf("", "&7No transaction history found")
                )), 4, 0)
                gui.addPane(emptyPane)
            }

            // Back button
            val bottomPane = StaticPane(0, 5, 9, 1)
            bottomPane.addItem(GuiItem(createItem(Material.DARK_OAK_DOOR, "&7Back to ATM", emptyList())) {
                showMainMenu(player, atm, card)
            }, 4, 0)
            gui.addPane(bottomPane)

            gui.show(player)
        }
    }

    // ==================== OPEN ACCOUNT MENU ====================

    private fun showOpenAccountMenu(player: Player, atm: ATM, card: Card) {
        val gui = ChestGui(4, ChatColor.translateAlternateColorCodes('&',"&1&lOpen New Account"))
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.LIME_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        val accountTypes = listOf(
            Triple(1, 1, AccountType.WALLET),
            Triple(2, 1, AccountType.CHECKING),
            Triple(4, 1, AccountType.SAVINGS),
            Triple(6, 1, AccountType.BUSINESS),
            Triple(7, 1, AccountType.STOCK)
        )

        for ((x, y, type) in accountTypes) {
            val material = when (type) {
                AccountType.WALLET -> Material.LEATHER
                AccountType.CHECKING -> Material.GOLD_INGOT
                AccountType.SAVINGS -> Material.DIAMOND
                AccountType.BUSINESS -> Material.EMERALD_BLOCK
                AccountType.STOCK -> Material.AMETHYST_SHARD
            }

            contentPane.addItem(GuiItem(createItem(
                material,
                "&6&l${type.displayName}",
                listOf(
                    "",
                    "&7${type.description}",
                    "",
                    "&7Interest Rate: &a${type.interestRate}%",
                    "&7Monthly Fee: &c${Messages.formatCurrency(type.monthlyFee)}",
                    "&7Min Balance: &e${Messages.formatCurrency(type.minBalance)}",
                    "&7Overdraft: ${if (type.allowsOverdraft) "&aYes" else "&cNo"}",
                    "",
                    "&aClick to open"
                )
            )) {
                plugin.launch {
                    val account = accountService.createAccount(player.uniqueId, type)
                    if (account != null) {
                        player.sendMessage(Messages.success("${type.displayName} opened!"))
                        player.sendMessage(Messages.info("Account #: ${account.accountNumber}"))
                        showAccountsMenu(player, atm, card)
                    } else {
                        player.sendMessage(Messages.error("You already have a ${type.displayName}!"))
                    }
                }
            }, x, y)
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "&7Back", emptyList())) {
            showMainMenu(player, atm, card)
        }, 4, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== TRANSACTION PROCESSING ====================

    private fun processWithdrawal(player: Player, atm: ATM, card: Card, amount: Double) {
        plugin.launch {
            // Get player's wallet
            val wallet = accountService.getPrimaryAccount(player.uniqueId)
            if (wallet == null) {
                player.sendMessage(Messages.error("You don't have a wallet!"))
                showMainMenu(player, atm, card)
                return@launch
            }

            // Can't withdraw from wallet to wallet
            if (card.linkedAccountNumber == wallet.accountNumber) {
                player.sendMessage(Messages.error("This is your wallet! Use a card linked to another account."))
                showMainMenu(player, atm, card)
                return@launch
            }

            val result = atmService.withdraw(atm.atmId, card.linkedAccountNumber, amount, player.uniqueId)

            if (result.success) {
                // Deposit to wallet (this is the "cash" balance)
                val depositSuccess = accountService.deposit(wallet.accountNumber, result.amount, player.uniqueId, "ATM Withdrawal")
                
                if (depositSuccess) {
                    // Give physical money items (don't update wallet again - already done above)
                    val moneyResult = moneyService.giveMoneyToPlayer(player, result.amount, false, updateWallet = false)
                    
                    player.sendMessage(Messages.success("Withdrew ${Messages.formatCurrency(result.amount)}"))
                    if (result.fee > 0) {
                        player.sendMessage(Messages.info("ATM fee: ${Messages.formatCurrency(result.fee)}"))
                    }
                    if (moneyResult.itemsGiven > 0) {
                        player.sendMessage(Messages.info("💵 Received ${moneyResult.itemsGiven} bill(s)"))
                    }
                    val walletBalance = accountService.getBalance(wallet.accountNumber) ?: 0.0
                    player.sendMessage(Messages.info("💰 Cash on hand: ${Messages.formatCurrency(walletBalance)}"))
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f)
                } else {
                    // Refund if wallet deposit failed
                    accountService.deposit(card.linkedAccountNumber, result.amount, player.uniqueId, "Withdrawal refund")
                    player.sendMessage(Messages.error("Could not add to wallet. Transaction cancelled."))
                }
                
                val newBalance = accountService.getBalance(card.linkedAccountNumber) ?: 0.0
                player.sendMessage(Messages.info("Account balance: ${Messages.formatCurrency(newBalance)}"))
            } else {
                player.sendMessage(Messages.error(result.message))
            }

            // Refresh and return to main menu
            val refreshedAtm = atmService.getATM(atm.atmId) ?: atm
            showMainMenu(player, refreshedAtm, card)
        }
    }

    private fun processDeposit(player: Player, atm: ATM, card: Card, amount: Double) {
        plugin.launch {
            // Get player's wallet
            val wallet = accountService.getPrimaryAccount(player.uniqueId)
            if (wallet == null) {
                player.sendMessage(Messages.error("You don't have a wallet!"))
                showDepositMenu(player, atm, card)
                return@launch
            }

            // Can't deposit from wallet to wallet
            if (card.linkedAccountNumber == wallet.accountNumber) {
                player.sendMessage(Messages.error("This is your wallet! Use a card linked to another account."))
                showDepositMenu(player, atm, card)
                return@launch
            }

            // Check if player has enough in wallet
            if (wallet.balance < amount) {
                player.sendMessage(Messages.error("Insufficient cash! You have ${Messages.formatCurrency(wallet.balance)} in your wallet"))
                showDepositMenu(player, atm, card)
                return@launch
            }
            
            // Transfer from wallet to linked account
            val result = accountService.transfer(
                wallet.accountNumber,
                card.linkedAccountNumber,
                amount,
                player.uniqueId,
                "ATM Deposit"
            )

            when (result) {
                net.crewco.Banking.services.sdata.TransferResult.SUCCESS -> {
                    // Take physical items from player (don't update wallet - already done via transfer)
                    moneyService.takeMoneyFromPlayer(player, amount, updateWallet = false)
                    
                    player.sendMessage(Messages.success("Deposited ${Messages.formatCurrency(amount)}"))
                    val newBalance = accountService.getBalance(card.linkedAccountNumber) ?: 0.0
                    val walletBalance = accountService.getBalance(wallet.accountNumber) ?: 0.0
                    player.sendMessage(Messages.info("Account balance: ${Messages.formatCurrency(newBalance)}"))
                    player.sendMessage(Messages.info("💰 Cash remaining: ${Messages.formatCurrency(walletBalance)}"))
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f)
                }
                net.crewco.Banking.services.sdata.TransferResult.INSUFFICIENT_FUNDS -> {
                    player.sendMessage(Messages.error("Insufficient cash in wallet!"))
                }
                else -> {
                    player.sendMessage(Messages.error("Deposit failed: ${result.name}"))
                }
            }

            // Refresh and return to main menu
            val refreshedAtm = atmService.getATM(atm.atmId) ?: atm
            showMainMenu(player, refreshedAtm, card)
        }
    }

    /**
     * Process deposit of all wallet cash to linked account
     */
    private fun processDepositAll(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            // Get player's wallet
            val wallet = accountService.getPrimaryAccount(player.uniqueId)
            if (wallet == null) {
                player.sendMessage(Messages.error("You don't have a wallet!"))
                showDepositMenu(player, atm, card)
                return@launch
            }

            // Can't deposit from wallet to wallet
            if (card.linkedAccountNumber == wallet.accountNumber) {
                player.sendMessage(Messages.error("This is your wallet! Use a card linked to another account."))
                showDepositMenu(player, atm, card)
                return@launch
            }

            if (wallet.balance <= 0) {
                player.sendMessage(Messages.error("You have no cash to deposit!"))
                showDepositMenu(player, atm, card)
                return@launch
            }
            
            val depositAmount = wallet.balance
            
            // Transfer all from wallet to linked account
            val result = accountService.transfer(
                wallet.accountNumber,
                card.linkedAccountNumber,
                depositAmount,
                player.uniqueId,
                "ATM Deposit All"
            )

            when (result) {
                net.crewco.Banking.services.sdata.TransferResult.SUCCESS -> {
                    // Remove all physical items (don't update wallet - already done via transfer)
                    moneyService.collectAllMoney(player, updateWallet = false)
                    
                    player.sendMessage(Messages.success("Deposited ${Messages.formatCurrency(depositAmount)}"))
                    val newBalance = accountService.getBalance(card.linkedAccountNumber) ?: 0.0
                    player.sendMessage(Messages.info("Account balance: ${Messages.formatCurrency(newBalance)}"))
                    player.sendMessage(Messages.info("💰 Cash remaining: ${Messages.formatCurrency(0.0)}"))
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f)
                }
                else -> {
                    player.sendMessage(Messages.error("Deposit failed: ${result.name}"))
                }
            }

            val refreshedAtm = atmService.getATM(atm.atmId) ?: atm
            showMainMenu(player, refreshedAtm, card)
        }
    }

    private fun processTransfer(player: Player, atm: ATM, card: Card, toAccount: BankAccount, amount: Double) {
        plugin.launch {
            val result = accountService.transfer(
                card.linkedAccountNumber,
                toAccount.accountNumber,
                amount,
                player.uniqueId,
                "ATM Transfer"
            )

            when (result) {
                net.crewco.Banking.services.sdata.TransferResult.SUCCESS -> {
                    player.sendMessage(Messages.success("Transferred ${Messages.formatCurrency(amount)} to ${toAccount.accountNumber}"))
                    val newBalance = accountService.getBalance(card.linkedAccountNumber) ?: 0.0
                    player.sendMessage(Messages.info("New balance: ${Messages.formatCurrency(newBalance)}"))
                }
                net.crewco.Banking.services.sdata.TransferResult.INSUFFICIENT_FUNDS -> {
                    player.sendMessage(Messages.error("Insufficient funds!"))
                }
                else -> {
                    player.sendMessage(Messages.error("Transfer failed: ${result.name}"))
                }
            }

            showMainMenu(player, atm, card)
        }
    }

    // ==================== UTILITY ====================

    private fun createItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList()
    ): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        // Display name
        meta.setDisplayName(
            if (name.contains('&'))
                ChatColor.translateAlternateColorCodes('&', name)
            else
                name
        )

        // Lore
        if (lore.isNotEmpty()) {
            meta.lore = lore.map { line ->
                if (line.contains('&'))
                    ChatColor.translateAlternateColorCodes('&', line)
                else
                    line
            }
        }

        item.itemMeta = meta
        return item
    }
}