package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class UpdateTransformsMessage extends SWGPacket {
	
	public static final int CRC = 0x1B24F808;
	private long objId;
	private short posX;
	private short posY;
	private short posZ;
	private int updateCounter;
	private byte direction;
	private float speed;
	
	public UpdateTransformsMessage() {
		this.objId = 0;
		this.posX = 0;
		this.posY = 0;
		this.posZ = 0;
		this.updateCounter = 0;
		this.direction = 0;
		this.speed = 0;
	}
	
	public UpdateTransformsMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objId = getLong(data);
		posX = getShort(data);
		posY = getShort(data);
		posZ = getShort(data);
		updateCounter = getInt(data);
		speed = getByte(data);
		direction = getByte(data);
		getByte(data);
		getByte(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(28);
		addShort(data, 10);
		addInt(  data, CRC);
		addLong( data, objId);
		addShort(data, posX);
		addShort(data, posY);
		addShort(data, posZ);
		addInt  (data, updateCounter);
		addByte (data, (byte) speed);
		addByte (data, (byte) direction);
		addByte (data, (byte) 1); // lookAtYaw
		addByte (data, (byte) 0); // useLookAtYaw
		return data;
	}
	
	public void setObjectId(long objId) { this.objId = objId; }
	public void setX(short x) { this.posX = x; }
	public void setY(short y) { this.posY = y; }
	public void setZ(short z) { this.posZ = z; }
	public void setUpdateCounter(int count) { this.updateCounter = count; }
	public void setDirection(byte d) { this.direction = d; }
	public void setSpeed(float speed) { this.speed = speed; }
	
	public long getObjectId() { return objId; }
	public short getX() { return posX; }
	public short getY() { return posY; }
	public short getZ() { return posZ; }
	public int getUpdateCounter() { return updateCounter; }
	public byte getDirection() { return direction; }
	public float getSpeed() { return speed; }
	
}
