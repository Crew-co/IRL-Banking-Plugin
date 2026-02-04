// src/main/java/net/crewco/Banking/api/events/AccountCreatedEvent.kt
package net.crewco.Banking.api.events

import net.crewco.Banking.data.models.BankAccount
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class AccountCreatedEvent(val account: BankAccount) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS
}