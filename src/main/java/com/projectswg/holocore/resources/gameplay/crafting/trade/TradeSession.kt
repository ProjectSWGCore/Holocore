/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.gameplay.crafting.trade

import com.projectswg.common.network.packets.SWGPacket
import com.projectswg.common.network.packets.swg.zone.trade.*
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class TradeSession(initiator: CreatureObject, receiver: CreatureObject) {
	private val initiator = TradeMember(initiator, receiver)
	private val receiver = TradeMember(receiver, initiator)
	private val status: AtomicReference<TradeStatus>

	init {
		this.status = AtomicReference(TradeStatus.STARTED)
	}

	fun addItem(creature: CreatureObject, tradeObject: SWGObject) {
		verifyInternalState()
		verifyCreature(creature)
		getTradeMember(creature)!!.offerItem(tradeObject)
	}

	fun setMoneyAmount(creature: CreatureObject, amount: Long) {
		verifyInternalState()
		verifyCreature(creature)
		status.set(TradeStatus.TRADING)
		getTradeMember(creature)!!.offerMoney(amount)
	}

	fun setTradeAccepted(creature: CreatureObject, accepted: Boolean) {
		verifyInternalState()
		verifyCreature(creature)
		if (status.get() != TradeStatus.TRADING) {
			Log.w("Trade %s in wrong state (%s) with creature: %s", if (accepted) "accepted" else "unaccepted", status.get(), creature)
			return
		}
		getTradeMember(creature)!!.setAccepted(accepted)
		if (accepted) {
			if (isTradeAccepted) {
				initiator.sendPacket(BeginVerificationMessage())
				receiver.sendPacket(BeginVerificationMessage())
			}
		} else if (initiator.isVerified() || receiver.isVerified()) {
			abortTrade()
		}
	}

	fun setTradeVerified(creature: CreatureObject) {
		verifyInternalState()
		verifyCreature(creature)
		if (!isTradeAccepted) {
			abortTrade()
			return
		}
		status.set(TradeStatus.VERIFYING)
		getTradeMember(creature)!!.verify()
		if (isTradeVerified) {
			status.set(TradeStatus.VERIFIED)
			completeTrade()
		}
	}

	fun isItemTraded(creature: CreatureObject, tradeObject: SWGObject): Boolean {
		verifyInternalState()
		verifyCreature(creature)
		return getTradeMember(creature)!!.isItemTraded(tradeObject)
	}

	val isTradeAccepted: Boolean
		get() {
			verifyInternalState()
			return initiator.isAccepted() && receiver.isAccepted()
		}

	val isTradeVerified: Boolean
		get() {
			verifyInternalState()
			return initiator.isVerified() && receiver.isVerified()
		}

	fun beginTrade() {
		if (!setStatus(TradeStatus.STARTED, TradeStatus.TRADING)) {
			Log.w("Trade begun in wrong state (%s) with %s and %s", status.get(), initiator.creature.objectName, receiver.creature.objectName)
			return
		}
		initiator.sendPacket(BeginTradeMessage(receiver.creature.objectId))
		receiver.sendPacket(BeginTradeMessage(initiator.creature.objectId))
		Log.i("TradeSession started between '%s' and '%s'", initiator.creature.objectName, receiver.creature.objectName)
	}

	fun denyTrade() {
		verifyInternalState()
		status.set(TradeStatus.ENDED)
		Log.i("TradeSession denied between '%s' and '%s'", initiator.creature.objectName, receiver.creature.objectName)
		initiator.denyTrade()
		receiver.denyTrade()
		closeSession()
	}

	fun abortTrade() {
		verifyInternalState()
		status.set(TradeStatus.ENDED)
		Log.i("TradeSession aborted between '%s' and '%s'", initiator.creature.objectName, receiver.creature.objectName)
		initiator.abortTrade()
		receiver.abortTrade()
		closeSession()
	}

	private fun completeTrade() {
		verifyInternalState()
		if (!isTradeVerified || !setStatus(TradeStatus.VERIFIED, TradeStatus.ENDED)) {
			Log.w("Invalid trade session status '%s' for completing the trade!", status.get())
			return
		}

		initiator.completeTrade()
		receiver.completeTrade()
		closeSession()
	}

	val isValidSession: Boolean
		get() {
			val valid = status.get() != TradeStatus.ENDED && initiator.creature.isLoggedInPlayer && receiver.creature.isLoggedInPlayer
			if (!valid) {
				Log.w("Invalid TradeSession. TradeStatus: %s  initiatorLoggedIn=%b  receiverLoggedIn=%b", status.get(), initiator.creature.isLoggedInPlayer, receiver.creature.isLoggedInPlayer)
				closeSession()
			}
			return valid
		}

	private fun closeSession() {
		status.set(TradeStatus.ENDED)
		initiator.creature.tradeSession = null
		receiver.creature.tradeSession = null
	}

	private fun verifyInternalState() {
		check(isValidSession) { "TradeSession is already ended!" }
	}

	private fun verifyCreature(creature: CreatureObject) {
		require(!(!initiator.isCreature(creature) && !receiver.isCreature(creature))) { "Creature is not a part of this trade session!" }
	}

	private fun getTradeMember(creature: CreatureObject): TradeMember? {
		if (initiator.isCreature(creature)) return initiator
		if (receiver.isCreature(creature)) return receiver
		return null
	}


	private fun setStatus(expectedStatus: TradeStatus, newStatus: TradeStatus): Boolean {
		return status.compareAndSet(expectedStatus, newStatus)
	}

	private enum class TradeStatus {
		STARTED,
		TRADING,
		VERIFYING,
		VERIFIED,
		ENDED
	}

	private class TradeMember(val creature: CreatureObject, private val partner: CreatureObject) {
		private val itemsOffered: MutableList<SWGObject> = CopyOnWriteArrayList()
		private val moneyOffered = AtomicLong(0)
		private val accepted = AtomicBoolean(false)
		private val verified = AtomicBoolean(false)

		fun isCreature(creature: CreatureObject): Boolean {
			return this.creature == creature
		}

		fun offerItem(item: SWGObject): Boolean {
			if (item.superParent !== creature) { // got to have the item on ya
				return false
			}
			invalidateAcceptance()
			itemsOffered.add(item)
			partner.setAware(AwarenessType.TRADE, itemsOffered)
			sendToPartner(AddItemMessage(item.objectId))
			return true
		}

		fun offerMoney(money: Long): Boolean {
			if (money > creature.cashBalance) // can only trade in cash
				return false
			invalidateAcceptance()
			moneyOffered.set(money)
			sendToPartner(GiveMoneyMessage(money.toInt()))
			return true
		}

		fun sendPacket(packet: SWGPacket?) {
			creature.owner!!.sendPacket(packet)
		}

		fun sendToPartner(packet: SWGPacket?) {
			partner.owner!!.sendPacket(packet)
		}

		fun setAccepted(accepted: Boolean) {
			this.accepted.getAndSet(accepted)
			if (accepted) sendToPartner(AcceptTransactionMessage())
			else sendToPartner(UnAcceptTransactionMessage())
		}

		fun verify() {
			verified.set(true)
			sendToPartner(VerifyTradeMessage())
		}

		fun isItemTraded(tradeObject: SWGObject): Boolean {
			return itemsOffered.contains(tradeObject)
		}

		fun isAccepted(): Boolean {
			return accepted.get()
		}

		fun isVerified(): Boolean {
			return verified.get()
		}

		fun abortTrade() {
			sendPacket(AbortTradeMessage())
		}

		fun denyTrade() {
			sendPacket(DenyTradeMessage())
			sendPacket(AbortTradeMessage())
		}

		fun completeTrade() {
			val inventory = partner.getSlottedObject("inventory")

			for (tradeObject in itemsOffered) {
				tradeObject.moveToContainer(inventory)
			}
			partner.setAware(AwarenessType.TRADE, emptyList())

			val cash = moneyOffered.get()
			creature.removeFromCashAndBank(cash)
			partner.removeFromCashAndBank(-cash)

			Log.d("Completed trade. [%s -> %s] Cash: %d  Items: %s", creature, partner, cash, itemsOffered)
			sendPacket(TradeCompleteMessage())
		}

		fun invalidateAcceptance() {
			accepted.set(false)
			verified.set(false)
		}
	}
}
