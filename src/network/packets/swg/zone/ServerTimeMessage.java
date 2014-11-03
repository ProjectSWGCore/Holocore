package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ServerTimeMessage extends SWGPacket {
	
	public static final int CRC = 0x2EBC3BD9;
	
	private long time = 0;
	
	public ServerTimeMessage() {
		
	}
	
	public ServerTimeMessage(long time) {
		this.time = time;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		time = getLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(14);
		addShort(data, 2);
		addInt(  data, CRC);
		addLong( data, time);
		return data;
	}
	
	public long getTime() {
		return time;
	}
}
