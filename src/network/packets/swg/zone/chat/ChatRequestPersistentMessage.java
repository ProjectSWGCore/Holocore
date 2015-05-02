package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatRequestPersistentMessage extends SWGPacket {
	public static final int CRC = 0x07E3559F;
	
	private int mailId;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getInt(data);
		mailId = getInt(data);
	}

	public int getMailId() { return this.mailId; }
}
