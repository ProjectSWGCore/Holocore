package network.packets.swg.zone.spatial;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class GetMapLocationsResponseMessage extends SWGPacket {
	public static final int CRC = 0x9F80464C;
	private String planet;

	public GetMapLocationsResponseMessage(String planet) {
		this.planet = planet;
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(32 + planet.length());

		addShort(data, 8);
		addInt(data, CRC);
		addAscii(data, planet);
		addInt(data, 0); // map locations size
		
		// Unknowns
		addInt(data, 0);
		addInt(data, 0);
		addInt(data, 0x480);
		addInt(data, 0x48D);
		addInt(data, 1);
		return data;
	}
	
}
