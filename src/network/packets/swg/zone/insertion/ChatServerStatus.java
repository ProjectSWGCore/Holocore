package network.packets.swg.zone.insertion;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatServerStatus extends SWGPacket {
	
	public static final int CRC = 0x7102B15F;
	private boolean online = false;
	
	public ChatServerStatus() {
		
	}
	
	public ChatServerStatus(boolean online) {
		this.online = online;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		online = getBoolean(data);
	}
	
	public ByteBuffer encode() {
		int length = 7;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(  data, 2);
		addInt(    data, CRC);
		addBoolean(data, online);
		return data;
	}
}
