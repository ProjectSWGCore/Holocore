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

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;


public class EnumerateCharacterId extends SWGPacket {
	public static final int CRC = getCrc("EnumerateCharacterId");
	
	private List<SWGCharacter> characters;
	
	public EnumerateCharacterId() {
		this.characters = new ArrayList<>();
	}
	
	public EnumerateCharacterId(List<SWGCharacter> characters) {
		this.characters = new ArrayList<>(characters);
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		characters = data.getList(SWGCharacter.class);
	}
	
	@Override
	public NetBuffer encode() {
		int length = 10;
		for (SWGCharacter c : characters) {
			length += c.getLength();
		}
		NetBuffer data = NetBuffer.allocate(length);
		data.addShort(2);
		data.addInt(CRC);
		data.addList(characters);
		return data;
	}
	
	public List<SWGCharacter> getCharacters() {
		return characters;
	}
	
	public static class SWGCharacter implements Encodable {
		
		private String name;
		private int raceCrc;
		private long id;
		private int galaxyId;
		private int type;
		
		public SWGCharacter() {
			
		}
		
		public SWGCharacter(String name, int raceCrc, long id, int galaxyId, int status) {
			this.name = name;
			this.raceCrc = raceCrc;
			this.id = id;
			this.galaxyId = galaxyId;
			this.type = status;
		}
		
		public int getLength() {
			return 24 + name.length() * 2;
		}
		
		@Override
		public void decode(NetBuffer data) {
			name     = data.getUnicode();
			raceCrc  = data.getInt();
			id       = data.getLong();
			galaxyId = data.getInt();
			type   = data.getInt();
		}
		
		@Override
		public byte [] encode() {
			NetBuffer data = NetBuffer.allocate(getLength());
			data.addUnicode(name);
			data.addInt(raceCrc);
			data.addLong(id);
			data.addInt(galaxyId);
			data.addInt(type);
			return data.array();
		}
		
		public void		setId(long id)			{ this.id = id; }
		public void		setName(String name)	{ this.name = name; }
		public void		setRaceCrc(int crc)		{ this.raceCrc = crc; }
		public void		setGalaxyId(int id)		{ this.galaxyId = id; }
		public void		setType(int type)	{ this.type = type; }
		
		public long		getId()			{ return id; }
		public String	getName()		{ return name; }
		public int		getRaceCrc()	{ return raceCrc; }
		public int		getGalaxyId()	{ return galaxyId; }
		public int		getType()		{ return type; }
		
		@Override
		public String toString() {
			return String.format("SWGCharacter[id=%d  name=%s  race=%d  galaxy=%d  type=%d", id, name, raceCrc, galaxyId, type);
		}
		
	}
}
