package net.crewco.Banking.data.cdata

enum class CardUseResult {
    SUCCESS,
    CARD_INACTIVE,
    CARD_FROZEN,
    CARD_EXPIRED,
    DAILY_LIMIT_EXCEEDED,
    INVALID_PIN,
    INSUFFICIENT_FUNDS
}