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
import intents.chat.ChatBroadcastIntent;
import intents.network.GalacticPacketIntent;

import java.io.File;
import java.io.IOException;

import network.packets.Packet;
import network.packets.swg.login.AccountFeatureBits;
import network.packets.swg.login.ClientIdMsg;
import network.packets.swg.login.ClientPermissionsMessage;
import network.packets.swg.login.ConnectionServerLagResponse;
import network.packets.swg.zone.HeartBeat;
import network.packets.swg.zone.LagRequest;
import network.packets.swg.zone.SetWaypointColor;
import network.packets.swg.zone.ShowBackpack;
import network.packets.swg.zone.ShowHelmet;
import network.packets.swg.zone.chat.ChatSystemMessage;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import resources.config.ConfigFile;
import resources.objects.creature.CreatureMood;
import resources.objects.player.PlayerObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.waypoint.WaypointObject.WaypointColor;
import resources.player.Player;
import resources.player.Player.PlayerServer;
import resources.player.PlayerEvent;
import resources.server_info.DataManager;

import com.projectswg.common.control.Manager;
import com.projectswg.common.debug.Log;

public class ZoneManager extends Manager {
	
	private final CharacterCreationService characterCreationService;
	
	private String commitHistory;
	
	public ZoneManager() {
		characterCreationService = new CharacterCreationService();
		commitHistory = "";
		
		addChildService(characterCreationService);
		
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei.getPlayer(), pei.getEvent()));
		registerForIntent(GalacticPacketIntent.class, gpi -> handlePacket(gpi, gpi.getPlayer(), gpi.getPacket()));
	}
	
	@Override
	public boolean initialize() {
		loadCommitHistory();
		return super.initialize();
	}
	
	private void handlePlayerEventIntent(Player player, PlayerEvent event) {
		switch (event) {
			case PE_FIRST_ZONE:
				player.getPlayerObject().initStartPlayTime();
				sendCommitHistory(player);
				sendMessageOfTheDay(player);
				break;
			case PE_ZONE_IN_SERVER:
				player.getCreatureObject().setMoodId(CreatureMood.NONE.getMood());
				break;
			default:
				break;
		}
	}
	
	private void handlePacket(GalacticIntent intent, Player player, Packet p) {
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
	}
	
	public boolean characterExistsForName(String name) {
		return characterCreationService.characterExistsForName(name);
	}
	
	private void handleLagRequest(Player player) {
		player.sendPacket(new ConnectionServerLagResponse());
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
				Log.e(e);
			}
		} catch (IOException e) {
			Log.e("Failed to open %s to read commit history", repoDir);
			// An exception is thrown if bash isn't installed.
			// https://www.eclipse.org/forums/index.php/t/1031740/
		}
	}
	
	private void sendCommitHistory(Player player) {
		player.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT, commitHistory));
	}
	
	private void sendMessageOfTheDay(Player player) {
		String message = DataManager.getConfig(ConfigFile.FEATURES).getString("FIRST-ZONE-MESSAGE", "");
		
		if(!message.isEmpty())	// If the message isn't nothing
			new ChatBroadcastIntent(player, message).broadcast();	// Send it
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
	
}
