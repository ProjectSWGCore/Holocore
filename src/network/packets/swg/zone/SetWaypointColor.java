package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class SetWaypointColor extends SWGPacket {
	public static final int CRC = 0x90C59FDE;
	
	private long objId;
	private String color;
	
	public SetWaypointColor() { }
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		
		objId = getLong(data);
		color = getAscii(data);
	}

	public long getObjId() {
		return objId;
	}

	public String getColor() {
		return color;
	}
}
