// src/main/java/net/crewco/Banking/api/events/CardEvent.kt
package net.crewco.Banking.api.events

import net.crewco.Banking.data.models.Card
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class CardEvent(val card: Card, val action: CardAction) : Event() {

    enum class CardAction {
        ISSUED,
        USED,
        FROZEN,
        UNFROZEN,
        CANCELLED
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS
}