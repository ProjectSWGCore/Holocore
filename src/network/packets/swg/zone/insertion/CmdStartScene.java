package network.packets.swg.zone.insertion;

import java.nio.ByteBuffer;

import resources.Location;
import resources.Race;
import resources.Terrain;
import network.packets.swg.SWGPacket;

public class CmdStartScene extends SWGPacket {
	
	public static final int CRC = 0x3AE6DFAE;
	
	private boolean ignoreLayoutFiles;
	private long charId;
	private Race race;
	private Location l;
	private long galacticTime;
	
	public CmdStartScene() {
		ignoreLayoutFiles = false;
		charId = 0;
		race = Race.HUMAN;
		l = new Location();
		galacticTime = 0;
	}
	
	public CmdStartScene(boolean ignoreLayoutFiles, long charId, Race race, Location l, long galacticTime) {
		this.ignoreLayoutFiles = ignoreLayoutFiles;
		this.charId = charId;
		this.race = race;
		this.l = l;
		this.galacticTime = galacticTime;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		ignoreLayoutFiles = getBoolean(data);
		charId = getLong(data);
		l.setTerrain(Terrain.getTerrainFromName(getAscii(data)));
		l.setX(getFloat(data));
		l.setY(getFloat(data));
		l.setZ(getFloat(data));
		getFloat(data);
		race = Race.getRaceByFile(getAscii(data));
		galacticTime = getLong(data);
		getInt(data); // 0x8EB5EA4E
	}
	
	public ByteBuffer encode() {
		int length = 47 + l.getTerrain().getFile().length() + race.getFilename().length();
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(  data, 2);
		addInt(    data, CRC);
		addBoolean(data, ignoreLayoutFiles);
		addLong(   data, charId);
		addAscii(  data, l.getTerrain().getFile());
		addFloat(  data, (float) l.getX());
		addFloat(  data, (float) l.getY());
		addFloat(  data, (float) l.getZ());
		addFloat(  data, (float) l.getYaw());
		addAscii(  data, race.getFilename());
		addLong(   data, galacticTime);
		addInt    (data, 0x8EB5EA4E);
		return data;
	}
	
	public long getCharacterId() { return charId; }
	public Location getLocation() { return l; }
	public Race getRace() { return race; }
	public long getGalacticTime() { return galacticTime; }
}
