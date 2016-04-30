package network.packets.swg.holo;

import java.nio.ByteBuffer;

public class HoloSetProtocolVersion extends HoloPacket {
	
	public static final int CRC = resources.common.CRC.getCrc("HoloSetProtocolVersion");
	
	private String protocol;
	
	public HoloSetProtocolVersion() {
		this("");
	}
	
	public HoloSetProtocolVersion(String protocol) {
		this.protocol = protocol;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		protocol = getAscii(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8 + protocol.length());
		addShort(data, 2);
		addInt(data, CRC);
		addAscii(data, protocol);
		return data;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
}
