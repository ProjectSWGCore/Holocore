package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatInstantMessageToCharacter extends SWGPacket {
	
	public static final int CRC = 0x84BB21F7;
	
	private String galaxy;
	private String character;
	private String message;
	private int sequence;
	
	public ChatInstantMessageToCharacter() {
		this("", "", "", 0);
	}
	
	public ChatInstantMessageToCharacter(String galaxy, String character, String message, int sequence) {
		this.galaxy = galaxy;
		this.character = character;
		this.message = message;
		this.sequence = sequence;
	}
	
	public ChatInstantMessageToCharacter(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getAscii(data);
		galaxy = getAscii(data);
		character = getAscii(data);
		message = getAscii(data);
		getInt(data);
		sequence = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(19 + character.length() + message.length());
		addShort(data, 2);
		addInt(  data, CRC);
		addAscii(data, "SWG");
		addAscii(data, galaxy);
		addAscii(data, character);
		addAscii(data, message);
		addInt  (data, 0);
		addInt  (data, sequence);
		return data;
	}
	
	public String getGalaxy() { return galaxy; }
	public String getCharacter() { return character; }
	public String getMessage() { return message; }
	public int getSequence() { return sequence; }
	
	public void setGalaxy(String galaxy) { this.galaxy = galaxy; }
	public void setCharacter(String character) { this.character = character; }
	public void setMessage(String message) { this.message = message; }
	public void setSequence(int sequence) { this.sequence = sequence; }
	
}
