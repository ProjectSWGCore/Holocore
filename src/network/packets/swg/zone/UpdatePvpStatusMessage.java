package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class UpdatePvpStatusMessage extends SWGPacket {
	
	public static final int CRC = 0x08A1C126;
	private int playerType = 16;
	private int playerFaction = 0;
	private long objId = 0;
	
	public UpdatePvpStatusMessage() {
		
	}
	
	public UpdatePvpStatusMessage(int playerType, int playerFaction, long objId) {
		this.playerType = playerType;
		this.playerFaction = playerFaction;
		this.objId = objId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		playerType = getInt(data);
		playerFaction = getInt(data);
		objId = getLong(data);
	}
	
	public ByteBuffer encode() {
		int length = 22;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 4);
		addInt(  data, CRC);
		addInt(  data, playerType);
		addInt(  data, playerFaction);
		addLong( data, objId);
		return data;
	}
	
	public long getObjectId() { return objId; }
	public int getPlayerFaction() { return playerFaction; }
	public int getPlayerType() { return playerType; }
	
}
