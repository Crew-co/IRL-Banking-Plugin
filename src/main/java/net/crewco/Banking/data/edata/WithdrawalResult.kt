package net.crewco.Banking.data.edata

enum class WithdrawalResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    DAILY_LIMIT_EXCEEDED,
    ACCOUNT_FROZEN
}