// src/main/java/net/crewco/Banking/util/BankCardItem.kt
package net.crewco.Banking.util

import net.crewco.Banking.data.models.Card
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

object BankCardItem {

    private lateinit var CARD_NUMBER_KEY: NamespacedKey
    private lateinit var CARD_TYPE_KEY: NamespacedKey
    private lateinit var OWNER_UUID_KEY: NamespacedKey

    fun initialize(plugin: Plugin) {
        CARD_NUMBER_KEY = NamespacedKey(plugin, "bank_card_number")
        CARD_TYPE_KEY = NamespacedKey(plugin, "bank_card_type")
        OWNER_UUID_KEY = NamespacedKey(plugin, "bank_card_owner")
    }

    /**
     * Creates a physical bank card item for a player
     */
    fun createCardItem(card: Card): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta

        // Set display name based on card type
        val cardColor = when {
            card.cardType.name.contains("CREDIT") -> NamedTextColor.GOLD
            card.cardType.name.contains("BUSINESS") -> NamedTextColor.DARK_PURPLE
            card.cardType.name.contains("PREMIUM") -> NamedTextColor.LIGHT_PURPLE
            else -> NamedTextColor.AQUA
        }

        meta.displayName(
            Component.text()
                .append(Component.text("💳 ", NamedTextColor.WHITE))
                .append(Component.text(card.cardType.displayName, cardColor, TextDecoration.BOLD))
                .build()
        )

        // Set lore
        meta.lore(listOf(
            Component.empty(),
            Component.text("Card Number: ", NamedTextColor.GRAY)
                .append(Component.text(card.getMaskedNumber(), NamedTextColor.WHITE)),
            Component.text("Expires: ", NamedTextColor.GRAY)
                .append(Component.text(card.expirationDate.toString(), NamedTextColor.WHITE)),
            Component.empty(),
            Component.text("Right-click an ATM to use", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
            Component.empty(),
            Component.text("CrewCo Bank", NamedTextColor.GOLD)
        ))

        // Store card data in persistent data container
        val pdc = meta.persistentDataContainer
        pdc.set(CARD_NUMBER_KEY, PersistentDataType.STRING, card.cardNumber)
        pdc.set(CARD_TYPE_KEY, PersistentDataType.STRING, card.cardType.name)
        pdc.set(OWNER_UUID_KEY, PersistentDataType.STRING, card.ownerUuid.toString())

        // Make it unique/unstackable
        meta.setCustomModelData(1001)

        item.itemMeta = meta
        return item
    }

    /**
     * Checks if an ItemStack is a valid bank card
     */
    fun isBankCard(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.PAPER) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(CARD_NUMBER_KEY, PersistentDataType.STRING)
    }

    /**
     * Gets the card number from a bank card item
     */
    fun getCardNumber(item: ItemStack): String? {
        if (!isBankCard(item)) return null
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(CARD_NUMBER_KEY, PersistentDataType.STRING)
    }

    /**
     * Gets the owner UUID from a bank card item
     */
    fun getOwnerUuid(item: ItemStack): String? {
        if (!isBankCard(item)) return null
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(OWNER_UUID_KEY, PersistentDataType.STRING)
    }

    /**
     * Updates a card item with new card data
     */
    fun updateCardItem(item: ItemStack, card: Card): ItemStack {
        return createCardItem(card)
    }
}