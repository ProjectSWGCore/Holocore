package com.projectswg.holocore.services.support.global.zone;

import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.login.AccountFeatureBits;
import com.projectswg.common.network.packets.swg.login.ClientIdMsg;
import com.projectswg.common.network.packets.swg.login.ClientPermissionsMessage;
import com.projectswg.common.network.packets.swg.login.ConnectionServerLagResponse;
import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.insertion.SelectCharacter;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
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
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		Player player = gpi.getPlayer();
		SWGPacket p = gpi.getPacket();
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
			handleSelectCharacter(player, ((SelectCharacter) p).getCharacterId());
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
