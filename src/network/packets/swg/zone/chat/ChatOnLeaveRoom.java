package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatOnLeaveRoom extends SWGPacket {
	
	public static final int CRC = 0x60B5098B;
	
	private String galaxyName;
	private String characterName;
	private int errorCode;
	private int chatRoomId;
	private int requestId;
	
	public ChatOnLeaveRoom() {
		this("", "", 0, 0, 0);
	}
	
	public ChatOnLeaveRoom(String galaxy, String character, int errorCode, int chatRoomId, int requestId) {
		this.galaxyName = galaxy;
		this.characterName = character;
		this.errorCode = errorCode;
		this.chatRoomId = chatRoomId;
		this.requestId = requestId;
	}
	
	public ChatOnLeaveRoom(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getAscii(data);
		galaxyName = getAscii(data);
		characterName = getAscii(data);
		errorCode = getInt(data);
		chatRoomId = getInt(data);
		requestId = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(27+galaxyName.length()+characterName.length());
		addShort(data, 2);
		addInt(  data, CRC);
		addAscii(data, "SWG");
		addAscii(data, galaxyName);
		addAscii(data, characterName);
		addInt  (data, errorCode);
		addInt  (data, chatRoomId);
		addInt  (data, requestId);
		return data;
	}
	
}
