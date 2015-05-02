package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

import network.packets.swg.zone.object_controller.ObjectController;

public class SitOnObject extends ObjectController {
	
	public static final int CRC = 0x013B;
	
	private long cellId;
	private float x;
	private float y;
	private float z;	
	
	public SitOnObject(long objectId) {
		super(objectId, CRC);
	}
	
	public SitOnObject(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public SitOnObject(long objectId, long cellId, float x, float y, float z ) {
		super(objectId, CRC);
		this.cellId = cellId;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public SitOnObject(long objectId, SitOnObject sit) {
		super(objectId, CRC);
		this.cellId = sit.cellId;
		this.x = sit.x;
		this.y = sit.y;
		this.z = sit.z;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		cellId = getLong(data);
		x = getFloat(data);
		z = getFloat(data);
		y = getFloat(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 20);
		encodeHeader(data);
		addLong(data, cellId);
		addFloat(data, x);
		addFloat(data, z);
		addFloat(data, y);
		return data;
	}
	
}
