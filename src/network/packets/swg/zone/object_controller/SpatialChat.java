package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;


public class SpatialChat extends ObjectController {
	
	public static final int CRC = 0x00F4;
	private long sourceId = 0;
	private long targetId = 0;
	private String text = "";
	private int balloonSize = 5;
	private int balloonType = 0;
	
	public SpatialChat() {
		
	}
	
	public SpatialChat(long sourceId, long targetId, String text, int balloonType) {
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.text = text;
		this.balloonType = balloonType;
	}
	
	public SpatialChat(ByteBuffer data) {
		decodeAsObjectController(data);
	}
	
	public void decodeAsObjectController(ByteBuffer data) {
		sourceId = getLong(data);
		targetId = getLong(data);
		text = getUnicode(data);
		getInt(data);
		balloonSize = getShort(data);
		balloonType = getShort(data);
	}
	
	public ByteBuffer encodeAsObjectController() {
		int length = 44 + text.length() * 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addLong(data, sourceId);
		addLong(data, targetId);
		addUnicode(data, text);
		addInt(data, 0);
		addShort(data, balloonSize);
		addShort(data, balloonType);
		addInt(data, 0);
		addInt(data, 0);
		addInt(data, 0);
		addInt(data, 0);
		return data;
	}
	
}
