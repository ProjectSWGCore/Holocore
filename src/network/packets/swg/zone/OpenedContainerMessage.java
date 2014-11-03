package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class OpenedContainerMessage extends SWGPacket {
	
	public static final int CRC = 0x2E11E4AB;
	
	private long containerId;
	
	public OpenedContainerMessage() {
		this(0);
	}
	
	public OpenedContainerMessage(long containerId) {
		this.containerId = 0;
	}
	
	public OpenedContainerMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getInt(data); // 0?
		containerId = getLong(data);
		getShort(data); // 0?
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(20);
		addShort(data, 2);
		addInt  (data, CRC);
		addInt  (data, 0);
		addLong (data, containerId);
		addShort(data, 0);
		return data;
	}

}
