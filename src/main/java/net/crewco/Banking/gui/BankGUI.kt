// src/main/java/net/crewco/Banking/gui/BankGUI.kt
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
import net.crewco.Banking.Startup.Companion.plugin
import net.crewco.Banking.Startup.Companion.transactionService
import net.crewco.Banking.data.edata.TransactionType
import net.crewco.Banking.data.models.AccountType
import net.crewco.Banking.data.models.BankAccount
import net.crewco.Banking.util.Messages
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class BankGUI(private val plugin: Startup) {

    fun openMainMenu(player: Player) {
        plugin.launch {
            val accounts = accountService.getAccountsByPlayer(player.uniqueId)
            showAccountsMenu(player, accounts)
        }
    }

    private fun showAccountsMenu(player: Player, accounts: List<BankAccount>) {
        val gui = ChestGui(6, "§1§lCrewCo Bank §8- §fYour Accounts")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
        val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        // Header
        val headerPane = StaticPane(0, 0, 9, 1)
        headerPane.addItem(GuiItem(createItem(
            Material.GOLD_BLOCK,
            "§6§lCrewCo Bank",
            listOf(
                "",
                "§7Total Accounts: §f${accounts.size}",
                "§7Total Balance: §a${Messages.formatCurrency(accounts.sumOf { it.balance })}"
            )
        )), 4, 0)
        gui.addPane(headerPane)

        // Accounts list with pagination
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

                val statusColor = if (account.frozen) "§c" else "§a"
                val status = if (account.frozen) "FROZEN" else "Active"

                GuiItem(createItem(
                    material,
                    "§f${account.accountName}",
                    listOf(
                        "§7Type: §f${account.accountType.displayName}",
                        "",
                        "§7Account #: §e${account.accountNumber}",
                        "§7Routing #: §e${account.routingNumber}",
                        "",
                        "§7Balance: §a${Messages.formatCurrency(account.balance)}",
                        "§7Available: §a${Messages.formatCurrency(account.getAvailableBalance())}",
                        "",
                        "§7Status: $statusColor$status",
                        "",
                        "§eClick for details"
                    )
                )) {
                    showAccountDetails(player, account)
                }
            }

            paginatedPane.populateWithGuiItems(accountItems)
            gui.addPane(paginatedPane)

            // Navigation buttons
            val navPane = StaticPane(0, 5, 9, 1)

            // Previous page
            navPane.addItem(GuiItem(createItem(
                Material.ARROW,
                "§7Previous Page",
                emptyList()
            )) {
                if (paginatedPane.page > 0) {
                    paginatedPane.page = paginatedPane.page - 1
                    gui.update()
                }
            }, 0, 0)

            // Next page
            navPane.addItem(GuiItem(createItem(
                Material.ARROW,
                "§7Next Page",
                emptyList()
            )) {
                if (paginatedPane.page < paginatedPane.pages - 1) {
                    paginatedPane.page = paginatedPane.page + 1
                    gui.update()
                }
            }, 8, 0)

            gui.addPane(navPane)
        } else {
            // No accounts message
            val emptyPane = StaticPane(0, 2, 9, 2)
            emptyPane.addItem(GuiItem(createItem(
                Material.BARRIER,
                "§cNo Accounts",
                listOf(
                    "",
                    "§7You don't have any bank accounts.",
                    "§7Use §e/bank open <type> §7to create one!"
                )
            )), 4, 0)
            gui.addPane(emptyPane)
        }

        // Bottom bar - Open new account
        val bottomPane = StaticPane(0, 5, 9, 1)
        bottomPane.addItem(GuiItem(createItem(
            Material.LIME_DYE,
            "§a§lOpen New Account",
            listOf(
                "",
                "§7Click to view available account types"
            )
        )) {
            showOpenAccountMenu(player)
        }, 4, 0)

        // Close button
        bottomPane.addItem(GuiItem(createItem(
            Material.BARRIER,
            "§c§lClose",
            emptyList()
        )) {
            player.closeInventory()
        }, 8, 0)

        gui.addPane(bottomPane)
        gui.show(player)
    }

    private fun showAccountDetails(player: Player, account: BankAccount) {
        val gui = ChestGui(6, "§1§lAccount §8- §f${account.accountName}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
        val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.GRAY_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 6)

        // Account info header
        contentPane.addItem(GuiItem(createItem(
            Material.BOOK,
            "§6§l${account.accountName}",
            listOf(
                "",
                "§7Type: §f${account.accountType.displayName}",
                "§7Account #: §e${account.accountNumber}",
                "§7Routing #: §e${account.routingNumber}",
                "",
                "§7Interest Rate: §a${account.accountType.interestRate}%",
                "§7Monthly Fee: §c${Messages.formatCurrency(account.accountType.monthlyFee)}",
                "§7Daily Limit: §e${if (account.accountType.maxDailyWithdrawal > 0) Messages.formatCurrency(account.accountType.maxDailyWithdrawal) else "Unlimited"}"
            )
        )), 1, 1)

        // Balance display
        contentPane.addItem(GuiItem(createItem(
            Material.GOLD_BLOCK,
            "§a§lBalance",
            listOf(
                "",
                "§7Current: §a${Messages.formatCurrency(account.balance)}",
                "§7Available: §a${Messages.formatCurrency(account.getAvailableBalance())}",
                "§7Overdraft Limit: §e${Messages.formatCurrency(account.overdraftLimit)}"
            )
        )), 4, 1)

        // Status
        val statusMaterial = if (account.frozen) Material.ICE else Material.EMERALD
        val statusColor = if (account.frozen) "§c" else "§a"
        contentPane.addItem(GuiItem(createItem(
            statusMaterial,
            "${statusColor}§lStatus",
            listOf(
                "",
                "§7Account is: $statusColor${if (account.frozen) "FROZEN" else "Active"}",
                if (account.frozen) "§7Contact support to unfreeze" else "§7All features available"
            )
        )), 7, 1)

        // View transactions button
        contentPane.addItem(GuiItem(createItem(
            Material.PAPER,
            "§e§lTransaction History",
            listOf(
                "",
                "§7View recent transactions",
                "",
                "§aClick to view"
            )
        )) {
            showTransactionHistory(player, account)
        }, 2, 3)

        // Quick deposit (informational)
        contentPane.addItem(GuiItem(createItem(
            Material.HOPPER,
            "§a§lDeposit",
            listOf(
                "",
                "§7Deposit money to this account",
                "",
                "§eUse: §f/bank deposit <amount> ${account.accountNumber}"
            )
        )), 4, 3)

        // Quick withdraw (informational)
        contentPane.addItem(GuiItem(createItem(
            Material.CHEST,
            "§c§lWithdraw",
            listOf(
                "",
                "§7Withdraw money from this account",
                "",
                "§eUse: §f/bank withdraw <amount> ${account.accountNumber}"
            )
        )), 6, 3)

        // Back button
        contentPane.addItem(GuiItem(createItem(
            Material.ARROW,
            "§7Back to Accounts",
            emptyList()
        )) {
            openMainMenu(player)
        }, 0, 5)

        // Close button
        contentPane.addItem(GuiItem(createItem(
            Material.BARRIER,
            "§c§lClose",
            emptyList()
        )) {
            player.closeInventory()
        }, 8, 5)

        gui.addPane(contentPane)
        gui.show(player)
    }

    private fun showTransactionHistory(player: Player, account: BankAccount) {
        plugin.launch {
            val transactions = transactionService.getTransactionHistory(account.accountNumber, 28)

            val gui = ChestGui(6, "§1§lTransactions §8- §f${account.accountNumber}")
            gui.setOnGlobalClick { event -> event.isCancelled = true }

            // Background
            val background = OutlinePane(0, 0, 9, 6, Pane.Priority.LOWEST)
            background.addItem(GuiItem(createItem(Material.BLACK_STAINED_GLASS_PANE, " ")))
            background.setRepeat(true)
            gui.addPane(background)

            if (transactions.isNotEmpty()) {
                val paginatedPane = PaginatedPane(1, 1, 7, 4)

                val txItems = transactions.map { tx ->
                    val isIncoming = tx.toAccountNumber == account.accountNumber
                    val material = if (isIncoming) Material.LIME_CONCRETE else Material.RED_CONCRETE
                    val arrow = if (isIncoming) "§a↓" else "§c↑"
                    val amountColor = if (isIncoming) "§a+" else "§c-"

                    val typeName = tx.type.name.replace("_", " ")

                    GuiItem(createItem(
                        material,
                        "$arrow §f$typeName",
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
                    listOf("", "§7No transaction history found.")
                )), 4, 0)
                gui.addPane(emptyPane)
            }

            // Back button
            val bottomPane = StaticPane(0, 5, 9, 1)
            bottomPane.addItem(GuiItem(createItem(
                Material.DARK_OAK_DOOR,
                "§7Back to Account",
                emptyList()
            )) {
                showAccountDetails(player, account)
            }, 4, 0)
            gui.addPane(bottomPane)

            gui.show(player)
        }
    }

    private fun showOpenAccountMenu(player: Player) {
        val gui = ChestGui(4, "§1§lOpen New Account")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.GRAY_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Account type options
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
                    "§7Daily Limit: §e${if (type.maxDailyWithdrawal > 0) Messages.formatCurrency(type.maxDailyWithdrawal) else "Unlimited"}",
                    "§7Overdraft: ${if (type.allowsOverdraft) "§aYes" else "§cNo"}",
                    "",
                    "§aClick to open"
                )
            )) {
                player.closeInventory()
                plugin.launch {
                    val account = accountService.createAccount(player.uniqueId, type)
                    if (account != null) {
                        player.sendMessage(Messages.success("${type.displayName} opened successfully!"))
                        player.sendMessage(Messages.info("Account #: ${account.accountNumber}"))
                    } else {
                        player.sendMessage(Messages.error("You already have a ${type.displayName}!"))
                    }
                }
            }, x, y)
        }

        // Back button
        contentPane.addItem(GuiItem(createItem(
            Material.ARROW,
            "§7Back",
            emptyList()
        )) {
            openMainMenu(player)
        }, 4, 3)

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