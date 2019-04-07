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
package com.projectswg.holocore.services.support.global.zone;

import com.projectswg.common.data.BCrypt;
import com.projectswg.common.data.encodables.galaxy.Galaxy;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.ErrorMessage;
import com.projectswg.common.network.packets.swg.holo.login.HoloLoginRequestPacket;
import com.projectswg.common.network.packets.swg.holo.login.HoloLoginResponsePacket;
import com.projectswg.common.network.packets.swg.login.*;
import com.projectswg.common.network.packets.swg.login.EnumerateCharacterId.SWGCharacter;
import com.projectswg.common.network.packets.swg.login.creation.DeleteCharacterRequest;
import com.projectswg.common.network.packets.swg.login.creation.DeleteCharacterResponse;
import com.projectswg.common.network.packets.swg.zone.GameServerLagResponse;
import com.projectswg.common.network.packets.swg.zone.LagRequest;
import com.projectswg.common.network.packets.swg.zone.ServerNowEpochTime;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.login.LoginEventIntent;
import com.projectswg.holocore.intents.support.global.login.LoginEventIntent.LoginEvent;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.creation.DeleteCharacterIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgUserDatabase.UserMetadata;
import com.projectswg.holocore.resources.support.global.network.DisconnectReason;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.Player.PlayerServer;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class LoginService extends Service {
	
	private static final String REQUIRED_VERSION = "20111130-15:46";
	private static final byte [] SESSION_TOKEN = new byte[24];
	
	private final Map<String, List<CreatureObject>> players;
	
	public LoginService() {
		this.players = new HashMap<>();
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject obj = oci.getObject();
		if (!(obj instanceof PlayerObject))
			return;
		PlayerObject player = (PlayerObject) obj;
		CreatureObject creature = (CreatureObject) player.getParent();
		if (creature == null)
			return;
		players.computeIfAbsent(player.getAccount(), a -> new CopyOnWriteArrayList<>()).add(creature);
	}
	
	@IntentHandler
	private void handleDeleteCharacterIntent(DeleteCharacterIntent dci) {
		SWGObject obj = dci.getCreature();
		if (PswgDatabase.objects().removeObject(obj.getObjectId())) {
			DestroyObjectIntent.broadcast(obj);
			Player owner = obj.getOwner();
			if (owner != null)
				CloseConnectionIntent.broadcast(owner, DisconnectReason.APPLICATION);
		}
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket p = gpi.getPacket();
		if (p instanceof HoloLoginRequestPacket) {
			handleLogin(gpi.getPlayer(), (HoloLoginRequestPacket) p);
		} else if (p instanceof LoginClientId) {
			handleLogin(gpi.getPlayer(), (LoginClientId) p);
		} else if (p instanceof DeleteCharacterRequest) {
			handleCharDeletion(gpi.getPlayer(), (DeleteCharacterRequest) p);
		} else if (p instanceof LagRequest) {
			Player player = gpi.getPlayer();
			if (player.getPlayerServer() == PlayerServer.LOGIN)
				handleLagRequest(player);
		}
	}
	
	private String getServerString() {
		String name = PswgDatabase.config().getString(this, "loginServerName", "LoginServer");
		int id = PswgDatabase.config().getInt(this, "loginServerId", 1);
		return name + ':' + id;
	}
	
	private void handleLogin(Player player, HoloLoginRequestPacket loginRequest) {
		if (player.getPlayerState() == PlayerState.LOGGED_IN) { // Client occasionally sends multiple login requests
			sendLoginSuccessPacket(player);
			return;
		}
		assert player.getPlayerState() == PlayerState.CONNECTED;
		assert player.getPlayerServer() == PlayerServer.NONE;
		player.setPlayerState(PlayerState.LOGGING_IN);
		player.setPlayerServer(PlayerServer.LOGIN);
		
		UserMetadata user = PswgDatabase.users().getUser(loginRequest.getUsername());
		player.setUsername(loginRequest.getUsername());
		if (user == null) {
			StandardLog.onPlayerEvent(this, player, "failed to login [incorrect username] from %s", loginRequest.getSocketAddress());
			onInvalidUserPass(player);
			player.sendPacket(new HoloLoginResponsePacket(false, "Incorrect username"));
		} else if (user.isBanned()) {
			StandardLog.onPlayerEvent(this, player, "failed to login [banned] from %s", loginRequest.getSocketAddress());
			onLoginBanned(player);
			player.sendPacket(new HoloLoginResponsePacket(false, "Sorry, you're banned!"));
		} else if (isUserValid(user, loginRequest.getPassword())) {
			StandardLog.onPlayerEvent(this, player, "logged in from %s", loginRequest.getSocketAddress());
			onSuccessfulLogin(user, player);
			player.sendPacket(new HoloLoginResponsePacket(true, "", getGalaxies(), getCharacters(user.getUsername())));
		} else {
			StandardLog.onPlayerEvent(this, player, "failed to login [incorrect password] from %s", loginRequest.getSocketAddress());
			onInvalidUserPass(player);
			player.sendPacket(new HoloLoginResponsePacket(false, "Incorrect password"));
		}
	}
	
	private void handleLagRequest(Player player) {
		player.sendPacket(new GameServerLagResponse());
	}
	
	private void handleCharDeletion(Player player, DeleteCharacterRequest request) {
		SWGObject obj = ObjectLookup.getObjectById(request.getPlayerId());
		boolean success;
		if (obj instanceof CreatureObject) {
			success = PswgDatabase.objects().removeObject(obj.getObjectId());
			players.getOrDefault(player.getAccountId(), new ArrayList<>()).remove(obj);
		} else {
			success = false;
		}
		player.sendPacket(new DeleteCharacterResponse(success));
		if (success) {
			DestroyObjectIntent.broadcast(obj);
			Player owner = obj.getOwner();
			if (owner != null)
				CloseConnectionIntent.broadcast(owner, DisconnectReason.APPLICATION);
			StandardLog.onPlayerEvent(this, player, "deleted character %s from %s", obj, request.getSocketAddress());
		} else {
			if (obj == null)
				StandardLog.onPlayerError(this, player, "failed to delete character %d from %s", request.getPlayerId(), request.getSocketAddress());
			else
				StandardLog.onPlayerError(this, player, "failed to delete character %s from %s", obj, request.getSocketAddress());
		}
	}
	
	private void handleLogin(Player player, LoginClientId id) {
		if (player.getPlayerState() == PlayerState.LOGGED_IN) { // Client occasionally sends multiple login requests
			sendLoginSuccessPacket(player);
			return;
		}
		assert player.getPlayerState() == PlayerState.CONNECTED;
		assert player.getPlayerServer() == PlayerServer.NONE;
		player.setPlayerState(PlayerState.LOGGING_IN);
		player.setPlayerServer(PlayerServer.LOGIN);
		final boolean doClientCheck = PswgDatabase.config().getBoolean(this, "loginVersionChecks", true);
		if (!id.getVersion().equals(REQUIRED_VERSION) && doClientCheck) {
			StandardLog.onPlayerEvent(this, player, "failed to login [incorrect version: %s] from %s", id.getVersion(), id.getSocketAddress());
			onLoginClientVersionError(player, id);
			return;
		}
		
		UserMetadata user = PswgDatabase.users().getUser(id.getUsername());
		player.setUsername(id.getUsername());
		if (user == null) {
			StandardLog.onPlayerEvent(this, player, "failed to login [incorrect username] from %s", id.getSocketAddress());
			onInvalidUserPass(player);
			player.sendPacket(new ErrorMessage("Login Failed!", "Incorrect username", false));
			player.sendPacket(new LoginIncorrectClientId(getServerString(), REQUIRED_VERSION));
		} else if (user.isBanned()) {
			StandardLog.onPlayerEvent(this, player, "failed to login [banned] from %s", id.getSocketAddress());
			onLoginBanned(player);
			player.sendPacket(new ErrorMessage("Login Failed!", "Sorry, you're banned!", false));
		} else if (isUserValid(user, id.getPassword())) {
			StandardLog.onPlayerEvent(this, player, "logged in from %s", id.getSocketAddress());
			onSuccessfulLogin(user, player);
			sendLoginSuccessPacket(player);
		} else {
			StandardLog.onPlayerEvent(this, player, "failed to login [incorrect password] from %s", id.getSocketAddress());
			onInvalidUserPass(player);
			player.sendPacket(new ErrorMessage("Login Failed!", "Incorrect password", false));
			player.sendPacket(new LoginIncorrectClientId(getServerString(), REQUIRED_VERSION));
		}
	}
	
	private void onLoginClientVersionError(Player player, LoginClientId id) {
		player.sendPacket(new ErrorMessage("Login Failed!", "Invalid Client Version Code: " + id.getVersion(), false));
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_INVALID_VERSION_CODE).broadcast();
	}
	
	private void onSuccessfulLogin(UserMetadata user, Player player) {
		switch(user.getAccessLevel()) {
			default:
			case "player": player.setAccessLevel(AccessLevel.PLAYER); break;
			case "warden": player.setAccessLevel(AccessLevel.WARDEN); break;
			case "csr": player.setAccessLevel(AccessLevel.CSR); break;
			case "qa": player.setAccessLevel(AccessLevel.QA); break;
			case "dev": player.setAccessLevel(AccessLevel.DEV); break;
		}
		player.setAccountId(user.getUsername());
		player.setPlayerState(PlayerState.LOGGED_IN);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_SUCCESS).broadcast();
	}
	
	private void onLoginBanned(Player player) {
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_BANNED).broadcast();
	}
	
	private void onInvalidUserPass(Player player) {
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_INVALID_USER_PASS).broadcast();
	}
	
	private void sendLoginSuccessPacket(Player player) {
		LoginClientToken token = new LoginClientToken(SESSION_TOKEN, 0, player.getUsername());
		LoginEnumCluster cluster = new LoginEnumCluster();
		LoginClusterStatus clusterStatus = new LoginClusterStatus();
		List<SWGCharacter> characters = getCharacters(player.getAccountId());
		for (Galaxy g : getGalaxies()) {
			cluster.addGalaxy(g);
			clusterStatus.addGalaxy(g);
		}
		cluster.setMaxCharacters(PswgDatabase.config().getInt(this, "galaxyMaxCharacters", 2));
		player.sendPacket(new ServerNowEpochTime((int)(System.currentTimeMillis()/1E3)));
		player.sendPacket(token);
		player.sendPacket(cluster);
		player.sendPacket(new CharacterCreationDisabled());
		player.sendPacket(new EnumerateCharacterId(characters));
		player.sendPacket(clusterStatus);
	}
	
	private boolean isUserValid(UserMetadata user, String password) {
		if (password.isEmpty())
			return false;
		String dbPass = user.getPassword();
		if (dbPass.length() != 60 && !dbPass.startsWith("$2"))
			return dbPass.equals(password);
		password = BCrypt.hashpw(BCrypt.hashpw(password, dbPass), dbPass);
		return dbPass.equals(password);
	}
	
	private List <Galaxy> getGalaxies() {
		List<Galaxy> galaxies = new ArrayList<>();
		galaxies.add(ProjectSWG.getGalaxy());
		return galaxies;
	}
	
	private List<SWGCharacter> getCharacters(String accountId) {
		List<SWGCharacter> characters = new ArrayList<>();
		List<CreatureObject> creatures = this.players.get(accountId);
		if (creatures != null) {
			for (CreatureObject creature : creatures) {
				characters.add(new SWGCharacter(creature.getObjectName(), creature.getRace().getCrc(), creature.getObjectId(), ProjectSWG.getGalaxy().getId(), 1));
			}
		}
		return characters;
	}
	
}
