package network.packets.swg.zone.spatial;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class NewTicketActivityResponseMessage extends SWGPacket {
	
	public static final int CRC = 0x6EA42D80;
	
	public NewTicketActivityResponseMessage() {
		
	}
	
	public NewTicketActivityResponseMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getByte(data);
		getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(11);
		addShort(data, 2);
		addInt  (data, CRC);
		addByte (data, 0);
		addInt  (data, 1);
		return data;
	}

}
