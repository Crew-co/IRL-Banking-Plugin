package net.crewco.Banking.data.ldata

enum class LoanType(
    val displayName: String,
    val baseInterestRate: Double,
    val maxTermMonths: Int,
    val maxAmount: Double,
    val requiresCollateral: Boolean
) {
    PERSONAL(
        displayName = "Personal Loan",
        baseInterestRate = 8.0,
        maxTermMonths = 60,
        maxAmount = 50000.0,
        requiresCollateral = false
    ),
    BUSINESS(
        displayName = "Business Loan",
        baseInterestRate = 6.5,
        maxTermMonths = 120,
        maxAmount = 500000.0,
        requiresCollateral = true
    ),
    MORTGAGE(
        displayName = "Mortgage",
        baseInterestRate = 4.5,
        maxTermMonths = 360,
        maxAmount = 1000000.0,
        requiresCollateral = true
    ),
    EMERGENCY(
        displayName = "Emergency Loan",
        baseInterestRate = 12.0,
        maxTermMonths = 12,
        maxAmount = 10000.0,
        requiresCollateral = false
    ),
    STUDENT(
        displayName = "Student Loan",
        baseInterestRate = 3.5,
        maxTermMonths = 120,
        maxAmount = 100000.0,
        requiresCollateral = false
    )
}