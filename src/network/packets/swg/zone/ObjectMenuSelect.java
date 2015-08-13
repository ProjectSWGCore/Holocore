package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ObjectMenuSelect extends SWGPacket {
	
	public static final int CRC = getCrc("ObjectMenuSelectMessage::MESSAGE_TYPE");
	
	private long objectId;
	private short selection;
	
	public ObjectMenuSelect() {
		this(0, (short) 0);
	}
	
	public ObjectMenuSelect(long objectId, short selection) {
		this.objectId = objectId;
		this.selection = selection;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objectId = getLong(data);
		selection = getShort(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(16);
		addShort(data, 3);
		addInt(data, CRC);
		addLong(data, objectId);
		addShort(data, selection);
		return data;
	}
	
	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}
	
	public void setSelection(short selection) {
		this.selection = selection;
	}
	
	public long getObjectId() {
		return objectId;
	}
	
	public short getSelection() {
		return selection;
	}
	
}
