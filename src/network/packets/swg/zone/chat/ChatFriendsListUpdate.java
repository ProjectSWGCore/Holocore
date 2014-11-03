package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatFriendsListUpdate extends SWGPacket {
	
	public static final int CRC = 0x6CD2FCD8;
	
	private String galaxy;
	private String friendName;
	private boolean online;
	
	public ChatFriendsListUpdate() {
		
	}
	
	public ChatFriendsListUpdate(String command) {
		
	}
	
	public ChatFriendsListUpdate(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getAscii(data); // SWG
		galaxy = getAscii(data);
		friendName = getAscii(data);
		online = getBoolean(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(6);
		addShort  (data, 3);
		addInt    (data, CRC);
		addAscii  (data, "SWG");
		addAscii  (data, galaxy);
		addAscii  (data, friendName);
		addBoolean(data, online);
		return data;
	}

}
