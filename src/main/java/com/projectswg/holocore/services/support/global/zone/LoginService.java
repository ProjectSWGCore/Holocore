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
import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.data.info.Config;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.ErrorMessage;
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
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.creation.DeleteCharacterIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.users.PswgUserDatabase;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.users.PswgUserDatabase.CharacterMetadata;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.users.PswgUserDatabase.UserMetadata;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.Player.PlayerServer;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.ArrayList;
import java.util.List;

public class LoginService extends Service {
	
	private static final String REQUIRED_VERSION = "20111130-15:46";
	private static final byte [] SESSION_TOKEN = new byte[24];
	
	private final PswgUserDatabase userDatabase;
	
	public LoginService() {
		this.userDatabase = new PswgUserDatabase();
	}
	
	@Override
	public boolean initialize() {
		userDatabase.initialize();
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		userDatabase.terminate();
		return super.terminate();
	}
	
	@IntentHandler
	private void handleDeleteCharacterIntent(DeleteCharacterIntent dci) {
		deleteCharacter(dci.getCreature());
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket p = gpi.getPacket();
		if (p instanceof LoginClientId) {
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
		Config c = DataManager.getConfig(ConfigFile.NETWORK);
		String name = c.getString("LOGIN-SERVER-NAME", "LoginServer");
		int id = c.getInt("LOGIN-SERVER-ID", 1);
		return name + ':' + id;
	}
	
	private void handleLagRequest(Player player) {
		player.sendPacket(new GameServerLagResponse());
	}
	
	private void handleCharDeletion(Player player, DeleteCharacterRequest request) {
		SWGObject obj = ObjectLookup.getObjectById(request.getPlayerId());
		boolean success = obj != null && deleteCharacter(obj);
		if (success) {
			new DestroyObjectIntent(obj).broadcast();
			Log.i("Deleted character %s for user %s", obj.getObjectName(), player.getUsername());
		} else {
			Log.e("Could not delete character! Character: ID: " + request.getPlayerId() + " / " + obj);
		}
		player.sendPacket(new DeleteCharacterResponse(success));
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
		final boolean doClientCheck = DataManager.getConfig(ConfigFile.NETWORK).getBoolean("LOGIN-VERSION-CHECKS", true);
		if (!id.getVersion().equals(REQUIRED_VERSION) && doClientCheck) {
			onLoginClientVersionError(player, id);
			return;
		}
		UserMetadata user = userDatabase.getUser(id.getUsername());
		if (user == null)
			onInvalidUserPass(player, id, false);
		else if (user.isBanned())
			onLoginBanned(player, id);
		else if (isUserValid(user, id.getPassword()))
			onSuccessfulLogin(user, player, id);
		else
			onInvalidUserPass(player, id, true);
	}
	
	private void onLoginClientVersionError(Player player, LoginClientId id) {
		Log.i("%s cannot login due to invalid version code: %s, expected %s from %s", player.getUsername(), id.getVersion(), REQUIRED_VERSION, id.getSocketAddress());
		String type = "Login Failed!";
		String message = "Invalid Client Version Code: " + id.getVersion();
		player.sendPacket(new ErrorMessage(type, message, false));
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_INVALID_VERSION_CODE).broadcast();
	}
	
	private void onSuccessfulLogin(UserMetadata user, Player player, LoginClientId id) {
		player.setUsername(user.getUsername());
		switch(user.getAccessLevel()) {
			case "player": player.setAccessLevel(AccessLevel.PLAYER); break;
			case "warden": player.setAccessLevel(AccessLevel.WARDEN); break;
			case "csr": player.setAccessLevel(AccessLevel.CSR); break;
			case "qa": player.setAccessLevel(AccessLevel.QA); break;
			case "dev": player.setAccessLevel(AccessLevel.DEV); break;
			default: player.setAccessLevel(AccessLevel.PLAYER); break;
		}
		player.setPlayerState(PlayerState.LOGGED_IN);
		sendLoginSuccessPacket(player);
		Log.i("%s connected to the login server from %s", player.getUsername(), id.getSocketAddress());
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_SUCCESS).broadcast();
	}
	
	private void onLoginBanned(Player player, LoginClientId id) {
		String type = "Login Failed!";
		String message = "Sorry, you're banned!";
		player.sendPacket(new ErrorMessage(type, message, false));
		Log.i("%s cannot login due to a ban, from %s", player.getUsername(), id.getSocketAddress());
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_BANNED).broadcast();
	}
	
	private void onInvalidUserPass(Player player, LoginClientId id, boolean usernameValid) {
		String type = "Login Failed!";
		String message = usernameValid ? "Incorrect password" : "Incorrect username";
		player.sendPacket(new ErrorMessage(type, message, false));
		player.sendPacket(new LoginIncorrectClientId(getServerString(), REQUIRED_VERSION));
		Log.i("%s cannot login due to invalid user/pass from %s", id.getUsername(), id.getSocketAddress());
		player.setPlayerState(PlayerState.DISCONNECTED);
		new LoginEventIntent(player.getNetworkId(), LoginEvent.LOGIN_FAIL_INVALID_USER_PASS).broadcast();
	}
	
	private void sendLoginSuccessPacket(Player player) {
		LoginClientToken token = new LoginClientToken(SESSION_TOKEN, 0, player.getUsername());
		LoginEnumCluster cluster = new LoginEnumCluster();
		LoginClusterStatus clusterStatus = new LoginClusterStatus();
		List<SWGCharacter> characters = getCharacters(player.getUsername());
		for (Galaxy g : getGalaxies()) {
			cluster.addGalaxy(g);
			clusterStatus.addGalaxy(g);
		}
		cluster.setMaxCharacters(DataManager.getConfig(ConfigFile.PRIMARY).getInt("GALAXY-MAX-CHARACTERS", 2));
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
	
	private List<SWGCharacter> getCharacters(String username) {
		List <SWGCharacter> characters = new ArrayList<>();
		for (CharacterMetadata meta : userDatabase.getCharacters(username)) {
			characters.add(new SWGCharacter(meta.getName(), Race.getRaceByFile(meta.getRace()).getCrc(), meta.getId(), ProjectSWG.getGalaxy().getId(), 1));
		}
		return characters;
	}
	
	private boolean deleteCharacter(SWGObject obj) {
		return userDatabase.deleteCharacter(obj.getObjectId());
	}
	
}
