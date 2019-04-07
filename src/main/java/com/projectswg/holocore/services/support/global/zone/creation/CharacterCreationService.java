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
package com.projectswg.holocore.services.support.global.zone.creation;

import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.login.creation.*;
import com.projectswg.common.network.packets.swg.login.creation.ClientVerifyAndLockNameResponse.ErrorMessage;
import com.projectswg.common.network.packets.swg.login.creation.CreateCharacterFailure.NameFailureReason;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.creation.CreatedCharacterIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.resources.support.data.namegen.NameFilter;
import com.projectswg.holocore.resources.support.data.namegen.SWGNameGenerator;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.TerrainZoneInsertionLoader.ZoneInsertion;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.global.zone.creation.CharacterCreation;
import com.projectswg.holocore.resources.support.global.zone.creation.CharacterCreationRestriction;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class CharacterCreationService extends Service {
	
	private final Map <String, Player> lockedNames;
	private final SWGNameGenerator nameGenerator;
	private final NameFilter nameFilter;
	private final CharacterCreationRestriction creationRestriction;
	
	public CharacterCreationService() {
		this.lockedNames = new HashMap<>();
		
		this.nameGenerator = new SWGNameGenerator();
		this.nameFilter = new NameFilter();
		this.creationRestriction = new CharacterCreationRestriction(2);
	}
	
	@Override
	public boolean start() {
		creationRestriction.setCreationsPerPeriod(PswgDatabase.config().getInt(this, "galaxyMaxCharactersPerPeriod", 2));
		return super.start();
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		Player player = gpi.getPlayer();
		SWGPacket p = gpi.getPacket();
		if (p instanceof RandomNameRequest)
			handleRandomNameRequest(player, (RandomNameRequest) p);
		if (p instanceof ClientVerifyAndLockNameRequest)
			handleApproveNameRequest(player, (ClientVerifyAndLockNameRequest) p);
		if (p instanceof ClientCreateCharacter)
			handleCharCreation(player, (ClientCreateCharacter) p);
	}
	
	private boolean characterExistsForName(String name) {
		name = name.toLowerCase(Locale.US);
		int spaceIndex = name.indexOf(' ');
		if (spaceIndex != -1)
			name = name.substring(0, spaceIndex);
		return PswgDatabase.objects().isCharacter(name);
	}
	
	private void handleRandomNameRequest(Player player, RandomNameRequest request) {
		RandomNameResponse response = new RandomNameResponse(request.getRace(), "");
		String randomName;
		do {
			randomName = nameGenerator.generateName(Race.getRaceByFile(request.getRace()));
		} while (getNameValidity(randomName, player.getAccessLevel() != AccessLevel.PLAYER) != ErrorMessage.NAME_APPROVED);
		response.setRandomName(randomName);
		player.sendPacket(response);
	}
	
	private void handleApproveNameRequest(Player player, ClientVerifyAndLockNameRequest request) {
		String name = request.getName();
		ErrorMessage err = getNameValidity(name, player.getAccessLevel() != AccessLevel.PLAYER);
		int max = PswgDatabase.config().getInt(this, "galaxyMaxCharacters", 0);
		if (max != 0 && getCharacterCount(player.getAccountId()) >= max)
			err = ErrorMessage.SERVER_CHARACTER_CREATION_MAX_CHARS;
		if (err == ErrorMessage.NAME_APPROVED_MODIFIED)
			name = nameFilter.cleanName(name);
		if (err == ErrorMessage.NAME_APPROVED || err == ErrorMessage.NAME_APPROVED_MODIFIED) {
			if (!lockName(name, player)) {
				err = ErrorMessage.NAME_DECLINED_IN_USE;
			}
		}
		player.sendPacket(new ClientVerifyAndLockNameResponse(name, err));
	}
	
	private void handleCharCreation(Player player, ClientCreateCharacter create) {
		CreatureObject creature = tryCharacterCreation(player, create);
		if (creature == null)
			return; // Unable to successfully create character
		assert creature.getPlayerObject() != null;
		assert creature.isPlayer();
		assert creature.getObjectId() > 0;
		StandardLog.onPlayerEvent(this, player, "created character '%s' from %s", create.getName(), create.getSocketAddress());
		player.sendPacket(new CreateCharacterSuccess(creature.getObjectId()));
		new CreatedCharacterIntent(creature).broadcast(); //Replaced PlayerEventIntent(PE_CREATE_CHARACTER)
	}
	
	private CreatureObject tryCharacterCreation(Player player, ClientCreateCharacter create) {
		// Valid Name
		ErrorMessage err = getNameValidity(create.getName(), player.getAccessLevel() != AccessLevel.PLAYER);
		if (err != ErrorMessage.NAME_APPROVED) {
			sendCharCreationFailure(player, create, err, "bad name");
			return null;
		}
		// Too many characters
		int max = PswgDatabase.config().getInt(this, "galaxyMaxCharacters", 0);
		if (max != 0 && getCharacterCount(player.getAccountId()) >= max) {
			sendCharCreationFailure(player, create, ErrorMessage.SERVER_CHARACTER_CREATION_MAX_CHARS, "too many characters");
			return null;
		}
		// Created too quickly
		if (!creationRestriction.isAbleToCreate(player)) {
			sendCharCreationFailure(player, create, ErrorMessage.NAME_DECLINED_TOO_FAST, "created characters too frequently");
			return null;
		}
		// Test for successful creation
		CreatureObject creature = createCharacter(player, create);
		if (creature == null) {
			sendCharCreationFailure(player, create, ErrorMessage.NAME_DECLINED_INTERNAL_ERROR, "can't create CreatureObject");
			return null;
		}
		// Test for hacking
		if (!creationRestriction.createdCharacter(player)) {
			DestroyObjectIntent.broadcast(creature);
			sendCharCreationFailure(player, create, ErrorMessage.NAME_DECLINED_INTERNAL_ERROR, "too many attempts - hacked");
			return null;
		}
		return creature;
	}
	
	private void sendCharCreationFailure(Player player, ClientCreateCharacter create, ErrorMessage err, String actualReason) {
		NameFailureReason reason = NameFailureReason.NAME_SYNTAX;
		switch (err) {
			case NAME_DECLINED_INTERNAL_ERROR:
			case NAME_APPROVED:
				reason = NameFailureReason.NAME_RETRY;
				break;
			case NAME_DECLINED_FICTIONALLY_INAPPROPRIATE:
				reason = NameFailureReason.NAME_FICTIONALLY_INAPPRORIATE;
				break;
			case NAME_DECLINED_IN_USE:   reason = NameFailureReason.NAME_IN_USE; break;
			case NAME_DECLINED_EMPTY:    reason = NameFailureReason.NAME_DECLINED_EMPTY; break;
			case NAME_DECLINED_RESERVED: reason = NameFailureReason.NAME_DEV_RESERVED; break;
			case NAME_DECLINED_TOO_FAST: reason = NameFailureReason.NAME_TOO_FAST; break;
			case SERVER_CHARACTER_CREATION_MAX_CHARS: reason = NameFailureReason.TOO_MANY_CHARACTERS; break;
			default:
				break;
		}
		StandardLog.onPlayerError(this, player, "failed to create character '%s' with server error [%s] from %s", create.getName(), actualReason, create.getSocketAddress());
		player.sendPacket(new CreateCharacterFailure(reason));
	}
	
	private int getCharacterCount(String username) {
		return PswgDatabase.objects().getCharacterCount(username);
	}
	
	private ErrorMessage getNameValidity(String name, boolean admin) {
		String modified = nameFilter.cleanName(name);
		if (nameFilter.isEmpty(modified)) // Empty name
			return ErrorMessage.NAME_DECLINED_EMPTY;
		if (nameFilter.containsBadCharacters(modified)) // Has non-alphabetic characters
			return ErrorMessage.NAME_DECLINED_SYNTAX;
		if (nameFilter.isProfanity(modified)) // Contains profanity
			return ErrorMessage.NAME_DECLINED_PROFANE;
		if (nameFilter.isFictionallyInappropriate(modified))
			return ErrorMessage.NAME_DECLINED_SYNTAX;
		if (modified.length() > 20)
			return ErrorMessage.NAME_DECLINED_SYNTAX;
		if (nameFilter.isReserved(modified) && !admin)
			return ErrorMessage.NAME_DECLINED_RESERVED;
		if (characterExistsForName(modified)) // User already exists.
			return ErrorMessage.NAME_DECLINED_IN_USE;
		if (nameFilter.isFictionallyReserved(modified))
			return ErrorMessage.NAME_DECLINED_FICTIONALLY_RESERVED;
		if (!modified.equals(name)) // If we needed to remove double spaces, trim the ends, etc
			return ErrorMessage.NAME_APPROVED_MODIFIED;
		return ErrorMessage.NAME_APPROVED;
	}
	
	private CreatureObject createCharacter(Player player, ClientCreateCharacter create) {
		String spawnLocation = PswgDatabase.config().getString(this, "primarySpawnLocation", "tat_moseisley");
		StandardLog.onPlayerTrace(this, player, "created player at spawn location %s", spawnLocation);
		ZoneInsertion info = DataLoader.zoneInsertions().getInsertion(spawnLocation);
		if (info == null) {
			Log.e("Failed to get spawn information for location: " + spawnLocation);
			return null;
		}
		CharacterCreation creation = new CharacterCreation(player, create);
		return creation.createCharacter(player.getAccessLevel(), info);
	}
	
	private boolean lockName(String name, Player player) {
		String firstName = name.split(" ", 2)[0].toLowerCase(Locale.ENGLISH);
		if (isLocked(player, firstName))
			return false;
		synchronized (lockedNames) {
			unlockName(player);
			lockedNames.put(firstName, player);
			StandardLog.onPlayerTrace(this, player, "locked name '%s' [full: '%s']", firstName, name);
		}
		return true;
	}
	
	private void unlockName(Player player) {
		synchronized (lockedNames) {
			String fName = null;
			for (Entry <String, Player> e : lockedNames.entrySet()) {
				Player locked = e.getValue();
				if (locked != null && locked.equals(player)) {
					fName = e.getKey();
					break;
				}
			}
			if (fName != null) {
				if (lockedNames.remove(fName) != null)
					StandardLog.onPlayerTrace(this, player, "unlocked name '%s'", fName);
			}
		}
	}
	
	private boolean isLocked(@NotNull Player assignedTo, String firstName) {
		Player player;
		synchronized (lockedNames) {
			player = lockedNames.get(firstName);
		}
		if (player == null || assignedTo.equals(player))
			return false;
		PlayerState state = player.getPlayerState();
		return state != PlayerState.DISCONNECTED;
	}
	
}
