package services.player;

import intents.GalacticIntent;
import intents.PlayerEventIntent;

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
import network.packets.swg.login.creation.CreateCharacterSuccess;
import network.packets.swg.login.creation.RandomNameRequest;
import network.packets.swg.login.creation.RandomNameResponse;
import network.packets.swg.zone.GalaxyLoopTimesRequest;
import network.packets.swg.zone.GalaxyLoopTimesResponse;
import network.packets.swg.zone.HeartBeatMessage;
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
import services.objects.ObjectManager;
import utilities.namegen.SWGNameGenerator;

public class ZoneService extends Service {
	
	private SWGNameGenerator nameGenerator;
	private Map<String, ProfTemplateData> profTemplates;
	private ClientFactory clientFac;
	
	public ZoneService() {
		nameGenerator = new SWGNameGenerator();
		clientFac = new ClientFactory();
		//profTemplates = new HashMap<String, ProfTemplateData>();
	}
	
	@Override
	public boolean initialize() {
		nameGenerator.loadAllRules();
		//loadProfTemplates(); TODO: Uncomment when object creation is implemented
		return super.initialize();
	}
	
	public void handlePacket(GalacticIntent intent, Player player, long networkId, Packet p) {
		if (p instanceof SessionRequest)
			sendServerInfo(networkId);
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
	
	private void sendServerInfo(long networkId) {
		Config c = getConfig(ConfigFile.PRIMARY);
		String name = c.getString("ZONE-SERVER-NAME", "ZoneServer");
		int id = c.getInt("ZONE-SERVER-ID", 1);
		sendPacket(networkId, new ServerString(name + ":" + id));
		sendPacket(networkId, new ServerId(id));
	}
	
	private void handleClientIdMsg(Player player, ClientIdMsg clientId) {
		System.out.println(player.getUsername() + " has connected to the zone server.");
		player.setGalaxyId(getConfig(ConfigFile.PRIMARY).getInt("ZONE-SERVER-ID", 1));
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
		Race race = Race.getRace(create.getRace());
		CreatureObject creatureObj = (CreatureObject) objManager.createObject(race.getFilename());
		PlayerObject playerObj = (PlayerObject) objManager.createObject("object/player/shared_player.iff");
		creatureObj.setRace(Race.getRace(create.getRace()));
		creatureObj.setAppearanceData(create.getCharCustomization());
		creatureObj.setHeight(create.getHeight());
		creatureObj.setName(create.getName());
		playerObj.setProfession(create.getProfession());
		TangibleObject hairObj = (TangibleObject) objManager.createObject(create.getHair());
		hairObj.setAppearanceData(create.getHairCustomization());
		creatureObj.addChild(playerObj);
		creatureObj.addChild(hairObj);
		Location start = new Location();
		start.setTerrain(Terrain.TATOOINE);
		start.setX(3525 + (Math.random()-.5) * 5);
		start.setY(4);
		start.setZ(-4807 + (Math.random()-.5) * 5);
		start.setOrientationX(0);
		start.setOrientationY(0);
		start.setOrientationZ(0);
		start.setOrientationW(1);
		creatureObj.setLocation(start);
		creatureObj.setPvpType(20);
		playerObj.setLocation(start);
		player.setCreatureObject(creatureObj);
		player.setPlayerObject(playerObj);
		sendPacket(player, new CreateCharacterSuccess(creatureObj.getObjectId()));
		
		new PlayerEventIntent(player, PlayerEvent.PE_CREATE_CHARACTER).broadcast();
		
		/*for (String item : profTemplates.get(create.getClothes()).getItems(create.getRace())) {
			// TODO: Create and add item to character, item variable will be the template of the item to create
		}*/
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
}
