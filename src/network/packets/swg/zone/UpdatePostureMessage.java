package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;
import resources.Posture;

public class UpdatePostureMessage extends SWGPacket {
	
	public static final int CRC = 0x00BDE6B41;
	private int posture = 0;
	private long objId = 0;
	
	public UpdatePostureMessage() {
		
	}
	
	public UpdatePostureMessage(Posture posture, long objId) {
		this.posture = posture.getId();
		this.objId = objId;
	}
	
	public UpdatePostureMessage(int posture, long objId) {
		this.posture = posture;
		this.objId = objId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		posture = getByte(data);
		objId = getLong(data);
	}
	
	public ByteBuffer encode() {
		int length = 16;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 3);
		addInt  (data, CRC);
		addByte (data, posture);
		addLong (data, objId);
		return data;
	}
}
