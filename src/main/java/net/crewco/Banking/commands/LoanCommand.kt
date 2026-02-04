// src/main/java/net/crewco/Banking/commands/LoanCommand.kt
package net.crewco.Banking.commands

import net.crewco.Banking.data.ldata.LoanStatus
import net.crewco.Banking.data.ldata.LoanType
import com.google.inject.Inject
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.Startup.Companion.loanService
import net.crewco.Banking.services.AccountService
import net.crewco.Banking.services.LoanService
import net.crewco.Banking.util.Messages
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.*
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import java.util.stream.Stream

class LoanCommand  {

    // ==================== Main Command ====================

    @Command("loan [action] [args]")
    @CommandDescription("Loan commands - use /loan help for more info")
    @Permission("banking.loan.use")
    suspend fun loan(
        player: Player,
        @Argument("action", suggestions = "loanActions") action: String?,
        @Argument("args", suggestions = "loanArgs") args: Array<String>?
    ) {
        val subcommand = action?.lowercase() ?: "list"
        val arguments = args ?: emptyArray()

        when (subcommand) {
            "list", "loans" -> listLoans(player)
            "types" -> showLoanTypes(player)
            "apply", "request" -> applyLoan(
                player,
                arguments.getOrNull(0),
                arguments.getOrNull(1),
                arguments.getOrNull(2),
                arguments.getOrNull(3),
                arguments.drop(4).joinToString(" ").ifBlank { null }
            )
            "info", "details" -> loanInfo(player, arguments.getOrNull(0))
            "pay", "payment" -> payLoan(player, arguments.getOrNull(0), arguments.getOrNull(1))
            "payoff" -> payoffLoan(player, arguments.getOrNull(0))
            "help" -> showHelp(player)
            else -> {
                player.sendMessage(Messages.error("Unknown subcommand: $subcommand"))
                showHelp(player)
            }
        }
    }

    // ==================== Suggestions ====================

    @Suggestions("loanActions")
    fun actionSuggestions(
        context: CommandContext<Player>,
        input: String
    ): Stream<String> {
        val actions = mutableListOf(
            "list",
            "types",
            "apply",
            "info",
            "pay",
            "payoff",
            "help"
        )
        return actions.stream().filter { it.startsWith(input.lowercase()) }
    }

    @Suggestions("loanArgs")
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
            "apply", "request" -> {
                LoanType.entries.forEach { type ->
                    suggestions.add(type.name.lowercase())
                }
            }
            "info", "details", "pay", "payment", "payoff" -> {
                suggestions.add("<loan_id>")
            }
        }

        return suggestions.stream().filter { it.startsWith(input.lowercase()) }
    }

    // ==================== Subcommand Handlers ====================

    private suspend fun listLoans(player: Player) {
        val loans = loanService.getLoansByPlayer(player.uniqueId)

        if (loans.isEmpty()) {
            player.sendMessage(Messages.info("You don't have any loans."))
            return
        }

        player.sendMessage(Messages.header("Your Loans"))

        for (loan in loans) {
            player.sendMessage(Messages.loanLine(loan))
        }
    }

    private fun showLoanTypes(player: Player) {
        player.sendMessage(Messages.header("Available Loan Types"))

        for (type in LoanType.entries) {
            player.sendMessage(Messages.loanTypeInfo(type))
        }
    }

    private suspend fun applyLoan(
        player: Player,
        loanTypeArg: String?,
        amountArg: String?,
        termArg: String?,
        accountNumber: String?,
        collateral: String?
    ) {
        if (loanTypeArg == null || amountArg == null || termArg == null || accountNumber == null) {
            player.sendMessage(Messages.error("Usage: /loan apply <type> <amount> <term_months> <account> [collateral]"))
            player.sendMessage(Messages.info("Loan types: ${LoanType.entries.joinToString(", ") { it.name.lowercase() }}"))
            return
        }

        val loanType = try {
            LoanType.valueOf(loanTypeArg.uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendMessage(Messages.error("Invalid loan type: $loanTypeArg"))
            player.sendMessage(Messages.info("Loan types: ${LoanType.entries.joinToString(", ") { it.name.lowercase() }}"))
            return
        }

        val amount = amountArg.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            player.sendMessage(Messages.error("Invalid amount: $amountArg"))
            return
        }

        val termMonths = termArg.toIntOrNull()
        if (termMonths == null || termMonths <= 0) {
            player.sendMessage(Messages.error("Invalid term: $termArg"))
            return
        }

        val account = accountService.getAccount(accountNumber)
        if (account == null || account.uuid != player.uniqueId) {
            player.sendMessage(Messages.error("Account not found or doesn't belong to you!"))
            return
        }

        val result = loanService.applyForLoan(
            player.uniqueId,
            accountNumber,
            loanType,
            amount,
            termMonths,
            collateral
        )

        if (result.success && result.loan != null) {
            player.sendMessage(Messages.success("Loan application submitted!"))
            player.sendMessage(Messages.loanApplicationDetails(result.loan))
            player.sendMessage(Messages.info("Your application is pending review."))
        } else {
            player.sendMessage(Messages.error(result.message))
        }
    }

    private suspend fun loanInfo(player: Player, loanId: String?) {
        if (loanId == null) {
            player.sendMessage(Messages.error("Usage: /loan info <loan_id>"))
            return
        }

        val loan = loanService.getLoan(loanId)

        if (loan == null || loan.borrowerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Loan not found or doesn't belong to you!"))
            return
        }

        player.sendMessage(Messages.header("Loan Details"))
        player.sendMessage(Messages.loanDetails(loan))
    }

    private suspend fun payLoan(player: Player, loanId: String?, amountArg: String?) {
        if (loanId == null) {
            player.sendMessage(Messages.error("Usage: /loan pay <loan_id> [amount]"))
            return
        }

        val loan = loanService.getLoan(loanId)

        if (loan == null || loan.borrowerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Loan not found or doesn't belong to you!"))
            return
        }

        val amount = amountArg?.toDoubleOrNull()

        val result = loanService.makePayment(loanId, amount)

        if (result.success) {
            player.sendMessage(Messages.success(result.message))
            if (result.loan?.status == LoanStatus.PAID_OFF) {
                player.sendMessage(Messages.success("🎉 Congratulations! Your loan has been fully paid off!"))
            } else {
                player.sendMessage(Messages.info("Remaining balance: ${Messages.formatCurrency(result.loan?.remainingBalance ?: 0.0)}"))
            }
        } else {
            player.sendMessage(Messages.error(result.message))
        }
    }

    private suspend fun payoffLoan(player: Player, loanId: String?) {
        if (loanId == null) {
            player.sendMessage(Messages.error("Usage: /loan payoff <loan_id>"))
            return
        }

        val loan = loanService.getLoan(loanId)

        if (loan == null || loan.borrowerUuid != player.uniqueId) {
            player.sendMessage(Messages.error("Loan not found or doesn't belong to you!"))
            return
        }

        val result = loanService.makePayment(loanId, loan.remainingBalance)

        if (result.success) {
            player.sendMessage(Messages.success("🎉 Loan paid off in full!"))
        } else {
            player.sendMessage(Messages.error(result.message))
        }
    }

    private fun showHelp(player: Player) {
        player.sendMessage(Messages.header("Loan Commands"))
        player.sendMessage(Messages.info("/loan - List all your loans"))
        player.sendMessage(Messages.info("/loan types - View available loan types"))
        player.sendMessage(Messages.info("/loan apply <type> <amount> <months> <account> [collateral]"))
        player.sendMessage(Messages.info("/loan info <loan_id> - View loan details"))
        player.sendMessage(Messages.info("/loan pay <loan_id> [amount] - Make a payment"))
        player.sendMessage(Messages.info("/loan payoff <loan_id> - Pay off entire balance"))
        player.sendMessage(Messages.info(""))
        player.sendMessage(Messages.info("Loan types: ${LoanType.entries.joinToString(", ") { it.name.lowercase() }}"))
    }
}