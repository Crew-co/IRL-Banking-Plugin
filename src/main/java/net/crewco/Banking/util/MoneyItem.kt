// src/main/java/net/crewco/Banking/util/MoneyItem.kt
package net.crewco.Banking.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * Represents different denominations of physical money
 */
enum class MoneyDenomination(
    val value: Double,
    val displayName: String,
    val material: Material,
    val customModelData: Int,
    val color: NamedTextColor,
    val isCoin: Boolean = false
) {
    // Coins
    PENNY(0.01, "1¢ Penny", Material.IRON_NUGGET, 2001, NamedTextColor.GOLD, true),
    NICKEL(0.05, "5¢ Nickel", Material.IRON_NUGGET, 2002, NamedTextColor.GRAY, true),
    DIME(0.10, "10¢ Dime", Material.IRON_NUGGET, 2003, NamedTextColor.WHITE, true),
    QUARTER(0.25, "25¢ Quarter", Material.IRON_NUGGET, 2004, NamedTextColor.WHITE, true),
    HALF_DOLLAR(0.50, "50¢ Half Dollar", Material.GOLD_NUGGET, 2005, NamedTextColor.GOLD, true),
    DOLLAR_COIN(1.00, "$1 Coin", Material.GOLD_NUGGET, 2006, NamedTextColor.GOLD, true),

    // Banknotes
    BILL_1(1.0, "$1 Bill", Material.PAPER, 2011, NamedTextColor.GREEN),
    BILL_5(5.0, "$5 Bill", Material.PAPER, 2012, NamedTextColor.GREEN),
    BILL_10(10.0, "$10 Bill", Material.PAPER, 2013, NamedTextColor.GREEN),
    BILL_20(20.0, "$20 Bill", Material.PAPER, 2014, NamedTextColor.GREEN),
    BILL_50(50.0, "$50 Bill", Material.PAPER, 2015, NamedTextColor.DARK_GREEN),
    BILL_100(100.0, "$100 Bill", Material.PAPER, 2016, NamedTextColor.DARK_GREEN),
    BILL_500(500.0, "$500 Bill", Material.MAP, 2017, NamedTextColor.GOLD),
    BILL_1000(1000.0, "$1000 Bill", Material.MAP, 2018, NamedTextColor.LIGHT_PURPLE);

    companion object {
        /**
         * Get denomination by value
         */
        fun fromValue(value: Double): MoneyDenomination? {
            return entries.find { it.value == value }
        }

        /**
         * Get all banknotes sorted by value descending
         */
        fun getBanknotes(): List<MoneyDenomination> {
            return entries.filter { !it.isCoin }.sortedByDescending { it.value }
        }

        /**
         * Get all coins sorted by value descending
         */
        fun getCoins(): List<MoneyDenomination> {
            return entries.filter { it.isCoin }.sortedByDescending { it.value }
        }

        /**
         * Get all denominations sorted by value descending
         */
        fun getAllSorted(): List<MoneyDenomination> {
            return entries.sortedByDescending { it.value }
        }
    }
}

/**
 * Utility object for creating and managing physical money items
 */
object MoneyItem {

    private lateinit var MONEY_VALUE_KEY: NamespacedKey
    private lateinit var MONEY_DENOMINATION_KEY: NamespacedKey
    private lateinit var MONEY_PLUGIN_KEY: NamespacedKey

    private const val MONEY_PLUGIN_ID = "crewco_banking_money"

    fun initialize(plugin: Plugin) {
        MONEY_VALUE_KEY = NamespacedKey(plugin, "money_value")
        MONEY_DENOMINATION_KEY = NamespacedKey(plugin, "money_denomination")
        MONEY_PLUGIN_KEY = NamespacedKey(plugin, "money_plugin_id")
    }

    /**
     * Creates a physical money item
     */
    fun createMoneyItem(denomination: MoneyDenomination, amount: Int = 1): ItemStack {
        val item = ItemStack(denomination.material, amount.coerceIn(1, 64))
        val meta = item.itemMeta

        // Set display name
        meta.displayName(
            Component.text()
                .append(Component.text("💵 ", NamedTextColor.WHITE))
                .append(Component.text(denomination.displayName, denomination.color, TextDecoration.BOLD))
                .build()
        )

        // Set lore
        val totalValue = denomination.value * amount
        meta.lore(listOf(
            Component.empty(),
            Component.text("Value: ", NamedTextColor.GRAY)
                .append(Component.text(Messages.formatCurrency(denomination.value), NamedTextColor.GREEN)),
            if (amount > 1) {
                Component.text("Total: ", NamedTextColor.GRAY)
                    .append(Component.text(Messages.formatCurrency(totalValue), NamedTextColor.GOLD))
            } else Component.empty(),
            Component.empty(),
            Component.text(if (denomination.isCoin) "Coin" else "Banknote", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
            Component.empty(),
            Component.text("CrewCo Bank Currency", NamedTextColor.AQUA)
        ).filter { it != Component.empty() || denomination.value > 0 })

        // Store money data in persistent data container
        val pdc = meta.persistentDataContainer
        pdc.set(MONEY_VALUE_KEY, PersistentDataType.DOUBLE, denomination.value)
        pdc.set(MONEY_DENOMINATION_KEY, PersistentDataType.STRING, denomination.name)
        pdc.set(MONEY_PLUGIN_KEY, PersistentDataType.STRING, MONEY_PLUGIN_ID)

        // Set custom model data for resource pack support
        meta.setCustomModelData(denomination.customModelData)

        item.itemMeta = meta
        return item
    }

    /**
     * Checks if an ItemStack is valid money
     */
    fun isMoneyItem(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) return false
        val meta = item.itemMeta ?: return false
        val pdc = meta.persistentDataContainer

        // Check for plugin identifier
        val pluginId = pdc.get(MONEY_PLUGIN_KEY, PersistentDataType.STRING)
        return pluginId == MONEY_PLUGIN_ID
    }

    /**
     * Gets the denomination value from a money item
     */
    fun getMoneyValue(item: ItemStack): Double {
        if (!isMoneyItem(item)) return 0.0
        val meta = item.itemMeta ?: return 0.0
        return meta.persistentDataContainer.get(MONEY_VALUE_KEY, PersistentDataType.DOUBLE) ?: 0.0
    }

    /**
     * Gets the total value of a money item stack
     */
    fun getTotalValue(item: ItemStack): Double {
        if (!isMoneyItem(item)) return 0.0
        return getMoneyValue(item) * item.amount
    }

    /**
     * Gets the denomination enum from a money item
     */
    fun getDenomination(item: ItemStack): MoneyDenomination? {
        if (!isMoneyItem(item)) return null
        val meta = item.itemMeta ?: return null
        val denomName = meta.persistentDataContainer.get(MONEY_DENOMINATION_KEY, PersistentDataType.STRING)
            ?: return null
        return try {
            MoneyDenomination.valueOf(denomName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Calculates the optimal breakdown of an amount into denominations
     * Returns a map of denomination to count
     */
    fun calculateBreakdown(amount: Double, useCoins: Boolean = true): Map<MoneyDenomination, Int> {
        val result = mutableMapOf<MoneyDenomination, Int>()
        var remaining = amount

        // Get denominations to use
        val denominations = if (useCoins) {
            MoneyDenomination.getAllSorted()
        } else {
            MoneyDenomination.getBanknotes()
        }

        for (denom in denominations) {
            if (remaining >= denom.value) {
                val count = (remaining / denom.value).toInt()
                if (count > 0) {
                    result[denom] = count
                    remaining -= count * denom.value
                    remaining = Math.round(remaining * 100.0) / 100.0 // Fix floating point issues
                }
            }
        }

        return result
    }

    /**
     * Calculates breakdown with a maximum number of items constraint
     * Prefers larger denominations to minimize item count
     */
    fun calculateBreakdownWithLimit(amount: Double, maxItems: Int = 576, useCoins: Boolean = false): Map<MoneyDenomination, Int> {
        val result = mutableMapOf<MoneyDenomination, Int>()
        var remaining = amount
        var totalItems = 0

        // Prefer banknotes for ATM withdrawals (no coins by default)
        val denominations = if (useCoins) {
            MoneyDenomination.getAllSorted()
        } else {
            MoneyDenomination.getBanknotes()
        }

        for (denom in denominations) {
            if (remaining >= denom.value && totalItems < maxItems) {
                val maxPossible = (remaining / denom.value).toInt()
                val maxAllowed = maxItems - totalItems
                val count = minOf(maxPossible, maxAllowed)

                if (count > 0) {
                    result[denom] = count
                    remaining -= count * denom.value
                    remaining = Math.round(remaining * 100.0) / 100.0
                    totalItems += count
                }
            }
        }

        return result
    }

    /**
     * Creates a list of ItemStacks for a given amount, respecting stack size limits
     */
    fun createMoneyItems(amount: Double, useCoins: Boolean = false): List<ItemStack> {
        val breakdown = calculateBreakdownWithLimit(amount, 576, useCoins)
        val items = mutableListOf<ItemStack>()

        for ((denom, count) in breakdown) {
            var remaining = count
            while (remaining > 0) {
                val stackSize = minOf(remaining, 64)
                items.add(createMoneyItem(denom, stackSize))
                remaining -= stackSize
            }
        }

        return items
    }

    /**
     * Counts total money value in an array of ItemStacks
     */
    fun countTotalMoney(items: Array<ItemStack?>): Double {
        return items.filterNotNull()
            .filter { isMoneyItem(it) }
            .sumOf { getTotalValue(it) }
    }

    /**
     * Counts total money value in a list of ItemStacks
     */
    fun countTotalMoney(items: List<ItemStack?>): Double {
        return items.filterNotNull()
            .filter { isMoneyItem(it) }
            .sumOf { getTotalValue(it) }
    }

    /**
     * Finds all money items in an inventory array
     */
    fun findMoneyItems(items: Array<ItemStack?>): List<Pair<Int, ItemStack>> {
        return items.mapIndexedNotNull { index, item ->
            if (item != null && isMoneyItem(item)) {
                Pair(index, item)
            } else null
        }
    }

    /**
     * Check if a specific amount can be represented exactly with available denominations
     */
    fun canRepresentExactly(amount: Double): Boolean {
        val breakdown = calculateBreakdown(amount, true)
        val total = breakdown.entries.sumOf { it.key.value * it.value }
        return Math.abs(total - amount) < 0.001
    }
}
