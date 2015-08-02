package network;

import resources.network.ServerType;
import resources.network.UDPServer.UDPPacket;

public interface PacketSender {
	
	void sendPacket(ServerType type, UDPPacket packet);
	
}
