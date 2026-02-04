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
import net.crewco.Banking.data.models.*
import net.crewco.Banking.data.cdata.CardType
import net.crewco.Banking.data.ldata.LoanStatus
import net.crewco.Banking.data.ldata.LoanType
import net.crewco.Banking.util.BankCardItem
import net.crewco.Banking.util.Messages
import org.bukkit.Material
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
        val gui = ChestGui(5, "§1§lCrewCo ATM §8- §7${card.getMaskedNumber()}")
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
            "§6§l💳 ${card.cardType.displayName}",
            listOf(
                "",
                "§7Card: §f${card.getMaskedNumber()}",
                "§7Account: §f${card.linkedAccountNumber}",
                "§7Expires: §f${card.expirationDate}",
                "",
                "§7Daily Limit: §e${Messages.formatCurrency(card.dailyLimit)}",
                "§7Spent Today: §c${Messages.formatCurrency(card.spentToday)}"
            )
        )), 4, 0)

        // ===== ROW 1: Quick Actions =====

        // Check Balance
        contentPane.addItem(GuiItem(createItem(
            Material.GOLD_INGOT,
            "§e§lCheck Balance",
            listOf(
                "",
                "§7View your account balance",
                "",
                "§aClick to check"
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
            "§c§lWithdraw Cash",
            listOf(
                "",
                "§7Withdraw money from your account",
                "§6ATM Fee: ${Messages.formatCurrency(atm.transactionFee)}",
                "",
                "§aClick to withdraw"
            )
        )) {
            showWithdrawMenu(player, atm, card)
        }, 3, 1)

        // Deposit
        contentPane.addItem(GuiItem(createItem(
            Material.LIME_WOOL,
            "§a§lDeposit Cash",
            listOf(
                "",
                "§7Deposit money to your account",
                "",
                "§aClick to deposit"
            )
        )) {
            showDepositMenu(player, atm, card)
        }, 5, 1)

        // Transfer
        contentPane.addItem(GuiItem(createItem(
            Material.ENDER_PEARL,
            "§d§lTransfer Funds",
            listOf(
                "",
                "§7Transfer between your accounts",
                "",
                "§aClick to transfer"
            )
        )) {
            showTransferMenu(player, atm, card)
        }, 7, 1)

        // ===== ROW 2: Banking Services =====

        // My Accounts
        contentPane.addItem(GuiItem(createItem(
            Material.CHEST,
            "§6§lMy Accounts",
            listOf(
                "",
                "§7View and manage your",
                "§7bank accounts",
                "",
                "§aClick to view"
            )
        )) {
            showAccountsMenu(player, atm, card)
        }, 1, 2)

        // My Cards
        contentPane.addItem(GuiItem(createItem(
            Material.MAP,
            "§b§lMy Cards",
            listOf(
                "",
                "§7View and manage your",
                "§7debit/credit cards",
                "",
                "§aClick to view"
            )
        )) {
            showCardsMenu(player, atm, card)
        }, 3, 2)

        // My Loans
        contentPane.addItem(GuiItem(createItem(
            Material.BOOK,
            "§e§lMy Loans",
            listOf(
                "",
                "§7View your loans and",
                "§7make payments",
                "",
                "§aClick to view"
            )
        )) {
            showLoansMenu(player, atm, card)
        }, 5, 2)

        // Transaction History
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§f§lTransaction History",
            listOf(
                "",
                "§7View recent transactions",
                "§7on this account",
                "",
                "§aClick to view"
            )
        )) {
            showTransactionHistory(player, atm, card)
        }, 7, 2)

        // ===== ROW 3: Info & Exit =====

        // ATM Info
        contentPane.addItem(GuiItem(createItem(
            Material.OAK_SIGN,
            "§b§lATM Information",
            listOf(
                "",
                "§7ATM ID: §f${atm.atmId}",
                "§7Max Withdrawal: §f${Messages.formatCurrency(atm.maxWithdrawal)}",
                "§7Available Cash: §f${Messages.formatCurrency(atm.cash)}",
                "§7Transaction Fee: §f${Messages.formatCurrency(atm.transactionFee)}"
            )
        )), 3, 3)

        // Open New Account
        contentPane.addItem(GuiItem(createItem(
            Material.LIME_DYE,
            "§a§lOpen New Account",
            listOf(
                "",
                "§7Open a new bank account",
                "",
                "§aClick to view options"
            )
        )) {
            showOpenAccountMenu(player, atm, card)
        }, 5, 3)

        // Exit
        contentPane.addItem(GuiItem(createItem(
            Material.BARRIER,
            "§c§lExit ATM",
            listOf(
                "",
                "§7Return your card and exit"
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
        val gui = ChestGui(4, "§1§lATM §8- §cWithdraw")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.RED_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Header
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§c§lSelect Withdrawal Amount",
            listOf(
                "",
                "§7Card: §f${card.getMaskedNumber()}",
                "§7Max per transaction: §f${Messages.formatCurrency(atm.maxWithdrawal)}",
                "§7ATM Fee: §6${Messages.formatCurrency(atm.transactionFee)}"
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
            val nameColor = if (canWithdraw) "§a" else "§c"

            val lore = mutableListOf(
                "",
                "§6Fee: ${Messages.formatCurrency(atm.transactionFee)}",
                "§eTotal Deducted: ${Messages.formatCurrency(amount + atm.transactionFee)}",
                ""
            )
            lore.add(if (canWithdraw) "§aClick to withdraw" else "§cUnavailable")

            contentPane.addItem(GuiItem(createItem(material, "$nameColor§l${Messages.formatCurrency(amount)}", lore)) {
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
            val nameColor = if (canWithdraw) "§a" else "§c"

            contentPane.addItem(GuiItem(createItem(
                material,
                "$nameColor§l${Messages.formatCurrency(amount)}",
                listOf("", if (canWithdraw) "§aClick to withdraw" else "§cUnavailable")
            )) {
                if (canWithdraw) {
                    processWithdrawal(player, atm, card, amount)
                }
            }, x, y)
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "§7Back", listOf("§8Return to main menu"))) {
            showMainMenu(player, atm, card)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== DEPOSIT MENU ====================

    private fun showDepositMenu(player: Player, atm: ATM, card: Card) {
        val gui = ChestGui(4, "§1§lATM §8- §aDeposit")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.LIME_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Header
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§a§lSelect Deposit Amount",
            listOf(
                "",
                "§7Card: §f${card.getMaskedNumber()}",
                "§7Account: §f${card.linkedAccountNumber}"
            )
        )), 4, 0)

        // Amount buttons
        val amounts = listOf(100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0, 25000.0, 50000.0)
        val slots = listOf(
            1 to 1, 2 to 1, 3 to 1, 5 to 1, 6 to 1, 7 to 1,
            2 to 2, 4 to 2, 6 to 2
        )

        for ((index, amount) in amounts.withIndex()) {
            if (index < slots.size) {
                val (x, y) = slots[index]
                val material = if (amount >= 10000.0) Material.GOLD_BLOCK else Material.GOLD_INGOT

                contentPane.addItem(GuiItem(createItem(
                    material,
                    "§6§l${Messages.formatCurrency(amount)}",
                    listOf("", "§aClick to deposit")
                )) {
                    processDeposit(player, atm, card, amount)
                }, x, y)
            }
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "§7Back", listOf("§8Return to main menu"))) {
            showMainMenu(player, atm, card)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== TRANSFER MENU ====================

    private fun showTransferMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val accounts = accountService.getAccountsByPlayer(player.uniqueId)
                .filter { it.accountNumber != card.linkedAccountNumber }

            val gui = ChestGui(4, "§1§lATM §8- §dTransfer")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.PURPLE_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            val contentPane = StaticPane(0, 0, 9, 4)

            // Header
            contentPane.addItem(GuiItem(createItem(
                Material.PAPER,
                "§d§lSelect Destination Account",
                listOf(
                    "",
                    "§7Transfer FROM: §f${card.linkedAccountNumber}",
                    "",
                    "§7Select an account to transfer to"
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
                        "§f${account.accountName}",
                        listOf(
                            "§7Type: §f${account.accountType.displayName}",
                            "§7Account: §e${account.accountNumber}",
                            "§7Balance: §a${Messages.formatCurrency(account.balance)}",
                            "",
                            "§aClick to select"
                        )
                    )) {
                        showTransferAmountMenu(player, atm, card, account)
                    }, x, 1)
                }
            } else {
                contentPane.addItem(GuiItem(createItem(
                    Material.BARRIER,
                    "§cNo Other Accounts",
                    listOf("", "§7You need another account to transfer to")
                )), 4, 1)
            }

            // Back button
            contentPane.addItem(GuiItem(createItem(Material.ARROW, "§7Back", emptyList())) {
                showMainMenu(player, atm, card)
            }, 0, 3)

            gui.addPane(contentPane)
            gui.show(player)
        }
    }

    private fun showTransferAmountMenu(player: Player, atm: ATM, card: Card, toAccount: BankAccount) {
        val gui = ChestGui(4, "§1§lTransfer to §f${toAccount.accountNumber}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.PURPLE_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Header
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§d§lSelect Transfer Amount",
            listOf(
                "",
                "§7From: §f${card.linkedAccountNumber}",
                "§7To: §f${toAccount.accountNumber} (${toAccount.accountType.displayName})"
            )
        )), 4, 0)

        // Amount buttons
        val amounts = listOf(100.0, 500.0, 1000.0, 5000.0, 10000.0, 50000.0)
        val slots = listOf(1 to 1, 2 to 1, 3 to 1, 5 to 1, 6 to 1, 7 to 1)

        for ((index, amount) in amounts.withIndex()) {
            val (x, y) = slots[index]
            contentPane.addItem(GuiItem(createItem(
                Material.ENDER_PEARL,
                "§d§l${Messages.formatCurrency(amount)}",
                listOf("", "§aClick to transfer")
            )) {
                processTransfer(player, atm, card, toAccount, amount)
            }, x, y)
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "§7Back", emptyList())) {
            showTransferMenu(player, atm, card)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== ACCOUNTS MENU ====================

    private fun showAccountsMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val accounts = accountService.getAccountsByPlayer(player.uniqueId)

            val gui = ChestGui(6, "§1§lMy Accounts")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            // Header
            val headerPane = StaticPane(0, 0, 9, 1)
            headerPane.addItem(GuiItem(createItem(
                Material.GOLD_BLOCK,
                "§6§lYour Bank Accounts",
                listOf(
                    "",
                    "§7Total Accounts: §f${accounts.size}",
                    "§7Total Balance: §a${Messages.formatCurrency(accounts.sumOf { it.balance })}"
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
                    val statusColor = if (account.frozen) "§c" else "§a"

                    GuiItem(createItem(
                        material,
                        "§f${account.accountName}${if (isLinked) " §7(This Card)" else ""}",
                        listOf(
                            "§7Type: §f${account.accountType.displayName}",
                            "",
                            "§7Account #: §e${account.accountNumber}",
                            "§7Routing #: §e${account.routingNumber}",
                            "",
                            "§7Balance: §a${Messages.formatCurrency(account.balance)}",
                            "§7Available: §a${Messages.formatCurrency(account.getAvailableBalance())}",
                            "",
                            "§7Status: $statusColor${if (account.frozen) "FROZEN" else "Active"}"
                        )
                    ))
                }

                paginatedPane.populateWithGuiItems(accountItems)
                gui.addPane(paginatedPane)

                // Navigation
                val navPane = StaticPane(0, 5, 9, 1)
                navPane.addItem(GuiItem(createItem(Material.ARROW, "§7Previous", emptyList())) {
                    if (paginatedPane.page > 0) {
                        paginatedPane.page -= 1
                        gui.update()
                    }
                }, 0, 0)
                navPane.addItem(GuiItem(createItem(Material.ARROW, "§7Next", emptyList())) {
                    if (paginatedPane.page < paginatedPane.pages - 1) {
                        paginatedPane.page += 1
                        gui.update()
                    }
                }, 8, 0)
                gui.addPane(navPane)
            }

            // Back button
            val bottomPane = StaticPane(0, 5, 9, 1)
            bottomPane.addItem(GuiItem(createItem(Material.DARK_OAK_DOOR, "§7Back to ATM", emptyList())) {
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

            val gui = ChestGui(6, "§1§lMy Cards")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            // Header
            val headerPane = StaticPane(0, 0, 9, 1)
            headerPane.addItem(GuiItem(createItem(
                Material.PAPER,
                "§b§l💳 Your Cards",
                listOf(
                    "",
                    "§7Total Cards: §f${cards.size}",
                    "§7Active: §a${cards.count { it.active && !it.frozen }}"
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
                        !c.active -> "§8"
                        c.frozen -> "§b"
                        c.isExpired() -> "§c"
                        else -> "§a"
                    }

                    val status = when {
                        !c.active -> "Cancelled"
                        c.frozen -> "Frozen"
                        c.isExpired() -> "Expired"
                        else -> "Active"
                    }

                    GuiItem(createItem(
                        material,
                        "§f${c.cardType.displayName}${if (isCurrentCard) " §a(In Use)" else ""}",
                        listOf(
                            "",
                            "§7Card: §e${c.getMaskedNumber()}",
                            "§7Expires: §f${c.expirationDate}",
                            "§7Linked Account: §f${c.linkedAccountNumber}",
                            "",
                            "§7Daily Limit: §e${Messages.formatCurrency(c.dailyLimit)}",
                            "§7Spent Today: §c${Messages.formatCurrency(c.spentToday)}",
                            "",
                            "§7Status: $statusColor$status",
                            "",
                            if (c.active && !c.frozen) "§eClick for options" else ""
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
            bottomPane.addItem(GuiItem(createItem(Material.DARK_OAK_DOOR, "§7Back to ATM", emptyList())) {
                showMainMenu(player, atm, card)
            }, 4, 0)
            gui.addPane(bottomPane)

            gui.show(player)
        }
    }

    private fun showCardOptions(player: Player, atm: ATM, sessionCard: Card, selectedCard: Card) {
        val gui = ChestGui(4, "§1§lCard §8- §f${selectedCard.getMaskedNumber()}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.CYAN_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Card info
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§6§l${selectedCard.cardType.displayName}",
            listOf(
                "",
                "§7Card Number:",
                "§e${selectedCard.cardNumber}",
                "",
                "§7CVV: §f${selectedCard.cvv}",
                "§7Expires: §f${selectedCard.expirationDate}"
            )
        )), 4, 0)

        // Get physical card
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§a§lGet Physical Card",
            listOf(
                "",
                "§7Receive a card item",
                "§7to use at other ATMs",
                "",
                "§aClick to receive"
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
                "§6§lUnfreeze Card",
                listOf("", "§7Unfreeze this card", "", "§aClick to unfreeze")
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
                "§b§lFreeze Card",
                listOf("", "§7Temporarily disable card", "", "§eClick to freeze")
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
            "§c§lCancel Card",
            listOf("", "§cPermanently deactivate", "§cThis cannot be undone!")
        )) {
            plugin.launch {
                cardService.cancelCard(selectedCard.cardNumber)
                player.sendMessage(Messages.warning("Card cancelled permanently."))
                showCardsMenu(player, atm, sessionCard)
            }
        }, 6, 2)

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "§7Back", emptyList())) {
            showCardsMenu(player, atm, sessionCard)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== LOANS MENU ====================

    private fun showLoansMenu(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val loans = loanService.getLoansByPlayer(player.uniqueId)

            val gui = ChestGui(6, "§1§lMy Loans")
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
                "§e§l📋 Your Loans",
                listOf(
                    "",
                    "§7Total Loans: §f${loans.size}",
                    "§7Active Loans: §e${activeLoans.size}",
                    "§7Total Owed: §c${Messages.formatCurrency(totalOwed)}"
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
                        LoanStatus.ACTIVE -> "§a"
                        LoanStatus.PENDING -> "§e"
                        LoanStatus.APPROVED -> "§b"
                        LoanStatus.PAID_OFF -> "§2"
                        LoanStatus.DEFAULTED -> "§c"
                        LoanStatus.REJECTED -> "§4"
                        LoanStatus.CANCELLED -> "§8"
                    }

                    val lore = mutableListOf(
                        "",
                        "§7Loan ID: §e${loan.loanId}",
                        "",
                        "§7Principal: §f${Messages.formatCurrency(loan.principalAmount)}",
                        "§7Remaining: §c${Messages.formatCurrency(loan.remainingBalance)}",
                        "§7Monthly Payment: §e${Messages.formatCurrency(loan.monthlyPayment)}",
                        "",
                        "§7Status: $statusColor${loan.status.name}"
                    )

                    if (loan.status == LoanStatus.ACTIVE) {
                        lore.add("")
                        lore.add("§eClick to make payment")
                    }

                    GuiItem(createItem(material, "§f${loan.loanType.displayName}", lore)) {
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
                    "§cNo Loans",
                    listOf("", "§7Use §e/loan apply §7to apply")
                )), 4, 0)
                gui.addPane(emptyPane)
            }

            // Bottom buttons
            val bottomPane = StaticPane(0, 5, 9, 1)
            bottomPane.addItem(GuiItem(createItem(Material.BOOK, "§e§lLoan Types Info", emptyList())) {
                showLoanTypesInfo(player, atm, card)
            }, 2, 0)
            bottomPane.addItem(GuiItem(createItem(Material.DARK_OAK_DOOR, "§7Back to ATM", emptyList())) {
                showMainMenu(player, atm, card)
            }, 4, 0)
            gui.addPane(bottomPane)

            gui.show(player)
        }
    }

    private fun showLoanPaymentMenu(player: Player, atm: ATM, card: Card, loan: Loan) {
        val gui = ChestGui(4, "§1§lLoan Payment §8- §f${loan.loanId}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.YELLOW_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Loan info
        contentPane.addItem(GuiItem(createItem(
            Material.BOOK,
            "§6§l${loan.loanType.displayName}",
            listOf(
                "",
                "§7Loan ID: §e${loan.loanId}",
                "§7Principal: §f${Messages.formatCurrency(loan.principalAmount)}",
                "§7Remaining: §c${Messages.formatCurrency(loan.remainingBalance)}",
                "",
                "§7Monthly Payment: §e${Messages.formatCurrency(loan.monthlyPayment)}",
                "§7Months Left: §f${loan.monthsRemaining}/${loan.termMonths}"
            )
        )), 4, 0)

        // Make monthly payment
        contentPane.addItem(GuiItem(createItem(
            Material.GOLD_INGOT,
            "§a§lMake Monthly Payment",
            listOf(
                "",
                "§7Pay: §e${Messages.formatCurrency(loan.monthlyPayment)}",
                "",
                "§aClick to pay"
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
            "§2§lPay Off Entire Loan",
            listOf(
                "",
                "§7Pay remaining: §c${Messages.formatCurrency(loan.remainingBalance)}",
                "",
                "§aClick to pay off"
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
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "§7Back", emptyList())) {
            showLoansMenu(player, atm, card)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    private fun showLoanTypesInfo(player: Player, atm: ATM, card: Card) {
        val gui = ChestGui(4, "§1§lLoan Types")
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
                "§6§l${type.displayName}",
                listOf(
                    "",
                    "§7Base Rate: §e${type.baseInterestRate}%",
                    "§7Max Amount: §f${Messages.formatCurrency(type.maxAmount)}",
                    "§7Max Term: §f${type.maxTermMonths} months",
                    "§7Collateral: ${if (type.requiresCollateral) "§cRequired" else "§aNot Required"}",
                    "",
                    "§eUse: §f/loan apply ${type.name.lowercase()} ..."
                )
            )), x, y)
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "§7Back", emptyList())) {
            showLoansMenu(player, atm, card)
        }, 4, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== TRANSACTION HISTORY ====================

    private fun showTransactionHistory(player: Player, atm: ATM, card: Card) {
        plugin.launch {
            val transactions = transactionService.getTransactionHistory(card.linkedAccountNumber, 28)

            val gui = ChestGui(6, "§1§lTransaction History")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            // Header
            val headerPane = StaticPane(0, 0, 9, 1)
            headerPane.addItem(GuiItem(createItem(
                Material.PAPER,
                "§f§lTransaction History",
                listOf(
                    "",
                    "§7Account: §f${card.linkedAccountNumber}",
                    "§7Showing last ${transactions.size} transactions"
                )
            )), 4, 0)
            gui.addPane(headerPane)

            if (transactions.isNotEmpty()) {
                val paginatedPane = PaginatedPane(1, 1, 7, 4)

                val txItems = transactions.map { tx ->
                    val isIncoming = tx.toAccountNumber == card.linkedAccountNumber
                    val material = if (isIncoming) Material.LIME_CONCRETE else Material.RED_CONCRETE
                    val arrow = if (isIncoming) "§a↓" else "§c↑"
                    val amountColor = if (isIncoming) "§a+" else "§c-"

                    GuiItem(createItem(
                        material,
                        "$arrow §f${tx.type.name.replace("_", " ")}",
                        listOf(
                            "",
                            "§7Amount: $amountColor${Messages.formatCurrency(tx.amount)}",
                            if (tx.fee > 0) "§7Fee: §c${Messages.formatCurrency(tx.fee)}" else "",
                            "",
                            "§7Transaction ID:",
                            "§8${tx.transactionId}",
                            "",
                            "§7Date: §f${tx.createdAt.toLocalDate()}",
                            "§7Time: §f${tx.createdAt.toLocalTime().toString().substringBefore(".")}"
                        ).filter { it.isNotEmpty() }
                    ))
                }

                paginatedPane.populateWithGuiItems(txItems)
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
                    "§cNo Transactions",
                    listOf("", "§7No transaction history found")
                )), 4, 0)
                gui.addPane(emptyPane)
            }

            // Back button
            val bottomPane = StaticPane(0, 5, 9, 1)
            bottomPane.addItem(GuiItem(createItem(Material.DARK_OAK_DOOR, "§7Back to ATM", emptyList())) {
                showMainMenu(player, atm, card)
            }, 4, 0)
            gui.addPane(bottomPane)

            gui.show(player)
        }
    }

    // ==================== OPEN ACCOUNT MENU ====================

    private fun showOpenAccountMenu(player: Player, atm: ATM, card: Card) {
        val gui = ChestGui(4, "§1§lOpen New Account")
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
                "§6§l${type.displayName}",
                listOf(
                    "",
                    "§7${type.description}",
                    "",
                    "§7Interest Rate: §a${type.interestRate}%",
                    "§7Monthly Fee: §c${Messages.formatCurrency(type.monthlyFee)}",
                    "§7Min Balance: §e${Messages.formatCurrency(type.minBalance)}",
                    "§7Overdraft: ${if (type.allowsOverdraft) "§aYes" else "§cNo"}",
                    "",
                    "§aClick to open"
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
        contentPane.addItem(GuiItem(createItem(Material.ARROW, "§7Back", emptyList())) {
            showMainMenu(player, atm, card)
        }, 4, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    // ==================== TRANSACTION PROCESSING ====================

    private fun processWithdrawal(player: Player, atm: ATM, card: Card, amount: Double) {
        plugin.launch {
            val result = atmService.withdraw(atm.atmId, card.linkedAccountNumber, amount, player.uniqueId)

            if (result.success) {
                player.sendMessage(Messages.success("Withdrew ${Messages.formatCurrency(result.amount)}"))
                if (result.fee > 0) {
                    player.sendMessage(Messages.info("ATM fee: ${Messages.formatCurrency(result.fee)}"))
                }
                val newBalance = accountService.getBalance(card.linkedAccountNumber) ?: 0.0
                player.sendMessage(Messages.info("New balance: ${Messages.formatCurrency(newBalance)}"))
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
            val result = atmService.deposit(atm.atmId, card.linkedAccountNumber, amount, player.uniqueId)

            if (result.success) {
                player.sendMessage(Messages.success("Deposited ${Messages.formatCurrency(result.amount)}"))
                val newBalance = accountService.getBalance(card.linkedAccountNumber) ?: 0.0
                player.sendMessage(Messages.info("New balance: ${Messages.formatCurrency(newBalance)}"))
            } else {
                player.sendMessage(Messages.error(result.message))
            }

            // Refresh and return to main menu
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