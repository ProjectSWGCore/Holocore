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
package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class SitOnObject extends ObjectController {
	
	public static final int CRC = 0x013B;
	
	private long cellId;
	private float x;
	private float y;
	private float z;	
	
	public SitOnObject(long objectId) {
		super(objectId, CRC);
	}
	
	public SitOnObject(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public SitOnObject(long objectId, long cellId, float x, float y, float z ) {
		super(objectId, CRC);
		this.cellId = cellId;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public SitOnObject(long objectId, SitOnObject sit) {
		super(objectId, CRC);
		this.cellId = sit.cellId;
		this.x = sit.x;
		this.y = sit.y;
		this.z = sit.z;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		cellId = getLong(data);
		x = getFloat(data);
		z = getFloat(data);
		y = getFloat(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 20);
		encodeHeader(data);
		addLong(data, cellId);
		addFloat(data, x);
		addFloat(data, z);
		addFloat(data, y);
		return data;
	}
	
}
