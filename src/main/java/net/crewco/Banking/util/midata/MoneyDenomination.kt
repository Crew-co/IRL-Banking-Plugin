package net.crewco.Banking.util.midata

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

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