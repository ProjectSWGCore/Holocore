package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ShowHelmet extends SWGPacket {

	public static final int CRC = -571479011;
	
	private long objectId;
	private boolean showHelmet;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		
		objectId = getLong(data);
		showHelmet = getByte(data) == 1;
	}
	
	public boolean showingHelmet() {
		return showHelmet;
	}
	
	public long getObjectId() {
		return objectId;
	}
	
}