package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class SceneDestroyObject extends SWGPacket {
	
	public static final int CRC = 0x4D45D504;
	
	private long objId;
	
	public SceneDestroyObject() {
		this(0);
	}
	
	public SceneDestroyObject(long objId) {
		
	}
	
	public SceneDestroyObject(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objId = getLong(data);
		getByte(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(14);
		addShort(data, 3);
		addInt  (data, CRC);
		addLong (data, objId);
		addByte (data, 0);
		return data;
	}
	
	public long getObjectId() { return objId; }
	public void setObjectId(long objId) { this.objId = objId; }
	
}
