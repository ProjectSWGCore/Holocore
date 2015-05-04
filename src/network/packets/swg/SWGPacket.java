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
package network.packets.swg;

import java.nio.ByteBuffer;

import resources.Location;
import network.PacketType;
import network.packets.Packet;


public class SWGPacket extends Packet {
	
	private ByteBuffer data = null;
	private int opcode = 0;
	private PacketType type = PacketType.UNKNOWN;
	
	protected void addLocation(ByteBuffer data, Location l) {
		addFloat(data, (float) (Double.isNaN(l.getOrientationX()) ? 0 : l.getOrientationX()));
		addFloat(data, (float) (Double.isNaN(l.getOrientationY()) ? 0 : l.getOrientationY()));
		addFloat(data, (float) (Double.isNaN(l.getOrientationZ()) ? 0 : l.getOrientationZ()));
		addFloat(data, (float) (Double.isNaN(l.getOrientationW()) ? 1 : l.getOrientationW()));
		addFloat(data, (float) (Double.isNaN(l.getX()) ? 0 : l.getX()));
		addFloat(data, (float) (Double.isNaN(l.getY()) ? 0 : l.getY()));
		addFloat(data, (float) (Double.isNaN(l.getZ()) ? 0 : l.getZ()));
	}
	
	protected Location getLocation(ByteBuffer data) {
		Location l = new Location();
		l.setOrientationX(getFloat(data));
		l.setOrientationY(getFloat(data));
		l.setOrientationZ(getFloat(data));
		l.setOrientationW(getFloat(data));
		l.setX(getFloat(data));
		l.setY(getFloat(data));
		l.setZ(getFloat(data));
		return l;
	}
	
	public void setSWGOpcode(int opcode) {
		this.opcode = opcode;
		this.type = PacketType.fromCrc(opcode);
	}
	
	public int getSWGOpcode() {
		return opcode;
	}
	
	public PacketType getPacketType() {
		return type;
	}
	
	public boolean decode(ByteBuffer data, int crc) {
		this.data = data;
		super.decode(data);
		data.position(2);
		setSWGOpcode(getInt(data));
		if (getSWGOpcode() != crc)
			return false;
		return true;
	}
	
	public void decode(ByteBuffer data) {
		this.data = data;
		if (data.array().length < 6)
			return;
		data.position(2);
		setSWGOpcode(getInt(data));
		data.position(0);
	}
	
	public ByteBuffer encode() {
		return data;
	}
	
	public ByteBuffer getData() {
		return data;
	}
}
