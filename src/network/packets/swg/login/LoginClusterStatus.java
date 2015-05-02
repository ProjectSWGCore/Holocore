package network.packets.swg.login;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;

import network.packets.swg.SWGPacket;
import resources.Galaxy;


public class LoginClusterStatus extends SWGPacket {
	
	public static final int CRC = 0x3436AEB6;
	
	private Vector <Galaxy> galaxies;
	
	public LoginClusterStatus() {
		galaxies = new Vector<Galaxy>();
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int serverCount = getInt(data);
		for (int i = 0; i < serverCount; i++) {
			Galaxy g = new Galaxy();
			g.setId(getInt(data));
			g.setAddress(getAscii(data));
			g.setZonePort(getShort(data));
			g.setPingPort(getShort(data));
			g.setPopulation(getInt(data));
			g.setPopulationStatus(getInt(data));
			g.setMaxCharacters(getInt(data));
			g.setTimeZone(getInt(data));
			g.setStatus(getInt(data));
			g.setRecommended(getBoolean(data));
			g.setOnlinePlayerLimit(getInt(data));
			g.setOnlineFreeTrialLimit(getInt(data));
			galaxies.add(g);
		}
	}
	
	public ByteBuffer encode() {
		int length = 10;
		for (Galaxy g : galaxies)
			length += 35 + g.getAddress().length() + 8;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, galaxies.size());
		for (Galaxy g : galaxies) {
			 addInt(    data, g.getId());
			 addAscii(  data, g.getAddress());
			 addShort(  data, g.getZonePort());
			 addShort(  data, g.getPingPort());
			 addInt(    data, g.getPopulation());
			 addInt(    data, getPopulationStatus(g.getPopulation()));
			 addInt(    data, g.getMaxCharacters());
			 addInt(    data, g.getTimeZone());
			 addInt(    data, g.getStatus().getStatus());
			 addBoolean(data, g.isRecommended());
			 addInt(    data, g.getOnlinePlayerLimit());
			 addInt(    data, g.getOnlineFreeTrialLimit());
		}
		return data;
	}
	
	public void addGalaxy(Galaxy g) {
		galaxies.add(g);
	}
	
	public List <Galaxy> getGalaxies() {
		return galaxies;
	}
	
	private int getPopulationStatus(int pop) {
		if (pop < 300)
			return 0; // Very Light
		if (pop < 600)
			return 1; // Light
		if (pop < 900)
			return 2; // Medium
		if (pop < 1200)
			return 3; // Heavy
		if (pop < 1500)
			return 4; // Very Heavy
		if (pop < 3000)
			return 5; // Extremely Heavy
		return 6; // Full
	}
	
}
