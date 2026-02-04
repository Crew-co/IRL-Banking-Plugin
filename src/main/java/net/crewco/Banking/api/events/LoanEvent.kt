// src/main/java/net/crewco/Banking/api/events/LoanEvent.kt
package net.crewco.Banking.api.events

import net.crewco.Banking.data.models.Loan
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class LoanEvent(val loan: Loan, val action: LoanAction) : Event() {

    enum class LoanAction {
        APPLIED,
        APPROVED,
        REJECTED,
        DISBURSED,
        PAYMENT_MADE,
        PAID_OFF,
        DEFAULTED
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS
}