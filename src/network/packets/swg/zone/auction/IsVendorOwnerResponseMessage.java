package network.packets.swg.zone.auction;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class IsVendorOwnerResponseMessage extends SWGPacket {
	
	public static final int CRC = 0xCE04173E;
	
	public IsVendorOwnerResponseMessage() {
		
	}
	
	public IsVendorOwnerResponseMessage(String command) {
		
	}
	
	public IsVendorOwnerResponseMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(6);
		addShort(data, 2);
		addInt  (data, CRC);
		return data;
	}

}
