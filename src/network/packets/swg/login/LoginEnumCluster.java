package network.packets.swg.login;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;

import network.packets.swg.SWGPacket;
import resources.Galaxy;


public class LoginEnumCluster extends SWGPacket {
	
	public static final int CRC = 0xC11C63B9;
	
	private Vector <Galaxy> galaxies;
	private int maxCharacters;
	
	public LoginEnumCluster() {
		galaxies = new Vector<Galaxy>();
	}
	
	public LoginEnumCluster(int maxCharacters) {
		galaxies = new Vector<Galaxy>();
		this.maxCharacters = maxCharacters;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int serverCount = getInt(data);
		for (int i = 0; i < serverCount; i++) {
			Galaxy g = new Galaxy();
			g.setId(getInt(data));
			g.setName(getAscii(data));
			g.setTimeZone(getInt(data));
			galaxies.add(g);
		}
		maxCharacters = getInt(data);
	}
	
	public ByteBuffer encode() {
		int length = 14;
		for (Galaxy g : galaxies)
			length += 10 + g.getName().length();
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 3);
		addInt(  data, CRC);
		addInt(  data, galaxies.size());
		for (Galaxy g : galaxies) {
			addInt(  data, g.getId());
			addAscii(data, g.getName());
			addInt(  data, g.getTimeZone());
		}
		addInt(data, maxCharacters);
		return data;
	}
	
	public void addGalaxy(Galaxy g) {
		galaxies.add(g);
	}
	
	public void setMaxCharacters(int max) {
		this.maxCharacters = max;
	}
	
	public int getMaxCharacters() {
		return maxCharacters;
	}
	
	public List <Galaxy> getGalaxies() {
		return galaxies;
	}
}
