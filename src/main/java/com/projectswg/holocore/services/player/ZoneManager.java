/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.player;

import com.projectswg.common.control.Manager;
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.login.AccountFeatureBits;
import com.projectswg.common.network.packets.swg.login.ClientIdMsg;
import com.projectswg.common.network.packets.swg.login.ClientPermissionsMessage;
import com.projectswg.common.network.packets.swg.login.ConnectionServerLagResponse;
import com.projectswg.common.network.packets.swg.zone.HeartBeat;
import com.projectswg.common.network.packets.swg.zone.LagRequest;
import com.projectswg.common.network.packets.swg.zone.SetWaypointColor;
import com.projectswg.common.network.packets.swg.zone.ShowBackpack;
import com.projectswg.common.network.packets.swg.zone.ShowHelmet;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.insertion.SelectCharacter;

import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.GalacticIntent;
import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.network.GalacticPacketIntent;
import com.projectswg.holocore.resources.config.ConfigFile;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureMood;
import com.projectswg.holocore.resources.objects.player.PlayerObject;
import com.projectswg.holocore.resources.objects.waypoint.WaypointObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.player.Player.PlayerServer;
import com.projectswg.holocore.resources.player.PlayerEvent;
import com.projectswg.holocore.resources.server_info.DataManager;
import com.projectswg.holocore.services.objects.ObjectManager;
import com.projectswg.holocore.services.player.zone.ZoneRequester;

public class ZoneManager extends Manager {
	
	private final CharacterCreationService characterCreationService;
	private final ZoneRequester zoneRequester;
	
	public ZoneManager() {
		characterCreationService = new CharacterCreationService();
		zoneRequester = new ZoneRequester();
		
		addChildService(characterCreationService);
		
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei.getPlayer(), pei.getEvent()));
		registerForIntent(GalacticPacketIntent.class, gpi -> handlePacket(gpi, gpi.getPlayer(), gpi.getPacket()));
	}
	
	private void handlePlayerEventIntent(Player player, PlayerEvent event) {
		switch (event) {
			case PE_FIRST_ZONE:
				player.getPlayerObject().initStartPlayTime();
				sendVersion(player);
				sendMessageOfTheDay(player);
				break;
			case PE_ZONE_IN_SERVER:
				player.getCreatureObject().setMoodId(CreatureMood.NONE.getMood());
				break;
			default:
				break;
		}
	}
	
	private void handlePacket(GalacticIntent intent, Player player, SWGPacket p) {
		characterCreationService.handlePacket(intent, player, p);
		if (p instanceof ClientIdMsg)
			handleClientIdMsg(player, (ClientIdMsg) p);
		if (p instanceof SetWaypointColor)
			handleSetWaypointColor(player, (SetWaypointColor) p);
		if(p instanceof ShowBackpack)
			handleShowBackpack(player, (ShowBackpack) p);
		if(p instanceof ShowHelmet)
			handleShowHelmet(player, (ShowHelmet) p);
		if (p instanceof LagRequest && player.getPlayerServer() == PlayerServer.ZONE)
			handleLagRequest(player);
		if (p instanceof SelectCharacter)
			handleSelectCharacter(intent.getObjectManager(), player, ((SelectCharacter) p).getCharacterId());
	}
	
	public boolean characterExistsForName(String name) {
		return characterCreationService.characterExistsForName(name);
	}
	
	private void handleLagRequest(Player player) {
		player.sendPacket(new ConnectionServerLagResponse());
	}
	
	private void sendVersion(Player player) {
		player.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT_BOX, "This server runs Holocore " + ProjectSWG.VERSION));
	}
	
	private void sendMessageOfTheDay(Player player) {
		String message = DataManager.getConfig(ConfigFile.FEATURES).getString("FIRST-ZONE-MESSAGE", "");
		
		if(!message.isEmpty())	// If the message isn't nothing
			new SystemMessageIntent(player, message).broadcast();	// Send it
	}
	
	private void handleShowBackpack(Player player, ShowBackpack p) {
		player.getPlayerObject().setShowBackpack(p.showingBackpack());
	}
	
	private void handleShowHelmet(Player player, ShowHelmet p) {
		player.getPlayerObject().setShowHelmet(p.showingHelmet());
	}

	private void handleSetWaypointColor(Player player, SetWaypointColor p) {
		// TODO Should move this to a different service, maybe make a service for other SWGPackets similar to this (ie misc.)
		PlayerObject ghost = player.getPlayerObject();
		
		WaypointObject waypoint = ghost.getWaypoint(p.getObjId());
		if (waypoint == null)
			return;
		
		switch(p.getColor()) {
			case "blue": waypoint.setColor(WaypointColor.BLUE); break;
			case "green": waypoint.setColor(WaypointColor.GREEN); break;
			case "orange": waypoint.setColor(WaypointColor.ORANGE); break;
			case "yellow": waypoint.setColor(WaypointColor.YELLOW); break;
			case "purple": waypoint.setColor(WaypointColor.PURPLE); break;
			case "white": waypoint.setColor(WaypointColor.WHITE); break;
			default: Log.e("Don't know color %s", p.getColor()); break;
		}
		
		ghost.updateWaypoint(waypoint);
	}
	
	private void handleClientIdMsg(Player player, ClientIdMsg clientId) {
		Log.i("%s connected to the zone server from %s", player.getUsername(), clientId.getSocketAddress());
		player.setPlayerServer(PlayerServer.ZONE);
		player.sendPacket(new HeartBeat());
		player.sendPacket(new AccountFeatureBits());
		player.sendPacket(new ClientPermissionsMessage());
	}
	
	private void handleSelectCharacter(ObjectManager objectManager, Player player, long characterId) {
		SWGObject creatureObj = objectManager.getObjectById(characterId);
		zoneRequester.onZoneRequested(creatureObj, player, characterId);
	}
	
}
