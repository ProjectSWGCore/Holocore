package com.projectswg.connection;

import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;

class HolocoreProtocol {
	
	public static final String VERSION = "2018-02-04";
	
	private static final byte [] EMPTY_PACKET = new byte[0];
	
	private final NetBufferStream inboundStream;
	
	public HolocoreProtocol() {
		this.inboundStream = new NetBufferStream();
	}
	
	public void reset() {
		inboundStream.reset();
	}
	
	public NetBuffer assemble(byte [] raw) {
		NetBuffer data = NetBuffer.allocate(raw.length + 4); // large array
		data.addArrayLarge(raw);
		data.flip();
		return data;
	}
	
	public boolean addToBuffer(byte [] data) {
		synchronized (inboundStream) {
			inboundStream.write(data);
			return hasPacket();
		}
	}
	
	public byte [] disassemble() {
		synchronized (inboundStream) {
			if (inboundStream.remaining() < 4) {
				return EMPTY_PACKET;
			}
			inboundStream.mark();
			int messageLength = inboundStream.getInt();
			if (inboundStream.remaining() < messageLength) {
				inboundStream.rewind();
				return EMPTY_PACKET;
			}
			byte [] data = inboundStream.getArray(messageLength);
			inboundStream.compact();
			return data;
		}
	}
	
	public boolean hasPacket() {
		synchronized (inboundStream) {
			if (inboundStream.remaining() < 4)
				return false;
			inboundStream.mark();
			try {
				int messageLength = inboundStream.getInt();
				return inboundStream.remaining() >= messageLength;
			} finally {
				inboundStream.rewind();
			}
		}
	}
	
}
