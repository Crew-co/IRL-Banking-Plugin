// src/main/java/net/crewco/Banking/data/repositories/ATMRepository.kt
package net.crewco.Banking.data.repositories

import net.crewco.Banking.Startup
import net.crewco.Banking.data.database.DatabaseManager
import net.crewco.Banking.data.models.ATM
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.LocalDateTime
import java.util.UUID

class ATMRepository(private val db: DatabaseManager, private val plugin: Startup) {

    suspend fun create(atm: ATM): Boolean {
        val affected = db.execute(
            """
            INSERT INTO bank_atms 
            (atm_id, world, x, y, z, bank_id, cash, max_withdrawal, transaction_fee, out_of_network_fee, active, placed_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            atm.atmId,
            atm.location.world?.name ?: "world",
            atm.location.x,
            atm.location.y,
            atm.location.z,
            atm.bankId,
            atm.cash,
            atm.maxWithdrawal,
            atm.transactionFee,
            atm.outOfNetworkFee,
            atm.active,
            atm.placedBy.toString()
        )
        return affected > 0
    }

    suspend fun findByAtmId(atmId: String): ATM? {
        val results = db.query(
            "SELECT * FROM bank_atms WHERE atm_id = ?",
            atmId
        )
        return results.firstOrNull()?.toATM()
    }

    suspend fun findByLocation(location: Location): ATM? {
        val results = db.query(
            """
            SELECT * FROM bank_atms 
            WHERE world = ? AND CAST(x AS INTEGER) = ? AND CAST(y AS INTEGER) = ? AND CAST(z AS INTEGER) = ?
            """.trimIndent(),
            location.world?.name ?: "world",
            location.blockX,
            location.blockY,
            location.blockZ
        )
        return results.firstOrNull()?.toATM()
    }

    suspend fun findNearby(location: Location, radius: Double): List<ATM> {
        val results = db.query(
            """
            SELECT * FROM bank_atms 
            WHERE world = ? 
            AND ABS(x - ?) <= ? 
            AND ABS(y - ?) <= ? 
            AND ABS(z - ?) <= ?
            AND active = 1
            """.trimIndent(),
            location.world?.name ?: "world",
            location.x, radius,
            location.y, radius,
            location.z, radius
        )
        return results.map { it.toATM() }
    }

    suspend fun getAllActive(): List<ATM> {
        val results = db.query(
            "SELECT * FROM bank_atms WHERE active = 1"
        )
        return results.map { it.toATM() }
    }

    suspend fun findByBankId(bankId: String): List<ATM> {
        val results = db.query(
            "SELECT * FROM bank_atms WHERE bank_id = ?",
            bankId
        )
        return results.map { it.toATM() }
    }

    suspend fun countByBankId(bankId: String): Int {
        val results = db.query(
            "SELECT COUNT(*) as cnt FROM bank_atms WHERE bank_id = ?",
            bankId
        )
        return (results.firstOrNull()?.get("cnt") as? Number)?.toInt() ?: 0
    }

    suspend fun updateCash(atmId: String, cash: Double): Boolean {
        val affected = db.execute(
            "UPDATE bank_atms SET cash = ? WHERE atm_id = ?",
            cash,
            atmId
        )
        return affected > 0
    }

    suspend fun updateFees(atmId: String, transactionFee: Double, outOfNetworkFee: Double): Boolean {
        val affected = db.execute(
            "UPDATE bank_atms SET transaction_fee = ?, out_of_network_fee = ? WHERE atm_id = ?",
            transactionFee,
            outOfNetworkFee,
            atmId
        )
        return affected > 0
    }

    suspend fun setActive(atmId: String, active: Boolean): Boolean {
        val affected = db.execute(
            "UPDATE bank_atms SET active = ? WHERE atm_id = ?",
            active,
            atmId
        )
        return affected > 0
    }

    suspend fun delete(atmId: String): Boolean {
        val affected = db.execute(
            "DELETE FROM bank_atms WHERE atm_id = ?",
            atmId
        )
        return affected > 0
    }

    private fun Map<String, Any?>.toATM(): ATM {
        val worldName = this["world"] as String
        val world = Bukkit.getWorld(worldName) ?: plugin.server.worlds.first()

        return ATM(
            id = (this["id"] as Number).toLong(),
            atmId = this["atm_id"] as String,
            location = Location(
                world,
                (this["x"] as Number).toDouble(),
                (this["y"] as Number).toDouble(),
                (this["z"] as Number).toDouble()
            ),
            bankId = this["bank_id"]?.toString() ?: "SYSTEM",
            cash = (this["cash"] as Number).toDouble(),
            maxWithdrawal = (this["max_withdrawal"] as Number).toDouble(),
            transactionFee = (this["transaction_fee"] as Number).toDouble(),
            outOfNetworkFee = (this["out_of_network_fee"] as? Number)?.toDouble() ?: 5.0,
            active = (this["active"] as Number).toInt() == 1,
            placedBy = UUID.fromString(this["placed_by"] as String),
            createdAt = parseDateTime(this["created_at"] as? String)
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