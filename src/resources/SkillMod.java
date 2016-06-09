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
package resources;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.encodables.Encodable;
import resources.network.NetBufferStream;
import resources.persistable.Persistable;

public class SkillMod implements Encodable, Persistable {
	
	private int base, modifier;
	
	public SkillMod() {
		this(0, 0);
	}
	
	public SkillMod(int base, int modifier) {
		this.base = base;
		this.modifier = modifier;
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 2).order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putInt(base);
		buffer.putInt(modifier);
		
		return buffer.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		base = data.getInt();
		modifier = data.getInt();
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addInt(base);
		stream.addInt(modifier);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		base = stream.getInt();
		modifier = stream.getInt();
	}

	public void adjustBase(int adjustment) {
		base += adjustment;
	}
	
	public void adjustModifier(int adjustment) {
		modifier += adjustment;
	}
	
	public int getValue() {
		return base + modifier;
	}
	
	public String toString() {
		return "SkillMod[Base="+base+", Modifier="+modifier+"]";
	}

}
