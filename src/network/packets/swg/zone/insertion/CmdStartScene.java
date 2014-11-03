package network.packets.swg.zone.insertion;

import java.nio.ByteBuffer;

import resources.Location;
import resources.Race;
import network.packets.swg.SWGPacket;

public class CmdStartScene extends SWGPacket {
	
	public static final int CRC = 0x3AE6DFAE;
	
	private boolean ignoreLayoutFiles;
	private long charId;
	private String terrain;
	private Race race;
	private Location l;
	private long galacticTime;
	
	public CmdStartScene() {
		ignoreLayoutFiles = false;
		charId = 0;
		terrain = "";
		race = Race.HUMAN;
		l = new Location();
		galacticTime = 0;
	}
	
	public CmdStartScene(boolean ignoreLayoutFiles, long charId, String terrain, String race, Location l, long galacticTime) {
		this.ignoreLayoutFiles = ignoreLayoutFiles;
		this.charId = charId;
		this.terrain = terrain;
		this.race = Race.HUMAN;
		this.l = l;
		this.galacticTime = galacticTime;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		ignoreLayoutFiles = getBoolean(data);
		charId = getLong(data);
		terrain = getAscii(data);
		l.setX(getFloat(data));
		l.setY(getFloat(data));
		l.setZ(getFloat(data));
		getFloat(data);
		race = Race.getRace(getAscii(data));
		galacticTime = getLong(data);
		getInt(data); // 0x8EB5EA4E
	}
	
	public ByteBuffer encode() {
		int length = 47 + terrain.length() + race.getIFF().length();
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(  data, 2);
		addInt(    data, CRC);
		addBoolean(data, ignoreLayoutFiles);
		addLong(   data, charId);
		addAscii(  data, terrain);
		addFloat(  data, l.getX());
		addFloat(  data, l.getY());
		addFloat(  data, l.getZ());
		addFloat(  data, 0);
		addAscii(  data, race.getIFF());
		addLong(   data, galacticTime);
		addInt    (data, 0x8EB5EA4E);
		return data;
	}
	
	public long getCharacterId() { return charId; }
	public String getTerrain() { return terrain; }
	public Location getLocation() { return l; }
	public Race getRace() { return race; }
	public long getGalacticTime() { return galacticTime; }
}
