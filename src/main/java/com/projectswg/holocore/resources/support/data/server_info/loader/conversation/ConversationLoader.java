/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader.conversation;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.holocore.resources.gameplay.conversation.model.Conversation;
import com.projectswg.holocore.resources.gameplay.conversation.model.Event;
import com.projectswg.holocore.resources.gameplay.conversation.model.PlayerResponse;
import com.projectswg.holocore.resources.gameplay.conversation.model.Requirement;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.events.*;
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.requirements.*;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.json.JSON;
import me.joshlarson.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ConversationLoader extends DataLoader {
	
	private final Map<String, Collection<String>> spawnConversationsMap;
	private final Map<String, RequirementParser<? extends Requirement>> requirementParserMap;
	private final Map<String, EventParser<? extends Event>> eventParserMap;
	
	public ConversationLoader() {
		spawnConversationsMap = new HashMap<>();
		requirementParserMap = new HashMap<>();
		eventParserMap = new HashMap<>();
	}
	
	@Nullable
	public Conversation getConversation(String conversationFile) {
		try {
			InputStream inputStream = new FileInputStream("serverdata/conversation/" + conversationFile + ".json");
			JSONObject jsonObject = JSON.readObject(inputStream);
			
			return readConversation(conversationFile, jsonObject);
		} catch (Throwable t) {
			Log.e("Unable to load conversation from file %s", conversationFile);
			Log.e(t);
			return null;
		}
	}
	
	@NotNull
	public List<Conversation> getInitialConversations(String conversationId) {
		if (!spawnConversationsMap.containsKey(conversationId)) {
			return Collections.emptyList();
		}
		
		Collection<String> conversationFiles = spawnConversationsMap.get(conversationId);
		
		return conversationFiles.stream()
				.map(this::getConversation)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
	
	@NotNull
	public Collection<String> getConversationIds(String spawnId) {
		return spawnConversationsMap.getOrDefault(spawnId, Collections.emptyList());
	}
	
	private Conversation readConversation(String conversationFile, JSONObject jsonObject) {
		Conversation conversation = new Conversation(conversationFile);
		
		Map<String, Object> npcMessageObj = (Map<String, Object>) jsonObject.get("npcMessage");
		
		ProsePackage npcMessage = readProsePackage(npcMessageObj);
		conversation.setNpcMessage(npcMessage);
		
		List<Map<String, Object>> playerResponseObjs = (List<Map<String, Object>>) jsonObject.get("playerResponses");
		List<PlayerResponse> playerResponses = readPlayerResponses(playerResponseObjs);
		
		for (PlayerResponse playerResponse : playerResponses) {
			conversation.addPlayerResponse(playerResponse);
		}
		
		List<Map<String, Object>> requirementObjs = (List<Map<String, Object>>) jsonObject.get("requirements");
		List<Requirement> requirements = readRequirements(requirementObjs);
		
		for (Requirement requirement : requirements) {
			conversation.addRequirement(requirement);
		}
		
		List<Map<String, Object>> eventObjs = (List<Map<String, Object>>) jsonObject.get("events");
		List<Event> events = readEvents(eventObjs);
		
		for (Event event : events) {
			conversation.addEvent(event);
		}
		
		return conversation;
	}
	
	private ProsePackage readProsePackage(Map<String, Object> object) {
		String file = object.get("file").toString();
		String key = object.get("key").toString();
		
		return new ProsePackage(file, key);
	}
	
	private List<PlayerResponse> readPlayerResponses(List<Map<String, Object>> playerResponseObjs) {
		List<PlayerResponse> playerResponses = new ArrayList<>();
		
		for (Map<String, Object> playerResponseObj : playerResponseObjs) {
			Map<String, Object> playerMessageRaw = (Map<String, Object>) playerResponseObj.get("playerMessage");
			ProsePackage prosePackage = readProsePackage(playerMessageRaw);
			
			String nextConversationId = (String) playerResponseObj.get("next");
			
			PlayerResponse playerResponse = new PlayerResponse(prosePackage, nextConversationId);
			
			playerResponses.add(playerResponse);
		}
		
		return playerResponses;
	}
	
	private List<Requirement> readRequirements(List<Map<String, Object>> requirementObjs) {
		List<Requirement> requirements = new ArrayList<>();
		
		for (Map<String, Object> requirementObj : requirementObjs) {
			String type = (String) requirementObj.get("type");
			Map<String, Object> args = (Map<String, Object>) requirementObj.get("args");
			RequirementParser<? extends Requirement> requirementParser = requirementParserMap.get(type);
			Requirement requirement = requirementParser.parse(args);
			
			requirements.add(requirement);
		}
		
		return requirements;
	}
	
	private List<Event> readEvents(List<Map<String, Object>> eventObjs) {
		List<Event> events = new ArrayList<>();
		
		for (Map<String, Object> eventObj : eventObjs) {
			String type = (String) eventObj.get("type");
			Map<String, Object> args = (Map<String, Object>) eventObj.get("args");
			EventParser<? extends Event> eventParser = eventParserMap.get(type);
			Event event = eventParser.parse(args);
			
			events.add(event);
		}
		
		return events;
	}
	
	@Override
	public void load() throws IOException {
		initRequirementParsers();
		initEventParsers();
		loadSpawnToConversations();
	}

	private void initRequirementParsers() {
		requirementParserMap.put("faction_name", new FactionNameRequirementParser());
		requirementParserMap.put("faction_status", new FactionStatusRequirementParser());
		requirementParserMap.put("active_quest", new ActiveQuestRequirementParser());
		requirementParserMap.put("complete_quest", new CompleteQuestRequirementParser());
	}
	
	private void initEventParsers() {
		eventParserMap.put("faction_change", new ChangePlayerFactionEventParser());
		eventParserMap.put("player_animation", new PlayerAnimationEventParser());
		eventParserMap.put("npc_animation", new NpcAnimationEventParser());
		eventParserMap.put("show_sellable_items", new ShowSellableItemsEventParser());
		eventParserMap.put("grant_quest", new GrantQuestEventParser());
		eventParserMap.put("advance_quest", new AdvanceQuestEventParser());
		eventParserMap.put("show_available_skills", new ShowAvailableSkillsEventParser());
	}
	
	private void loadSpawnToConversations() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/conversation/spawn_conversation_map.msdb"))) {
			while (set.next()) {
				String conversationId = set.getText("conversation_id");
				String conversationFile = set.getText("conversation_file");
				
				spawnConversationsMap.computeIfAbsent(conversationId, a -> new ArrayList<>()).add(conversationFile);
			}
		}
	}
	
}
