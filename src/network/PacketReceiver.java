package network;

import resources.network.ServerType;
import resources.network.UDPServer.UDPPacket;

public interface PacketReceiver {
	
	void receivePacket(ServerType type, UDPPacket packet);
	
}
