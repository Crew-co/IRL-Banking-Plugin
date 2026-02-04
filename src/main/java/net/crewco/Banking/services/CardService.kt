// src/main/java/net/crewco/Banking/services/CardService.kt
package net.crewco.Banking.services

import net.crewco.Banking.api.events.CardEvent
import net.crewco.Banking.data.cdata.CardType
import net.crewco.Banking.data.cdata.CardUseResult
import net.crewco.Banking.data.edata.WithdrawalResult
import net.crewco.Banking.data.models.*
import net.crewco.Banking.data.repositories.AccountRepository
import net.crewco.Banking.data.repositories.CardRepository
import net.crewco.Banking.util.BankCardItem
import org.bukkit.Bukkit
import java.time.LocalDate
import java.util.UUID

class CardService(
    private val cardRepository: CardRepository,
    private val accountRepository: AccountRepository,
    private val numberGenerator: NumberGeneratorService,
    private val transactionService: TransactionService
) {

    suspend fun issueCard(
        ownerUuid: UUID,
        linkedAccountNumber: String,
        cardType: CardType,
        pin: String,
        giveItem: Boolean = true
    ): Card? {
        // Verify account exists and belongs to player
        val account = accountRepository.findByAccountNumber(linkedAccountNumber) ?: return null
        if (account.uuid != ownerUuid) return null

        // Check card type compatibility with account type
        if (!isCardTypeCompatible(cardType, account.accountType)) return null

        val card = Card(
            cardNumber = numberGenerator.generateCardNumber(),
            cvv = numberGenerator.generateCVV(),
            linkedAccountNumber = linkedAccountNumber,
            ownerUuid = ownerUuid,
            cardType = cardType,
            expirationDate = LocalDate.now().plusYears(3),
            pin = numberGenerator.hashPin(pin),
            dailyLimit = cardType.defaultDailyLimit
        )

        val success = cardRepository.create(card)

        if (success) {
            Bukkit.getPluginManager().callEvent(CardEvent(card, CardEvent.CardAction.ISSUED))

            // Give physical card item to player if online
            if (giveItem) {
                val player = Bukkit.getPlayer(ownerUuid)
                if (player != null) {
                    val cardItem = BankCardItem.createCardItem(card)
                    player.inventory.addItem(cardItem)
                }
            }

            return card
        }

        return null
    }

    private fun isCardTypeCompatible(cardType: CardType, accountType: AccountType): Boolean {
        return when (cardType) {
            CardType.DEBIT, CardType.CREDIT -> accountType in listOf(
                AccountType.CHECKING, AccountType.SAVINGS, AccountType.WALLET
            )
            CardType.BUSINESS_DEBIT, CardType.BUSINESS_CREDIT -> accountType == AccountType.BUSINESS
            CardType.PREMIUM -> true
        }
    }

    suspend fun getCard(cardNumber: String): Card? {
        return cardRepository.findByCardNumber(cardNumber)
    }

    suspend fun getCardsByPlayer(ownerUuid: UUID): List<Card> {
        return cardRepository.findByOwner(ownerUuid)
    }

    suspend fun getCardsByAccount(accountNumber: String): List<Card> {
        return cardRepository.findByAccount(accountNumber)
    }

    suspend fun useCard(
        cardNumber: String,
        pin: String,
        amount: Double,
        merchantDescription: String
    ): CardUseResult {
        val card = cardRepository.findByCardNumber(cardNumber) ?: return CardUseResult.CARD_INACTIVE

        // Verify PIN
        if (!numberGenerator.verifyPin(pin, card.pin)) {
            return CardUseResult.INVALID_PIN
        }

        // Check card status
        val cardCheck = card.canSpend(amount)
        if (cardCheck != CardUseResult.SUCCESS) return cardCheck

        // Check account balance
        val account = accountRepository.findByAccountNumber(card.linkedAccountNumber)
            ?: return CardUseResult.INSUFFICIENT_FUNDS

        val withdrawResult = account.canWithdraw(amount)
        if (withdrawResult != WithdrawalResult.SUCCESS) {
            return CardUseResult.INSUFFICIENT_FUNDS
        }

        // Process payment
        accountRepository.updateBalance(card.linkedAccountNumber, account.balance - amount)
        cardRepository.updateSpentToday(cardNumber, card.spentToday + amount)

        // Record transaction
        transactionService.recordCardPurchase(
            card.linkedAccountNumber,
            amount,
            card.ownerUuid,
            merchantDescription
        )

        Bukkit.getPluginManager().callEvent(CardEvent(card, CardEvent.CardAction.USED))

        return CardUseResult.SUCCESS
    }

    suspend fun freezeCard(cardNumber: String): Boolean {
        val card = cardRepository.findByCardNumber(cardNumber) ?: return false
        val success = cardRepository.setFrozen(cardNumber, true)
        if (success) {
            Bukkit.getPluginManager().callEvent(CardEvent(card, CardEvent.CardAction.FROZEN))
        }
        return success
    }

    suspend fun unfreezeCard(cardNumber: String): Boolean {
        val card = cardRepository.findByCardNumber(cardNumber) ?: return false
        val success = cardRepository.setFrozen(cardNumber, false)
        if (success) {
            Bukkit.getPluginManager().callEvent(CardEvent(card, CardEvent.CardAction.UNFROZEN))
        }
        return success
    }

    suspend fun cancelCard(cardNumber: String): Boolean {
        val card = cardRepository.findByCardNumber(cardNumber) ?: return false
        val success = cardRepository.setActive(cardNumber, false)
        if (success) {
            Bukkit.getPluginManager().callEvent(CardEvent(card, CardEvent.CardAction.CANCELLED))
        }
        return success
    }

    suspend fun changePin(cardNumber: String, oldPin: String, newPin: String): Boolean {
        val card = cardRepository.findByCardNumber(cardNumber) ?: return false

        if (!numberGenerator.verifyPin(oldPin, card.pin)) {
            return false
        }

        return cardRepository.updatePin(cardNumber, numberGenerator.hashPin(newPin))
    }

    suspend fun setDailyLimit(cardNumber: String, newLimit: Double): Boolean {
        val card = cardRepository.findByCardNumber(cardNumber) ?: return false
        // Admin function - would need additional validation in real implementation
        return true // Would update in DB
    }
}