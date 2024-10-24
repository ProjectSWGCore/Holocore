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
package com.projectswg.holocore.services.support.global.zone;

import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.login.AccountFeatureBits;
import com.projectswg.common.network.packets.swg.login.ClientIdMsg;
import com.projectswg.common.network.packets.swg.login.ClientPermissionsMessage;
import com.projectswg.common.network.packets.swg.login.ConnectionServerLagResponse;
import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.insertion.SelectCharacter;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.Player.PlayerServer;
import com.projectswg.holocore.resources.support.global.zone.ZoneRequester;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureMood;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

public class ZoneService extends Service {
	
	private final ZoneRequester zoneRequester;
	
	public ZoneService() {
		zoneRequester = new ZoneRequester();
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player player = pei.getPlayer();
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				sendMessageOfTheDay(player);
				break;
			case PE_ZONE_IN_SERVER:
				player.getCreatureObject().setMoodId(CreatureMood.NONE.getMood());
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		Player player = gpi.getPlayer();
		SWGPacket p = gpi.getPacket();
		if (p instanceof ClientIdMsg)
			handleClientIdMsg(player, (ClientIdMsg) p);
		if (p instanceof SetWaypointColor)
			handleSetWaypointColor(player, (SetWaypointColor) p);
		if (p instanceof LagRequest && player.getPlayerServer() == PlayerServer.ZONE)
			handleLagRequest(player);
		if (p instanceof SelectCharacter)
			handleSelectCharacter(player, ((SelectCharacter) p).getCharacterId());
	}
	
	private void handleLagRequest(Player player) {
		player.sendPacket(new ConnectionServerLagResponse());
	}
	
	private void sendMessageOfTheDay(Player player) {
		String message = PswgDatabase.INSTANCE.getConfig().getString(this, "firstZoneMessage", "");
		
		if(!message.isEmpty())	// If the message isn't nothing
			new SystemMessageIntent(player, message).broadcast();	// Send it
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
		StandardLog.onPlayerEvent(this, player, "connected to zone server from %s", clientId.getSocketAddress());
		player.setPlayerServer(PlayerServer.ZONE);
		player.sendPacket(new HeartBeat());
		player.sendPacket(new AccountFeatureBits());
		player.sendPacket(new ClientPermissionsMessage());
	}
	
	private void handleSelectCharacter(Player player, long characterId) {
		SWGObject creatureObj = ObjectLookup.getObjectById(characterId);
		zoneRequester.onZoneRequested(creatureObj, player, characterId);
	}
	
}
