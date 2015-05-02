package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatInstantMessageToClient extends SWGPacket {
	
	public static final int CRC = 0x3C565CED;
	
	private String galaxy;
	private String character;
	private String message;
	private String outOfBand;
	
	public ChatInstantMessageToClient() {
		this("", "", "", "");
	}
	
	public ChatInstantMessageToClient(String galaxy, String character, String message) {
		this(galaxy, character, message, "");
	}
	
	public ChatInstantMessageToClient(String galaxy, String character, String message, String outOfBand) {
		this.galaxy = galaxy;
		this.character = character;
		this.message = message;
		this.outOfBand = outOfBand;
	}
	
	public ChatInstantMessageToClient(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getAscii(data); // "SWG"
		galaxy = getAscii(data);
		character = getAscii(data);
		message = getUnicode(data);
		outOfBand = getUnicode(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(23 + galaxy.length() + character.length() + message.length()*2 + outOfBand.length()*2);
		addShort(data, 2);
		addInt(  data, CRC);
		addAscii(data, "SWG");
		addAscii(data, galaxy);
		addAscii(data, character);
		addUnicode(data, message);
		addUnicode(data, outOfBand);
		return data;
	}
	
	public String getGalaxy() { return galaxy; }
	public String getCharacter() { return character; }
	public String getMessage() { return message; }
	public String getOutOfBand() { return outOfBand; }
	
	public void setGalaxy(String galaxy) { this.galaxy = galaxy; }
	public void setCharacter(String character) { this.character = character; }
	public void setMessage(String message) { this.message = message; }
	public void setOutOfBand(String outOfBand) { this.outOfBand = outOfBand; }
	
}
