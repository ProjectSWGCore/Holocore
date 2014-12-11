package services.player;

import intents.GalacticIntent;
import intents.PlayerEventIntent;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

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
import resources.objects.weapon.WeaponObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.services.Config;
import services.objects.ObjectManager;
import utilities.namegen.SWGNameGenerator;

public class ZoneService extends Service {
	
	private SWGNameGenerator nameGenerator;
	private Map <String, ProfTemplateData> profTemplates;
	private ClientFactory clientFac;
	
	private PreparedStatement createCharacter;
	
	public ZoneService() {
		nameGenerator = new SWGNameGenerator();
		clientFac = new ClientFactory();
		//profTemplates = new HashMap<String, ProfTemplateData>();
	}
	
	@Override
	public boolean initialize() {
		String createCharacterSql = "INSERT INTO characters (id, name, race, userId, galaxyId) VALUES (?, ?, ?, ?, ?)";
		createCharacter = getLocalDatabase().prepareStatement(createCharacterSql);
		nameGenerator.loadAllRules();
		//loadProfTemplates(); TODO: Uncomment when object creation is implemented
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
			handleApproveNameRequest(player, (ClientVerifyAndLockNameRequest) p);
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
		String race = Race.getRace(request.getRace()).getIFF().split("_")[0];
		response.setRandomName(nameGenerator.generateRandomName(race));
		sendPacket(player.getNetworkId(), response);
	}
	
	private void handleApproveNameRequest(Player player, ClientVerifyAndLockNameRequest request) {
		// TODO: Add error checking here... can't approve everybody's name
		sendPacket(player.getNetworkId(), new ClientVerifyAndLockNameResponse(request.getName(), ErrorMessage.NAME_APPROVED));
	}
	
	private void handleCharCreation(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		System.out.println("Create Character: " + create.getName());
		long characterId = createCharacter(objManager, player, create);
		synchronized (createCharacter) {
			try {
				createCharacter.setLong(1, characterId);
				createCharacter.setString(2, create.getName());
				createCharacter.setString(3, ((CreatureObject)player.getCreatureObject()).getRace().getFilename());
				createCharacter.setInt(4, player.getUserId());
				createCharacter.setInt(5, player.getGalaxyId());
				if (createCharacter.executeUpdate() != 1) {
					System.err.println("ZoneService: Unable to create character and put into database!");
					sendPacket(player, new CreateCharacterFailure(NameFailureReason.NAME_RETRY));
					return;
				}
			} catch (SQLException e) {
				System.err.println("ZoneService: Unable to create character and put into database!");
				e.printStackTrace();
				sendPacket(player, new CreateCharacterFailure(NameFailureReason.NAME_RETRY));
				return;
			}
		}
		sendPacket(player, new CreateCharacterSuccess(characterId));
		new PlayerEventIntent(player, PlayerEvent.PE_CREATE_CHARACTER).broadcast();
	}
	
	private long createCharacter(ObjectManager objManager, Player player, ClientCreateCharacter create) {
		Location start = getStartLocation(create.getStart());
		Race race = Race.getRace(create.getRace());
		CreatureObject creatureObj = (CreatureObject) objManager.createObject(race.getFilename());
		creatureObj.setVolume(0x000F4240);
		PlayerObject playerObj     = (PlayerObject)   objManager.createObject("object/player/shared_player.iff");
		TangibleObject hairObj     = (TangibleObject) objManager.createObject(create.getHair());
		WeaponObject defaultWeap   = (WeaponObject) objManager.createObject("object/weapon/melee/unarmed/shared_unarmed_default_player.iff");
		creatureObj.setWeapon(defaultWeap);
		setCreatureObjectValues(creatureObj, create);
		playerObj.setProfession(create.getProfession());
		hairObj.setAppearanceData(create.getHairCustomization());
		creatureObj.addChild(playerObj);
		creatureObj.addChild(hairObj);
		creatureObj.setLocation(start);
		playerObj.setLocation(start);
		player.setCreatureObject(creatureObj);
		player.setPlayerObject(playerObj);
		return creatureObj.getObjectId();
	}
	
	private void setCreatureObjectValues(CreatureObject creatureObj, ClientCreateCharacter create) {
		creatureObj.setRace(Race.getRace(create.getRace()));
		creatureObj.setAppearanceData(create.getCharCustomization());
		creatureObj.setHeight(create.getHeight());
		creatureObj.setName(create.getName());
		creatureObj.setPvpType(20);
	}
	
	private void handleGalaxyLoopTimesRequest(Player player, GalaxyLoopTimesRequest req) {
		sendPacket(player, new GalaxyLoopTimesResponse(ProjectSWG.getCoreTime()/1000));
	}
	
	private void loadProfTemplates() {
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
