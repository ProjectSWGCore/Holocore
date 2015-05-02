package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ConnectPlayerResponseMessage extends SWGPacket {
	
	public static final int CRC = 0x6137556F;
	
	public ConnectPlayerResponseMessage() {
		
	}
	
	public ConnectPlayerResponseMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10);
		addShort(data, 2);
		addInt  (data, CRC);
		addInt  (data, 0);
		return data;
	}

}
