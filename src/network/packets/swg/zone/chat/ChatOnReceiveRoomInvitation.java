package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatOnReceiveRoomInvitation extends SWGPacket {
	
	public static final int CRC = 0xC17EB06D;
	
	public ChatOnReceiveRoomInvitation() {
		
	}
	
	public ChatOnReceiveRoomInvitation(String command) {
		
	}
	
	public ChatOnReceiveRoomInvitation(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(6);
		addShort(data, 2);
		addInt  (data, CRC);
		return data;
	}

}
