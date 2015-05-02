package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

import resources.Posture;

public class PostureUpdate extends ObjectController {
	
	public static final int CRC = 0x0131;
	
	private Posture posture;
	
	public PostureUpdate(long objectId, Posture posture) {
		super(objectId, CRC);
		this.posture = posture;
	}
	
	public PostureUpdate(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		posture = Posture.getFromId(getByte(data));
		getBoolean(data); // isClientImmediate
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 2);
		encodeHeader(data);
		addByte(data, posture.getId());
		addBoolean(data, true); // isClientImmediate
		return data;
	}
	
	public Posture getPosture() { return posture; }
	
	public void setPosture(Posture posture) { this.posture = posture; }
	
}
