package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class CraftAcknowledge extends ObjectController {
	
	public static final int CRC = 0x010C;
	
	private int acknowledgeId;
	private int errorId;
	private byte updateCounter;
	
	public CraftAcknowledge(int acknowledgeId, int errorId, byte updateCounter) {
		super(CRC);
		this.acknowledgeId = acknowledgeId;
		this.errorId = errorId;
		this.updateCounter = updateCounter;
	}
	
	public CraftAcknowledge(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		acknowledgeId = getInt(data);
		errorId = getInt(data);
		updateCounter = getByte(data);		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 9);
		encodeHeader(data);
		addInt(data, acknowledgeId);
		addInt(data, errorId);
		addByte(data, updateCounter);
		return data;
	}

	public int getAcknowledgeId() {
		return acknowledgeId;
	}

	public void setAcknowledgeId(int acknowledgeId) {
		this.acknowledgeId = acknowledgeId;
	}

	public int getErrorId() {
		return errorId;
	}

	public void setErrorId(int errorId) {
		this.errorId = errorId;
	}

	public byte getUpdateCounter() {
		return updateCounter;
	}

	public void setUpdateCounter(byte updateCounter) {
		this.updateCounter = updateCounter;
	}
}