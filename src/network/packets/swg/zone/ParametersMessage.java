package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ParametersMessage extends SWGPacket {
	
	public static final int CRC = 0x487652DA;
	
	public ParametersMessage() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getInt(data);
	}
	
	public ByteBuffer encode() {
		int length = 14;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, 900);
		return data;
	}
}
