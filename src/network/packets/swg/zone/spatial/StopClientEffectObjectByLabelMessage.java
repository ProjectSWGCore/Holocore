package network.packets.swg.zone.spatial;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class StopClientEffectObjectByLabelMessage extends SWGPacket {
	
	public static final int CRC = 0xAD6F6B26;
	
	private long objectId;
	private String effect;
	
	public StopClientEffectObjectByLabelMessage() {
		
	}
	
	public StopClientEffectObjectByLabelMessage(long objectId, String effect) {
		this.objectId = objectId;
		this.effect = effect;
	}
	
	public StopClientEffectObjectByLabelMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objectId = getLong(data);
		effect = getAscii(data);
		getByte(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(6);
		addShort(data, 4);
		addInt  (data, CRC);
		addLong (data, objectId);
		addAscii(data, effect);
		addByte (data, 1);
		return data;
	}

}
