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
package com.projectswg.holocore.services.gameplay.trade

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.object_controller.SecureTrade
import com.projectswg.common.network.packets.swg.zone.trade.*
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.resources.gameplay.crafting.trade.TradeSession
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log

class TradeService : Service() {
	private val tradeSessions = mutableListOf<TradeSession>()

	override fun stop(): Boolean {
		for (tradeSession in tradeSessions) {
			tradeSession.abortTrade()
		}
		return super.stop()
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		if (pei.player.creatureObject == null) return
		val tradeSession = pei.player.creatureObject.tradeSession ?: return
		when (pei.event) {
			PlayerEvent.PE_FIRST_ZONE, PlayerEvent.PE_LOGGED_OUT -> tradeSession.abortTrade()
			else                                                 -> {}
		}
	}

	@IntentHandler
	private fun handleInboundPacketIntent(gpi: InboundPacketIntent) {
		when (val packet = gpi.packet) {
			is SecureTrade                -> handleSecureTrade(packet, gpi.player)
			is AbortTradeMessage          -> handleAbortTradeMessage(gpi.player)
			is DenyTradeMessage           -> handleDenyTradeMessage(gpi.player)
			is AcceptTransactionMessage   -> handleAcceptTransactionMessage(gpi.player)
			is UnAcceptTransactionMessage -> handleUnAcceptTransactionMessage(gpi.player)
			is AddItemMessage             -> handleAddItemMessage(packet, gpi.player)
			is GiveMoneyMessage           -> handleGiveMoneyMessage(packet, gpi.player)
			is VerifyTradeMessage         -> handleVerifyTradeMessage(gpi.player)
		}
	}

	private fun handleSecureTrade(packet: SecureTrade, player: Player) {
		val initiator = player.creatureObject
		val accepterObject = ObjectLookup.getObjectById(packet.accepterId)
		if (accepterObject !is CreatureObject || !accepterObject.isPlayer) {
			sendSystemMessage(initiator.owner, "start_fail_target_not_player")
			return
		}
		if (initiator.isInCombat || accepterObject.isInCombat) {
			sendSystemMessage(initiator.owner, "request_player_unreachable_no_obj")
			return
		}
		if (initiator.posture == Posture.INCAPACITATED || accepterObject.posture == Posture.INCAPACITATED) {
			sendSystemMessage(initiator.owner, "player_incapacitated")
			return
		}
		if (initiator.posture == Posture.DEAD || accepterObject.posture == Posture.DEAD) {
			sendSystemMessage(initiator.owner, "player_dead")
			return
		}
		val tradeSession = TradeSession(initiator, accepterObject)
		tradeSessions.add(tradeSession)
		val oldSession = initiator.tradeSession
		initiator.tradeSession = tradeSession
		oldSession?.abortTrade()
		handleTradeSessionRequest(player, initiator, accepterObject)
	}

	private fun handleAbortTradeMessage(player: Player) {
		val creature = player.creatureObject
		val tradeSession = creature.tradeSession
		if (!verifyTradeSession(tradeSession, creature)) return
		tradeSession.abortTrade()
	}

	private fun handleDenyTradeMessage(player: Player) {
		val creature = player.creatureObject
		val tradeSession = creature.tradeSession
		if (!verifyTradeSession(tradeSession, creature)) return
		tradeSession.denyTrade()
	}

	private fun handleAcceptTransactionMessage(player: Player) {
		val creature = player.creatureObject
		val tradeSession = creature.tradeSession
		if (!verifyTradeSession(tradeSession, creature)) return
		tradeSession.setTradeAccepted(creature, true)
	}

	private fun handleUnAcceptTransactionMessage(player: Player) {
		val creature = player.creatureObject
		val tradeSession = creature.tradeSession
		if (!verifyTradeSession(tradeSession, creature)) return
		tradeSession.setTradeAccepted(creature, false)
	}

	private fun handleAddItemMessage(packet: AddItemMessage, player: Player) {
		val creature = player.creatureObject
		val tradeSession = creature.tradeSession
		if (!verifyTradeSession(tradeSession, creature)) return
		val tradeObject = ObjectLookup.getObjectById(packet.objectId)
		if (tradeObject == null || tradeObject.isNoTrade || tradeObject.superParent !== creature) {
			Log.w("Invalid object to trade: %s for creature: %s", tradeObject, creature)
			tradeSession.abortTrade()
			return
		}
		tradeSession.addItem(creature, tradeObject)
	}

	private fun handleGiveMoneyMessage(packet: GiveMoneyMessage, player: Player) {
		val creature = player.creatureObject
		val tradeSession = creature.tradeSession
		if (!verifyTradeSession(tradeSession, creature)) return
		tradeSession.setMoneyAmount(creature, packet.moneyAmount.toLong() and 0x00000000FFFFFFFFL)
	}

	private fun handleVerifyTradeMessage(player: Player) {
		val creature = player.creatureObject
		val tradeSession = creature.tradeSession
		if (!verifyTradeSession(tradeSession, creature)) return
		tradeSession.setTradeVerified(creature)
	}

	private fun handleTradeSessionRequest(packetSender: Player, initiator: CreatureObject, accepter: CreatureObject) {
		val player = accepter.owner ?: return
		SuiMessageBox().run {
			title = "Trade Request"
			prompt = "${initiator.objectName} wants to trade with you. Do you want to accept the request?"
			addOkButtonCallback("handleTradeRequest") { _: SuiEvent, _: Map<String, String> ->
				val tradeSession = initiator.tradeSession ?: return@addOkButtonCallback
				accepter.tradeSession = tradeSession
				tradeSession.beginTrade()
			}
			addCancelButtonCallback("handleTradeRequestDeny") { _: SuiEvent, _: Map<String, String> ->
				packetSender.sendPacket(
					DenyTradeMessage(), AbortTradeMessage()
				)
			}
			display(player)
		}
	}

	private fun verifyTradeSession(session: TradeSession?, creature: CreatureObject): Boolean {
		if (session == null) {
			Log.w("Invalid TradeSession. Creature %s: ", creature)
			val owner = creature.owner
			owner?.sendPacket(DenyTradeMessage(), AbortTradeMessage())
			return false
		}
		return true
	}

	private fun sendSystemMessage(player: Player?, str: String) {
		SystemMessageIntent.broadcastPersonal(player!!, "@ui_trade:$str")
	}
}
