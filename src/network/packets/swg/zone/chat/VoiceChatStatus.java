package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class VoiceChatStatus extends SWGPacket {
	
	public static final int CRC = 0x9E601905;
	
	public VoiceChatStatus() {
		
	}
	
	public VoiceChatStatus(ByteBuffer data) {
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
		addInt  (data, 1);
		return data;
	}

}
