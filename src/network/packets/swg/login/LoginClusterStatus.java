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
			getInt(data); // Server Port and Ping Port (blah!)
			getInt(data); // Spacer of some kind.. always 0xFFFFFFFF
			g.setPopulation(getInt(data));
			g.setMaxCharacters(getInt(data));
			g.setDistance(getInt(data));
			g.setStatus(getInt(data));
			g.setRecommended(getBoolean(data));
			getInt(data);
			getInt(data);
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
			 addShort(  data, 44463);
			 addShort(  data, 44462);
			 addInt(    data, g.getPopulation());
			 //addInt(    data, 0xFFFFFFFF);
			 /*
			  * Very Light		= 0
			  * Light			= 300
			  * Medium 			= 600
			  * Heavy 			= 900
			  * Very Heavy		= 1200
			  * Extreamly Heavy = 1500
			  * Full 			= 3000
			  */
			 addInt(    data, 0); // Very Light
			 addInt(    data, g.getMaxCharacters());
			 addInt(    data, g.getDistance());
			 addInt(    data, g.getStatus());
			 addBoolean(data, g.isRecommended());
			 data.put(new byte[] { (byte)0xC4, 0x09, 0, 0, (byte)0xFA, 0, 0, 0});
		}
		return data;
	}
	
	public void addGalaxy(Galaxy g) {
		galaxies.add(g);
	}
	
	public List <Galaxy> getGalaxies() {
		return galaxies;
	}
	
}
