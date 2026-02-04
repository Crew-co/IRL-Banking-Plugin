// src/main/java/net/crewco/Banking/data/repositories/CardRepository.kt
package net.crewco.Banking.data.repositories

import net.crewco.Banking.data.cdata.CardType
import net.crewco.Banking.data.database.DatabaseManager
import net.crewco.Banking.data.models.Card
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CardRepository(private val db: DatabaseManager) {

    suspend fun create(card: Card): Boolean {
        val affected = db.execute(
            """
            INSERT INTO bank_cards 
            (card_number, cvv, linked_account_number, owner_uuid, card_type, 
             expiration_date, pin, daily_limit, spent_today, active, frozen)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            card.cardNumber,
            card.cvv,
            card.linkedAccountNumber,
            card.ownerUuid.toString(),
            card.cardType.name,
            card.expirationDate.toString(),
            card.pin,
            card.dailyLimit,
            card.spentToday,
            card.active,
            card.frozen
        )
        return affected > 0
    }

    suspend fun findByCardNumber(cardNumber: String): Card? {
        val results = db.query(
            "SELECT * FROM bank_cards WHERE card_number = ?",
            cardNumber
        )
        return results.firstOrNull()?.toCard()
    }

    suspend fun findByOwner(ownerUuid: UUID): List<Card> {
        val results = db.query(
            "SELECT * FROM bank_cards WHERE owner_uuid = ?",
            ownerUuid.toString()
        )
        return results.map { it.toCard() }
    }

    suspend fun findByAccount(accountNumber: String): List<Card> {
        val results = db.query(
            "SELECT * FROM bank_cards WHERE linked_account_number = ?",
            accountNumber
        )
        return results.map { it.toCard() }
    }

    suspend fun updateSpentToday(cardNumber: String, amount: Double): Boolean {
        val affected = db.execute(
            """
            UPDATE bank_cards 
            SET spent_today = ?, last_used_date = ? 
            WHERE card_number = ?
            """.trimIndent(),
            amount,
            LocalDateTime.now().toString(),
            cardNumber
        )
        return affected > 0
    }

    suspend fun setFrozen(cardNumber: String, frozen: Boolean): Boolean {
        val affected = db.execute(
            "UPDATE bank_cards SET frozen = ? WHERE card_number = ?",
            frozen,
            cardNumber
        )
        return affected > 0
    }

    suspend fun setActive(cardNumber: String, active: Boolean): Boolean {
        val affected = db.execute(
            "UPDATE bank_cards SET active = ? WHERE card_number = ?",
            active,
            cardNumber
        )
        return affected > 0
    }

    suspend fun updatePin(cardNumber: String, newPin: String): Boolean {
        val affected = db.execute(
            "UPDATE bank_cards SET pin = ? WHERE card_number = ?",
            newPin,
            cardNumber
        )
        return affected > 0
    }

    suspend fun delete(cardNumber: String): Boolean {
        val affected = db.execute(
            "DELETE FROM bank_cards WHERE card_number = ?",
            cardNumber
        )
        return affected > 0
    }

    suspend fun resetDailySpending(): Int {
        val affected = db.execute(
            """
            UPDATE bank_cards 
            SET spent_today = 0 
            WHERE date(last_used_date) < date('now')
            """.trimIndent()
        )
        return affected.toInt()
    }

    private fun Map<String, Any?>.toCard(): Card {
        return Card(
            id = (this["id"] as Number).toLong(),
            cardNumber = this["card_number"] as String,
            cvv = this["cvv"] as String,
            linkedAccountNumber = this["linked_account_number"] as String,
            ownerUuid = UUID.fromString(this["owner_uuid"] as String),
            cardType = CardType.valueOf(this["card_type"] as String),
            expirationDate = LocalDate.parse(this["expiration_date"] as String),
            pin = this["pin"] as String,
            dailyLimit = (this["daily_limit"] as Number).toDouble(),
            spentToday = (this["spent_today"] as Number).toDouble(),
            lastUsedDate = (this["last_used_date"] as? String)?.let { parseDateTime(it) },
            active = (this["active"] as Number).toInt() == 1,
            frozen = (this["frozen"] as Number).toInt() == 1,
            createdAt = parseDateTime(this["created_at"] as? String)
        )
    }

    private fun parseDateTime(value: String?): LocalDateTime {
        return try {
            if (value != null) LocalDateTime.parse(value) else LocalDateTime.now()
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}