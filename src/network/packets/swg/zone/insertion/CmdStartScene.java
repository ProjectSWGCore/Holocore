/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package network.packets.swg.zone.insertion;

import network.packets.swg.SWGPacket;
import resources.Location;
import resources.Race;
import resources.Terrain;

import java.nio.ByteBuffer;

public class CmdStartScene extends SWGPacket {
	public static final int CRC = getCrc("CmdStartScene");
	
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
		getFloat(data); // yaw
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
