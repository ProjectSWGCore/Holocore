package network.packets.swg.zone.spatial;

import java.nio.ByteBuffer;
import network.packets.swg.SWGPacket;

public class GetMapLocationsMessage extends SWGPacket {
	
	public static final int CRC = 0x1A7AB839;
	
	private String planet;
	private float x;
	private float y;
	private boolean category;
	private boolean subcategory;
	private boolean icon;
	
	public GetMapLocationsMessage() {
		this("", 0, 0, false, false, false);
	}
	
	public GetMapLocationsMessage(String planet, float x, float y, boolean category, boolean subcategory, boolean icon) {
		this.planet = planet;
		this.x = x;
		this.y = y;
		this.category = category;
		this.subcategory = subcategory;
		this.icon = icon;
	}
	
	public GetMapLocationsMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		planet = getAscii(data);
		x = getFloat(data);
		y = getFloat(data);
		category = getByte(data) != 0;
		subcategory = getByte(data) != 0;
		icon = getByte(data) != 0;
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(19 + planet.length());
		addShort(data, 28);
		addInt  (data, CRC);
		addAscii(data, planet);
		addFloat(data, x);
		addFloat(data, y);
		addByte (data, category ? 1 : 0);
		addByte (data, subcategory ? 1 : 0);
		addByte (data, icon ? 1 : 0);
		return data;
	}
	
}

