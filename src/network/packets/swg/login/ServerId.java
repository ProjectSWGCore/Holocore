package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ServerId extends SWGPacket {
	
	public static final int CRC = 0x58C07F21;
	private int serverId = 0;
	
	public ServerId() {
		
	}
	
	public ServerId(int id) {
		this.serverId = id;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		serverId = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, serverId);
		return data;
	}
	
	public int getServerId() {
		return serverId;
	}
	
}
