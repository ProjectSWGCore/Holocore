package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ShowHelmet extends SWGPacket {

	public static final int CRC = 0xDDEFEC1D;
	
	private long objectId;
	private boolean showHelmet;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		
		objectId = getLong(data);
		showHelmet = getBoolean(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(15);
		addShort(data, 3);
		addInt(data, CRC);
		addLong(data, objectId);
		addBoolean(data, showHelmet);
		return data;
	}
	
	public boolean showingHelmet() {
		return showHelmet;
	}
	
	public long getObjectId() {
		return objectId;
	}
	
}