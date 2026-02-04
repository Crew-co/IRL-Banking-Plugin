// src/main/java/net/crewco/Banking/data/models/ATM.kt
package net.crewco.Banking.data.models

import net.crewco.Banking.data.ATMResult
import org.bukkit.Location
import java.time.LocalDateTime
import java.util.UUID

data class ATM(
    val id: Long = 0,
    val atmId: String,
    val location: Location,
    var cash: Double = 100000.0,             // Cash available in ATM
    var maxWithdrawal: Double = 5000.0,      // Max single withdrawal
    var transactionFee: Double = 2.50,       // Fee per transaction
    var active: Boolean = true,
    val placedBy: UUID,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun canDispense(amount: Double): ATMResult {
        if (!active) return ATMResult.ATM_OFFLINE
        if (amount > maxWithdrawal) return ATMResult.EXCEEDS_LIMIT
        if (amount > cash) return ATMResult.INSUFFICIENT_CASH
        return ATMResult.SUCCESS
    }
}
