package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueCraftRequestSession extends ObjectController{

	private final static int CRC = 0x010F;
	
	private long sessionId; //wants to create a session for this particular craftingsession
	private int sequenceId;
	
	public MessageQueueCraftRequestSession(long sessionId, int sequenceId) {
		super(CRC);
		this.sessionId = sessionId;
		this.sequenceId = sequenceId;
	}
		
	public MessageQueueCraftRequestSession(ByteBuffer data) {
		super(CRC);
		decode(data);
	}	

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		sessionId = getLong(data);
		sequenceId = getInt(data);		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 12);
		encodeHeader(data);
		addLong(data, sessionId);
		addInt(data, sequenceId);
		return data;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public int getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(int sequenceId) {
		this.sequenceId = sequenceId;
	}
}