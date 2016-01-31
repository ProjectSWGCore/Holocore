package network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface PacketSender {
	
	void sendPacket(InetSocketAddress sock, ByteBuffer data);
	
}
