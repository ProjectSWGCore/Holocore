package network.packets.swg;

import java.nio.ByteBuffer;


public class ServerUnixEpochTime extends SWGPacket {
	
	public static final int CRC = 0x24B73893;
	private int time = 0;
	
	public ServerUnixEpochTime() {
		
	}
	
	public ServerUnixEpochTime(int time) {
		this.time = time;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		time = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, time);
		return data;
	}
	
	public int getTime() {
		return time;
	}
}
