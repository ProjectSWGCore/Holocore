package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class CmdSceneReady extends SWGPacket {
	
	public static final int CRC = 0x43FD1C22;
	
	public CmdSceneReady() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
	}
	
	public ByteBuffer encode() {
		int length = 6;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 1);
		addInt(  data, CRC);
		return data;
	}
}
