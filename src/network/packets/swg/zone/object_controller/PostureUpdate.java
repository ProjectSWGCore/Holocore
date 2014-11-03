package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

import resources.Posture;

public class PostureUpdate extends ObjectController {
	
	public static final int CRC = 0x00000131;
	
	private Posture posture;
	
	public PostureUpdate() {
		this((byte)0);
	}
	
	public PostureUpdate(Posture posture) {
		this.posture = posture;
	}
	
	public PostureUpdate(byte posture) {
		this.posture = Posture.getFromId(posture);
	}
	
	public PostureUpdate(ByteBuffer data) {
		decodeAsObjectController(data);
	}
	
	public void decodeAsObjectController(ByteBuffer data) {
		posture = Posture.getFromId(data.get());
		data.get();
	}
	
	public ByteBuffer encodeAsObjectController() {
		ByteBuffer data = ByteBuffer.allocate(2);
		data.put(posture.getId());
		data.put((byte)1);
		return data;
	}
	
	
	public Posture getPosture() { return posture; }
	public void setPosture(byte posture) { this.posture = Posture.getFromId(posture); }
	public void setPosture(Posture posture) { this.posture = posture; }
}
