package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ApproveNameResponse extends SWGPacket {
	
	public static final int CRC = 0x9B2C6BA7;
	private String name = "";
	
	public ApproveNameResponse() {
		
	}
	
	public ApproveNameResponse(String name) {
		this.name = name;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		name = getUnicode(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(33 + name.length() * 2);
		addShort(  data, 9);
		addInt(    data, CRC);
		addUnicode(data, name);
		addAscii(  data, "ui");
		addInt(    data, 0);
		addAscii(  data, "name_approved");
		return data;
	}
	
}
