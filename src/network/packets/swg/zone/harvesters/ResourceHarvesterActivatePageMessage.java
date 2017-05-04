package network.packets.swg.zone.harvesters;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class ResourceHarvesterActivatePageMessage extends SWGPacket {
	public static final int CRC = getCrc("ResourceHarvesterActivatePageMessage");
	
	private long harvesterId;
		
	public ResourceHarvesterActivatePageMessage(long harvesterId) {
		this.harvesterId = harvesterId;
	}
	
	public ResourceHarvesterActivatePageMessage(NetBuffer data) {
		decode(data);
	}

	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		harvesterId = data.getLong();
	}
	
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(8);
		data.addLong(harvesterId);
		return data;
	}

	public long getHarvesterId() {
		return harvesterId;
	}

	public void setHarvesterId(long harvesterId) {
		this.harvesterId = harvesterId;
	}
}