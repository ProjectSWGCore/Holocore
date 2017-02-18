package network.packets.swg.zone.harvesters;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ResourceHarvesterActivatePageMessage extends SWGPacket {
	public static final int CRC = getCrc("ResourceHarvesterActivatePageMessage");
	
	private long harvesterId;
		
	public ResourceHarvesterActivatePageMessage(long harvesterId) {
		this.harvesterId = harvesterId;
	}
	
	public ResourceHarvesterActivatePageMessage(ByteBuffer data) {
		decode(data);
	}

	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		harvesterId = getLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8);
		addLong(data, harvesterId);
		return data;
	}

	public long getHarvesterId() {
		return harvesterId;
	}

	public void setHarvesterId(long harvesterId) {
		this.harvesterId = harvesterId;
	}
}