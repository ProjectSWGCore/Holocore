package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ServerString extends SWGPacket {
	
	public static final int CRC = 0x0E20D7E9;
	private String serverName = "";
	
	public ServerString() {
		
	}
	
	public ServerString(String name) {
		this.serverName = name;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		serverName = getAscii(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8 + serverName.length());
		addShort(data, 2);
		addInt(  data, CRC);
		addAscii(data, serverName);
		return data;
	}
	
	public String getServerString() {
		return serverName;
	}
	
}
