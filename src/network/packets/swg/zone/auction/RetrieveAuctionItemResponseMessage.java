package network.packets.swg.zone.auction;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class RetrieveAuctionItemResponseMessage extends SWGPacket {
	
	public static final int CRC = 0x9499EF8C;
	
	public RetrieveAuctionItemResponseMessage() {
		
	}
	
	public RetrieveAuctionItemResponseMessage(String command) {
		
	}
	
	public RetrieveAuctionItemResponseMessage(ByteBuffer data) {
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
