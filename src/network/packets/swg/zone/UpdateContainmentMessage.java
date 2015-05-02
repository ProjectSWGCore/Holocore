package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class UpdateContainmentMessage extends SWGPacket {
	
	public static final int CRC = 0x56CBDE9E;
	
	private long containerId = 0;
	private long objectId = 0;
	private int slotIndex = 0;
	
	public UpdateContainmentMessage() {
		
	}
	
	public UpdateContainmentMessage(long objectId, long containerId, int slotIndex) {
		this.objectId = objectId;
		this.containerId = containerId;
		this.slotIndex = slotIndex;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objectId = getLong(data);
		containerId = getLong(data);
		slotIndex = getInt(data);
	}
	
	public ByteBuffer encode() {
		int length = 26;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 4);
		addInt(  data, CRC);
		addLong( data, objectId);
		addLong( data, containerId);
		addInt(  data, slotIndex);
		return data;
	}
	
	public long getObjectId() { return objectId; }
	public long getContainerId() { return containerId; }
	public int getSlotIndex() { return slotIndex; }
}
