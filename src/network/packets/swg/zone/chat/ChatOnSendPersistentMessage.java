package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatOnSendPersistentMessage extends SWGPacket {
	public static final int CRC = 0x94E7A7AE;
	
	private int errorCode;
	private int count;
	
	public ChatOnSendPersistentMessage(int errorCode, int count) {
		this.errorCode = errorCode;
		this.count = count;
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(20);
		addShort(data, 3);
		addInt(data, errorCode);
		addInt(data, count);
		return data;
	}
	
}
