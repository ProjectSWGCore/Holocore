package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class CreateCharacterSuccess extends SWGPacket {
	
	public static final int CRC = 0x1DB575CC;
	private long id = 0;
	
	public CreateCharacterSuccess() {
		
	}
	
	public CreateCharacterSuccess(long charId) {
		this.id = charId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		id = getLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(14);
		addShort(data, 2);
		addInt(  data, CRC);
		addLong( data, id);
		return data;
	}
	
	public long getId() { return id; }
	public void setId(long id) { this.id = id; }
}
