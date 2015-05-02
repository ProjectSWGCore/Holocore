package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;
import resources.Location;

public class SceneCreateObjectByCrc extends SWGPacket {
	
	public static final int CRC = 0xFE89DDEA;
	private long objId = 0;
	private Location l = new Location();
	private int objCrc = 0;
	
	public SceneCreateObjectByCrc() {
		
	}
	
	public SceneCreateObjectByCrc(long objId, Location l, int objCrc) {
		this.objId = objId;
		this.l = l;
		this.objCrc = objCrc;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objId = getLong(data);
		l = getLocation();
		objCrc = getInt(data);
		getByte(data); // Unknown Byte
	}
	
	public ByteBuffer encode() {
		int length = 47;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 5);
		addInt(  data, CRC);
		addLong( data, objId);
		addLocation(data, l);
		addInt(  data, objCrc);
		addByte( data, 0); // Unknown Byte
		return data;
	}
	
	public void setObjectId(long objId) { this.objId = objId; }
	public void setLocation(Location l) { this.l = l; }
	public void setObjectCrc(int objCrc) { this.objCrc = objCrc; }
	
	public long getObjectId() { return objId; }
	public Location getLocation() { return l; }
	public int getObjectCrc() { return objCrc; }
}
