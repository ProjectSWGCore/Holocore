package network.packets.swg.zone.building;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class UpdateCellPermissionMessage extends SWGPacket {
	
	public static final int CRC = 0xF612499C;
	
	private byte permissionFlag;
	private long cellId;
	
	public UpdateCellPermissionMessage() {
		permissionFlag = 0;
		cellId = 0;
	}
	
	public UpdateCellPermissionMessage(byte permissionFlag, long cellId) {
		this.permissionFlag = permissionFlag;
		this.cellId = cellId;
	}
	
	public UpdateCellPermissionMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		permissionFlag = getByte(data);
		cellId = getLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(15);
		addShort(data, 2);
		addInt(  data, CRC);
		addByte( data, permissionFlag);
		addLong( data, cellId);
		return data;
	}
	
	public long getCellId() { return cellId; }
	public byte getPermissions() { return permissionFlag; }
	
}
