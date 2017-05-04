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

import java.time.ZoneOffset;
import java.util.List;
import java.util.Vector;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;
import resources.Galaxy;


public class LoginClusterStatus extends SWGPacket {
	
	public static final int CRC = getCrc("LoginClusterStatus");
	
	private Vector <Galaxy> galaxies;
	
	public LoginClusterStatus() {
		galaxies = new Vector<Galaxy>();
	}
	
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		int serverCount = data.getInt();
		for (int i = 0; i < serverCount; i++) {
			Galaxy g = new Galaxy();
			g.setId(data.getInt());
			g.setAddress(data.getAscii());
			g.setZonePort(data.getShort());
			g.setPingPort(data.getShort());
			g.setPopulation(data.getInt());
			data.getInt(); // population status
			g.setMaxCharacters(data.getInt());
			g.setZoneOffset(ZoneOffset.ofTotalSeconds(data.getInt()));
			g.setStatus(data.getInt());
			g.setRecommended(data.getBoolean());
			g.setOnlinePlayerLimit(data.getInt());
			g.setOnlineFreeTrialLimit(data.getInt());
			galaxies.add(g);
		}
	}
	
	public NetBuffer encode() {
		int length = 10;
		for (Galaxy g : galaxies)
			length += 39 + g.getAddress().length();
		NetBuffer data = NetBuffer.allocate(length);
		data.addShort(2);
		data.addInt(CRC);
		data.addInt(galaxies.size());
		for (Galaxy g : galaxies) {
			data.addInt(g.getId());
			data.addAscii(g.getAddress());
			data.addShort(g.getZonePort());
			data.addShort(g.getPingPort());
			data.addInt(g.getPopulation());
			data.addInt(g.getPopulationStatus());
			data.addInt(g.getMaxCharacters());
			data.addInt(g.getDistance());
			data.addInt(g.getStatus().getStatus());
			data.addBoolean(g.isRecommended());
			data.addInt(g.getOnlinePlayerLimit());
			data.addInt(g.getOnlineFreeTrialLimit());
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
