package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class LoginIncorrectClientId extends SWGPacket {
	
	public static final int CRC = 0x20E7E510;
	
	private String serverId;
	private String serverAppVersion;
	
	public LoginIncorrectClientId() {
		this("", "");
	}
	
	public LoginIncorrectClientId(ByteBuffer data) {
		decode(data);
	}
	
	public LoginIncorrectClientId(String serverId, String serverAppVersion) {
		this.serverId = serverId;
		this.serverAppVersion = serverAppVersion;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		serverId = getAscii(data);
		serverAppVersion = getAscii(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(10 + serverId.length() + serverAppVersion.length());
		addShort(data, 4);
		addInt(data, CRC);
		addAscii(data, serverId);
		addAscii(data, serverAppVersion);
		return data;
	}
	
}
