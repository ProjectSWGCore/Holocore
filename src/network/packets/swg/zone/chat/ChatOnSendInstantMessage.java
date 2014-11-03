package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatOnSendInstantMessage extends SWGPacket {
	
	public static final int CRC = 0x88DBB381;
	
	private int errorCode;
	private int count;
	
	public ChatOnSendInstantMessage() {
		this(0, 0);
	}
	
	public ChatOnSendInstantMessage(int errorCode, int count) {
		this.errorCode = errorCode;
		this.count = count;
	}
	
	public ChatOnSendInstantMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		errorCode = getInt(data);
		count = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(14);
		addShort(data, 2);
		addInt  (data, CRC);
		addInt  (data, errorCode);
		addInt  (data, count);
		return data;
	}
	
}
