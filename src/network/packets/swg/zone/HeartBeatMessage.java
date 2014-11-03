package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class HeartBeatMessage extends SWGPacket {
	
	public static final int CRC = 0xA16CF9AF;
	
	public HeartBeatMessage() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
	}
	
	public ByteBuffer encode() {
		int length = 10;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 1);
		addInt(  data, CRC);
		addInt(  data, 0);
		return data;
	}
}
