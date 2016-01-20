package network;

import java.net.InetSocketAddress;

public interface PacketSender {
	
	void sendPacket(InetSocketAddress sock, byte [] data);
	
}
