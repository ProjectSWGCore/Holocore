package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import resources.encodables.OutOfBand;
import network.packets.swg.SWGPacket;

public class ChatSystemMessage extends SWGPacket {
	
	public static final int CRC = 0x6D2A6413;
	private int type = 0;
	private String message = "";
	private OutOfBand oob;
	
	public ChatSystemMessage() {
		
	}
	
	public ChatSystemMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	public ChatSystemMessage(int type, OutOfBand oob) {
		this.type = type;
		this.oob = oob;
	}
	
	public ChatSystemMessage(SystemChatType type, String message) {
		this(type.ordinal(), message);
	}
	
	public ChatSystemMessage(SystemChatType type, OutOfBand oob) {
		this(type.ordinal(), oob);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		type = getByte(data);
		message = getUnicode(data);
		getUnicode(data);
	}
	
	public ByteBuffer encode() {
		byte[] oobData = (oob != null ? oob.encode() : null);
		int length = 7;
		
		if (oobData == null) length+= 15 + message.length() * 2;
		else length+=  4 + oobData.length;
		
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(  data, 4);
		addInt(    data, CRC);
		addByte(   data, type);
		if (oobData == null) {
			addUnicode(data, message);
			addUnicode(data, "");
		} else {
			addInt(data, 0);
			data.put(oobData);
		}
		
		return data;
	}
	
	public SystemChatType getType() {
		for (SystemChatType t : SystemChatType.values()) {
			if (type == t.ordinal()) return t;
		}
		return SystemChatType.SCREEN_AND_CHAT;
	}
	public String getMessage() { return message; }
	
	public enum SystemChatType {
		SCREEN_AND_CHAT,
		SCREEN,
		CHAT
	}
}
