package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class NextCraftingStage extends ObjectController{
	
	public static final int CRC = 0x0109;
	
	private int requestId;
	private int response;
	private byte sequenceId;
	
	public NextCraftingStage(int requestId, int response, byte sequenceId) {
		super(CRC);
		this.requestId = requestId;
		this.response = response;
		this.sequenceId = sequenceId;
	}

	public NextCraftingStage(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		requestId = getInt(data);
		response = getInt(data);
		sequenceId = getByte(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 9);
		encodeHeader(data);
		addInt(data, requestId);
		addInt(data, response);
		addByte(data, sequenceId);
		return data;
	}

	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public int getResponse() {
		return response;
	}

	public void setResponse(int response) {
		this.response = response;
	}

	public byte getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(byte sequenceId) {
		this.sequenceId = sequenceId;
	}
}