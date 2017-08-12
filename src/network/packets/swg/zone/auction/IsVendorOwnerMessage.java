package network.packets.swg.zone.auction;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class IsVendorOwnerMessage extends SWGPacket {

	public static final int CRC = com.projectswg.common.data.CRC.getCrc("IsVendorOwnerMessage");
	
	private long terminalId;	
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		terminalId = data.getLong();
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(14);
		data.addShort(2);
		data.addInt(CRC);
		data.addLong(terminalId);
		return data;
	}
	
	public long getTerminalId() {
		return terminalId;
	}
}