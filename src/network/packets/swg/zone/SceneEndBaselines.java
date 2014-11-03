package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class SceneEndBaselines extends SWGPacket {
	
	public static final int CRC = 0x2C436037;
	private long objId = 0;
	
	public SceneEndBaselines() {
		
	}
	
	public SceneEndBaselines(long objId) {
		this.objId = objId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objId = getLong(data);
	}
	
	public ByteBuffer encode() {
		int length = 14;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addLong( data, objId);
		return data;
	}
	
	public long getObjectId() { return objId; }
	
}
