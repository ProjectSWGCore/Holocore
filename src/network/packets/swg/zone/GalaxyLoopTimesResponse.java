package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class GalaxyLoopTimesResponse extends SWGPacket {
	
	public static final int CRC = 0x4E428088;
	private long time = 0;
	
	public GalaxyLoopTimesResponse() {
		
	}
	
	public GalaxyLoopTimesResponse(long time) {
		this.time = time;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		time = getLong(data);
	}
	
	public ByteBuffer encode() {
		int length = 14;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 3);
		addInt(  data, CRC);
		addLong( data, time);
		return data;
	}
}
