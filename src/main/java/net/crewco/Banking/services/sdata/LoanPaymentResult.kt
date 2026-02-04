package net.crewco.Banking.services.sdata

import net.crewco.Banking.data.models.Loan


data class LoanPaymentResult(
    val success: Boolean,
    val message: String,
    val loan: Loan? = null
)