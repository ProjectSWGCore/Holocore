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
package com.projectswg.holocore.resources.support.data.server_info.loader.conversation

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.holocore.resources.gameplay.conversation.model.Conversation
import com.projectswg.holocore.resources.gameplay.conversation.model.Event
import com.projectswg.holocore.resources.gameplay.conversation.model.PlayerResponse
import com.projectswg.holocore.resources.gameplay.conversation.model.Requirement
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.*
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.events.*
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.requirements.ActiveQuestRequirementParser
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.requirements.CompleteQuestRequirementParser
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.requirements.FactionNameRequirementParser
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.requirements.FactionStatusRequirementParser
import me.joshlarson.jlcommon.log.Log
import me.joshlarson.json.JSON
import me.joshlarson.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

class ConversationLoader : DataLoader() {
	private val spawnConversationsMap: MutableMap<String, MutableCollection<String>> = HashMap()
	private val requirementParserMap: MutableMap<String, RequirementParser<out Requirement>> = HashMap()
	private val eventParserMap: MutableMap<String, EventParser<out Event>> = HashMap()

	fun getConversation(conversationFile: String): Conversation? {
		try {
			val jsonObject = FileInputStream("serverdata/conversation/$conversationFile.json").use { JSON.readObject(it) }

			return readConversation(conversationFile, jsonObject)
		} catch (t: Throwable) {
			Log.e("Unable to load conversation from file %s", conversationFile)
			Log.e(t)
			return null
		}
	}

	fun getInitialConversations(conversationId: String): List<Conversation> {
		val conversationFiles: Collection<String> = spawnConversationsMap[conversationId] ?: return emptyList()

		@Suppress("UNCHECKED_CAST") // It's due to the nulls, but they are explicitly filtered out
		return conversationFiles.stream()
			.map { getConversation(it) }
			.filter { it != null }
			.collect(Collectors.toUnmodifiableList()) as List<Conversation>
	}

	fun getConversationIds(spawnId: String): Collection<String> {
		return spawnConversationsMap.getOrDefault(spawnId, emptyList())
	}

	private fun readConversation(conversationFile: String, jsonObject: JSONObject): Conversation {
		val conversation = Conversation(conversationFile)

		val npcMessageObj = jsonObject["npcMessage"] as Map<String, Any>

		val npcMessage = readProsePackage(npcMessageObj)
		conversation.npcMessage = npcMessage

		val playerResponseObjs = jsonObject["playerResponses"] as List<Map<String, Any>>
		val playerResponses = readPlayerResponses(playerResponseObjs)

		for (playerResponse in playerResponses) {
			conversation.addPlayerResponse(playerResponse)
		}

		val requirementObjs = jsonObject["requirements"] as List<Map<String, Any>>
		val requirements = readRequirements(requirementObjs)

		for (requirement in requirements) {
			conversation.addRequirement(requirement)
		}

		val eventObjs = jsonObject["events"] as List<Map<String, Any>>
		val events = readEvents(eventObjs)

		for (event in events) {
			conversation.addEvent(event)
		}

		return conversation
	}

	private fun readProsePackage(obj: Map<String, Any>): ProsePackage {
		val file = obj["file"].toString()
		val key = obj["key"].toString()

		return ProsePackage(file, key)
	}

	private fun readPlayerResponses(playerResponseObjs: List<Map<String, Any>>): List<PlayerResponse> {
		val playerResponses: MutableList<PlayerResponse> = ArrayList()

		for (playerResponseObj in playerResponseObjs) {
			val playerMessageRaw = playerResponseObj["playerMessage"] as Map<String, Any>? ?: continue
			val prosePackage = readProsePackage(playerMessageRaw)

			val nextConversationId = playerResponseObj["next"] as String? ?: continue

			val playerResponse = PlayerResponse(prosePackage, nextConversationId)

			playerResponses.add(playerResponse)
		}

		return playerResponses
	}

	private fun readRequirements(requirementObjs: List<Map<String, Any>>): List<Requirement> {
		val requirements: MutableList<Requirement> = ArrayList()

		for (requirementObj in requirementObjs) {
			val type = requirementObj["type"] as String?
			val args = requirementObj["args"] as Map<String, Any>
			val requirementParser = requirementParserMap[type]!!
			val requirement = requirementParser.parse(args)

			requirements.add(requirement)
		}

		return requirements
	}

	private fun readEvents(eventObjs: List<Map<String, Any>>): List<Event> {
		val events: MutableList<Event> = ArrayList()

		for (eventObj in eventObjs) {
			val type = eventObj["type"] as String?
			val args = eventObj["args"] as Map<String, Any>
			val eventParser = eventParserMap[type]!!
			val event = eventParser.parse(args)

			events.add(event)
		}

		return events
	}

	@Throws(IOException::class)
	override fun load() {
		initRequirementParsers()
		initEventParsers()
		loadSpawnToConversations()
	}

	private fun initRequirementParsers() {
		requirementParserMap["faction_name"] = FactionNameRequirementParser()
		requirementParserMap["faction_status"] = FactionStatusRequirementParser()
		requirementParserMap["active_quest"] = ActiveQuestRequirementParser()
		requirementParserMap["complete_quest"] = CompleteQuestRequirementParser()
	}

	private fun initEventParsers() {
		eventParserMap["faction_change"] = ChangePlayerFactionEventParser()
		eventParserMap["player_animation"] = PlayerAnimationEventParser()
		eventParserMap["npc_animation"] = NpcAnimationEventParser()
		eventParserMap["show_sellable_items"] = ShowSellableItemsEventParser()
		eventParserMap["grant_buff"] = GrantBuffEventParser()
		eventParserMap["grant_quest"] = GrantQuestEventParser()
		eventParserMap["show_available_skills"] = ShowAvailableSkillsEventParser()
		eventParserMap["emit_quest_signal"] = EmitQuestSignalEventParser()
		eventParserMap["teleport"] = TeleportEventParser()
	}

	@Throws(IOException::class)
	private fun loadSpawnToConversations() {
		SdbLoader.load(File("serverdata/conversation/spawn_conversation_map.msdb")).use { set ->
			while (set.next()) {
				val conversationId = set.getText("conversation_id")
				val conversationFile = set.getText("conversation_file")

				spawnConversationsMap.computeIfAbsent(conversationId) { ArrayList() }.add(conversationFile)
			}
		}
	}
}
