package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ClientIdMsg extends SWGPacket {
	
	public static final int CRC = 0xD5899226;
	private int gameBitsToClear;
	private byte [] sessionToken;
	private String version;
	
	public ClientIdMsg() {
		this(0, new byte[0], "");
	}
	
	public ClientIdMsg(ByteBuffer data) {
		decode(data);
	}
	
	public ClientIdMsg(int gameBitsToClear, byte [] sessionKey, String version) {
		this.gameBitsToClear = gameBitsToClear;
		this.sessionToken = sessionKey;
		this.version = version;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		gameBitsToClear = getInt(data);
		int sessionKeyLength = getInt(data);
		sessionToken = new byte[sessionKeyLength];
		data.get(sessionToken);
		version = getAscii(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(16 + sessionToken.length + version.length());
		addShort(data, 4);
		addInt(  data, CRC);
		addInt(  data, gameBitsToClear);
		addInt(  data, sessionToken.length);
		data.put(sessionToken);
		addAscii(data, version);
		return data;
	}
	
	public int getGameBitsToClear() { return gameBitsToClear; }
	public byte [] getSessionToken() { return sessionToken; }
	public String getVersion() { return version; }
}
