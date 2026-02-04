// src/main/java/net/crewco/Banking/data/models/Card.kt
package net.crewco.Banking.data.models

import net.crewco.Banking.data.cdata.CardType
import net.crewco.Banking.data.cdata.CardUseResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Card(
    val id: Long = 0,
    val cardNumber: String,                  // 16-digit card number
    val cvv: String,                         // 3-digit CVV
    val linkedAccountNumber: String,
    val ownerUuid: UUID,
    val cardType: CardType,
    val expirationDate: LocalDate,
    var pin: String,                         // 4-digit PIN (hashed)
    var dailyLimit: Double,
    var spentToday: Double = 0.0,
    var lastUsedDate: LocalDateTime? = null,
    var active: Boolean = true,
    var frozen: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun isExpired(): Boolean = LocalDate.now().isAfter(expirationDate)

    fun canSpend(amount: Double): CardUseResult {
        if (!active) return CardUseResult.CARD_INACTIVE
        if (frozen) return CardUseResult.CARD_FROZEN
        if (isExpired()) return CardUseResult.CARD_EXPIRED
        if (spentToday + amount > dailyLimit) return CardUseResult.DAILY_LIMIT_EXCEEDED
        return CardUseResult.SUCCESS
    }

    fun getMaskedNumber(): String {
        return "**** **** **** ${cardNumber.takeLast(4)}"
    }
}

