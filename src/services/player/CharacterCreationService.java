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
import intents.object.DestroyObjectIntent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import network.packets.Packet;
import network.packets.swg.login.creation.ClientCreateCharacter;
import network.packets.swg.login.creation.ClientVerifyAndLockNameRequest;
import network.packets.swg.login.creation.ClientVerifyAndLockNameResponse;
import network.packets.swg.login.creation.CreateCharacterFailure;
import network.packets.swg.login.creation.CreateCharacterSuccess;
import network.packets.swg.login.creation.RandomNameRequest;
import network.packets.swg.login.creation.RandomNameResponse;
import network.packets.swg.login.creation.ClientVerifyAndLockNameResponse.ErrorMessage;
import network.packets.swg.login.creation.CreateCharacterFailure.NameFailureReason;
import resources.Race;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.ProfTemplateData;
import resources.config.ConfigFile;
import resources.control.Assert;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;
import resources.server_info.Log;
import resources.zone.NameFilter;
import services.objects.ObjectManager;
import services.player.TerrainZoneInsertion.SpawnInformation;
import services.player.creation.CharacterCreation;
import utilities.namegen.SWGNameGenerator;

public class CharacterCreationService extends Service {
	
	private static final String CREATE_CHARACTER_SQL = "INSERT INTO characters (id, name, race, userId) VALUES (?, ?, ?, ?)";
	private static final String GET_CHARACTER_SQL = "SELECT * FROM characters WHERE name == ?";
	private static final String GET_LIKE_CHARACTER_SQL = "SELECT name FROM characters WHERE name ilike ?"; // NOTE: ilike is not SQL standard. It is an extension for postgres only.
	private static final String GET_CHARACTER_COUNT_SQL = "SELECT count(*) FROM characters WHERE userId = ?";
	
	private final Map <String, Player> lockedNames;
	private final Map <String, ProfTemplateData> profTemplates;
	private final NameFilter nameFilter;
	private final SWGNameGenerator nameGenerator;
	private final CharacterCreationRestriction creationRestriction;
	private final TerrainZoneInsertion insertion;
	
	private PreparedStatement createCharacter;
	private PreparedStatement getCharacter;
	private PreparedStatement getLikeCharacterName;
	private PreparedStatement getCharacterCount;
	
	public CharacterCreationService() {
		lockedNames = new HashMap<String, Player>();
		profTemplates = new ConcurrentHashMap<String, ProfTemplateData>();
		nameFilter = new NameFilter("namegen/bad_word_list.txt", "namegen/reserved_words.txt", "namegen/fiction_reserved.txt");
		nameGenerator = new SWGNameGenerator(nameFilter);
		creationRestriction = new CharacterCreationRestriction(2);
		insertion = new TerrainZoneInsertion();
	}
	
	@Override
	public boolean initialize() {
		createCharacter = getLocalDatabase().prepareStatement(CREATE_CHARACTER_SQL);
		getCharacter = getLocalDatabase().prepareStatement(GET_CHARACTER_SQL);
		getLikeCharacterName = getLocalDatabase().prepareStatement(GET_LIKE_CHARACTER_SQL);
		getCharacterCount = getLocalDatabase().prepareStatement(GET_CHARACTER_COUNT_SQL);
		nameGenerator.loadAllRules();
		loadProfTemplates();
		if (!nameFilter.load())
			Log.e(this, "Failed to load name filter!");
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		creationRestriction.setCreationsPerPeriod(getConfig(ConfigFile.PRIMARY).getInt("GALAXY-MAX-CHARACTERS-PER-PERIOD", 2));
		return super.start();
	}
	
	public void handlePacket(GalacticIntent intent, Player player, Packet p) {
		if (p instanceof RandomNameRequest)
			handleRandomNameRequest(player, (RandomNameRequest) p);
		if (p instanceof ClientVerifyAndLockNameRequest)
			handleApproveNameRequest(intent.getPlayerManager(), player, (ClientVerifyAndLockNameRequest) p);
		if (p instanceof ClientCreateCharacter)
			handleCharCreation(intent.getObjectManager(), player, (ClientCreateCharacter) p);
	}
	
	public boolean characterExistsForName(String name) {
		synchronized (getCharacter) {
			ResultSet set = null;
			try {
				String nameSplitStr[] = name.split(" ");
				String charExistsPrepStmtStr = nameSplitStr[0] + "%"; //Only the first name should be unique.
				getLikeCharacterName.setString(1, charExistsPrepStmtStr);
				set = getLikeCharacterName.executeQuery();
				while (set.next()){
					String dbName = set.getString("name");
					if(nameSplitStr[0].equalsIgnoreCase(dbName.split(" ")[0])){
						return true;
					}
				}
				return false;
			} catch (SQLException e) {
				Log.e(this, e);
				return false;
			} finally {
				try {
					if (set != null)
						set.close();
				} catch (SQLException e) {
					Log.e(this, e);
				}
			}
		}
	}
	
	private void handleRandomNameRequest(Player player, RandomNameRequest request) {
		RandomNameResponse response = new RandomNameResponse(request.getRace(), "");
		String race = Race.getRaceByFile(request.getRace()).getSpecies();
		String randomName = null;
		while (randomName == null) {
			randomName = nameGenerator.generateRandomName(race);
			if (getNameValidity(randomName, player.getAccessLevel() != AccessLevel.PLAYER) != ErrorMessage.NAME_APPROVED) {
				randomName = null;
			}
		}
		response.setRandomName(randomName);
		player.sendPacket(response);
	}
	
	private void handleApproveNameRequest(PlayerManager playerMgr, Player player, ClientVerifyAndLockNameRequest request) {
		String name = request.getName();
		ErrorMessage err = getNameValidity(name, player.getAccessLevel() != AccessLevel.PLAYER);
		int max = getConfig(ConfigFile.PRIMARY).getInt("GALAXY-MAX-CHARACTERS", 0);
		if (max != 0 && getCharacterCount(player.getUserId()) >= max)
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
	
	private void handleCharCreation(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		CreatureObject creature = tryCharacterCreation(objManager, player, create);
		if (creature == null)
			return; // Unable to successfully create character
		Assert.notNull(creature.getPlayerObject());
		Assert.test(creature.isPlayer());
		Assert.test(creature.getObjectId() > 0);
		Log.i(this, "%s created character %s from %s:%d", player.getUsername(), create.getName(), create.getAddress(), create.getPort());
		player.sendPacket(new CreateCharacterSuccess(creature.getObjectId()));
		new PlayerEventIntent(player, PlayerEvent.PE_CREATE_CHARACTER).broadcast();
	}
	
	private CreatureObject tryCharacterCreation(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		// Valid Name
		ErrorMessage err = getNameValidity(create.getName(), player.getAccessLevel() != AccessLevel.PLAYER);
		if (err != ErrorMessage.NAME_APPROVED) {
			sendCharCreationFailure(player, create, err);
			return null;
		}
		// Too many characters
		int max = getConfig(ConfigFile.PRIMARY).getInt("GALAXY-MAX-CHARACTERS", 0);
		if (max != 0 && getCharacterCount(player.getUserId()) >= max) {
			sendCharCreationFailure(player, create, ErrorMessage.SERVER_CHARACTER_CREATION_MAX_CHARS);
			return null;
		}
		// Created too quickly
		if (!creationRestriction.isAbleToCreate(player)) {
			sendCharCreationFailure(player, create, ErrorMessage.NAME_DECLINED_TOO_FAST);
			return null;
		}
		// Test for successful creation
		CreatureObject creature = createCharacter(objManager, player, create);
		if (creature == null) {
			Log.e(this, "Failed to create CreatureObject!");
			sendCharCreationFailure(player, create, ErrorMessage.NAME_DECLINED_INTERNAL_ERROR);
			return null;
		}
		// Test for hacking
		if (!creationRestriction.createdCharacter(player)) {
			new DestroyObjectIntent(creature).broadcast();
			sendCharCreationFailure(player, create, ErrorMessage.NAME_DECLINED_INTERNAL_ERROR);
			return null;
		}
		// Test for successful database insertion
		if (!createCharacterInDb(creature, create.getName(), player)) {
			Log.e(this, "Failed to create character %s for user %s with server error from %s:%d", create.getName(), player.getUsername(), create.getAddress(), create.getPort());
			new DestroyObjectIntent(creature).broadcast();
			sendCharCreationFailure(player, create, ErrorMessage.NAME_DECLINED_INTERNAL_ERROR);
			return null;
		}
		return creature;
	}
	
	private void sendCharCreationFailure(Player player, ClientCreateCharacter create, ErrorMessage err) {
		NameFailureReason reason = NameFailureReason.NAME_SYNTAX;
		switch (err) {
			case NAME_APPROVED:
				err = ErrorMessage.NAME_DECLINED_INTERNAL_ERROR;
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
			case NAME_DECLINED_INTERNAL_ERROR: reason = NameFailureReason.NAME_RETRY;
			default:
				break;
		}
		Log.e("ZoneService", "Failed to create character %s for user %s with error %s and reason %s from %s:%d", create.getName(), player.getUsername(), err, reason, create.getAddress(), create.getPort());
		player.sendPacket(new CreateCharacterFailure(reason));
	}
	
	private boolean createCharacterInDb(CreatureObject creature, String name, Player player) {
		if (characterExistsForName(name))
			return false;
		synchronized (createCharacter) {
			try {
				createCharacter.setLong(1, creature.getObjectId());
				createCharacter.setString(2, name);
				createCharacter.setString(3, creature.getRace().getFilename());
				createCharacter.setInt(4, player.getUserId());
				return createCharacter.executeUpdate() == 1;
			} catch (SQLException e) {
				Log.e(this, e);
				return false;
			}
		}
	}
	
	private int getCharacterCount(int userId) {
		synchronized (getCharacterCount) {
			try {
				getCharacterCount.setInt(1, userId);
				try (ResultSet set = getCharacterCount.executeQuery()) {
					if (set.next())
						return set.getInt("count");
				}
			} catch (SQLException e) {
				Log.e(this, e);
			}
		}
		return 0;
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
	
	private CreatureObject createCharacter(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		String spawnLocation = getConfig(ConfigFile.PRIMARY).getString("PRIMARY-SPAWN-LOCATION", "tat_moseisley");
		SpawnInformation info = insertion.generateSpawnLocation(spawnLocation);
		if (info == null) {
			Log.e("CharacterCreationService", "Failed to get spawn information for location: " + spawnLocation);
			return null;
		}
		CharacterCreation creation = new CharacterCreation(objManager, profTemplates.get(create.getClothes()), create);
		return creation.createCharacter(player.getAccessLevel(), info);
	}
	
	private void loadProfTemplates() {
		profTemplates.put("crafting_artisan", (ProfTemplateData) ClientFactory.getInfoFromFile("creation/profession_defaults_crafting_artisan.iff"));
		profTemplates.put("combat_brawler", (ProfTemplateData) ClientFactory.getInfoFromFile("creation/profession_defaults_combat_brawler.iff"));
		profTemplates.put("social_entertainer", (ProfTemplateData) ClientFactory.getInfoFromFile("creation/profession_defaults_social_entertainer.iff"));
		profTemplates.put("combat_marksman", (ProfTemplateData) ClientFactory.getInfoFromFile("creation/profession_defaults_combat_marksman.iff"));
		profTemplates.put("science_medic", (ProfTemplateData) ClientFactory.getInfoFromFile("creation/profession_defaults_science_medic.iff"));
		profTemplates.put("outdoors_scout", (ProfTemplateData) ClientFactory.getInfoFromFile("creation/profession_defaults_outdoors_scout.iff"));
		profTemplates.put("jedi", (ProfTemplateData) ClientFactory.getInfoFromFile("creation/profession_defaults_jedi.iff"));
	}
	
	private boolean lockName(String name, Player player) {
		String firstName = name.split(" ", 2)[0].toLowerCase(Locale.ENGLISH);
		if (isLocked(player, firstName))
			return false;
		synchronized (lockedNames) {
			unlockName(player);
			lockedNames.put(firstName, player);
			Log.i("ZoneService", "Locked name %s for user %s", firstName, player.getUsername());
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
					Log.i("ZoneService", "Unlocked name %s for user %s", fName, player.getUsername());
			}
		}
	}
	
	private boolean isLocked(Player assignedTo, String firstName) {
		Player player = null;
		synchronized (lockedNames) {
			player = lockedNames.get(firstName);
		}
		if (player == null || assignedTo.getUserId() == player.getUserId())
			return false;
		PlayerState state = player.getPlayerState();
		return state != PlayerState.DISCONNECTED;
	}
	
}
