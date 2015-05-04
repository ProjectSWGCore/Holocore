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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import network.packets.swg.zone.SetWaypointColor;
import network.packets.swg.zone.spatial.GetMapLocationsMessage;
import network.packets.swg.zone.spatial.GetMapLocationsResponseMessage;
import resources.Galaxy;
import resources.Location;
import resources.Race;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.ProfTemplateData;
import resources.config.ConfigFile;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.waypoint.WaypointObject.WaypointColor;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;
import resources.services.Config;
import resources.zone.NameFilter;
import services.objects.ObjectManager;
import utilities.namegen.SWGNameGenerator;

public class ZoneService extends Service {
	
	private SWGNameGenerator nameGenerator;
	private Map <String, ProfTemplateData> profTemplates;
	private ClientFactory clientFac;
	private NameFilter nameFilter;
	
	private PreparedStatement createCharacter;
	private PreparedStatement getCharacter;
	private PreparedStatement getLikeCharacterName;
	
	public ZoneService() {
		clientFac = new ClientFactory();
		nameFilter = new NameFilter("namegen/bad_word_list.txt", "namegen/reserved_words.txt");
		nameGenerator = new SWGNameGenerator(nameFilter);
	}
	
	@Override
	public boolean initialize() {
		String createCharacterSql = "INSERT INTO characters (id, name, race, userId, galaxyId) VALUES (?, ?, ?, ?, ?)";
		createCharacter = getLocalDatabase().prepareStatement(createCharacterSql);
		getCharacter = getLocalDatabase().prepareStatement("SELECT * FROM characters WHERE name == ?");
		getLikeCharacterName = getLocalDatabase().prepareStatement("SELECT name FROM characters WHERE name ilike ?"); //NOTE: ilike is not SQL standard. It is an extension for postgres only.
		nameGenerator.loadAllRules();
		loadProfTemplates();
		if (!nameFilter.load())
			System.err.println("Failed to load name filter!");
		return super.initialize();
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
		if (p instanceof GetMapLocationsMessage)
			handleMapLocationsResponse(player, (GetMapLocationsMessage) p);
	}
	
	private void handleMapLocationsResponse(Player player, GetMapLocationsMessage p) {
		// TODO Implement actual handling in GU2, this is to avoid constant map location requests from the client
		player.sendPacket(new GetMapLocationsResponseMessage(p.getPlanet()));
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
		Config c = getConfig(ConfigFile.PRIMARY);
		String name = c.getString("ZONE-SERVER-NAME", galaxy.getName());
		int id = c.getInt("ZONE-SERVER-ID", galaxy.getId());
		sendPacket(networkId, new ServerString(name + ":" + id));
		sendPacket(networkId, new ServerId(id));
	}
	
	private void handleCmdSceneReady(Player player, CmdSceneReady p) {
		player.setPlayerState(PlayerState.ZONED_IN);
		player.sendPacket(p);
		System.out.println("[" + player.getUsername() +"] " + player.getCharacterName() + " zoned in");
	}

	private void handleClientIdMsg(Player player, ClientIdMsg clientId) {
		System.out.println("[" + player.getUsername() + "] Connected to the zone server. IP: " + clientId.getAddress() + ":" + clientId.getPort());
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
		sendPacket(player.getNetworkId(), new ClientVerifyAndLockNameResponse(name, err));
	}
	
	private void handleCharCreation(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		System.out.println("[" + player.getUsername() + "] Create Character: " + create.getName() + ". IP: " + create.getAddress() + ":" + create.getPort());
		long characterId = createCharacter(objManager, player, create);
		
		ErrorMessage err = getNameValidity(create.getName(), player.getAccessLevel() != AccessLevel.PLAYER);
		if (err == ErrorMessage.NAME_APPROVED && createCharacterInDb(characterId, create.getName(), player)) {
			sendPacket(player, new CreateCharacterSuccess(characterId));
			new PlayerEventIntent(player, PlayerEvent.PE_CREATE_CHARACTER).broadcast();
		} else {
			NameFailureReason reason = NameFailureReason.NAME_SYNTAX;
			if (err == ErrorMessage.NAME_APPROVED) { // Then it must have been a database error
				err = ErrorMessage.NAME_DECLINED_INTERNAL_ERROR;
				reason = NameFailureReason.NAME_RETRY;
			} else if (err == ErrorMessage.NAME_DECLINED_IN_USE)
				reason = NameFailureReason.NAME_IN_USE;
			else if (err == ErrorMessage.NAME_DECLINED_EMPTY)
				reason = NameFailureReason.NAME_DECLINED_EMPTY;
			System.err.println("ZoneService: Unable to create character [Name: " + create.getName() + "  User: " + player.getUsername() + "] and put into database! Reason: " + err);
			sendPacket(player, new CreateCharacterFailure(reason));
		}
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
			return ErrorMessage.NAME_DECLINED_FICTIONALLY_INAPPROPRIATE;
		if (nameFilter.isReserved(modified) && !admin)
			return ErrorMessage.NAME_DECLINED_RESERVED;
		if (characterExistsForName(modified)) // User already exists.
			return ErrorMessage.NAME_DECLINED_IN_USE;
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
		creatureObj.setSlot("ghost", playerObj);
		playerObj.setAdminTag(player.getAccessLevel());
		playerObj.setOwner(player);
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
		creatureObj.setSlot("hair", hairObj);
		creatureObj.addEquipment(hairObj);
	}
	
	private void setCreatureObjectValues(ObjectManager objManager, CreatureObject creatureObj, ClientCreateCharacter create) {
		TangibleObject inventory	= createTangible(objManager, "object/tangible/inventory/shared_character_inventory.iff");
		TangibleObject datapad		= createTangible(objManager, "object/tangible/datapad/shared_character_datapad.iff");
		
		creatureObj.setRace(Race.getRaceByFile(create.getRace()));
		creatureObj.setAppearanceData(create.getCharCustomization());
		creatureObj.setHeight(create.getHeight());
		creatureObj.setName(create.getName());
		creatureObj.setPvpType(20);
		creatureObj.getSkills().add("species_" + creatureObj.getRace().getSpecies());
		creatureObj.setSlot("inventory", inventory);
		creatureObj.setSlot("datapad", datapad);
		
		creatureObj.addEquipment(inventory);
		creatureObj.addEquipment(datapad);
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
			TangibleObject clothing = createTangible(objManager, template);
			player.addChild(clothing);
		}
	}
	
	private void loadProfTemplates() {
		profTemplates = new ConcurrentHashMap<String, ProfTemplateData>();
		
		profTemplates.put("crafting_artisan", (ProfTemplateData) clientFac.getInfoFromFile("creation/profession_defaults_combat_brawler.iff"));
		profTemplates.put("combat_brawler", (ProfTemplateData) clientFac.getInfoFromFile("creation/profession_defaults_combat_brawler.iff"));
		profTemplates.put("social_entertainer", (ProfTemplateData) clientFac.getInfoFromFile("creation/profession_defaults_social_entertainer.iff"));
		profTemplates.put("combat_marksman", (ProfTemplateData) clientFac.getInfoFromFile("creation/profession_defaults_combat_marksman.iff"));
		profTemplates.put("science_medic", (ProfTemplateData) clientFac.getInfoFromFile("creation/profession_defaults_science_medic.iff"));
		profTemplates.put("outdoors_scout", (ProfTemplateData) clientFac.getInfoFromFile("creation/profession_defaults_outdoors_scout.iff"));
		profTemplates.put("jedi", (ProfTemplateData) clientFac.getInfoFromFile("creation/profession_defaults_jedi.iff"));
	}
	
	private Location getStartLocation(String start) {
		return TerrainZoneInsertion.getInsertionForTerrain(Terrain.DATHOMIR);
//		return TerrainZoneInsertion.getInsertionForArea(Terrain.CORELLIA, -5436, 24, -6211);
	}
}
