package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ShowBackpack extends SWGPacket {

	public static final int CRC = -2010876549;
	
	private long objectId;
	private boolean showBackpack;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		
		objectId = getLong(data);
		showBackpack = getByte(data) == 1;
	}
	
	public boolean showingBackpack() {
		return showBackpack;
	}
	
	public long getObjectId() {
		return objectId;
	}
	
}