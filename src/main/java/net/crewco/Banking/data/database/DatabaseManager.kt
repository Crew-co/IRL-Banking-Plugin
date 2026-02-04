// src/main/java/net/crewco/Banking/data/database/DatabaseManager.kt
package net.crewco.Banking.data.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.crewco.Banking.Startup
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.logging.Level

class DatabaseManager(private val plugin: Startup) {

    private lateinit var connection: Connection
    private val dbFile: File = File(plugin.dataFolder, "banking.db")

    suspend fun connect() {
        withContext(Dispatchers.IO) {
            // Ensure plugin folder exists
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }

            // Load SQLite driver
            Class.forName("org.sqlite.JDBC")

            // Connect to database
            connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            connection.createStatement().execute("PRAGMA foreign_keys = ON")

            plugin.logger.info("Connected to SQLite database!")

            createTables()
        }
    }

    private suspend fun createTables() {
        // Accounts table
        execute("""
            CREATE TABLE IF NOT EXISTS bank_accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                account_number TEXT UNIQUE NOT NULL,
                routing_number TEXT NOT NULL,
                account_type TEXT NOT NULL,
                balance REAL DEFAULT 0.0,
                frozen INTEGER DEFAULT 0,
                overdraft_limit REAL DEFAULT 500.0,
                daily_withdrawn_today REAL DEFAULT 0.0,
                last_withdrawal_date TEXT,
                account_name TEXT DEFAULT '',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())

        execute("CREATE INDEX IF NOT EXISTS idx_accounts_uuid ON bank_accounts(uuid)")
        execute("CREATE INDEX IF NOT EXISTS idx_accounts_number ON bank_accounts(account_number)")

        // Transactions table
        execute("""
            CREATE TABLE IF NOT EXISTS bank_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                transaction_id TEXT UNIQUE NOT NULL,
                from_account_number TEXT,
                to_account_number TEXT,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                description TEXT,
                fee REAL DEFAULT 0.0,
                initiated_by TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                processed_at TEXT
            )
        """.trimIndent())

        execute("CREATE INDEX IF NOT EXISTS idx_tx_from ON bank_transactions(from_account_number)")
        execute("CREATE INDEX IF NOT EXISTS idx_tx_to ON bank_transactions(to_account_number)")
        execute("CREATE INDEX IF NOT EXISTS idx_tx_id ON bank_transactions(transaction_id)")

        // Cards table
        execute("""
            CREATE TABLE IF NOT EXISTS bank_cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                card_number TEXT UNIQUE NOT NULL,
                cvv TEXT NOT NULL,
                linked_account_number TEXT NOT NULL,
                owner_uuid TEXT NOT NULL,
                card_type TEXT NOT NULL,
                expiration_date TEXT NOT NULL,
                pin TEXT NOT NULL,
                daily_limit REAL NOT NULL,
                spent_today REAL DEFAULT 0.0,
                last_used_date TEXT,
                active INTEGER DEFAULT 1,
                frozen INTEGER DEFAULT 0,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())

        execute("CREATE INDEX IF NOT EXISTS idx_cards_owner ON bank_cards(owner_uuid)")
        execute("CREATE INDEX IF NOT EXISTS idx_cards_account ON bank_cards(linked_account_number)")

        // Loans table
        execute("""
            CREATE TABLE IF NOT EXISTS bank_loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                loan_id TEXT UNIQUE NOT NULL,
                borrower_uuid TEXT NOT NULL,
                linked_account_number TEXT NOT NULL,
                loan_type TEXT NOT NULL,
                principal_amount REAL NOT NULL,
                interest_rate REAL NOT NULL,
                remaining_balance REAL NOT NULL,
                monthly_payment REAL NOT NULL,
                total_paid REAL DEFAULT 0.0,
                missed_payments INTEGER DEFAULT 0,
                term_months INTEGER NOT NULL,
                months_remaining INTEGER NOT NULL,
                status TEXT NOT NULL,
                collateral TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                approved_at TEXT,
                next_payment_due TEXT,
                last_payment_date TEXT
            )
        """.trimIndent())

        execute("CREATE INDEX IF NOT EXISTS idx_loans_borrower ON bank_loans(borrower_uuid)")
        execute("CREATE INDEX IF NOT EXISTS idx_loans_id ON bank_loans(loan_id)")

        // ATMs table
        execute("""
            CREATE TABLE IF NOT EXISTS bank_atms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                atm_id TEXT UNIQUE NOT NULL,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                cash REAL DEFAULT 100000.0,
                max_withdrawal REAL DEFAULT 5000.0,
                transaction_fee REAL DEFAULT 2.50,
                active INTEGER DEFAULT 1,
                placed_by TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())

        execute("CREATE INDEX IF NOT EXISTS idx_atms_id ON bank_atms(atm_id)")

        plugin.logger.info("Database tables created/verified!")
    }

    suspend fun execute(query: String, vararg params: Any?): Long = withContext(Dispatchers.IO) {
        try {
            val statement = connection.prepareStatement(query)
            params.forEachIndexed { index, param ->
                when (param) {
                    null -> statement.setNull(index + 1, java.sql.Types.NULL)
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Double -> statement.setDouble(index + 1, param)
                    is Boolean -> statement.setInt(index + 1, if (param) 1 else 0)
                    is java.time.LocalDateTime -> statement.setString(index + 1, param.toString())
                    is java.time.LocalDate -> statement.setString(index + 1, param.toString())
                    else -> statement.setString(index + 1, param.toString())
                }
            }

            val result = statement.executeUpdate()
            statement.close()
            result.toLong()
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Database execute error: ${e.message}", e)
            0L
        }
    }

    suspend fun query(query: String, vararg params: Any?): List<Map<String, Any?>> = withContext(Dispatchers.IO) {
        try {
            val statement = connection.prepareStatement(query)
            params.forEachIndexed { index, param ->
                when (param) {
                    null -> statement.setNull(index + 1, java.sql.Types.NULL)
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Double -> statement.setDouble(index + 1, param)
                    is Boolean -> statement.setInt(index + 1, if (param) 1 else 0)
                    is java.time.LocalDateTime -> statement.setString(index + 1, param.toString())
                    is java.time.LocalDate -> statement.setString(index + 1, param.toString())
                    else -> statement.setString(index + 1, param.toString())
                }
            }

            val resultSet = statement.executeQuery()
            val results = resultSet.toMapList()
            statement.close()
            results
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Database query error: ${e.message}", e)
            emptyList()
        }
    }

    private fun ResultSet.toMapList(): List<Map<String, Any?>> {
        val results = mutableListOf<Map<String, Any?>>()
        val metaData = this.metaData
        val columnCount = metaData.columnCount

        while (this.next()) {
            val row = mutableMapOf<String, Any?>()
            for (i in 1..columnCount) {
                val columnName = metaData.getColumnName(i)
                row[columnName] = this.getObject(i)
            }
            results.add(row)
        }

        return results
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
            plugin.logger.info("Disconnected from database.")
        }
    }

    fun isConnected(): Boolean = ::connection.isInitialized && !connection.isClosed
}