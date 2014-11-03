package network.packets.swg.zone.combat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class GrantCommandMessage extends SWGPacket {
	
	public static final int CRC = 0xE67E3875;
	
	private String command;
	
	public GrantCommandMessage() {
		command = "";
	}
	
	public GrantCommandMessage(String command) {
		this.command = command;
	}
	
	public GrantCommandMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		this.command = getAscii(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8 + command.length());
		addShort(data, 2);
		addInt(  data, CRC);
		addAscii(data, command);
		return data;
	}
	
	public String getCommand() {
		return command;
	}
	
	public void setCommand(String command) {
		this.command = command;
	}
	
}
