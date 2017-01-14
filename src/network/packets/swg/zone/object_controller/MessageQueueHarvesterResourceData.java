package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueHarvesterResourceData extends ObjectController {

	public static final int CRC = 0x00EA;
	
	private long harvesterId;
	private int resourceListSize;
	private long[] resourceId;
	private String[] resourceName;
	private String[] resourceType;
	private byte[] resourceDensity;
		
	public MessageQueueHarvesterResourceData(long harvesterId, int resourceListSize, long[] resourceId, String[] resourceName, String[] resourceType, byte[] resourceDensity) {
		super(CRC);
		this.harvesterId = harvesterId;
		this.resourceListSize = resourceListSize;
		this.resourceId = resourceId;
		this.resourceName = resourceName;
		this.resourceType = resourceType;
		this.resourceDensity = resourceDensity;
	}
	
	public MessageQueueHarvesterResourceData(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		harvesterId = getLong(data);
		resourceListSize = getInt(data);
		for(int i = 0; i < resourceListSize; i++){
			resourceId[i] = getLong(data);
			resourceName[i] = getUnicode(data);
			resourceType[i] = getUnicode(data);
			resourceDensity[i] = getByte(data);
		}
		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 29 + resourceId.length + resourceName.length + resourceType.length + resourceDensity.length);
		encodeHeader(data);
		addLong(data, harvesterId);
		addInt(data, resourceListSize);
		for(int i = 0; i < resourceListSize; i++){
			addLong(data, resourceId[i]);
			addUnicode(data, resourceName[i]);
			addUnicode(data, resourceType[i]);
			addByte(data, resourceDensity[i]);
		}
		
		return data;
	}

	public long getHarvesterId() {
		return harvesterId;
	}

	public void setHarvesterId(long harvesterId) {
		this.harvesterId = harvesterId;
	}

	public int getResourceListSize() {
		return resourceListSize;
	}

	public void setResourceListSize(int resourceListSize) {
		this.resourceListSize = resourceListSize;
	}

	public long[] getResourceId() {
		return resourceId;
	}

	public void setResourceId(long[] resourceId) {
		this.resourceId = resourceId;
	}

	public String[] getResourceName() {
		return resourceName;
	}

	public void setResourceName(String[] resourceName) {
		this.resourceName = resourceName;
	}

	public String[] getResourceType() {
		return resourceType;
	}

	public void setResourceTypo(String[] resourceType) {
		this.resourceType = resourceType;
	}

	public byte[] getResourceDensity() {
		return resourceDensity;
	}

	public void setResourceDensity(byte[] resourceDensity) {
		this.resourceDensity = resourceDensity;
	}
}