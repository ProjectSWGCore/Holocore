package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class LoginClientId extends SWGPacket {
	
	public static final int CRC = 0x41131F96;
	private String username;
	private String password;
	private String version;
	
	public LoginClientId() {
		this("", "", "");
	}
	
	public LoginClientId(ByteBuffer data) {
		decode(data);
	}
	
	public LoginClientId(String username, String password, String version) {
		this.username = username;
		this.password = password;
		this.version  = version;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		username  = getAscii(data);
		password  = getAscii(data);
		version = getAscii(data);
	}
	
	public ByteBuffer encode() {
		int length = 6 + 6 + username.length() * 2 + password.length() * 2 + version.length() * 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 4);
		addInt(  data, CRC);
		addAscii(data, username);
		addAscii(data, password);
		addAscii(data, version);
		return data;
	}
	
	public String getUsername()  { return username; }
	public String getPassword()  { return password; }
	public String getVersion()  { return version; }
}
