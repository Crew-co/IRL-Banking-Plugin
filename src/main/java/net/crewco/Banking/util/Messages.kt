package net.crewco.Banking.util

import net.crewco.Banking.data.edata.TransactionType
import net.crewco.Banking.data.ldata.LoanStatus
import net.crewco.Banking.data.ldata.LoanType
import net.crewco.Banking.data.models.ATM
import net.crewco.Banking.data.models.BankAccount
import net.crewco.Banking.data.models.Card
import net.crewco.Banking.data.models.Loan
import net.crewco.Banking.data.models.Transaction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

object Messages {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    fun formatCurrency(amount: Double): String = currencyFormatter.format(amount)

    fun header(title: String): Component = Component.text()
        .append(Component.text("═══════ ", NamedTextColor.GOLD))
        .append(Component.text(title, NamedTextColor.YELLOW, TextDecoration.BOLD))
        .append(Component.text(" ═══════", NamedTextColor.GOLD))
        .build()

    fun success(message: String): Component = Component.text()
        .append(Component.text("✓ ", NamedTextColor.GREEN))
        .append(Component.text(message, NamedTextColor.GREEN))
        .build()

    fun error(message: String): Component = Component.text()
        .append(Component.text("✗ ", NamedTextColor.RED))
        .append(Component.text(message, NamedTextColor.RED))
        .build()

    fun info(message: String): Component = Component.text()
        .append(Component.text("ℹ ", NamedTextColor.AQUA))
        .append(Component.text(message, NamedTextColor.GRAY))
        .build()

    /**
     * Info message with a label and value - uses proper Adventure components
     */
    fun infoLabeled(label: String, value: String): Component = Component.text()
        .append(Component.text("ℹ ", NamedTextColor.AQUA))
        .append(Component.text("$label: ", NamedTextColor.GRAY))
        .append(Component.text(value, NamedTextColor.WHITE))
        .build()

    /**
     * Account type header for admin display
     */
    fun accountTypeHeader(typeName: String): Component = Component.text()
        .append(Component.text("ℹ ", NamedTextColor.AQUA))
        .append(Component.text("$typeName Account", NamedTextColor.YELLOW))
        .build()

    /**
     * Wallet info display with proper Adventure components
     */
    fun walletInfo(accountNumber: String, balance: Double): Component = Component.text()
        .append(Component.text("ℹ ", NamedTextColor.AQUA))
        .append(Component.text("Active Wallet: ", NamedTextColor.GREEN))
        .append(Component.text(accountNumber, NamedTextColor.WHITE))
        .append(Component.newline())
        .append(Component.text("ℹ ", NamedTextColor.AQUA))
        .append(Component.text("Wallet Balance: ", NamedTextColor.GREEN))
        .append(Component.text(formatCurrency(balance), NamedTextColor.WHITE))
        .build()

    fun warning(message: String): Component = Component.text()
        .append(Component.text("⚠ ", NamedTextColor.GOLD))
        .append(Component.text(message, NamedTextColor.YELLOW))
        .build()

    fun welcome(): Component = Component.text()
        .append(Component.text("\n"))
        .append(header("Welcome to CrewCo Bank!"))
        .append(Component.text("\n"))
        .append(info("A personal wallet has been created for you."))
        .append(Component.text("\n"))
        .append(info("Use /bank to view your accounts."))
        .append(Component.text("\n"))
        .append(info("Use /bank open <type> to open new accounts."))
        .append(Component.text("\n"))
        .build()

    fun noAccounts(): Component = info("You don't have any bank accounts. Use /bank open <type> to create one!")

    fun accountNotFound(): Component = error("Account not found or doesn't belong to you!")

    fun accountLine(name: String, type: String, accountNumber: String, balance: Double): Component =
        Component.text()
            .append(Component.text("  • ", NamedTextColor.GRAY))
            .append(Component.text(name, NamedTextColor.WHITE))
            .append(Component.text(" ($type)", NamedTextColor.DARK_GRAY))
            .append(Component.text(" #$accountNumber", NamedTextColor.GRAY))
            .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
            .append(Component.text(formatCurrency(balance), NamedTextColor.GREEN))
            .build()

    fun totalBalance(total: Double): Component = Component.text()
        .append(Component.text("  Total: ", NamedTextColor.GOLD))
        .append(Component.text(formatCurrency(total), NamedTextColor.GREEN, TextDecoration.BOLD))
        .build()

    fun balanceInfo(accountName: String, balance: Double, available: Double): Component =
        Component.text()
            .append(Component.text("$accountName Balance: ", NamedTextColor.GRAY))
            .append(Component.text(formatCurrency(balance), NamedTextColor.GREEN))
            .append(Component.text(" (Available: ${formatCurrency(available)})", NamedTextColor.DARK_GRAY))
            .build()

    fun accountDetails(account: BankAccount): Component = Component.text()
        .append(Component.text("  Name: ", NamedTextColor.GRAY))
        .append(Component.text(account.accountName, NamedTextColor.WHITE))
        .append(Component.text("\n"))
        .append(Component.text("  Type: ", NamedTextColor.GRAY))
        .append(Component.text(account.accountType.displayName, NamedTextColor.AQUA))
        .append(Component.text("\n"))
        .append(Component.text("  Account #: ", NamedTextColor.GRAY))
        .append(Component.text(account.accountNumber, NamedTextColor.YELLOW))
        .append(Component.text("\n"))
        .append(Component.text("  Routing #: ", NamedTextColor.GRAY))
        .append(Component.text(account.routingNumber, NamedTextColor.YELLOW))
        .append(Component.text("\n"))
        .append(Component.text("  Balance: ", NamedTextColor.GRAY))
        .append(Component.text(formatCurrency(account.balance), NamedTextColor.GREEN))
        .append(Component.text("\n"))
        .append(Component.text("  Status: ", NamedTextColor.GRAY))
        .append(if (account.frozen)
            Component.text("FROZEN", NamedTextColor.RED)
        else Component.text("Active", NamedTextColor.GREEN))
        .build()

    fun depositSuccess(amount: Double, newBalance: Double): Component =
        success("Deposited ${formatCurrency(amount)}. New balance: ${formatCurrency(newBalance)}")

    fun withdrawSuccess(amount: Double, newBalance: Double): Component =
        success("Withdrew ${formatCurrency(amount)}. New balance: ${formatCurrency(newBalance)}")

    fun transferSuccess(amount: Double, toAccount: String): Component =
        success("Transferred ${formatCurrency(amount)} to account #$toAccount")

    fun transactionLine(tx: Transaction): Component {
        val arrow = when (tx.type) {
            TransactionType.DEPOSIT, TransactionType.ATM_DEPOSIT,
            TransactionType.LOAN_DISBURSEMENT, TransactionType.INTEREST_CREDIT -> "↓"
            else -> "↑"
        }
        val color = when (tx.type) {
            TransactionType.DEPOSIT, TransactionType.ATM_DEPOSIT,
            TransactionType.LOAN_DISBURSEMENT, TransactionType.INTEREST_CREDIT -> NamedTextColor.GREEN
            else -> NamedTextColor.RED
        }

        return Component.text()
            .append(Component.text("  $arrow ", color))
            .append(Component.text(tx.type.name.replace("_", " "), NamedTextColor.WHITE))
            .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
            .append(Component.text(formatCurrency(tx.amount), color))
            .append(Component.text(" (${tx.createdAt.format(dateFormatter)})", NamedTextColor.DARK_GRAY))
            .build()
    }

    fun cardLine(card: Card): Component = Component.text()
        .append(Component.text("  • ", NamedTextColor.GRAY))
        .append(Component.text(card.cardType.displayName, NamedTextColor.AQUA))
        .append(Component.text(" ${card.getMaskedNumber()}", NamedTextColor.WHITE))
        .append(
            Component.text(" (${if (card.frozen) "FROZEN" else if (!card.active) "INACTIVE" else "Active"})",
                if (card.frozen || !card.active) NamedTextColor.RED else NamedTextColor.GREEN))
        .build()

    fun cardDetails(card: Card): Component = Component.text()
        .append(Component.text("  Type: ", NamedTextColor.GRAY))
        .append(Component.text(card.cardType.displayName, NamedTextColor.AQUA))
        .append(Component.text("\n"))
        .append(Component.text("  Number: ", NamedTextColor.GRAY))
        .append(Component.text(card.cardNumber, NamedTextColor.YELLOW))
        .append(Component.text("\n"))
        .append(Component.text("  CVV: ", NamedTextColor.GRAY))
        .append(Component.text(card.cvv, NamedTextColor.YELLOW))
        .append(Component.text("\n"))
        .append(Component.text("  Expires: ", NamedTextColor.GRAY))
        .append(Component.text(card.expirationDate.toString(), NamedTextColor.WHITE))
        .append(Component.text("\n"))
        .append(Component.text("  Daily Limit: ", NamedTextColor.GRAY))
        .append(Component.text(formatCurrency(card.dailyLimit), NamedTextColor.GREEN))
        .append(Component.text("\n"))
        .append(Component.text("  Spent Today: ", NamedTextColor.GRAY))
        .append(Component.text(formatCurrency(card.spentToday), NamedTextColor.GOLD))
        .build()

    fun loanLine(loan: Loan): Component = Component.text()
        .append(Component.text("  • ", NamedTextColor.GRAY))
        .append(Component.text(loan.loanType.displayName, NamedTextColor.AQUA))
        .append(Component.text(" #${loan.loanId}", NamedTextColor.DARK_GRAY))
        .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
        .append(Component.text(formatCurrency(loan.remainingBalance), NamedTextColor.GOLD))
        .append(Component.text(" remaining", NamedTextColor.GRAY))
        .append(
            Component.text(" (${loan.status.name})",
                when (loan.status) {
                    LoanStatus.ACTIVE -> NamedTextColor.GREEN
                    LoanStatus.PENDING, LoanStatus.APPROVED -> NamedTextColor.YELLOW
                    LoanStatus.PAID_OFF -> NamedTextColor.AQUA
                    else -> NamedTextColor.RED
                }))
        .build()

    fun loanDetails(loan: Loan): Component = Component.text()
        .append(Component.text("  Loan ID: ", NamedTextColor.GRAY))
        .append(Component.text(loan.loanId, NamedTextColor.YELLOW))
        .append(Component.text("\n"))
        .append(Component.text("  Type: ", NamedTextColor.GRAY))
        .append(Component.text(loan.loanType.displayName, NamedTextColor.AQUA))
        .append(Component.text("\n"))
        .append(Component.text("  Principal: ", NamedTextColor.GRAY))
        .append(Component.text(formatCurrency(loan.principalAmount), NamedTextColor.WHITE))
        .append(Component.text("\n"))
        .append(Component.text("  Interest Rate: ", NamedTextColor.GRAY))
        .append(Component.text("${loan.interestRate}%", NamedTextColor.GOLD))
        .append(Component.text("\n"))
        .append(Component.text("  Monthly Payment: ", NamedTextColor.GRAY))
        .append(Component.text(formatCurrency(loan.monthlyPayment), NamedTextColor.GREEN))
        .append(Component.text("\n"))
        .append(Component.text("  Remaining: ", NamedTextColor.GRAY))
        .append(Component.text(formatCurrency(loan.remainingBalance), NamedTextColor.RED))
        .append(Component.text("\n"))
        .append(Component.text("  Months Left: ", NamedTextColor.GRAY))
        .append(Component.text("${loan.monthsRemaining}/${loan.termMonths}", NamedTextColor.WHITE))
        .append(Component.text("\n"))
        .append(Component.text("  Status: ", NamedTextColor.GRAY))
        .append(Component.text(loan.status.name, NamedTextColor.YELLOW))
        .build()

    fun loanTypeInfo(type: LoanType): Component = Component.text()
        .append(Component.text("  ${type.displayName}", NamedTextColor.AQUA, TextDecoration.BOLD))
        .append(Component.text("\n"))
        .append(Component.text("    Rate: ${type.baseInterestRate}% | ", NamedTextColor.GRAY))
        .append(Component.text("Max: ${formatCurrency(type.maxAmount)} | ", NamedTextColor.GRAY))
        .append(Component.text("Term: ${type.maxTermMonths} months", NamedTextColor.GRAY))
        .append(if (type.requiresCollateral) Component.text(" | Collateral Required", NamedTextColor.RED) else Component.empty())
        .build()

    fun loanApplicationDetails(loan: Loan): Component = Component.text()
        .append(Component.text("  Loan ID: ${loan.loanId}", NamedTextColor.YELLOW))
        .append(Component.text("\n"))
        .append(Component.text("  Amount: ${formatCurrency(loan.principalAmount)}", NamedTextColor.WHITE))
        .append(Component.text("\n"))
        .append(Component.text("  Monthly Payment: ${formatCurrency(loan.monthlyPayment)}", NamedTextColor.GREEN))
        .append(Component.text("\n"))
        .append(Component.text("  Total Interest: ${formatCurrency(loan.calculateTotalInterest())}", NamedTextColor.GOLD))
        .build()

    fun atmLine(atm: ATM, distance: Double): Component = Component.text()
        .append(Component.text("  • ", NamedTextColor.GRAY))
        .append(Component.text("ATM ${atm.atmId}", NamedTextColor.AQUA))
        .append(Component.text(" - ${String.format("%.1f", distance)} blocks away", NamedTextColor.GRAY))
        .append(Component.text(" (Fee: ${formatCurrency(atm.transactionFee)})", NamedTextColor.DARK_GRAY))
        .build()

    // Admin messages
    fun adminAccountDetails(account: BankAccount): Component = Component.text()
        .append(accountDetails(account))
        .append(Component.text("\n"))
        .append(Component.text("  Owner UUID: ", NamedTextColor.GRAY))
        .append(Component.text(account.uuid.toString(), NamedTextColor.DARK_GRAY))
        .append(Component.text("\n"))
        .append(Component.text("  Created: ", NamedTextColor.GRAY))
        .append(Component.text(account.createdAt.format(dateFormatter), NamedTextColor.DARK_GRAY))
        .build()

    fun adminLoanLine(loan: Loan): Component = Component.text()
        .append(loanLine(loan))
        .append(Component.text("\n"))
        .append(Component.text("    Borrower: ${loan.borrowerUuid}", NamedTextColor.DARK_GRAY))
        .build()

    fun adminAtmLine(atm: ATM): Component = Component.text()
        .append(Component.text("  • ", NamedTextColor.GRAY))
        .append(Component.text(atm.atmId, NamedTextColor.AQUA))
        .append(Component.text(" @ ${atm.location.world?.name}:${atm.location.blockX},${atm.location.blockY},${atm.location.blockZ}", NamedTextColor.GRAY))
        .append(Component.text(" Cash: ${formatCurrency(atm.cash)}", NamedTextColor.GREEN))
        .append(
            Component.text(" (${if (atm.active) "Active" else "Inactive"})",
                if (atm.active) NamedTextColor.GREEN else NamedTextColor.RED))
        .build()
}