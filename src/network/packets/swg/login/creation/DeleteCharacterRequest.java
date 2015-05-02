package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class DeleteCharacterRequest extends SWGPacket {
	
	public static final int CRC = 0xE87AD031;
	private int serverId = 0;
	private long playerId = 0;
	
	public DeleteCharacterRequest() {
		
	}
	
	public DeleteCharacterRequest(int serverId, long playerId) {
		this.serverId = serverId;
		this.playerId = playerId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		serverId = getInt(data);
		playerId = getLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(18);
		addShort(data, 3);
		addInt(  data, CRC);
		addInt(  data, serverId);
		addLong( data, playerId);
		return data;
	}
	
	public int getServerId() { return serverId; }
	public long getPlayerId() { return playerId; }
}
