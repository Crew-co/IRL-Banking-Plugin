// src/main/java/net/crewco/Banking/data/repositories/AccountRepository.kt
package net.crewco.Banking.data.repositories

import net.crewco.Banking.data.database.DatabaseManager
import net.crewco.Banking.data.models.AccountType
import net.crewco.Banking.data.models.BankAccount
import java.time.LocalDateTime
import java.util.UUID

class AccountRepository(private val db: DatabaseManager) {

    suspend fun create(account: BankAccount): Boolean {
        val affected = db.execute(
            """
            INSERT INTO bank_accounts 
            (uuid, account_number, routing_number, account_type, balance, frozen, 
             overdraft_limit, daily_withdrawn_today, last_withdrawal_date, account_name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            account.uuid.toString(),
            account.accountNumber,
            account.routingNumber,
            account.accountType.name,
            account.balance,
            account.frozen,
            account.overdraftLimit,
            account.dailyWithdrawnToday,
            account.lastWithdrawalDate.toString(),
            account.accountName
        )
        return affected > 0
    }

    suspend fun findByAccountNumber(accountNumber: String): BankAccount? {
        val results = db.query(
            "SELECT * FROM bank_accounts WHERE account_number = ?",
            accountNumber
        )
        return results.firstOrNull()?.toAccount()
    }

    suspend fun findByUuid(uuid: UUID): List<BankAccount> {
        val results = db.query(
            "SELECT * FROM bank_accounts WHERE uuid = ?",
            uuid.toString()
        )
        return results.map { it.toAccount() }
    }

    suspend fun findByUuidAndType(uuid: UUID, type: AccountType): BankAccount? {
        val results = db.query(
            "SELECT * FROM bank_accounts WHERE uuid = ? AND account_type = ?",
            uuid.toString(),
            type.name
        )
        return results.firstOrNull()?.toAccount()
    }

    suspend fun updateBalance(accountNumber: String, newBalance: Double): Boolean {
        val affected = db.execute(
            "UPDATE bank_accounts SET balance = ?, updated_at = ? WHERE account_number = ?",
            newBalance,
            LocalDateTime.now().toString(),
            accountNumber
        )
        return affected > 0
    }

    suspend fun updateDailyWithdrawal(accountNumber: String, amount: Double): Boolean {
        val now = LocalDateTime.now().toString()
        val affected = db.execute(
            """
            UPDATE bank_accounts 
            SET daily_withdrawn_today = ?, last_withdrawal_date = ?, updated_at = ? 
            WHERE account_number = ?
            """.trimIndent(),
            amount,
            now,
            now,
            accountNumber
        )
        return affected > 0
    }

    suspend fun setFrozen(accountNumber: String, frozen: Boolean): Boolean {
        val affected = db.execute(
            "UPDATE bank_accounts SET frozen = ?, updated_at = ? WHERE account_number = ?",
            frozen,
            LocalDateTime.now().toString(),
            accountNumber
        )
        return affected > 0
    }

    suspend fun delete(accountNumber: String): Boolean {
        val affected = db.execute(
            "DELETE FROM bank_accounts WHERE account_number = ?",
            accountNumber
        )
        return affected > 0
    }

    suspend fun resetDailyWithdrawals(): Int {
        val affected = db.execute(
            """
            UPDATE bank_accounts 
            SET daily_withdrawn_today = 0 
            WHERE date(last_withdrawal_date) < date('now')
            """.trimIndent()
        )
        return affected.toInt()
    }

    suspend fun getAllAccounts(): List<BankAccount> {
        val results = db.query("SELECT * FROM bank_accounts")
        return results.map { it.toAccount() }
    }

    private fun Map<String, Any?>.toAccount(): BankAccount {
        return BankAccount(
            id = (this["id"] as Number).toLong(),
            uuid = UUID.fromString(this["uuid"] as String),
            accountNumber = this["account_number"] as String,
            routingNumber = this["routing_number"] as String,
            accountType = AccountType.valueOf(this["account_type"] as String),
            balance = (this["balance"] as Number).toDouble(),
            frozen = (this["frozen"] as Number).toInt() == 1,
            overdraftLimit = (this["overdraft_limit"] as Number).toDouble(),
            dailyWithdrawnToday = (this["daily_withdrawn_today"] as Number).toDouble(),
            lastWithdrawalDate = parseDateTime(this["last_withdrawal_date"] as? String),
            accountName = this["account_name"] as? String ?: "",
            createdAt = parseDateTime(this["created_at"] as? String),
            updatedAt = parseDateTime(this["updated_at"] as? String)
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