// src/main/java/net/crewco/Banking/gui/LoanGUI.kt
package net.crewco.Banking.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.loanService
import net.crewco.Banking.data.ldata.LoanStatus
import net.crewco.Banking.data.ldata.LoanType
import net.crewco.Banking.data.models.Loan
import net.crewco.Banking.util.Messages
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class LoanGUI(private val plugin: Startup) {

    fun openMainMenu(player: Player) {
        plugin.launch {
            val loans = loanService.getLoansByPlayer(player.uniqueId)
            showLoansMenu(player, loans)
        }
    }

    private fun showLoansMenu(player: Player, loans: List<Loan>) {
        val gui = ChestGui(6, "&1&lCrewCo Bank &8- &fYour Loans")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
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
            "&6&l📋 Your Loans",
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
                    "&7Type: &f${loan.loanType.displayName}",
                    "",
                    "&7Principal: &f${Messages.formatCurrency(loan.principalAmount)}",
                    "&7Interest Rate: &e${loan.interestRate}%",
                    "&7Monthly Payment: &e${Messages.formatCurrency(loan.monthlyPayment)}",
                    "",
                    "&7Remaining: &c${Messages.formatCurrency(loan.remainingBalance)}",
                    "&7Total Paid: &a${Messages.formatCurrency(loan.totalPaid)}",
                    "",
                    "&7Status: $statusColor${loan.status.name}"
                )

                if (loan.status == LoanStatus.ACTIVE && loan.nextPaymentDue != null) {
                    lore.add("")
                    lore.add("&7Next Payment: &f${loan.nextPaymentDue!!.toLocalDate()}")
                    if (loan.isOverdue()) {
                        lore.add("&c⚠ OVERDUE!")
                    }
                }

                if (loan.status == LoanStatus.ACTIVE) {
                    lore.add("")
                    lore.add("&eClick for options")
                }

                GuiItem(createItem(material, "&f${loan.loanType.displayName}", lore)) {
                    if (loan.status == LoanStatus.ACTIVE) {
                        showLoanOptions(player, loan)
                    }
                }
            }

            paginatedPane.populateWithGuiItems(loanItems)
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
                "&cNo Loans",
                listOf(
                    "",
                    "&7You don't have any loans.",
                    "&7Use &e/loan apply &7to apply for one!"
                )
            )), 4, 0)
            gui.addPane(emptyPane)
        }

        // Loan types info button
        val bottomPane = StaticPane(0, 5, 9, 1)
        bottomPane.addItem(GuiItem(createItem(
            Material.BOOK,
            "&e&lView Loan Types",
            listOf("", "&7See available loan options")
        )) {
            showLoanTypes(player)
        }, 3, 0)

        // Close button
        bottomPane.addItem(GuiItem(createItem(
            Material.BARRIER,
            "&c&lClose",
            emptyList()
        )) {
            player.closeInventory()
        }, 5, 0)
        gui.addPane(bottomPane)

        gui.show(player)
    }

    private fun showLoanOptions(player: Player, loan: Loan) {
        val gui = ChestGui(4, "&1&lLoan &8- &f${loan.loanId}")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
        val background = OutlinePane(0, 0, 9, 4, Pane.Priority.LOWEST)
        background.addItem(GuiItem(createItem(Material.GRAY_STAINED_GLASS_PANE, " ")))
        background.setRepeat(true)
        gui.addPane(background)

        val contentPane = StaticPane(0, 0, 9, 4)

        // Loan summary
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

        // Make payment
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
            player.closeInventory()
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
            player.closeInventory()
            plugin.launch {
                val result = loanService.makePayment(loan.loanId, loan.remainingBalance)
                if (result.success) {
                    player.sendMessage(Messages.success("🎉 Loan paid off in full!"))
                } else {
                    player.sendMessage(Messages.error(result.message))
                }
            }
        }, 6, 2)

        // Back button
        contentPane.addItem(GuiItem(createItem(
            Material.ARROW,
            "&7Back",
            emptyList()
        )) {
            openMainMenu(player)
        }, 0, 3)

        gui.addPane(contentPane)
        gui.show(player)
    }

    private fun showLoanTypes(player: Player) {
        val gui = ChestGui(4, "&1&lAvailable Loan Types")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        // Background
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
                    "&7${type.name.lowercase().replaceFirstChar { it.uppercase() }} loans",
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
        contentPane.addItem(GuiItem(createItem(
            Material.ARROW,
            "&7Back",
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