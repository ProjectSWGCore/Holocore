package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueSpaceMiningSellResource extends ObjectController{
	
	public static final int CRC = 0x04B6;
	
	private long shipId;
	private long spaceStationId;
	private long resourceId;
	private int amount;
	
	public MessageQueueSpaceMiningSellResource(long shipId, long spaceStationId, long resourceId, int amount) {
		super(CRC);
		this.shipId = shipId;
		this.spaceStationId = spaceStationId;
		this.resourceId = resourceId;
		this.amount = amount;
	}
	
	public MessageQueueSpaceMiningSellResource(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		shipId = getLong(data);
		spaceStationId = getLong(data);
		resourceId = getLong(data);
		amount = getInt(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 28 );
		encodeHeader(data);
		addLong(data, shipId);
		addLong(data, spaceStationId);
		addLong(data, resourceId);
		addInt(data, amount);
		return data;
	}

	public long getShipId() {
		return shipId;
	}

	public void setShipId(long shipId) {
		this.shipId = shipId;
	}

	public long getSpaceStationId() {
		return spaceStationId;
	}

	public void setSpaceStationId(long spaceStationId) {
		this.spaceStationId = spaceStationId;
	}

	public long getResourceId() {
		return resourceId;
	}

	public void setResourceId(long resourceId) {
		this.resourceId = resourceId;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}	
}