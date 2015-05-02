package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatOnSendRoomMessage extends SWGPacket {
	
	public static final int CRC = 0xE7B61633;
	private int error = 0;
	private int messageId = 0;
	
	public ChatOnSendRoomMessage() {
		
	}
	
	public ChatOnSendRoomMessage(int error, int messageId) {
		this.error = error;
		this.messageId = messageId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		error = getInt(data);
		messageId = getInt(data);
	}
	
	public ByteBuffer encode() {
		int length = 14;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 3);
		addInt(  data, CRC);
		addInt(  data, error);
		addInt(  data, messageId);
		return data;
	}
}
