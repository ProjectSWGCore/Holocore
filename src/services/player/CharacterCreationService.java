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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
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
import resources.PvpFlag;
import resources.Race;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.ProfTemplateData;
import resources.config.ConfigFile;
import resources.containers.ContainerPermissions;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.weapon.WeaponObject;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;
import resources.server_info.Log;
import resources.zone.NameFilter;
import services.objects.ObjectManager;
import services.player.TerrainZoneInsertion.SpawnInformation;
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
			System.err.println("Failed to load name filter!");
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		creationRestriction.setCreationsPerPeriod(getConfig(ConfigFile.PRIMARY).getInt("GALAXY-MAX-CHARACTERS-PER-PERIOD", 2));
		return super.start();
	}
	
	public void handlePacket(GalacticIntent intent, Player player, long networkId, Packet p) {
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
				e.printStackTrace();
				return false;
			} finally {
				try {
					if (set != null)
						set.close();
				} catch (SQLException e) {
					e.printStackTrace();
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
		sendPacket(player.getNetworkId(), response);
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
		sendPacket(player.getNetworkId(), new ClientVerifyAndLockNameResponse(name, err));
	}
	
	private void handleCharCreation(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		ErrorMessage err = getNameValidity(create.getName(), player.getAccessLevel() != AccessLevel.PLAYER);
		boolean success = false;
		int max = getConfig(ConfigFile.PRIMARY).getInt("GALAXY-MAX-CHARACTERS", 0);
		if (max != 0 && getCharacterCount(player.getUserId()) >= max)
			err = ErrorMessage.SERVER_CHARACTER_CREATION_MAX_CHARS;
		else if (!creationRestriction.isAbleToCreate(player))
			err = ErrorMessage.NAME_DECLINED_TOO_FAST;
		else if (err == ErrorMessage.NAME_APPROVED) {
			err = completeCharCreation(objManager, player, create);
			success = (err == ErrorMessage.NAME_APPROVED);
		}
		if (!success)
			sendCharCreationFailure(player, create, err);
	}
	
	private ErrorMessage completeCharCreation(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		long characterId = createCharacter(objManager, player, create);
		if (characterId == -1) {
			return ErrorMessage.NAME_DECLINED_INTERNAL_ERROR;
		} else if (createCharacterInDb(characterId, create.getName(), player)) {
			creationRestriction.createdCharacter(player);
			System.out.println("[" + player.getUsername() + "] Create Character: " + create.getName() + ". IP: " + create.getAddress() + ":" + create.getPort());
			Log.i("ZoneService", "%s created character %s from %s:%d", player.getUsername(), create.getName(), create.getAddress(), create.getPort());
			sendPacket(player, new CreateCharacterSuccess(characterId));
			new PlayerEventIntent(player, PlayerEvent.PE_CREATE_CHARACTER).broadcast();
			return ErrorMessage.NAME_APPROVED;
		} else {
			Log.e("ZoneService", "Failed to create character %s for user %s with server error from %s:%d", create.getName(), player.getUsername(), create.getAddress(), create.getPort());
			objManager.deleteObject(characterId);
			return ErrorMessage.NAME_DECLINED_INTERNAL_ERROR;
		}
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
			default:
				break;
		}
		System.err.println("ZoneService: Unable to create character [Name: " + create.getName() + "  User: " + player.getUsername() + "] and put into database! Reason: " + err);
		Log.e("ZoneService", "Failed to create character %s for user %s with error %s and reason %s from %s:%d", create.getName(), player.getUsername(), err, reason, create.getAddress(), create.getPort());
		sendPacket(player, new CreateCharacterFailure(reason));
	}
	
	private boolean createCharacterInDb(long characterId, String name, Player player) {
		if (characterExistsForName(name))
			return false;
		synchronized (createCharacter) {
			try {
				createCharacter.setLong(1, characterId);
				createCharacter.setString(2, name);
				createCharacter.setString(3, player.getCreatureObject().getRace().getFilename());
				createCharacter.setInt(4, player.getUserId());
				return createCharacter.executeUpdate() == 1;
			} catch (SQLException e) {
				e.printStackTrace();
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
				e.printStackTrace();
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
	
	private long createCharacter(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		Race			race		= Race.getRaceByFile(create.getRace());
		CreatureObject	creatureObj	= createCreature(objManager, race.getFilename(), getConfig(ConfigFile.PRIMARY).getString("PRIMARY-SPAWN-LOCATION", "tat_moseisley"));
		if (creatureObj == null)
			return -1;
		PlayerObject	playerObj	= createPlayer(objManager, "object/player/shared_player.iff");
		
		setCreatureObjectValues(objManager, creatureObj, create);
		setPlayerObjectValues(playerObj, create);
		createHair(objManager, creatureObj, create.getHair(), create.getHairCustomization());
		createStarterClothing(objManager, creatureObj, create.getRace(), create.getClothes());
		
		creatureObj.setVolume(0x000F4240);
		creatureObj.setOwner(player);
		creatureObj.addObject(playerObj); // ghost slot
		
		playerObj.setAdminTag(player.getAccessLevel());
		player.setCreatureObject(creatureObj);
		return creatureObj.getObjectId();
	}
	
	private CreatureObject createCreature(ObjectManager objManager, String template, String spawnLocation) {
		SpawnInformation info = insertion.generateSpawnLocation(spawnLocation);
		if (info == null) {
			Log.e("CharacterCreationService", "Failed to get spawn information for location: " + spawnLocation);
			return null;
		}
		if (info.building)
			return createCreatureBuilding(objManager, template, info);
		else {
			SWGObject obj = objManager.createObject(template, info.location);
			if (obj instanceof CreatureObject)
				return (CreatureObject) obj;
		}
		return null;
	}
	
	private CreatureObject createCreatureBuilding(ObjectManager objManager, String template, SpawnInformation info) {
		SWGObject parent = objManager.getObjectById(info.buildingId);
		if (parent == null || !(parent instanceof BuildingObject)) {
			Log.e("CharacterCreationService", "Invalid parent! Either null or not a building: %s  BUID: %d", parent, info.buildingId);
			return null;
		}
		CellObject cell = ((BuildingObject) parent).getCellByName(info.cell);
		if (cell == null) {
			Log.e("CharacterCreationService", "Invalid cell! Cell does not exist: %s  B-Template: %s  BUID: %d", info.cell, parent.getTemplate(), info.buildingId);
			return null;
		}
		SWGObject obj = objManager.createObject(template, info.location);
		cell.addObject(obj);
		if (obj instanceof CreatureObject)
			return (CreatureObject) obj;
		return null;
	}
	
	private PlayerObject createPlayer(ObjectManager objManager, String template) {
		SWGObject obj = objManager.createObject(template);
		if (obj instanceof PlayerObject)
			return (PlayerObject) obj;
		return null;
	}
	
	private TangibleObject createTangible(ObjectManager objManager, String template) {
		SWGObject obj = objManager.createObject(template);
		if (obj instanceof TangibleObject)
			return (TangibleObject) obj;
		return null;
	}
	
	private SWGObject createInventoryObject(ObjectManager objManager, CreatureObject creatureObj, String template) {
		SWGObject obj = objManager.createObject(creatureObj, template);
		obj.setContainerPermissions(ContainerPermissions.INVENTORY);
		return obj;
	}
	
	private void createHair(ObjectManager objManager, CreatureObject creatureObj, String hair, byte [] customization) {
		if (hair.isEmpty())
			return;
		TangibleObject hairObj = createTangible(objManager, ClientFactory.formatToSharedFile(hair));
		hairObj.setAppearanceData(customization);

		creatureObj.addObject(hairObj); // slot = hair
		creatureObj.addEquipment(hairObj);
	}
	
	private void setCreatureObjectValues(ObjectManager objManager, CreatureObject creatureObj, ClientCreateCharacter create) {
		creatureObj.setRace(Race.getRaceByFile(create.getRace()));
		creatureObj.setAppearanceData(create.getCharCustomization());
		creatureObj.setHeight(create.getHeight());
		creatureObj.setName(create.getName());
		creatureObj.setPvpFlags(PvpFlag.PLAYER, PvpFlag.OVERT);
		creatureObj.getSkills().add("species_" + creatureObj.getRace().getSpecies());

		WeaponObject defWeapon = (WeaponObject) createInventoryObject(objManager, creatureObj, "object/weapon/melee/unarmed/shared_unarmed_default_player.iff");
		defWeapon.setMaxRange(5);
		creatureObj.setEquippedWeapon(defWeapon);
		creatureObj.addEquipment(createInventoryObject(objManager, creatureObj, "object/tangible/inventory/shared_character_inventory.iff"));
		creatureObj.addEquipment(createInventoryObject(objManager, creatureObj, "object/tangible/datapad/shared_character_datapad.iff"));
		creatureObj.addEquipment(createInventoryObject(objManager, creatureObj, "object/tangible/inventory/shared_appearance_inventory.iff"));
		creatureObj.addEquipment(defWeapon);
		createInventoryObject(objManager, creatureObj, "object/tangible/bank/shared_character_bank.iff");
		createInventoryObject(objManager, creatureObj, "object/tangible/mission_bag/shared_mission_bag.iff");
		
		// Any character can perform the basic dance.
		creatureObj.addAbility("startDance+basic");
		
		creatureObj.joinPermissionGroup("world");
	}
	
	private void setPlayerObjectValues(PlayerObject playerObj, ClientCreateCharacter create) {
		playerObj.setProfession(create.getProfession());
		Calendar date = Calendar.getInstance();
		playerObj.setBornDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
	}
	

	private void createStarterClothing(ObjectManager objManager, CreatureObject player, String race, String profession) {
		if (player.getSlottedObject("inventory") == null)
			return;

		for (String template : profTemplates.get(profession).getItems(ClientFactory.formatToSharedFile(race))) {
			TangibleObject item = createTangible(objManager, template);
			if (item == null)
				return;
			// Move the new item to the player's clothing slots and add to equipment list
			item.moveToContainer(player, player);
			player.addEquipment(item);
		}
		
		SWGObject inventory = player.getSlottedObject("inventory");
		SWGObject item = objManager.createObject("object/tangible/npe/shared_npe_uniform_box.iff");
		item.moveToContainer(inventory);

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
