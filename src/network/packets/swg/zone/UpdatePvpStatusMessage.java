package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class UpdatePvpStatusMessage extends SWGPacket {
	
	public static final int CRC = 0x08A1C126;
	
	public static final int ATTACKABLE = 1;
	public static final int AGGRESSIVE = 2;
	public static final int OVERT = 4;
	public static final int TEF = 8;
	public static final int PLAYER = 16;
	public static final int ENEMY = 32;
	public static final int	GOING_OVERT = 64; // purple/blue blink
	public static final int GOING_COVERT = 128; // green blink
	public static final int DUEL = 256;

	private int flag = 16;
	private int playerFaction = 0;
	private long objId = 0;
	
	public UpdatePvpStatusMessage() {
		
	}
	
	public UpdatePvpStatusMessage(int playerType, int flag, long objId) {
		this.flag = playerType;
		this.playerFaction = flag;
		this.objId = objId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		flag = getInt(data);
		playerFaction = getInt(data);
		objId = getLong(data);
	}
	
	public ByteBuffer encode() {
		int length = 22;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 4);
		addInt(  data, CRC);
		addInt(  data, flag);
		addInt(  data, playerFaction);
		addLong( data, objId);
		return data;
	}
	
	public long getObjectId() { return objId; }
	public int getPlayerFaction() { return playerFaction; }
	public int getPlayerType() { return flag; }
	
}
