package network.packets.swg;

import java.nio.ByteBuffer;


public class ErrorMessage extends SWGPacket {
	public static final int CRC = 0xB5ABF91A;
	private String type;
	private String message;
	private boolean fatal;
	
	public ErrorMessage() {
		
	}
	
	public ErrorMessage(String type, String message, boolean fatal) {
		this.type = type;
		this.message = message;
		this.fatal = fatal;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		type = getAscii(data);
		message = getAscii(data);
		fatal = getBoolean(data);
	}
	
	public ByteBuffer encode() {
		int length = 11 + type.length() + message.length();
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 3);
		addInt(  data, CRC);
		addAscii(data, type);
		addAscii(data, message);
		addBoolean(data, fatal);
		return data;
	}
}
