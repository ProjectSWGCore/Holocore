package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueCraftFillSlot extends ObjectController{
	
	public static final int CRC = 0x0107;
	
	private long resourceId;
	private int slotId;
	private int option;
	private byte sequenceId;
		
	public MessageQueueCraftFillSlot(long resourceId, int slotId, int option, byte sequenceId) {
		super(CRC);
		this.resourceId = resourceId;
		this.slotId = slotId;
		this.option = option;
		this.sequenceId = sequenceId;
	}
	
	public MessageQueueCraftFillSlot(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		resourceId = getLong(data);
		slotId = getInt(data);
		option = getInt(data);
		sequenceId = getByte(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 17 );
		encodeHeader(data);
		addLong(data, resourceId);
		addInt(data, slotId);
		addInt(data, option);
		addByte(data, sequenceId);
		return data;
	}

	public long getResourceId() {
		return resourceId;
	}

	public void setResourceId(long resourceId) {
		this.resourceId = resourceId;
	}

	public int getSlotId() {
		return slotId;
	}

	public void setSlotId(int slotId) {
		this.slotId = slotId;
	}

	public int getOption() {
		return option;
	}

	public void setOption(int option) {
		this.option = option;
	}

	public byte getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(byte sequenceId) {
		this.sequenceId = sequenceId;
	}
}