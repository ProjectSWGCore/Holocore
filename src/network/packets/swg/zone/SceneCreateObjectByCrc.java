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
		l.setOrientationX(getFloat(data));
		l.setOrientationY(getFloat(data));
		l.setOrientationZ(getFloat(data));
		l.setOrientationW(getFloat(data));
		l.setX(getFloat(data));
		l.setZ(getFloat(data));
		l.setY(getFloat(data));
		objCrc = getInt(data);
		getByte(data); // Unknown Byte
	}
	
	public ByteBuffer encode() {
		int length = 47;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 5);
		addInt(  data, CRC);
		addLong( data, objId);
		addFloat(data, l.getOrientationX());
		addFloat(data, l.getOrientationY());
		addFloat(data, l.getOrientationZ());
		addFloat(data, l.getOrientationW());
		addFloat(data, l.getX());
		addFloat(data, l.getY());
		addFloat(data, l.getZ());
		addInt(  data, objCrc);
		addByte( data, 0); // Unknown Byte
		return data;
	}
	
	public long getObjectId() { return objId; }
	public Location getLocation() { return l; }
	public int getObjectCrc() { return objCrc; }
}
