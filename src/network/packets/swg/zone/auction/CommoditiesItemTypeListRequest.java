package network.packets.swg.zone.auction;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class CommoditiesItemTypeListRequest extends SWGPacket {
	
	public static final int CRC = com.projectswg.common.data.CRC.getCrc("CommoditiesItemTypeListRequest");
	
	private String from;

	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;		
		from = data.getAscii();	
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(6 + from.length());
		data.addShort(2);
		data.addInt(CRC);
		data.addAscii(from);
		return data;
	}
	
	public String getFrom() {
		return from;
	}
}