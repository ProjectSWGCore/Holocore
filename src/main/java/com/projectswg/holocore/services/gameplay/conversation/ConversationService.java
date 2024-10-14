/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.conversation;

import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.zone.object_controller.conversation.NpcConversationMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.conversation.NpcConversationOptions;
import com.projectswg.common.network.packets.swg.zone.object_controller.conversation.StartNpcConversation;
import com.projectswg.common.network.packets.swg.zone.object_controller.conversation.StopNpcConversation;
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.gameplay.conversation.ProgressConversationIntent;
import com.projectswg.holocore.intents.gameplay.conversation.StartConversationIntent;
import com.projectswg.holocore.intents.gameplay.conversation.StopConversationIntent;
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent;
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent;
import com.projectswg.holocore.resources.gameplay.conversation.model.Conversation;
import com.projectswg.holocore.resources.gameplay.conversation.model.Event;
import com.projectswg.holocore.resources.gameplay.conversation.model.PlayerResponse;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.ConversationLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConversationService extends Service {
	
	private static final double ALLOWED_DISTANCE = 7.0;	// Max allowed amount of distance between player and NPC before conversation is interrupted
	
	private final Map<CreatureObject, Session> sessions;
	
	public ConversationService() {
		sessions = new HashMap<>();
	}
	
	@IntentHandler
	private void handleCombatStartedIntent(EnterCombatIntent intent) {
		TangibleObject source = intent.getSource();
		TangibleObject target = intent.getTarget();
		
		if (source instanceof CreatureObject sourceCreature && isConversing(sourceCreature)) {
			abortConversation(sourceCreature);
		}
		
		if (target instanceof CreatureObject sourceTarget && isConversing(sourceTarget)) {
			abortConversation(sourceTarget);
		}
	}
	
	@IntentHandler
	private void handleMoveObjectIntent(MoveObjectIntent intent) {
		SWGObject object = intent.getObj();
		
		if (!(object instanceof CreatureObject creatureObject)) {
			return;
		}

		if (!isConversing(creatureObject)) {
			return;
		}
		
		Session session = sessions.get(creatureObject);
		AIObject npc = session.npc();
		
		if (isWithinRange(creatureObject, npc)) {
			return;
		}
		
		// At this point, the conversation should be interrupted
		abortConversation(creatureObject);
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent intent) {
		SWGObject object = intent.getObj();
		
		if (!(object instanceof AIObject npc)) {
			return;
		}

		if (!isConversableNpc(npc)) {
			return;
		}
		
		npc.addOptionFlags(OptionFlag.CONVERSABLE);
	}
	
	@IntentHandler
	private void handleStartConversationIntent(StartConversationIntent intent) {
		ConversationLoader conversationLoader = ServerData.INSTANCE.getConversationLoader();
		AIObject npc = intent.getNpc();
		CreatureObject starter = intent.getStarter();
		
		Spawner spawner = npc.getSpawner();
		if (spawner == null)
			return;
		String conversationId = spawner.getConversationId();

		List<Conversation> conversations = conversationLoader.getInitialConversations(conversationId);
		
		Conversation conversation = reduce(conversations, starter.getOwner());
		
		if (conversation == null) {
			StandardLog.onPlayerEvent(this, starter, "No eligible conversations for spawner with ID %s", spawner.getId());
			return;
		}
		
		progressConversation(starter, npc, conversation);
		
		rotateNpc(npc, starter);
	}
	
	@IntentHandler
	private void handleProgressConversationIntent(ProgressConversationIntent intent) {
		int selection = intent.getSelection();
		CreatureObject starter = intent.getStarter();
		
		Session session = sessions.get(starter);
		Conversation currentConversation = session.conversation();
		AIObject npc = session.npc();
		
		List<PlayerResponse> filteredResponses = getFilteredResponses(starter, currentConversation);
		PlayerResponse selectedResponse = filteredResponses.get(selection);
		String nextConversationId = selectedResponse.getNext();
		ConversationLoader conversationLoader = ServerData.INSTANCE.getConversationLoader();
		
		Conversation nextConversation = conversationLoader.getConversation(nextConversationId);
		
		if (nextConversation == null) {
			StandardLog.onPlayerError(this, starter, "Unable to progress conversation, conversation by ID %s does not exist", nextConversationId);
			return;
		}
		
		progressConversation(starter, npc, nextConversation);
	}
	
	@IntentHandler
	private void handleStopConversationIntent(StopConversationIntent intent) {
		CreatureObject creatureObject = intent.getCreatureObject();
		
		if (isConversing(creatureObject)) {
			Session session = sessions.remove(creatureObject);
			
			if (!isWithinRange(creatureObject, session.npc())) {
				abortConversation(creatureObject);
			}
		}
	}
	
	private boolean isConversableNpc(AIObject npc) {
		ConversationLoader conversationLoader = ServerData.INSTANCE.getConversationLoader();
		Spawner spawner = npc.getSpawner();
		if (spawner == null)
			return false;
		String conversationId = spawner.getConversationId();

		Collection<String> spawnConversationIds = conversationLoader.getConversationIds(conversationId);
		
		return !spawnConversationIds.isEmpty();
	}
	
	@Nullable
	private Conversation reduce(List<Conversation> conversations, Player player) {
		Conversation conversation = null;
		
		for (Conversation candidate : conversations) {
			if (candidate.isAllowed(player)) {
				conversation = candidate;
				break;
			}
		}
		
		return conversation;
	}
	
	private List<PlayerResponse> getFilteredResponses(CreatureObject starter, Conversation conversation) {
		List<PlayerResponse> playerResponses = conversation.getPlayerResponses();
		ConversationLoader conversationLoader = ServerData.INSTANCE.getConversationLoader();
		
		return playerResponses.stream()
				.filter(playerResponse -> {
					String nextConversationId = playerResponse.getNext();

					Conversation nextConversation = conversationLoader.getConversation(nextConversationId);
					
					if (nextConversation == null) {
						return false;
					}
					
					return nextConversation.isAllowed(starter.getOwner());
				})
				.collect(Collectors.toList());
	}
	
	private void progressConversation(CreatureObject starter, AIObject npc, Conversation conversation) {
		ProsePackage npcMessageProse = conversation.getNpcMessage();
		
		List<PlayerResponse> filteredResponses = getFilteredResponses(starter, conversation);
		List<OutOfBandPackage> replies = filteredResponses.stream()
				.map(PlayerResponse::getProsePackage)
				.map(OutOfBandPackage::new)
				.peek(outOfBandPackage -> outOfBandPackage.setConversation(true))
				.collect(Collectors.toList());
		
		if (!sessions.containsKey(starter)) {
			starter.sendSelf(new StartNpcConversation(npc.getObjectId(), starter.getObjectId()));
		}
		
		if (filteredResponses.isEmpty()) {
			sessions.remove(starter);
			starter.sendSelf(new StopNpcConversation(starter.getObjectId(), npc.getObjectId(), npcMessageProse.getBase()));
		} else {
			OutOfBandPackage npcMessage = new OutOfBandPackage(npcMessageProse);
			npcMessage.setConversation(true);
			Session session = new Session(conversation, npc);
			
			sessions.put(starter, session);
			starter.sendSelf(new NpcConversationMessage(starter.getObjectId(), npcMessage));
			starter.sendSelf(new NpcConversationOptions(starter.getObjectId(), replies));
		}
		
		List<Event> events = conversation.getEvents();
		
		for (Event event : events) {
			try {
				event.trigger(starter.getOwner(), npc);
			} catch (Throwable t) {
				StandardLog.onPlayerError(this, starter, "Error while triggering conversation event of type %s", event.getClass().getSimpleName());
				Log.e(t);
			}
		}
	}
	
	private void rotateNpc(AIObject npc, CreatureObject starter) {
		Location oldNpcWorldLocation = npc.getLocation();
		Point3D starterWorldLocation = starter.getLocation().getPosition();
		
		double headingTo = oldNpcWorldLocation.getHeadingTo(starterWorldLocation);
		
		Location newNpcWorldLocation = new Location.LocationBuilder(oldNpcWorldLocation)
				.setHeading(headingTo)
				.build();
		
		new MoveObjectIntent(npc, npc.getParent(), newNpcWorldLocation, 0).broadcast();
	}
	
	private void abortConversation(CreatureObject creatureObject) {
		Session session = sessions.remove(creatureObject);
		
		abortConversation(creatureObject, session);
	}
	
	private void abortConversation(CreatureObject creatureObject, Session session) {
		Conversation conversation = session.conversation();
		AIObject npc = session.npc();
		ProsePackage npcMessage = conversation.getNpcMessage();
		StringId npcMessageBase = npcMessage.getBase();
		
		StopNpcConversation stopNpcConversation = new StopNpcConversation(creatureObject.getObjectId(), npc.getObjectId(), npcMessageBase);
		
		creatureObject.sendSelf(stopNpcConversation);
	}
	
	private boolean isConversing(CreatureObject creatureObject) {
		return sessions.containsKey(creatureObject);
	}
	
	private boolean isWithinRange(CreatureObject creature, AIObject npc) {
		Location playerWorldLocation = creature.getWorldLocation();
		Location npcWorldLocation = npc.getWorldLocation();
		
		Terrain playerTerrain = playerWorldLocation.getTerrain();
		Terrain npcTerrain = npcWorldLocation.getTerrain();
		
		if (playerTerrain != npcTerrain) {
			return false;
		}
		
		double distance = playerWorldLocation.distanceTo(npcWorldLocation);
		
		return distance <= ALLOWED_DISTANCE;
	}

	private record Session(Conversation conversation, AIObject npc) {

	}
}
