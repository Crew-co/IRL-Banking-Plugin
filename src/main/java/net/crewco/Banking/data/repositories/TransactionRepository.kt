// src/main/java/net/crewco/Banking/data/repositories/TransactionRepository.kt
package net.crewco.Banking.data.repositories

import net.crewco.Banking.data.database.DatabaseManager
import net.crewco.Banking.data.edata.TransactionStatus
import net.crewco.Banking.data.edata.TransactionType
import net.crewco.Banking.data.models.Transaction
import java.time.LocalDateTime
import java.util.UUID

class TransactionRepository(private val db: DatabaseManager) {

    suspend fun create(transaction: Transaction): Boolean {
        val affected = db.execute(
            """
            INSERT INTO bank_transactions 
            (transaction_id, from_account_number, to_account_number, amount, type, 
             status, description, fee, initiated_by, created_at, processed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            transaction.transactionId,
            transaction.fromAccountNumber,
            transaction.toAccountNumber,
            transaction.amount,
            transaction.type.name,
            transaction.status.name,
            transaction.description,
            transaction.fee,
            transaction.initiatedBy.toString(),
            transaction.createdAt.toString(),
            transaction.processedAt?.toString()
        )
        return affected > 0
    }

    suspend fun findByTransactionId(transactionId: String): Transaction? {
        val results = db.query(
            "SELECT * FROM bank_transactions WHERE transaction_id = ?",
            transactionId
        )
        return results.firstOrNull()?.toTransaction()
    }

    suspend fun findByAccount(accountNumber: String, limit: Int = 50): List<Transaction> {
        val results = db.query(
            """
            SELECT * FROM bank_transactions 
            WHERE from_account_number = ? OR to_account_number = ?
            ORDER BY created_at DESC
            LIMIT ?
            """.trimIndent(),
            accountNumber,
            accountNumber,
            limit
        )
        return results.map { it.toTransaction() }
    }

    suspend fun findByUuid(uuid: UUID, limit: Int = 50): List<Transaction> {
        val results = db.query(
            """
            SELECT * FROM bank_transactions 
            WHERE initiated_by = ?
            ORDER BY created_at DESC
            LIMIT ?
            """.trimIndent(),
            uuid.toString(),
            limit
        )
        return results.map { it.toTransaction() }
    }

    suspend fun updateStatus(transactionId: String, status: TransactionStatus): Boolean {
        val affected = db.execute(
            """
            UPDATE bank_transactions 
            SET status = ?, processed_at = ? 
            WHERE transaction_id = ?
            """.trimIndent(),
            status.name,
            LocalDateTime.now().toString(),
            transactionId
        )
        return affected > 0
    }

    private fun Map<String, Any?>.toTransaction(): Transaction {
        return Transaction(
            id = (this["id"] as Number).toLong(),
            transactionId = this["transaction_id"] as String,
            fromAccountNumber = this["from_account_number"] as? String,
            toAccountNumber = this["to_account_number"] as? String,
            amount = (this["amount"] as Number).toDouble(),
            type = TransactionType.valueOf(this["type"] as String),
            status = TransactionStatus.valueOf(this["status"] as String),
            description = this["description"] as? String ?: "",
            fee = (this["fee"] as Number).toDouble(),
            initiatedBy = UUID.fromString(this["initiated_by"] as String),
            createdAt = parseDateTime(this["created_at"] as? String),
            processedAt = (this["processed_at"] as? String)?.let { parseDateTime(it) }
        )
    }

    private fun parseDateTime(value: String?): LocalDateTime {
        return try {
            if (value != null) LocalDateTime.parse(value) else LocalDateTime.now()
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}