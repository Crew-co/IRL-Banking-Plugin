package net.crewco.Banking.services.sdata

import net.crewco.Banking.data.models.Loan

data class LoanApplicationResult(
    val success: Boolean,
    val loan: Loan?,
    val message: String
)