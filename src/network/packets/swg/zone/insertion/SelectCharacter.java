package network.packets.swg.zone.insertion;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class SelectCharacter extends SWGPacket {
	
	public static final int CRC = 0xB5098D76;
	
	private long charId = 0;
	
	public SelectCharacter() {
		
	}
	
	public SelectCharacter(long id) {
		this.charId = id;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		charId = getLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(14);
		addShort(data, 2);
		addInt(  data, CRC);
		addLong( data, charId);
		return data;
	}
	
	public long getCharacterId() { return charId; }
}
