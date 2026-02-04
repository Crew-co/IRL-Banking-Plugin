// src/main/java/net/crewco/Banking/api/events/TransactionEvent.kt
package net.crewco.Banking.api.events

import net.crewco.Banking.data.models.Transaction
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TransactionEvent(val transaction: Transaction) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS
}