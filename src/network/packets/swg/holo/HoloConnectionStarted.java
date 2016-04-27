package network.packets.swg.holo;

import java.nio.ByteBuffer;

public class HoloConnectionStarted extends HoloPacket {
	
	public static final int CRC = resources.common.CRC.getCrc("HoloConnectionStarted");
	
	public HoloConnectionStarted() {
		
	}

	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(6);
		addShort(data, 1);
		addInt(data, CRC);
		return data;
	}
	
}
