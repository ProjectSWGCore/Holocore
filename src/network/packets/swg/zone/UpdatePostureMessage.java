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
package network.packets.swg.zone;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;
import resources.Posture;

public class UpdatePostureMessage extends SWGPacket {
	public static final int CRC = getCrc("UpdatePostureMessage");

	private int posture = 0;
	private long objId = 0;
	
	public UpdatePostureMessage() {
		
	}
	
	public UpdatePostureMessage(Posture posture, long objId) {
		this.posture = posture.getId();
		this.objId = objId;
	}
	
	public UpdatePostureMessage(int posture, long objId) {
		this.posture = posture;
		this.objId = objId;
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		posture = data.getByte();
		objId = data.getLong();
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(15);
		data.addShort(3);
		data.addInt(CRC);
		data.addByte(posture);
		data.addLong(objId);
		return data;
	}
	
	public int getPosture() {
		return posture;
	}
	
	public long getObjectId() {
		return objId;
	}
	
	public void setPosture(int posture) {
		this.posture = posture;
	}
	
	public void setObjId(long objId) {
		this.objId = objId;
	}
}
