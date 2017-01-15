package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueCraftEmptySlot extends ObjectController{

	public static final int CRC = 0x0108;
	
	private int slot;
	private long containerId;
	private byte staleFlag;

	public MessageQueueCraftEmptySlot(int slot, long containerId, byte staleFlag) {
		super(CRC);
		this.slot = slot;
		this.containerId = containerId;
		this.staleFlag = staleFlag;
	}
	
	public MessageQueueCraftEmptySlot(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		slot = getInt(data);
		containerId = getLong(data);
		staleFlag = getByte(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 13);
		encodeHeader(data);
		addInt(data, slot);
		addLong(data, containerId);
		addByte(data, staleFlag);
		return data;
	}

	public int getSlot() {
		return slot;
	}

	public void setSlot(int slot) {
		this.slot = slot;
	}

	public long getContainerId() {
		return containerId;
	}

	public void setContainerId(long containerId) {
		this.containerId = containerId;
	}

	public byte getStaleFlag() {
		return staleFlag;
	}

	public void setStaleFlag(byte staleFlag) {
		this.staleFlag = staleFlag;
	}
}