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
package network.packets.swg.login;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;

import network.packets.swg.SWGPacket;
import resources.Galaxy;


public class LoginClusterStatus extends SWGPacket {
	
	public static final int CRC = getCrc("LoginClusterStatus");
	
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
	
}
