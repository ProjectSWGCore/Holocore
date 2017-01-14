package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueCraftRequestSession extends ObjectController{

	private final static int CRC = 0x010F;
	
	private long stationId;
	private int sequenceId;
	
	public MessageQueueCraftRequestSession(long stationId, int sequenceId) {
		super(CRC);
		this.stationId = stationId;
		this.sequenceId = sequenceId;
	}
		
	public MessageQueueCraftRequestSession(ByteBuffer data) {
		super(CRC);
		decode(data);
	}	

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		stationId = getLong(data);
		sequenceId = getInt(data);		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 13);
		encodeHeader(data);
		addLong(data, stationId);
		addInt(data, sequenceId);
		return data;
	}

	public long getStationId() {
		return stationId;
	}

	public void setStationId(long stationId) {
		this.stationId = stationId;
	}

	public int getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(int sequenceId) {
		this.sequenceId = sequenceId;
	}
}