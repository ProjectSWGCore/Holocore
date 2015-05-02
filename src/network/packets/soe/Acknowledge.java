package network.packets.soe;

import java.nio.ByteBuffer;

import network.packets.Packet;

public class Acknowledge extends Packet {
	
	private short sequence;
	
	public Acknowledge() {
		sequence = 0;
	}
	
	public Acknowledge(ByteBuffer data) {
		decode(data);
	}
	
	public Acknowledge(short sequence) {
		this.sequence = sequence;
	}
	
	public void decode(ByteBuffer data) {
		if (data.array().length < 4)
			return;
		data.position(2);
		sequence = getNetShort(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(4);
		addNetShort(data, 21);
		addNetShort(data, sequence);
		return data;
	}
	
	public void setSequence(short sequence) { this.sequence = sequence; }
	
	public short getSequence() { return sequence; }
	
}
