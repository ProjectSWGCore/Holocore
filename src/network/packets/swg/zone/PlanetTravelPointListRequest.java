package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class PlanetTravelPointListRequest extends SWGPacket {

	public static final int CRC = getCrc("PlanetTravelPointListRequest");
	
	private long requesterObjId;	// The object ID of the CreatureObject belonging to the player clicking the travel terminal
	private String planetName;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		requesterObjId = getLong(data);
		planetName = getAscii(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(16 + planetName.length());	// ascii length short + opcount short + long + int = 16
		addShort(data, 3);	// Operand count of 3
		addInt(data, CRC);
		addLong(data, requesterObjId);
		addAscii(data, planetName);
		return data;
	}
	
	public String getPlanetName() {
		return planetName;
	}
	
}
