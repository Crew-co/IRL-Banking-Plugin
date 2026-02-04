package net.crewco.Banking.data.ldata

enum class LoanStatus {
    PENDING,      // Application submitted
    APPROVED,     // Approved, awaiting disbursement
    ACTIVE,       // Funds disbursed, loan active
    PAID_OFF,     // Fully repaid
    DEFAULTED,    // Borrower defaulted
    REJECTED,     // Application rejected
    CANCELLED     // Cancelled before disbursement
}