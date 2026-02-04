package net.crewco.Banking.data.models
// src/main/java/net/crewco/Banking/data/models/AccountType.kt
enum class AccountType(
    val displayName: String,
    val description: String,
    val interestRate: Double,      // Annual interest rate (for savings, etc.)
    val monthlyFee: Double,        // Monthly maintenance fee
    val maxDailyWithdrawal: Double, // Max daily withdrawal (-1 = unlimited)
    val minBalance: Double,        // Minimum balance requirement
    val allowsOverdraft: Boolean
) {
    WALLET(
        displayName = "Wallet",
        description = "Personal cash wallet with no fees",
        interestRate = 0.0,
        monthlyFee = 0.0,
        maxDailyWithdrawal = -1.0,
        minBalance = 0.0,
        allowsOverdraft = false
    ),
    CHECKING(
        displayName = "Checking Account",
        description = "Standard checking account for everyday transactions",
        interestRate = 0.01,
        monthlyFee = 5.0,
        maxDailyWithdrawal = 10000.0,
        minBalance = 0.0,
        allowsOverdraft = false
    ),
    SAVINGS(
        displayName = "Savings Account",
        description = "High-yield savings account for long-term storage",
        interestRate = 3.5,
        monthlyFee = 0.0,
        maxDailyWithdrawal = 5000.0,
        minBalance = 100.0,
        allowsOverdraft = false
    ),
    BUSINESS(
        displayName = "Business Account",
        description = "Business account with higher limits and features",
        interestRate = 1.0,
        monthlyFee = 25.0,
        maxDailyWithdrawal = 50000.0,
        minBalance = 500.0,
        allowsOverdraft = true
    ),
    STOCK(
        displayName = "Investment Account",
        description = "Account for stock and investment holdings",
        interestRate = 0.0, // Returns depend on investments
        monthlyFee = 10.0,
        maxDailyWithdrawal = 25000.0,
        minBalance = 1000.0,
        allowsOverdraft = false
    )
}