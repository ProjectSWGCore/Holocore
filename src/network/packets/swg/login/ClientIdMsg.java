package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ClientIdMsg extends SWGPacket {
	
	public static final int CRC = 0xD5899226;
	private byte [] sessionKey;
	private String version;
	
	public ClientIdMsg() {
		this(new byte[0], "");
	}
	
	public ClientIdMsg(ByteBuffer data) {
		decode(data);
	}
	
	public ClientIdMsg(byte [] sessionKey, String version) {
		this.sessionKey = sessionKey;
		this.version = version;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int sessionKeyLength = getInt(data);
		sessionKey = new byte[sessionKeyLength];
		data.get(sessionKey);
		version = getAscii(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10 + sessionKey.length + version.length());
		addShort(data, 4);
		addInt(  data, CRC);
		addInt(  data, sessionKey.length);
		data.put(sessionKey);
		addAscii(data, version);
		return data;
	}
}
