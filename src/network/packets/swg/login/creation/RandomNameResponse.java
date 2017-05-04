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
package network.packets.swg.login.creation;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;


public class RandomNameResponse extends SWGPacket {
	public static final int CRC = getCrc("ClientRandomNameResponse");
	
	private String race;
	private String randomName;
	
	public RandomNameResponse() {
		this.race = "object/creature/player/human_male.iff";
		this.randomName = "";
	}
	
	public RandomNameResponse(String race, String randomName) {
		this.race = race;
		this.randomName = randomName;
	}
	
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		race = data.getAscii();
		randomName = data.getUnicode();
		data.getAscii();
		data.getInt();
		data.getAscii();
	}
	
	public NetBuffer encode() {
		int length = 35 + race.length() + randomName.length() * 2;
		NetBuffer data = NetBuffer.allocate(length);
		data.addShort(4);
		data.addInt(CRC);
		data.addAscii(race);
		data.addUnicode(randomName);
		data.addAscii("ui");
		data.addInt(0);
		data.addAscii("name_approved");
		return data;
	}
	
	public void setRace(String race) { this.race = race; }
	public void setRandomName(String randomName) { this.randomName = randomName; }
	
	public String getRace() { return race; }
	public String getRandomName() { return randomName; }
}
