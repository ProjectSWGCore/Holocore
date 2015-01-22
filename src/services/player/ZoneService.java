package services.player;

import intents.GalacticIntent;
import intents.PlayerEventIntent;

import java.io.File;
import java.sql.PreparedStatement;
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
import network.packets.swg.zone.GalaxyLoopTimesRequest;
import network.packets.swg.zone.GalaxyLoopTimesResponse;
import network.packets.swg.zone.HeartBeatMessage;
import resources.Galaxy;
import resources.Location;
import resources.Race;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.ProfTemplateData;
import resources.config.ConfigFile;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;
import resources.player.PlayerEvent;
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
	
	public ZoneService() {
		nameGenerator = new SWGNameGenerator();
		clientFac = new ClientFactory();
		nameFilter = new NameFilter(new File("bad_word_list.txt"));
	}
	
	@Override
	public boolean initialize() {
		String createCharacterSql = "INSERT INTO characters (id, name, race, userId, galaxyId) VALUES (?, ?, ?, ?, ?)";
		createCharacter = getLocalDatabase().prepareStatement(createCharacterSql);
		getCharacter = getLocalDatabase().prepareStatement("SELECT * FROM characters WHERE name = ?");
		nameGenerator.loadAllRules();
		loadProfTemplates();
		return nameFilter.load() && super.initialize();
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
	}
	
	private void sendServerInfo(Galaxy galaxy, long networkId) {
		Config c = getConfig(ConfigFile.PRIMARY);
		String name = c.getString("ZONE-SERVER-NAME", galaxy.getName());
		int id = c.getInt("ZONE-SERVER-ID", galaxy.getId());
		sendPacket(networkId, new ServerString(name + ":" + id));
		sendPacket(networkId, new ServerId(id));
	}
	
	private void handleClientIdMsg(Player player, ClientIdMsg clientId) {
		System.out.println(player.getUsername() + " has connected to the zone server.");
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
		ErrorMessage err = getNameValidity(name);
		if (err == ErrorMessage.NAME_APPROVED_MODIFIED)
			name = nameFilter.cleanName(name);
		sendPacket(player.getNetworkId(), new ClientVerifyAndLockNameResponse(name, err));
	}
	
	private void handleCharCreation(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		System.out.println("ZoneService: Create Character: " + create.getName() + "  User: " + player.getUsername() + "  IP: " + create.getAddress() + ":" + create.getPort());
		long characterId = createCharacter(objManager, player, create);
		
		ErrorMessage err = getNameValidity(create.getName());
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
				createCharacter.setString(3, ((CreatureObject)player.getCreatureObject()).getRace().getFilename());
				createCharacter.setInt(4, player.getUserId());
				createCharacter.setInt(5, player.getGalaxyId());
				return createCharacter.executeUpdate() == 1;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	private ErrorMessage getNameValidity(String name) {
		String modified = nameFilter.cleanName(name);
		if (nameFilter.isEmpty(modified)) // Empty name
			return ErrorMessage.NAME_DECLINED_EMPTY;
		if (nameFilter.containsBadCharacters(modified)) // Has non-alphabetic characters
			return ErrorMessage.NAME_DECLINED_SYNTAX;
		if (nameFilter.isProfanity(modified)) // Contains profanity
			return ErrorMessage.NAME_DECLINED_PROFANE;
		if (characterExistsForName(modified)) // User already exists
			return ErrorMessage.NAME_DECLINED_IN_USE;
		if (!modified.equals(name)) // If we needed to remove double spaces, trim the ends, etc
			return ErrorMessage.NAME_APPROVED_MODIFIED;
		return ErrorMessage.NAME_APPROVED;
	}
	
	private boolean characterExistsForName(String name) {
		synchronized (getCharacter) {
			try {
				getCharacter.setString(1, name);
				return getCharacter.executeQuery().next();
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	private long createCharacter(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		Location		start		= getStartLocation(create.getStart());
		Race			race		= Race.getRaceByFile(create.getRace());
		CreatureObject	creatureObj	= (CreatureObject) objManager.createObject(race.getFilename(), start);
		PlayerObject	playerObj	= (PlayerObject)   objManager.createObject("object/player/shared_player.iff");
		
		setCreatureObjectValues(objManager, creatureObj, create);
		setPlayerObjectValues(playerObj, create);
		createHair(objManager, creatureObj, create.getHair(), create.getHairCustomization());
		createStarterClothing(objManager, creatureObj, create.getRace(), create.getClothes());
		
		creatureObj.setVolume(0x000F4240);
		creatureObj.setOwner(player);
		creatureObj.setSlot("ghost", playerObj);
		playerObj.setTag(player.getAccessLevel());
		player.setCreatureObject(creatureObj);
		return creatureObj.getObjectId();
	}
	
	private void createHair(ObjectManager objManager, CreatureObject creatureObj, String hair, byte [] customization) {
		if (hair.isEmpty())
			return;
		TangibleObject hairObj = (TangibleObject) objManager.createObject(ClientFactory.formatToSharedFile(hair));
		hairObj.setAppearanceData(customization);
		creatureObj.setSlot("hair", hairObj);
		creatureObj.addEquipment(hairObj);
	}
	
	private void setCreatureObjectValues(ObjectManager objManager, CreatureObject creatureObj, ClientCreateCharacter create) {
		TangibleObject inventory	= (TangibleObject) objManager.createObject("object/tangible/inventory/shared_character_inventory.iff");
		TangibleObject datapad		= (TangibleObject) objManager.createObject("object/tangible/datapad/shared_character_datapad.iff");

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
			TangibleObject clothing = (TangibleObject) objManager.createObject(template);
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
		Location location = new Location();
		location.setTerrain(Terrain.TATOOINE);
		location.setX(3525 + (Math.random()-.5) * 5);
		location.setY(4);
		location.setZ(-4807 + (Math.random()-.5) * 5);
		location.setOrientationX(0);
		location.setOrientationY(0);
		location.setOrientationZ(0);
		location.setOrientationW(1);
		return location;
	}
}
