// src/main/java/net/crewco/Banking/listeners/PlayerJoinListener.kt
package net.crewco.Banking.listeners

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import net.crewco.Banking.Startup
import net.crewco.Banking.Startup.Companion.accountService
import net.crewco.Banking.Startup.Companion.plugin
import net.crewco.Banking.services.AccountService
import net.crewco.Banking.util.Messages
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener: Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        plugin.launch {
            // Ensure the player has a wallet
            val wallet = accountService.ensureWalletExists(player.uniqueId)

            // First time player message
            if (wallet.balance == 0.0) {
                val accounts = accountService.getAccountsByPlayer(player.uniqueId)
                if (accounts.size == 1) { // Only the wallet we just created
                    player.sendMessage(Messages.welcome())
                }
            }
        }
    }
}