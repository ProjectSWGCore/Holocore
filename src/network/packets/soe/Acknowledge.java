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
package network.packets.soe;

import java.nio.ByteBuffer;

import network.packets.Packet;

public class Acknowledge extends Packet {
	
	private short sequence;
	
	public Acknowledge() {
		sequence = 0;
	}
	
	public Acknowledge(ByteBuffer data) {
		decode(data);
	}
	
	public Acknowledge(short sequence) {
		this.sequence = sequence;
	}
	
	public void decode(ByteBuffer data) {
		if (data.array().length < 4)
			return;
		data.position(2);
		sequence = getNetShort(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(4);
		addNetShort(data, 21);
		addNetShort(data, sequence);
		return data;
	}
	
	public void setSequence(short sequence) { this.sequence = sequence; }
	
	public short getSequence() { return sequence; }
	
}
