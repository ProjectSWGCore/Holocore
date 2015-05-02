package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class CharacterCreationDisabled extends SWGPacket {
	
	public static final int CRC = 0xF4A15265;
	
	private String [] serverNames = new String[0];
	
	public CharacterCreationDisabled() {
		this(new String[0]);
	}
	
	public CharacterCreationDisabled(String [] serverNames) {
		this.serverNames = serverNames;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int listSize = getInt(data);
		serverNames = new String[listSize];
		for (int i = 0; i < listSize; i++) {
			serverNames[i] = getAscii(data);
		}
	}
	
	public ByteBuffer encode() {
		int length = 10;
		for (int i = 0; i < serverNames.length; i++)
			length += 2 + serverNames[i].length();
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, serverNames.length);
		for (int i = 0; i < serverNames.length; i++) {
			addAscii(data, serverNames[i]);
		}
		return data;
	}
	
	public String [] getServerNames() {
		return serverNames;
	}
	
}
