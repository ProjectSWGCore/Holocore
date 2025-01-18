/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.services.gameplay.conversation

import com.projectswg.common.data.encodables.oob.OutOfBandPackage
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.location.Location.LocationBuilder
import com.projectswg.common.network.packets.swg.zone.object_controller.conversation.NpcConversationMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.conversation.NpcConversationOptions
import com.projectswg.common.network.packets.swg.zone.object_controller.conversation.StartNpcConversation
import com.projectswg.common.network.packets.swg.zone.object_controller.conversation.StopNpcConversation
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent
import com.projectswg.holocore.intents.gameplay.conversation.ProgressConversationIntent
import com.projectswg.holocore.intents.gameplay.conversation.StartConversationIntent
import com.projectswg.holocore.intents.gameplay.conversation.StopConversationIntent
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.gameplay.conversation.model.Conversation
import com.projectswg.holocore.resources.gameplay.conversation.model.PlayerResponse
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.stream.Collectors

class ConversationService : Service() {
	private val sessions = HashMap<CreatureObject, Session>()

	@IntentHandler
	private fun handleCombatStartedIntent(intent: EnterCombatIntent) {
		val source = intent.source
		val target = intent.target

		if (source is CreatureObject && isConversing(source)) {
			abortConversation(source)
		}

		if (target is CreatureObject && isConversing(target)) {
			abortConversation(target)
		}
	}

	@IntentHandler
	private fun handleMoveObjectIntent(intent: MoveObjectIntent) {
		val `object` = intent.obj as? CreatureObject ?: return

		if (!isConversing(`object`)) {
			return
		}

		val session = sessions[`object`]
		val npc = session!!.npc

		if (isWithinRange(`object`, npc)) {
			return
		}


		// At this point, the conversation should be interrupted
		abortConversation(`object`)
	}

	@IntentHandler
	private fun handleObjectCreatedIntent(intent: ObjectCreatedIntent) {
		val `object` = intent.obj as? AIObject ?: return

		if (!isConversableNpc(`object`)) {
			return
		}

		`object`.addOptionFlags(OptionFlag.CONVERSABLE)
	}

	@IntentHandler
	private fun handleStartConversationIntent(intent: StartConversationIntent) {
		val npc = intent.npc
		val starter = intent.starter

		val spawner = npc.spawner ?: return
		val conversationId = spawner.conversationId

		val conversations = ServerData.conversationLoader.getInitialConversations(conversationId)

		val conversation = reduce(conversations, starter.owner!!)

		if (conversation == null) {
			StandardLog.onPlayerEvent(this, starter, "No eligible conversations for spawner with ID %s", spawner.id)
			return
		}

		progressConversation(starter, npc, conversation)

		rotateNpc(npc, starter)
	}

	@IntentHandler
	private fun handleProgressConversationIntent(intent: ProgressConversationIntent) {
		val selection = intent.selection
		val starter = intent.starter

		val session = sessions[starter]
		val currentConversation = session!!.conversation
		val npc = session.npc

		val filteredResponses = getFilteredResponses(starter, currentConversation)
		val selectedResponse = filteredResponses[selection]
		val nextConversationId = selectedResponse.next

		val nextConversation = ServerData.conversationLoader.getConversation(nextConversationId)

		if (nextConversation == null) {
			StandardLog.onPlayerError(this, starter, "Unable to progress conversation, conversation by ID %s does not exist", nextConversationId)
			return
		}

		progressConversation(starter, npc, nextConversation)
	}

	@IntentHandler
	private fun handleStopConversationIntent(intent: StopConversationIntent) {
		val creatureObject = intent.creatureObject

		if (isConversing(creatureObject)) {
			val session = sessions.remove(creatureObject)

			if (!isWithinRange(creatureObject, session!!.npc)) {
				abortConversation(creatureObject)
			}
		}
	}

	private fun isConversableNpc(npc: AIObject): Boolean {
		val spawner = npc.spawner ?: return false
		val conversationId = spawner.conversationId

		val spawnConversationIds = ServerData.conversationLoader.getConversationIds(conversationId)

		return !spawnConversationIds.isEmpty()
	}

	private fun reduce(conversations: List<Conversation>, player: Player): Conversation? {
		var conversation: Conversation? = null

		for (candidate in conversations) {
			if (candidate.isAllowed(player)) {
				conversation = candidate
				break
			}
		}

		return conversation
	}

	private fun getFilteredResponses(starter: CreatureObject, conversation: Conversation): List<PlayerResponse> {
		val playerResponses = conversation.getPlayerResponses()

		val conversationLoader = ServerData.conversationLoader
		return playerResponses.stream().filter { playerResponse: PlayerResponse ->
			val nextConversationId = playerResponse.next
			val nextConversation = conversationLoader.getConversation(nextConversationId) ?: return@filter false
			nextConversation.isAllowed(starter.owner!!)
		}.collect(Collectors.toList())
	}

	private fun progressConversation(starter: CreatureObject, npc: AIObject, conversation: Conversation) {
		val npcMessageProse = conversation.npcMessage

		val filteredResponses = getFilteredResponses(starter, conversation)
		val replies = filteredResponses.stream().map(PlayerResponse::prosePackage)
			.map { outOfBandData: ProsePackage? -> OutOfBandPackage(outOfBandData!!) }
			.peek { outOfBandPackage: OutOfBandPackage -> outOfBandPackage.isConversation = true }
			.collect(Collectors.toList())

		if (!sessions.containsKey(starter)) {
			starter.sendSelf(StartNpcConversation(npc.objectId, starter.objectId))
		}

		if (filteredResponses.isEmpty()) {
			sessions.remove(starter)
			starter.sendSelf(StopNpcConversation(starter.objectId, npc.objectId, npcMessageProse!!.base))
		} else {
			val npcMessage = OutOfBandPackage(npcMessageProse!!)
			npcMessage.isConversation = true
			val session = Session(conversation, npc)

			sessions[starter] = session
			starter.sendSelf(NpcConversationMessage(starter.objectId, npcMessage))
			starter.sendSelf(NpcConversationOptions(starter.objectId, replies))
		}

		val events = conversation.getEvents()

		for (event in events) {
			try {
				event.trigger(starter.owner!!, npc)
			} catch (t: Throwable) {
				StandardLog.onPlayerError(this, starter, "Error while triggering conversation event of type %s", event.javaClass.simpleName)
				Log.e(t)
			}
		}
	}

	private fun rotateNpc(npc: AIObject, starter: CreatureObject) {
		val oldNpcWorldLocation = npc.location
		val starterWorldLocation = starter.location.position

		val headingTo = oldNpcWorldLocation.getHeadingTo(starterWorldLocation)

		val newNpcWorldLocation = LocationBuilder(oldNpcWorldLocation).setHeading(headingTo).build()

		MoveObjectIntent(npc, npc.parent, newNpcWorldLocation, 0.0).broadcast()
	}

	private fun abortConversation(creatureObject: CreatureObject) {
		val session = sessions.remove(creatureObject)

		abortConversation(creatureObject, session!!)
	}

	private fun abortConversation(creatureObject: CreatureObject, session: Session) {
		val conversation = session.conversation
		val npc = session.npc
		val npcMessage = conversation.npcMessage
		val npcMessageBase = npcMessage!!.base

		val stopNpcConversation = StopNpcConversation(creatureObject.objectId, npc.objectId, npcMessageBase)

		creatureObject.sendSelf(stopNpcConversation)
	}

	private fun isConversing(creatureObject: CreatureObject): Boolean {
		return sessions.containsKey(creatureObject)
	}

	private fun isWithinRange(creature: CreatureObject, npc: AIObject): Boolean {
		val playerWorldLocation = creature.worldLocation
		val npcWorldLocation = npc.worldLocation

		val playerTerrain = playerWorldLocation.terrain
		val npcTerrain = npcWorldLocation.terrain

		if (playerTerrain != npcTerrain) {
			return false
		}

		val distance = playerWorldLocation.distanceTo(npcWorldLocation)

		return distance <= ALLOWED_DISTANCE
	}

	@JvmRecord
	private data class Session(val conversation: Conversation, val npc: AIObject)
	companion object {
		private const val ALLOWED_DISTANCE = 7.0 // Max allowed amount of distance between player and NPC before conversation is interrupted
	}
}
