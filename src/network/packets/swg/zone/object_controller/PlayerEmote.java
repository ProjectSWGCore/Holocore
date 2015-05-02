package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class PlayerEmote extends ObjectController {
	
	public static final int CRC = 0x012E;
	
	private long sourceId;
	private long targetId;
	private short emoteId;
	
	public PlayerEmote(long objectId) {
		super(objectId, CRC);
	}
	
	public PlayerEmote(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public PlayerEmote(long objectId, long sourceId, long targetId, short emoteId) {
		super(objectId, CRC);
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.emoteId = emoteId;
	}
	
	public PlayerEmote(long objectId, PlayerEmote emote) {
		super(objectId, CRC);
		this.sourceId = emote.sourceId;
		this.targetId = emote.targetId;
		this.emoteId = emote.emoteId;
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		sourceId = getLong(data);
		targetId = getLong(data);
		emoteId = getShort(data);
		getShort(data); // Should be 0
		getByte(data); // Should be 3
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 21);
		encodeHeader(data);
		addLong(data, sourceId);
		addLong(data, targetId);
		addShort(data, emoteId);
		addShort(data, (short) 0);
		addByte(data, (byte) 3);
		return data;
	}
	
}
