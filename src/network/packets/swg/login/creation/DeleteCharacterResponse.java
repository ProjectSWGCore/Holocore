package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class DeleteCharacterResponse extends SWGPacket {
	
	public static final int CRC = 0x8268989B;
	private boolean deleted = true;
	
	public DeleteCharacterResponse() {
		
	}
	
	public DeleteCharacterResponse(boolean deleted) {
		this.deleted = deleted;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		deleted = getInt(data) == 0;
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, deleted ? 0 : 1);
		return data;
	}
}
