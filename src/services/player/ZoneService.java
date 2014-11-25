package services.player;

import network.packets.Packet;
import network.packets.soe.SessionRequest;
import network.packets.swg.login.AccountFeatureBits;
import network.packets.swg.login.ClientIdMsg;
import network.packets.swg.login.ClientPermissionsMessage;
import network.packets.swg.login.ServerId;
import network.packets.swg.login.ServerString;
import network.packets.swg.login.creation.ApproveNameRequest;
import network.packets.swg.login.creation.ApproveNameResponse;
import network.packets.swg.login.creation.ClientCreateCharacter;
import network.packets.swg.login.creation.RandomNameRequest;
import network.packets.swg.login.creation.RandomNameResponse;
import network.packets.swg.zone.HeartBeatMessage;
import resources.Race;
import resources.config.ConfigFile;
import resources.control.Service;
import resources.player.Player;
import resources.services.Config;
import utilities.namegen.SWGNameGenerator;

public class ZoneService extends Service {
	
	private SWGNameGenerator nameGenerator;
	
	public ZoneService() {
		nameGenerator = new SWGNameGenerator();
	}
	
	@Override
	public boolean initialize() {
		nameGenerator.loadAllRules();
		return super.initialize();
	}
	
	public void handlePacket(Player player, long networkId, Packet p) {
		if (p instanceof SessionRequest)
			sendServerInfo(networkId);
		if (p instanceof ClientIdMsg)
			handleClientIdMsg(player, (ClientIdMsg) p);
		if (p instanceof RandomNameRequest)
			handleRandomNameRequest(player, (RandomNameRequest) p);
		if (p instanceof ApproveNameRequest)
			handleApproveNameRequest(player, (ApproveNameRequest) p);
		if (p instanceof ClientCreateCharacter)
			handleCharCreation(player, (ClientCreateCharacter) p);
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
	
	private void handleApproveNameRequest(Player player, ApproveNameRequest request) {
		sendPacket(player.getNetworkId(), new ApproveNameResponse(request.getName()));
	}
	
	private void handleCharCreation(Player player, ClientCreateCharacter create) {
		System.out.println("Create Character: " + create.getName());
	}
	
	
}
