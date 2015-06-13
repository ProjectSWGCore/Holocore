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

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import main.ProjectSWG;
import network.packets.Packet;
import network.packets.soe.SessionRequest;
import network.packets.swg.login.AccountFeatureBits;
import network.packets.swg.login.ClientIdMsg;
import network.packets.swg.login.ClientPermissionsMessage;
import network.packets.swg.login.ServerId;
import network.packets.swg.login.ServerString;
import network.packets.swg.login.creation.ClientVerifyAndLockNameRequest;
import network.packets.swg.login.creation.ClientVerifyAndLockNameResponse;
import network.packets.swg.login.creation.ClientCreateCharacter;
import network.packets.swg.login.creation.ClientVerifyAndLockNameResponse.ErrorMessage;
import network.packets.swg.login.creation.CreateCharacterFailure;
import network.packets.swg.login.creation.CreateCharacterFailure.NameFailureReason;
import network.packets.swg.login.creation.CreateCharacterSuccess;
import network.packets.swg.login.creation.RandomNameRequest;
import network.packets.swg.login.creation.RandomNameResponse;
import network.packets.swg.zone.CmdSceneReady;
import network.packets.swg.zone.GalaxyLoopTimesRequest;
import network.packets.swg.zone.GalaxyLoopTimesResponse;
import network.packets.swg.zone.HeartBeatMessage;
import network.packets.swg.zone.ParametersMessage;
import network.packets.swg.zone.SetWaypointColor;
import network.packets.swg.zone.ShowBackpack;
import network.packets.swg.zone.ShowHelmet;
import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.chat.ChatOnConnectAvatar;
import network.packets.swg.zone.chat.ChatSystemMessage;
import network.packets.swg.zone.chat.VoiceChatStatus;
import network.packets.swg.zone.insertion.ChatServerStatus;
import network.packets.swg.zone.insertion.CmdStartScene;
import resources.Galaxy;
import resources.Location;
import resources.Race;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.ProfTemplateData;
import resources.config.ConfigFile;
import resources.containers.ContainerPermissions;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureMood;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.waypoint.WaypointObject.WaypointColor;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerFlags;
import resources.player.PlayerState;
import resources.server_info.Config;
import resources.server_info.Log;
import resources.zone.NameFilter;
import services.objects.ObjectManager;
import utilities.namegen.SWGNameGenerator;

public class ZoneService extends Service {

	private final Map <String, Player> lockedNames;
	private final Map <String, ProfTemplateData> profTemplates;
	private final NameFilter nameFilter;
	private final SWGNameGenerator nameGenerator;
	private final CharacterCreationRestriction creationRestriction;
	
	private PreparedStatement createCharacter;
	private PreparedStatement getCharacter;
	private PreparedStatement getLikeCharacterName;
	private String commitHistory;
	
	public ZoneService() {
		lockedNames = new HashMap<String, Player>();
		profTemplates = new ConcurrentHashMap<String, ProfTemplateData>();
		nameFilter = new NameFilter("namegen/bad_word_list.txt", "namegen/reserved_words.txt", "namegen/fiction_reserved.txt");
		nameGenerator = new SWGNameGenerator(nameFilter);
		creationRestriction = new CharacterCreationRestriction(2);
		commitHistory = "";
	}
	
	@Override
	public boolean initialize() {
		String createCharacterSql = "INSERT INTO characters (id, name, race, userId, galaxyId) VALUES (?, ?, ?, ?, ?)";
		createCharacter = getLocalDatabase().prepareStatement(createCharacterSql);
		getCharacter = getLocalDatabase().prepareStatement("SELECT * FROM characters WHERE name == ?");
		getLikeCharacterName = getLocalDatabase().prepareStatement("SELECT name FROM characters WHERE name ilike ?"); //NOTE: ilike is not SQL standard. It is an extension for postgres only.
		nameGenerator.loadAllRules();
		loadProfTemplates();
		loadCommitHistory();
		if (!nameFilter.load())
			System.err.println("Failed to load name filter!");
		registerForIntent(RequestZoneInIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		creationRestriction.setCreationsPerPeriod(getConfig(ConfigFile.PRIMARY).getInt("GALAXY-MAX-CHARACTERS", 2));
		return super.start();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof RequestZoneInIntent) {
			RequestZoneInIntent zii = (RequestZoneInIntent) i;
			zoneInPlayer(zii.getPlayer(), zii.getCreature(), zii.getGalaxy());
		}
	}
	
	public void handlePacket(GalacticIntent intent, Player player, long networkId, Packet p) {
		if (p instanceof SessionRequest)
			sendServerInfo(intent.getGalaxy(), networkId);
		if (p instanceof ClientIdMsg)
			handleClientIdMsg(player, (ClientIdMsg) p);
		if (p instanceof RandomNameRequest)
			handleRandomNameRequest(player, (RandomNameRequest) p);
		if (p instanceof ClientVerifyAndLockNameRequest)
			handleApproveNameRequest(intent.getPlayerManager(), player, (ClientVerifyAndLockNameRequest) p);
		if (p instanceof ClientCreateCharacter)
			handleCharCreation(intent.getObjectManager(), player, (ClientCreateCharacter) p);
		if (p instanceof GalaxyLoopTimesRequest)
			handleGalaxyLoopTimesRequest(player, (GalaxyLoopTimesRequest) p);
		if (p instanceof CmdSceneReady)
			handleCmdSceneReady(player, (CmdSceneReady) p);
		if (p instanceof SetWaypointColor)
			handleSetWaypointColor(player, (SetWaypointColor) p);
		if(p instanceof ShowBackpack)
			handleShowBackpack(player, (ShowBackpack) p);
		if(p instanceof ShowHelmet)
			handleShowHelmet(player, (ShowHelmet) p);
	}
	
	private void zoneInPlayer(Player player, CreatureObject creature, String galaxy) {
		PlayerObject playerObj = creature.getPlayerObject();
		player.setPlayerState(PlayerState.ZONING_IN);
		player.setCreatureObject(creature);
		creature.setOwner(player);
		
		sendZonePackets(player, creature);
		initPlayerBeforeZoneIn(player, creature, playerObj);
		creature.createObject(player);
		System.out.printf("[%s] %s is zoning in%n", player.getUsername(), player.getCharacterName());
		Log.i("ObjectManager", "Zoning in %s with character %s", player.getUsername(), player.getCharacterName());
		sendCommitHistory(player);
		PlayerEventIntent firstZone = new PlayerEventIntent(player, galaxy, PlayerEvent.PE_FIRST_ZONE);
		PlayerEventIntent primary = new PlayerEventIntent(player, galaxy, PlayerEvent.PE_ZONE_IN);
		primary.broadcastWithIntent(firstZone);
	}
	
	private void loadCommitHistory() {
		File repoDir = new File("./" + Constants.DOT_GIT); // Ziggy: Get the git directory
		Git git = null;
		Repository repo = null;
		int commitCount = 3;
		int iterations = 0;
		
		try {
			git = Git.open(repoDir);
			repo = git.getRepository();
			
			try {
				commitHistory = "The " + commitCount + " most recent commits in branch '" + repo.getBranch() + "':\n";
				
				for(RevCommit commit : git.log().setMaxCount(commitCount).call()) {
					commitHistory += commit.getName().substring(0, 7) + " " + commit.getShortMessage();
					
					if(commitCount > iterations++)
						commitHistory += "\n";
				}
				
			} catch (Throwable t) {
				t.printStackTrace();
			}
			
		} catch (IOException e) {
			// Ziggy: An exception is thrown if bash isn't installed
			// It works fine anyways.
			// https://www.eclipse.org/forums/index.php/t/1031740/
		}
		

	}
	
	private void sendCommitHistory(Player player) {
		player.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT, commitHistory));
	}
	
	private void sendZonePackets(Player player, CreatureObject creature) {
		long objId = creature.getObjectId();
		Race race = creature.getRace();
		Location l = creature.getLocation();
		long time = (long)(ProjectSWG.getCoreTime()/1E3);
		sendPacket(player, new HeartBeatMessage());
		sendPacket(player, new ChatServerStatus(true));
		sendPacket(player, new VoiceChatStatus());
		sendPacket(player, new ParametersMessage());
		sendPacket(player, new ChatOnConnectAvatar());
		sendPacket(player, new CmdStartScene(false, objId, race, l, time));
		sendPacket(player, new UpdatePvpStatusMessage(creature.getPvpType(), creature.getPvpFactionId(), creature.getObjectId()));
	}
	
	private void initPlayerBeforeZoneIn(Player player, CreatureObject creatureObj, PlayerObject playerObj) {
		playerObj.setStartPlayTime((int) System.currentTimeMillis());
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
		PlayerObject ghost = (PlayerObject) player.getPlayerObject();
		
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

	private void sendServerInfo(Galaxy galaxy, long networkId) {
		Config c = getConfig(ConfigFile.NETWORK);
		String name = c.getString("ZONE-SERVER-NAME", galaxy.getName());
		int id = c.getInt("ZONE-SERVER-ID", galaxy.getId());
		sendPacket(networkId, new ServerString(name + ":" + id));
		sendPacket(networkId, new ServerId(id));
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
		sendPacket(player.getNetworkId(), new HeartBeatMessage());
		sendPacket(player.getNetworkId(), new AccountFeatureBits());
		sendPacket(player.getNetworkId(), new ClientPermissionsMessage());
	}
	
	private void handleRandomNameRequest(Player player, RandomNameRequest request) {
		RandomNameResponse response = new RandomNameResponse(request.getRace(), "");
		String race = Race.getRaceByFile(request.getRace()).getSpecies();
		response.setRandomName(nameGenerator.generateRandomName(race));
		sendPacket(player.getNetworkId(), response);
	}
	
	private void handleApproveNameRequest(PlayerManager playerMgr, Player player, ClientVerifyAndLockNameRequest request) {
		String name = request.getName();
		ErrorMessage err = getNameValidity(name, player.getAccessLevel() != AccessLevel.PLAYER);
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
		if (!creationRestriction.isAbleToCreate(player))
			err = ErrorMessage.NAME_DECLINED_TOO_FAST;
		if (err == ErrorMessage.NAME_APPROVED) {
			long characterId = createCharacter(objManager, player, create);
			if (createCharacterInDb(characterId, create.getName(), player)) {
				creationRestriction.createdCharacter(player);
				System.out.println("[" + player.getUsername() + "] Create Character: " + create.getName() + ". IP: " + create.getAddress() + ":" + create.getPort());
				Log.i("ZoneService", "%s created character %s from %s:%d", player.getUsername(), create.getName(), create.getAddress(), create.getPort());
				sendPacket(player, new CreateCharacterSuccess(characterId));
				new PlayerEventIntent(player, PlayerEvent.PE_CREATE_CHARACTER).broadcast();
				return;
			}
			Log.e("ZoneService", "Failed to create character %s for user %s with server error from %s:%d", create.getName(), player.getUsername(), create.getAddress(), create.getPort());
			objManager.deleteObject(characterId);
		}
		sendCharCreationFailure(player, create, err);
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
				createCharacter.setInt(5, player.getGalaxyId());
				return createCharacter.executeUpdate() == 1;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
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
	
	private boolean characterExistsForName(String name) {
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
	
	private long createCharacter(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		Location		start		= getStartLocation(create.getStart());
		Race			race		= Race.getRaceByFile(create.getRace());
		CreatureObject	creatureObj	= createCreature(objManager, race.getFilename(), start);
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
	
	private CreatureObject createCreature(ObjectManager objManager, String template, Location location) {
		SWGObject obj = objManager.createObject(template, location);
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
	
	private void createHair(ObjectManager objManager, CreatureObject creatureObj, String hair, byte [] customization) {
		if (hair.isEmpty())
			return;
		TangibleObject hairObj = createTangible(objManager, ClientFactory.formatToSharedFile(hair));
		hairObj.setAppearanceData(customization);

		creatureObj.addObject(hairObj); // slot = hair
		creatureObj.addEquipment(hairObj);
	}
	
	private void setCreatureObjectValues(ObjectManager objManager, CreatureObject creatureObj, ClientCreateCharacter create) {
		TangibleObject inventory	= createTangible(objManager, "object/tangible/inventory/shared_character_inventory.iff");
		inventory.setContainerPermissions(ContainerPermissions.INVENTORY);
		TangibleObject datapad		= createTangible(objManager, "object/tangible/datapad/shared_character_datapad.iff");
		datapad.setContainerPermissions(ContainerPermissions.INVENTORY);
		TangibleObject apprncInventory = createTangible(objManager, "object/tangible/inventory/shared_appearance_inventory.iff");
		apprncInventory.setContainerPermissions(ContainerPermissions.INVENTORY);
		
		creatureObj.setRace(Race.getRaceByFile(create.getRace()));
		creatureObj.setAppearanceData(create.getCharCustomization());
		creatureObj.setHeight(create.getHeight());
		creatureObj.setName(create.getName());
		creatureObj.setPvpType(20);
		creatureObj.getSkills().add("species_" + creatureObj.getRace().getSpecies());

		creatureObj.addObject(inventory); // slot = inventory
		creatureObj.addObject(datapad); // slot = datapad
		creatureObj.addObject(apprncInventory); // slot = appearance_inventory
		
		creatureObj.addEquipment(inventory);
		creatureObj.addEquipment(datapad);
		creatureObj.addEquipment(apprncInventory);

		creatureObj.joinPermissionGroup("world");
	}
	
	private void setPlayerObjectValues(PlayerObject playerObj, ClientCreateCharacter create) {
		playerObj.setProfession(create.getProfession());
		Calendar date = Calendar.getInstance();
		playerObj.setBornDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
	}
	
	private void handleGalaxyLoopTimesRequest(Player player, GalaxyLoopTimesRequest req) {
		sendPacket(player, new GalaxyLoopTimesResponse(ProjectSWG.getCoreTime()/1000));
	}
	
	private void createStarterClothing(ObjectManager objManager, CreatureObject player, String race, String profession) {
		if (player.getSlottedObject("inventory") == null)
			return;

		for (String template : profTemplates.get(profession).getItems(ClientFactory.formatToSharedFile(race))) {
			TangibleObject item = createTangible(objManager, template);
			// Move the new item to the player's clothing slots and add to equipment list
			item.moveToContainer(player, player);
			player.addEquipment(item);
		}

	}
	
	private void loadProfTemplates() {
		profTemplates.put("crafting_artisan", (ProfTemplateData) ClientFactory.getInfoFromFile("creation/profession_defaults_combat_brawler.iff"));
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
		if (player == null || assignedTo == player)
			return false;
		PlayerState state = player.getPlayerState();
		return state != PlayerState.DISCONNECTED && state != PlayerState.LOGGED_OUT;
	}
	
	private Location getStartLocation(String start) {
		return TerrainZoneInsertion.getInsertionForTerrain(Terrain.TATOOINE);
//		return TerrainZoneInsertion.getInsertionForArea(Terrain.CORELLIA, -5436, 24, -6211);
	}
}
