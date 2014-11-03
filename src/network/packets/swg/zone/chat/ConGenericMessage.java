package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ConGenericMessage extends SWGPacket {
	
	public static final int CRC = 0x08C5FC76;
	
	private String message;
	
	public ConGenericMessage() {
		this("");
	}
	
	public ConGenericMessage(String message) {
		this.message = message;
	}
	
	public ConGenericMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		message = getAscii(data);
		getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8 + message.length());
		addShort(data, 3);
		addInt  (data, CRC);
		addAscii(data, message);
		addInt  (data, 0);
		return data;
	}

}
