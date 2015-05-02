package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class AccountFeatureBits extends SWGPacket {
	
	public static final int CRC = 0x979F0279;
	
	public AccountFeatureBits() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		// Not sure how to decode this.. still a mystery
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(22);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, 0x025C8231);
		addInt(  data, 1);
		addInt(  data, 6);
		addInt(  data, 0x4EEAC08A);
		return data;
	}
	
}
