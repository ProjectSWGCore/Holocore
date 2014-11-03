package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class RandomNameRequest extends SWGPacket {
	
	public static final int CRC = 0xD6D1B6D1;
	private String raceCrc = "object/creature/player/human_male.iff";
	
	public RandomNameRequest() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		raceCrc = getAscii(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8 + raceCrc.length());
		addShort(data, 2);
		addInt(  data, CRC);
		addAscii(data, raceCrc);
		return data;
	}
	
	public String getRace() { return raceCrc; }
}
