package network.packets.soe;

import java.nio.ByteBuffer;

import network.packets.Packet;


public class OutOfOrder extends Packet {
	
	private short sequence;
	
	public OutOfOrder() {
		
	}
	
	public OutOfOrder(short sequence) {
		this.sequence = sequence;
	}
	
	public OutOfOrder(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		data.position(2);
		sequence = getNetShort(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(4);
		addNetShort(data, 0x11);
		addNetShort(data, sequence);
		return data;
	}
	
	public short getSequence() { return sequence; }
}
