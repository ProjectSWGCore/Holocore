package network.packets.swg.zone.auction;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class GetAuctionDetails extends SWGPacket {
	
	public static final int CRC = 0xD36EFAE4;
	
	private long auctionId;
	
	public GetAuctionDetails() {
		this(0);
	}
	
	public GetAuctionDetails(long auctionId) {
		this.auctionId = auctionId;
	}
	
	public GetAuctionDetails(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		auctionId = getLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(14);
		addShort(data, 2);
		addInt  (data, CRC);
		addLong (data, auctionId);
		return data;
	}

}
