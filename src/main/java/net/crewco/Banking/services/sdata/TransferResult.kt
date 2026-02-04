package net.crewco.Banking.services.sdata

enum class TransferResult {
    SUCCESS,
    INVALID_AMOUNT,
    SAME_ACCOUNT,
    FROM_ACCOUNT_NOT_FOUND,
    TO_ACCOUNT_NOT_FOUND,
    FROM_ACCOUNT_FROZEN,
    TO_ACCOUNT_FROZEN,
    INSUFFICIENT_FUNDS,
    DAILY_LIMIT_EXCEEDED,
    FAILED
}