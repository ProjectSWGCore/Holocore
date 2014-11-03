package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ClientPermissionsMessage extends SWGPacket {
	
	public static final int CRC = 0xE00730E5;
	
	public ClientPermissionsMessage() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getByte(data); // 1
		getByte(data); // 1
		getByte(data); // 0
		getByte(data); // 1
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10);
		addShort(data, 5);
		addInt(  data, CRC);
		addByte( data, 1);
		addByte( data, 1);
		addByte( data, 0);
		addByte( data, 1);
		return data;
	}
}
