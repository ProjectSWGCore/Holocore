package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class UpdateTransformsMessage extends SWGPacket {
	
	public static final int CRC = 0x1B24F808;
	private long objId;
	private int posX;
	private int posY;
	private int posZ;
	private int updateCounter;
	private byte posture;
	private byte direction;
	
	public UpdateTransformsMessage() {
		this(0, 0, 0, 0, 0, (byte)0, (byte)0);
	}
	
	public UpdateTransformsMessage(long objId, int posX, int posY, int posZ, int updateCounter, byte posture, byte direction) {
		this.objId = objId;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.updateCounter = updateCounter;
		this.posture = posture;
		this.direction = direction;
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
		posture = getByte(data);
		direction = getByte(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(26);
		addShort(data, 2);
		addInt(  data, CRC);
		addLong( data, objId);
		addShort(data, posX);
		addShort(data, posY);
		addShort(data, posZ);
		addInt  (data, updateCounter);
		addByte (data, posture);
		addByte (data, direction);
		return data;
	}
	
	public long getObjectId() { return objId; }
	
}
