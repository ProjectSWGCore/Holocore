package network.packets.swg.zone.baselines;

import java.nio.ByteBuffer;


public class PLAY6 extends Baseline {
	
	public void decodeBaseline(ByteBuffer data) {
		
	}
	
	public ByteBuffer encodeBaseline() {
		int length = 81;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 17);
		addInt(  data, 0x4C);
		addAscii(data, "string_id_table");
		addLong( data, 0);
		addLong( data, 0);
		addLong( data, 0);
		addLong( data, 0);
		addLong( data, 0);
		addLong( data, 0);
		addLong( data, 0);
		addShort(data, 0);
		return data;
	}
}
