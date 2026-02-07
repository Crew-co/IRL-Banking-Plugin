package net.crewco.Banking.services.mdata

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