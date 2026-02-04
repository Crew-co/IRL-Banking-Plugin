package net.crewco.Banking.services.sdata

data class ATMWithdrawResult(
    val success: Boolean,
    val message: String,
    val amount: Double = 0.0,
    val fee: Double = 0.0
)