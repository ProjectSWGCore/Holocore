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

import com.projectswg.common.network.NetBuffer;

import resources.Posture;

public class PostureUpdate extends ObjectController {
	
	public static final int CRC = 0x0131;
	
	private Posture posture;
	
	public PostureUpdate(long objectId, Posture posture) {
		super(objectId, CRC);
		this.posture = posture;
	}
	
	public PostureUpdate(NetBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public void decode(NetBuffer data) {
		decodeHeader(data);
		posture = Posture.getFromId(data.getByte());
		data.getBoolean(); // isClientImmediate
	}
	
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 2);
		encodeHeader(data);
		data.addByte(posture.getId());
		data.addBoolean(true); // isClientImmediate
		return data;
	}
	
	public Posture getPosture() { return posture; }
	
	public void setPosture(Posture posture) { this.posture = posture; }
	
}
