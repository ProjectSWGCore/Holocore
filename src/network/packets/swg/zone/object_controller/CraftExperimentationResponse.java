package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class CraftExperimentationResponse extends ObjectController{
	
	public static final int CRC = 0x0113;
	
	private int acknowledgeId;
	private int stringId;
	private byte staleFlag;
	
	public CraftExperimentationResponse(int acknowledgeId, int stringId, byte staleFlag) {
		super(CRC);
		this.acknowledgeId = acknowledgeId;
		this.stringId = stringId;
		this.staleFlag = staleFlag;
	}

	public CraftExperimentationResponse(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		acknowledgeId = getInt(data);
		stringId = getInt(data);
		staleFlag= getByte(data);				
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 9);
		encodeHeader(data);
		addInt(data, acknowledgeId);
		addInt(data, stringId);
		addByte(data, staleFlag);
		return data;
	}

	public int getAcknowledgeId() {
		return acknowledgeId;
	}

	public void setAcknowledgeId(int acknowledgeId) {
		this.acknowledgeId = acknowledgeId;
	}

	public int getStringId() {
		return stringId;
	}

	public void setStringId(int stringId) {
		this.stringId = stringId;
	}

	public byte getStaleFlag() {
		return staleFlag;
	}

	public void setStaleFlag(byte staleFlag) {
		this.staleFlag = staleFlag;
	}
}