package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class RequestExtendedClusters extends SWGPacket {
	
	public static final int CRC = 0x8E33ED05;
	
	public RequestExtendedClusters() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, 0);
		return data;
	}
}
