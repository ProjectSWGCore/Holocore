package network.packets.swg.zone.baselines;

import java.nio.ByteBuffer;


public class CREO8 extends Baseline {
	
	public CREO8() {
		
	}
	
	public void decodeBaseline(ByteBuffer data) {
		
	}
	
	public ByteBuffer encodeBaseline() {
		int length = 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 0);
		return data;
	}
}
