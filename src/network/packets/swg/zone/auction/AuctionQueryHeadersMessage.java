package network.packets.swg.zone.auction;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class AuctionQueryHeadersMessage extends SWGPacket {
	
	public static final int CRC = 0x679E0D00;
	
	public AuctionQueryHeadersMessage() {
		
	}
	
	public AuctionQueryHeadersMessage(String command) {
		
	}
	
	public AuctionQueryHeadersMessage(ByteBuffer data) {
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
