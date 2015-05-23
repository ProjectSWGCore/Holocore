package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ShowBackpack extends SWGPacket {

	public static final int CRC = 0x8824757B;
	
	private long objectId;
	private boolean showBackpack;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		
		objectId = getLong(data);
		showBackpack = getBoolean(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(15);
		addShort(data, 3);
		addInt(data, CRC);
		addLong(data, objectId);
		addBoolean(data, showBackpack);
		return data;
	}
	
	public boolean showingBackpack() {
		return showBackpack;
	}
	
	public long getObjectId() {
		return objectId;
	}
	
}