package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class StationIdHasJediSlot extends SWGPacket {
	
	public static final int CRC = 0xCC9FCCF8;
	
	private int jedi;
	
	public StationIdHasJediSlot() {
		this.jedi = 1;
	}
	
	public StationIdHasJediSlot(int jedi) {
		this.jedi = jedi;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		jedi = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, jedi);
		return data;
	}
	
	public int getJediSlot() {
		return jedi;
	}
	
}
