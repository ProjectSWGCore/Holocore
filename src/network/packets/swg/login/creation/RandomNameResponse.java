package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class RandomNameResponse extends SWGPacket {
	
	public static final int CRC = 0xE85FB868;
	
	private String race;
	private String randomName;
	
	public RandomNameResponse() {
		this.race = "object/creature/player/human_male.iff";
		this.randomName = "";
	}
	
	public RandomNameResponse(String race, String randomName) {
		this.race = race;
		this.randomName = randomName;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		race = getAscii(data);
		randomName = getUnicode(data);
		getAscii(data);
		getInt(data);
		getAscii(data);
	}
	
	public ByteBuffer encode() {
		int length = 35 + race.length() + randomName.length() * 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(  data, 4);
		addInt(    data, CRC);
		addAscii(  data, race);
		addUnicode(data, randomName);
		addAscii(  data, "ui");
		addInt(    data, 0);
		addAscii(  data, "name_approved");
		return data;
	}
	
	public void setRace(String race) { this.race = race; }
	public void setRandomName(String randomName) { this.randomName = randomName; }
	
	public String getRace() { return race; }
	public String getRandomName() { return randomName; }
}
