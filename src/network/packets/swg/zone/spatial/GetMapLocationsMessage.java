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
	
	public String getPlanet() { return planet; }
}

