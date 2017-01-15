package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class CraftingSessionEnded extends ObjectController{
	
	public static final int CRC = 0x01C2;
	
	private long playerId; //crafterId
	private int sessionId; //sessionId, must be 0 after the Craftingsession
	private byte count; //Itemcount created in that Session
	
	public CraftingSessionEnded(long playerId, int sessionId, byte count) {
		super(CRC);
		this.playerId = playerId;
		this.sessionId = sessionId;
		this.count = count;
	}
	
	public CraftingSessionEnded(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		playerId = getLong(data);
		sessionId = getInt(data);
		count = getByte(data);		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 9);
		encodeHeader(data);
		addLong(data, playerId);
		addInt(data, sessionId);
		addByte(data, count);
		return data;
	}

	public long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(long playerId) {
		this.playerId = playerId;
	}

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	public byte getCount() {
		return count;
	}

	public void setCount(byte count) {
		this.count = count;
	}	
}