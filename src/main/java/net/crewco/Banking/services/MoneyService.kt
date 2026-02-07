// src/main/java/net/crewco/Banking/services/MoneyService.kt
package net.crewco.Banking.services

import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.util.MoneyDenomination
import net.crewco.Banking.util.MoneyItem
import net.crewco.Banking.util.Messages
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Result of a money operation
 */
data class MoneyOperationResult(
    val success: Boolean,
    val message: String,
    val amount: Double = 0.0,
    val itemsGiven: Int = 0,
    val itemsTaken: Int = 0,
    val remainder: Double = 0.0
)

/**
 * Service for handling physical money items
 * Cash on hand is tied to the player's wallet balance
 */
class MoneyService {

    /**
     * Gives physical money to a player and updates their wallet
     * Returns a result indicating success/failure and details
     */
    suspend fun giveMoneyToPlayer(player: Player, amount: Double, useCoins: Boolean = false, updateWallet: Boolean = true): MoneyOperationResult {
        if (amount <= 0) {
            return MoneyOperationResult(false, "Invalid amount")
        }

        // Check if amount can be represented
        if (!useCoins && amount < MoneyDenomination.BILL_1.value) {
            return MoneyOperationResult(false, "Amount too small for banknotes. Minimum is ${Messages.formatCurrency(1.0)}")
        }

        // Calculate items needed
        val moneyItems = MoneyItem.createMoneyItems(amount, useCoins)

        if (moneyItems.isEmpty()) {
            return MoneyOperationResult(false, "Cannot create money items for this amount")
        }

        // Check inventory space
        val emptySlots = player.inventory.storageContents.count { it == null || it.type.isAir }
        if (moneyItems.size > emptySlots) {
            return MoneyOperationResult(
                false,
                "Not enough inventory space! Need ${moneyItems.size} slots, have $emptySlots",
                amount,
                0,
                0,
                amount
            )
        }

        // Update wallet balance first
        if (updateWallet) {
            val wallet = accountService.getPrimaryAccount(player.uniqueId)
            if (wallet != null) {
                accountService.deposit(wallet.accountNumber, amount, player.uniqueId, "Cash received")
            }
        }

        // Give items to player
        var itemsGiven = 0
        var totalGiven = 0.0
        val leftover = mutableListOf<ItemStack>()

        for (item in moneyItems) {
            val notFit = player.inventory.addItem(item)
            if (notFit.isEmpty()) {
                itemsGiven++
                totalGiven += MoneyItem.getTotalValue(item)
            } else {
                leftover.addAll(notFit.values)
            }
        }

        // Drop any leftover items at player's feet
        for (item in leftover) {
            player.world.dropItemNaturally(player.location, item)
            itemsGiven++
            totalGiven += MoneyItem.getTotalValue(item)
        }

        val remainder = amount - totalGiven
        return MoneyOperationResult(
            true,
            "Received ${Messages.formatCurrency(totalGiven)}",
            totalGiven,
            itemsGiven,
            0,
            remainder
        )
    }

    /**
     * Takes a specific amount of money from a player's inventory and wallet
     * Returns the actual amount taken (may be less if player doesn't have enough)
     */
    suspend fun takeMoneyFromPlayer(player: Player, amount: Double, updateWallet: Boolean = true): MoneyOperationResult {
        if (amount <= 0) {
            return MoneyOperationResult(false, "Invalid amount")
        }

        // Check wallet balance first (source of truth)
        val wallet = accountService.getPrimaryAccount(player.uniqueId)
        val walletBalance = wallet?.balance ?: 0.0
        
        if (walletBalance < amount) {
            return MoneyOperationResult(
                false,
                "Insufficient cash! You have ${Messages.formatCurrency(walletBalance)}",
                0.0,
                0,
                0,
                amount
            )
        }

        val inventory = player.inventory
        val moneySlots = MoneyItem.findMoneyItems(inventory.storageContents)

        // Sort by denomination value descending (prefer taking larger bills first)
        val sortedSlots = moneySlots.sortedByDescending { MoneyItem.getMoneyValue(it.second) }

        var remaining = amount
        var itemsTaken = 0
        val changeNeeded = mutableMapOf<MoneyDenomination, Int>()

        for ((slot, item) in sortedSlots) {
            if (remaining <= 0) break

            val itemValue = MoneyItem.getMoneyValue(item)
            val stackValue = MoneyItem.getTotalValue(item)

            if (stackValue <= remaining) {
                // Take the entire stack
                inventory.setItem(slot, null)
                remaining -= stackValue
                remaining = Math.round(remaining * 100.0) / 100.0
                itemsTaken += item.amount
            } else {
                // Take partial stack or need to make change
                val itemsNeeded = (remaining / itemValue).toInt()
                if (itemsNeeded > 0 && itemsNeeded < item.amount) {
                    // Can take partial stack
                    item.amount -= itemsNeeded
                    remaining -= itemsNeeded * itemValue
                    remaining = Math.round(remaining * 100.0) / 100.0
                    itemsTaken += itemsNeeded
                } else if (itemsNeeded == 0 && remaining > 0) {
                    // Need to break a larger bill
                    // Take one item and calculate change
                    val changeAmount = itemValue - remaining
                    item.amount -= 1
                    if (item.amount <= 0) {
                        inventory.setItem(slot, null)
                    }
                    remaining = 0.0
                    itemsTaken += 1

                    // Add change needed
                    val changeBreakdown = MoneyItem.calculateBreakdown(changeAmount, true)
                    for ((denom, count) in changeBreakdown) {
                        changeNeeded[denom] = changeNeeded.getOrDefault(denom, 0) + count
                    }
                }
            }
        }

        // Give change to player (don't update wallet for change - it's already accounted for)
        for ((denom, count) in changeNeeded) {
            var remainingChange = count
            while (remainingChange > 0) {
                val stackSize = minOf(remainingChange, 64)
                val changeItem = MoneyItem.createMoneyItem(denom, stackSize)
                val notFit = inventory.addItem(changeItem)
                if (notFit.isNotEmpty()) {
                    // Drop change on ground
                    for (leftover in notFit.values) {
                        player.world.dropItemNaturally(player.location, leftover)
                    }
                }
                remainingChange -= stackSize
            }
        }

        // Update wallet balance
        if (updateWallet && wallet != null) {
            accountService.withdraw(wallet.accountNumber, amount, player.uniqueId, "Cash spent")
        }

        return MoneyOperationResult(
            true,
            "Paid ${Messages.formatCurrency(amount)}",
            amount,
            0,
            itemsTaken,
            0.0
        )
    }

    /**
     * Counts total cash on hand - uses wallet balance as source of truth
     */
    suspend fun countPlayerMoney(player: Player): Double {
        val wallet = accountService.getPrimaryAccount(player.uniqueId)
        return wallet?.balance ?: 0.0
    }

    /**
     * Counts physical money items in inventory (for display/sync purposes)
     */
    fun countPhysicalMoney(player: Player): Double {
        return MoneyItem.countTotalMoney(player.inventory.storageContents)
    }

    /**
     * Collects all money from player's inventory and clears wallet
     * Returns the total amount collected
     */
    suspend fun collectAllMoney(player: Player, updateWallet: Boolean = true): MoneyOperationResult {
        val inventory = player.inventory
        val moneySlots = MoneyItem.findMoneyItems(inventory.storageContents)

        if (moneySlots.isEmpty()) {
            return MoneyOperationResult(false, "No cash found in inventory", 0.0, 0, 0, 0.0)
        }

        // Get wallet balance (source of truth)
        val wallet = accountService.getPrimaryAccount(player.uniqueId)
        val walletBalance = wallet?.balance ?: 0.0

        var itemsTaken = 0

        // Remove all physical cash items
        for ((slot, item) in moneySlots) {
            itemsTaken += item.amount
            inventory.setItem(slot, null)
        }

        // Update wallet
        if (updateWallet && wallet != null && walletBalance > 0) {
            accountService.withdraw(wallet.accountNumber, walletBalance, player.uniqueId, "Cash deposited")
        }

        return MoneyOperationResult(
            true,
            "Collected ${Messages.formatCurrency(walletBalance)}",
            walletBalance,
            0,
            itemsTaken,
            0.0
        )
    }

    /**
     * Exchanges money - breaks large bills into smaller denominations
     */
    fun exchangeMoney(player: Player, fromDenomination: MoneyDenomination, count: Int, toDenomination: MoneyDenomination): MoneyOperationResult {
        val totalValue = fromDenomination.value * count

        // Check if the exchange makes sense
        if (toDenomination.value >= fromDenomination.value) {
            return MoneyOperationResult(false, "Can only exchange to smaller denominations")
        }

        // Check if player has the money
        val inventory = player.inventory
        var foundCount = 0
        val slotsToModify = mutableListOf<Pair<Int, Int>>() // slot -> amount to remove

        for ((slot, item) in MoneyItem.findMoneyItems(inventory.storageContents)) {
            if (MoneyItem.getDenomination(item) == fromDenomination) {
                val available = item.amount
                val toTake = minOf(available, count - foundCount)
                if (toTake > 0) {
                    slotsToModify.add(slot to toTake)
                    foundCount += toTake
                    if (foundCount >= count) break
                }
            }
        }

        if (foundCount < count) {
            return MoneyOperationResult(
                false,
                "You don't have enough ${fromDenomination.displayName}. Have: $foundCount, Need: $count"
            )
        }

        // Calculate output
        val outputCount = (totalValue / toDenomination.value).toInt()
        val remainder = totalValue - (outputCount * toDenomination.value)

        // Check inventory space
        val stacksNeeded = (outputCount + 63) / 64
        val emptySlots = inventory.storageContents.count { it == null || it.type.isAir }
        val slotsFreed = slotsToModify.count { slot ->
            val item = inventory.getItem(slot.first)
            item != null && item.amount == slot.second
        }

        if (stacksNeeded > emptySlots + slotsFreed) {
            return MoneyOperationResult(false, "Not enough inventory space for exchange")
        }

        // Remove old money
        for ((slot, amountToRemove) in slotsToModify) {
            val item = inventory.getItem(slot)
            if (item != null) {
                if (item.amount <= amountToRemove) {
                    inventory.setItem(slot, null)
                } else {
                    item.amount -= amountToRemove
                }
            }
        }

        // Give new money (no wallet update - it's just exchanging, same total value)
        var remainingOutput = outputCount
        while (remainingOutput > 0) {
            val stackSize = minOf(remainingOutput, 64)
            val newItem = MoneyItem.createMoneyItem(toDenomination, stackSize)
            val notFit = inventory.addItem(newItem)
            if (notFit.isNotEmpty()) {
                for (leftover in notFit.values) {
                    player.world.dropItemNaturally(player.location, leftover)
                }
            }
            remainingOutput -= stackSize
        }

        // Give remainder in appropriate denominations
        if (remainder > 0) {
            val remainderItems = MoneyItem.createMoneyItems(remainder, true)
            for (item in remainderItems) {
                val notFit = inventory.addItem(item)
                if (notFit.isNotEmpty()) {
                    for (leftover in notFit.values) {
                        player.world.dropItemNaturally(player.location, leftover)
                    }
                }
            }
        }

        return MoneyOperationResult(
            true,
            "Exchanged $count x ${fromDenomination.displayName} into $outputCount x ${toDenomination.displayName}",
            totalValue,
            outputCount,
            count,
            remainder
        )
    }

    /**
     * Consolidates small bills/coins into larger denominations
     */
    suspend fun consolidateMoney(player: Player): MoneyOperationResult {
        val total = countPlayerMoney(player)
        if (total <= 0) {
            return MoneyOperationResult(false, "No cash to consolidate")
        }

        // Remove all physical items (don't update wallet - just reorganizing)
        val inventory = player.inventory
        val moneySlots = MoneyItem.findMoneyItems(inventory.storageContents)
        for ((slot, _) in moneySlots) {
            inventory.setItem(slot, null)
        }

        // Give back as optimal denominations (don't update wallet)
        return giveMoneyToPlayer(player, total, false, updateWallet = false)
    }

    /**
     * Gets a breakdown of money in player's inventory by denomination
     */
    fun getMoneyBreakdown(player: Player): Map<MoneyDenomination, Int> {
        val result = mutableMapOf<MoneyDenomination, Int>()

        for ((_, item) in MoneyItem.findMoneyItems(player.inventory.storageContents)) {
            val denom = MoneyItem.getDenomination(item) ?: continue
            result[denom] = result.getOrDefault(denom, 0) + item.amount
        }

        return result.toSortedMap(compareByDescending { it.value })
    }

    /**
     * Syncs physical cash items with wallet balance
     * Call this if items and wallet get out of sync
     */
    suspend fun syncWalletWithItems(player: Player) {
        val physicalTotal = countPhysicalMoney(player)
        val wallet = accountService.getPrimaryAccount(player.uniqueId) ?: return
        val walletBalance = wallet.balance

        val difference = physicalTotal - walletBalance
        if (Math.abs(difference) > 0.001) {
            if (difference > 0) {
                // More physical cash than wallet - add to wallet
                accountService.deposit(wallet.accountNumber, difference, player.uniqueId, "Cash sync")
            } else {
                // Less physical cash than wallet - remove from wallet
                accountService.withdraw(wallet.accountNumber, -difference, player.uniqueId, "Cash sync")
            }
        }
    }
}
