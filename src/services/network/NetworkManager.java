package services.network;

import intents.OutboundUdpPacketIntent;
import resources.Galaxy;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.network.UDPServer.UDPPacket;

public class NetworkManager extends Manager {
	
	private NetworkListenerService netListenerService;
	private NetworkClientManager netClientManager;
	
	public NetworkManager(Galaxy galaxy) {
		netListenerService = new NetworkListenerService(galaxy);
		netClientManager = new NetworkClientManager();
		
		addChildService(netClientManager);
		addChildService(netListenerService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(OutboundUdpPacketIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof OutboundUdpPacketIntent) {
			UDPPacket packet = ((OutboundUdpPacketIntent) i).getPacket();
			ServerType type = ((OutboundUdpPacketIntent) i).getServerType();
			netListenerService.send(type, packet.getAddress(), packet.getPort(), packet.getData());
		}
	}
	
}
