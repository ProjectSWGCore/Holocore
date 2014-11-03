package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ApproveNameRequest extends SWGPacket {
	
	public static final int CRC = 0x9EB04B9F;
	private String race = "";
	private String name = "";
	
	public ApproveNameRequest() {
		
	}
	
	public ApproveNameRequest(String race, String name) {
		this.race = race;
		this.name = name;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		race = getAscii(data);
		name = getUnicode(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10 + race.length() + name.length() * 2);
		addShort(  data, 4);
		addInt(    data, CRC);
		addAscii(  data, race);
		addUnicode(data, name);
		return data;
	}
	
	public String getRace() { return race; }
	public String getName() { return name; }
}
