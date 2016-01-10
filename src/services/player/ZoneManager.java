/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package services.player;

import intents.GalacticIntent;
import intents.PlayerEventIntent;
import intents.RequestZoneInIntent;
import intents.chat.ChatBroadcastIntent;
import intents.network.GalacticPacketIntent;
import main.ProjectSWG;
import network.packets.Packet;
import network.packets.swg.login.AccountFeatureBits;
import network.packets.swg.login.ClientIdMsg;
import network.packets.swg.login.ClientPermissionsMessage;
import network.packets.swg.zone.CmdSceneReady;
import network.packets.swg.zone.GalaxyLoopTimesResponse;
import network.packets.swg.zone.HeartBeat;
import network.packets.swg.zone.ParametersMessage;
import network.packets.swg.zone.RequestGalaxyLoopTimes;
import network.packets.swg.zone.SetWaypointColor;
import network.packets.swg.zone.ShowBackpack;
import network.packets.swg.zone.ShowHelmet;
import network.packets.swg.zone.chat.ChatOnConnectAvatar;
import network.packets.swg.zone.chat.ChatSystemMessage;
import network.packets.swg.zone.chat.VoiceChatStatus;
import network.packets.swg.zone.insertion.ChatServerStatus;
import network.packets.swg.zone.insertion.CmdStartScene;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import resources.Location;
import resources.Race;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureMood;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.waypoint.WaypointObject.WaypointColor;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerFlags;
import resources.player.PlayerState;
import resources.server_info.Log;

import java.io.File;
import java.io.IOException;

public class ZoneManager extends Manager {
	
	private final CharacterCreationService characterCreationService;
	
	private String commitHistory;
	
	public ZoneManager() {
		characterCreationService = new CharacterCreationService();
		commitHistory = "";
		
		addChildService(characterCreationService);
		
		registerForIntent(RequestZoneInIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		loadCommitHistory();
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof RequestZoneInIntent) {
			RequestZoneInIntent zii = (RequestZoneInIntent) i;
			zoneInPlayer(zii.getPlayer(), zii.getCreature(), zii.isFirstZone());
		} else if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			handlePacket(gpi, gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId()), gpi.getNetworkId(), gpi.getPacket());
		}
	}
	
	public void handlePacket(GalacticIntent intent, Player player, long networkId, Packet p) {
		characterCreationService.handlePacket(intent, player, networkId, p);
		if (p instanceof ClientIdMsg)
			handleClientIdMsg(player, (ClientIdMsg) p);
		if (p instanceof RequestGalaxyLoopTimes)
			handleGalaxyLoopTimesRequest(player, (RequestGalaxyLoopTimes) p);
		if (p instanceof CmdSceneReady)
			handleCmdSceneReady(player, (CmdSceneReady) p);
		if (p instanceof SetWaypointColor)
			handleSetWaypointColor(player, (SetWaypointColor) p);
		if(p instanceof ShowBackpack)
			handleShowBackpack(player, (ShowBackpack) p);
		if(p instanceof ShowHelmet)
			handleShowHelmet(player, (ShowHelmet) p);
	}
	
	public boolean characterExistsForName(String name) {
		return characterCreationService.characterExistsForName(name);
	}
	
	private void zoneInPlayer(Player player, CreatureObject creature, boolean firstZone) {
		PlayerObject playerObj = creature.getPlayerObject();
		player.setPlayerState(PlayerState.ZONING_IN);
		player.setCreatureObject(creature);
		creature.setOwner(player);
		
		if (firstZone)
			sendZonePackets(player, creature);
		startScene(creature, creature.getLocation());
		if (firstZone)
			playerObj.setStartPlayTime((int) System.currentTimeMillis());
		initPlayerBeforeZoneIn(player, creature, playerObj);
		System.out.printf("[%s] %s is zoning in%n", player.getUsername(), player.getCharacterName());
		Log.i("ObjectManager", "Zoning in %s with character %s", player.getUsername(), player.getCharacterName());
		if (firstZone) {
			new PlayerEventIntent(player, PlayerEvent.PE_FIRST_ZONE).broadcast();
			sendCommitHistory(player);
			sendMessageOfTheDay(player);
		}
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN).broadcast();
	}
	
	private void startScene(CreatureObject object, Location newLocation) {
		long time = (long) (ProjectSWG.getCoreTime() / 1E3);
		Race race = ((CreatureObject)object).getRace();
		sendPacket(object.getOwner(), new CmdStartScene(false, object.getObjectId(), race, newLocation, time, (int)(System.currentTimeMillis()/1E3)));
		recursiveCreateObject(object, object.getOwner());
	}
	
	private void recursiveCreateObject(SWGObject obj, Player p) {
		SWGObject parent = obj.getParent();
		if (parent != null)
			recursiveCreateObject(parent, p);
		obj.createObject(p);
	}
	
	private void loadCommitHistory() {
		File repoDir = new File("./" + Constants.DOT_GIT);
		int commitCount = 3;
		int iterations = 0;
		
		try {
			Git git = Git.open(repoDir);
			Repository repo = git.getRepository();
			
			try {
				commitHistory = "The " + commitCount + " most recent commits in branch '" + repo.getBranch() + "':\n";
				
				for (RevCommit commit : git.log().setMaxCount(commitCount).call()) {
					commitHistory += commit.getName().substring(0, 7) + " " + commit.getShortMessage();
					
					if (commitCount > iterations++)
						commitHistory += "\n";
				}
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			System.err.println("ZoneManager: Failed to open "+repoDir+" to read commit history");
			// An exception is thrown if bash isn't installed.
			// https://www.eclipse.org/forums/index.php/t/1031740/
		}
	}
	
	private void sendCommitHistory(Player player) {
		player.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT, commitHistory));
	}
	
	private void sendMessageOfTheDay(Player player) {
		String message = getConfig(ConfigFile.FEATURES).getString("FIRST-ZONE-MESSAGE", "");
		
		if(!message.isEmpty())	// If the message isn't nothing
			new ChatBroadcastIntent(player, message).broadcast();	// Send it
	}
	
	private void sendZonePackets(Player player, CreatureObject creature) {
		sendPacket(player, new HeartBeat());
		sendPacket(player, new ChatServerStatus(true));
		sendPacket(player, new VoiceChatStatus());
		sendPacket(player, new ParametersMessage());
		sendPacket(player, new ChatOnConnectAvatar());
	}
	
	private void initPlayerBeforeZoneIn(Player player, CreatureObject creatureObj, PlayerObject playerObj) {
		creatureObj.setMoodId(CreatureMood.NONE.getMood());
		playerObj.clearFlagBitmask(PlayerFlags.LD);	// Ziggy: Clear the LD flag in case it wasn't already.
	}
	
	private void handleShowBackpack(Player player, ShowBackpack p) {
		player.getPlayerObject().setShowBackpack(p.showingBackpack());
	}
	
	private void handleShowHelmet(Player player, ShowHelmet p) {
		player.getPlayerObject().setShowHelmet(p.showingHelmet());
	}

	private void handleSetWaypointColor(Player player, SetWaypointColor p) {
		// TODO Should move this to a different service, maybe make a service for other packets similar to this (ie misc.)
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
			default: System.err.println("Don't know color " + p.getColor());
		}
		
		ghost.updateWaypoint(waypoint);
	}
	
	private void handleCmdSceneReady(Player player, CmdSceneReady p) {
		player.setPlayerState(PlayerState.ZONED_IN);
		player.sendPacket(p);
		System.out.println("[" + player.getUsername() +"] " + player.getCharacterName() + " zoned in");
		Log.i("ZoneService", "%s with character %s zoned in from %s:%d", player.getUsername(), player.getCharacterName(), p.getAddress(), p.getPort());
	}
	
	private void handleClientIdMsg(Player player, ClientIdMsg clientId) {
		System.out.println("[" + player.getUsername() + "] Connected to the zone server. IP: " + clientId.getAddress() + ":" + clientId.getPort());
		Log.i("ZoneService", "%s connected to the zone server from %s:%d", player.getUsername(), clientId.getAddress(), clientId.getPort());
		sendPacket(player.getNetworkId(), new HeartBeat());
		sendPacket(player.getNetworkId(), new AccountFeatureBits());
		sendPacket(player.getNetworkId(), new ClientPermissionsMessage());
	}
	
	private void handleGalaxyLoopTimesRequest(Player player, RequestGalaxyLoopTimes req) {
		sendPacket(player, new GalaxyLoopTimesResponse(ProjectSWG.getCoreTime()/1000));
	}
	
}
