package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatDeletePersistentMessage extends SWGPacket {
	public static final int CRC = 0x8F251641;
	
	private int mailId;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		
		mailId = getInt(data);
	}

	public int getMailId() { return mailId; }
}
