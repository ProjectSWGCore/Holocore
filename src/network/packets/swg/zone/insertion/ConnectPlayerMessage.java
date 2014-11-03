package network.packets.swg.zone.insertion;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ConnectPlayerMessage extends SWGPacket {
	
	public static final int CRC = 0x2E365218;
	
	public ConnectPlayerMessage() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
	}
	
	public ByteBuffer encode() {
		int length = 10;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, 0);
		return data;
	}
}
