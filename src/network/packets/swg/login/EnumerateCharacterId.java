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
import java.util.Vector;

import network.packets.swg.SWGPacket;


public class EnumerateCharacterId extends SWGPacket {
	public static final int CRC = getCrc("EnumerateCharacterId");
	
	private SWGCharacter [] characters;
	
	public EnumerateCharacterId() {
		characters = new SWGCharacter[0];
	}
	
	public EnumerateCharacterId(SWGCharacter [] characters) {
		this.characters = characters;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int characterLength = getInt(data);
		Vector <SWGCharacter> _characters = new Vector<SWGCharacter>();
		for (int i = 0; i < characterLength; i++) {
			SWGCharacter c = new SWGCharacter();
			c.decode(data);
			_characters.add(c);
		}
		characters = _characters.toArray(new SWGCharacter[0]);
	}
	
	public ByteBuffer encode() {
		int length = 10;
		for (int i = 0; i < characters.length; i++) {
			length += characters[i].getLength();
		}
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, characters.length);
		for (SWGCharacter c : characters) {
			data.put(c.encode().array());
		}
		return data;
	}
	
	public SWGCharacter [] getCharacters () {
		return characters;
	}
	
	public static class SWGCharacter extends EnumerateCharacterId {
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
		
		public void decode(ByteBuffer data) {
			name     = getUnicode(data);
			raceCrc  = getInt(data);
			id       = getLong(data);
			galaxyId = getInt(data);
			type   = getInt(data);
		}
		
		public ByteBuffer encode() {
			ByteBuffer data = ByteBuffer.allocate(getLength());
			addUnicode(data, name);
			addInt(    data, raceCrc);
			addLong(   data, id);
			addInt(    data, galaxyId);
			addInt(    data, type);
			return data;
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
		
	}
}
