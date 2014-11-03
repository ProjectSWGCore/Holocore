package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

import resources.Location;


public class DataTransform extends ObjectController{
	
	public static final int CRC = 0x0071;
	private int updateCounter = 0;
	private Location l;
	private float speed = 0;
	
	public DataTransform() {
		l = new Location();
	}
	
	public DataTransform(Location l) {
		this.l = l;
	}
	
	public DataTransform(ByteBuffer data) {
		decodeAsObjectController(data);
	}
	
	public void decodeAsObjectController(ByteBuffer data) {
		getInt(data);
		getInt(data);
		updateCounter = getInt(data);
		l = getLocation(data);
		speed = getFloat(data);
	}
	
	public ByteBuffer encodeAsObjectController() {
		int length = 36;
		ByteBuffer data = ByteBuffer.allocate(length);
		addInt(data, updateCounter);
		addLocation(data, l);
		addFloat(data, speed);
		return data;
	}
	
	public int getUpdateCounter() { return updateCounter; }
	public Location getLocation() { return l; }
	public float getSpeed() { return speed; }
}
