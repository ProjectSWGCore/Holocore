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
package network.packets.swg.zone.chat;

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class ChatIgnoreList extends SWGPacket {
	
	public static final int CRC = 0xF8C275B0;
	
	private long objectId;
	private List <IgnoreListItem> ignoreList;
	
	public ChatIgnoreList() {
		objectId = 0;
		ignoreList = new ArrayList<IgnoreListItem>();
	}
	
	public ChatIgnoreList(NetBuffer data) {
		decode(data);
	}
	
	public void addName(String game, String galaxy, String name) {
		addName(new IgnoreListItem(game, galaxy, name));
	}
	
	public void addName(IgnoreListItem name) {
		ignoreList.add(name);
	}
	
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		objectId = data.getLong();
		int listCount = data.getInt();
		for (int i = 0; i < listCount; i++) {
			String game = data.getAscii();
			String galaxy = data.getAscii();
			String name = data.getAscii();
			addName(game, galaxy, name);
		}
	}
	
	public NetBuffer encode() {
		int extraSize = 0;
		for (IgnoreListItem item : ignoreList)
			extraSize += 6 + item.getGame().length() + item.getGalaxy().length() + item.getName().length();
		NetBuffer data = NetBuffer.allocate(18 + extraSize);
		data.addShort(3);
		data.addInt(CRC);
		data.addLong(objectId);
		data.addInt(ignoreList.size());
		for (IgnoreListItem item : ignoreList) {
			data.addAscii(item.getGame());
			data.addAscii(item.getGalaxy());
			data.addAscii(item.getName());
		}
		return data;
	}
	
	public static class IgnoreListItem {
		private String game;
		private String galaxy;
		private String name;
		
		public IgnoreListItem() {
			this("SWG", "", "");
		}
		
		public IgnoreListItem(String game, String galaxy, String name) {
			this.game = game;
			this.galaxy = galaxy;
			this.name = name;
		}
		
		public String getGame() { return game; }
		public String getGalaxy() { return galaxy; }
		public String getName() { return name; }
		public void setGame(String game) { this.game = game; }
		public void setGalaxy(String galaxy) { this.galaxy = galaxy; }
		public void setName(String name) { this.name = name; }
	}
	
}
