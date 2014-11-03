package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class LoginClientToken extends SWGPacket {
	
	public static final int CRC = 0xAAB296C6;
	private byte [] sessionKey;
	private int userId;
	private String username;
	
	public LoginClientToken() {
		
	}
	
	public LoginClientToken(ByteBuffer data) {
		decode(data);
	}
	
	public LoginClientToken(byte [] sessionKey, int userId, String username) {
		this.sessionKey = sessionKey;
		this.userId = userId;
		this.username = username;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int sessionKeyLength = getInt(data);
		if (sessionKeyLength > data.remaining())
			return;
		sessionKey = new byte[sessionKeyLength];
		data.get(sessionKey);
		userId = getInt(data);
		username = getAscii(data);
	}
	
	public ByteBuffer encode() {
		int length = 16 + sessionKey.length + username.length();
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 4);
		addInt(  data, CRC);
		addInt(  data, sessionKey.length);
		data.put(sessionKey);
		addInt(  data, userId);
		addAscii(data, username);
		return data;
	}
	
	public byte [] getSessionKey() {
		return sessionKey;
	}
	
	public int getUserId() {
		return userId;
	}
	
	public String getUsername() {
		return username;
	}
	
}
