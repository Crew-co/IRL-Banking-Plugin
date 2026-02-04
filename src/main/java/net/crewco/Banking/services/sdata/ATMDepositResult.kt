package net.crewco.Banking.services.sdata

data class ATMDepositResult(
    val success: Boolean,
    val message: String,
    val amount: Double = 0.0
)