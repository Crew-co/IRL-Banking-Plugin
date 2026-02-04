// src/main/java/net/crewco/Banking/data/models/Transaction.kt
package net.crewco.Banking.data.models

import net.crewco.Banking.data.edata.TransactionStatus
import net.crewco.Banking.data.edata.TransactionType
import java.time.LocalDateTime
import java.util.UUID

data class Transaction(
    val id: Long = 0,
    val transactionId: String,              // Unique transaction reference
    val fromAccountNumber: String?,         // Null for deposits
    val toAccountNumber: String?,           // Null for withdrawals
    val amount: Double,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val description: String = "",
    val fee: Double = 0.0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val processedAt: LocalDateTime? = null,
    val initiatedBy: UUID                   // Player who initiated
)
