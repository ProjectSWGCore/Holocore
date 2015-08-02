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

import network.packets.swg.SWGPacket;

import java.nio.ByteBuffer;

public class UpdatePvpStatusMessage extends SWGPacket {
	public static final int CRC = getCrc("UpdatePvpStatusMessage");
	
	public static final int ATTACKABLE = 1;
	public static final int AGGRESSIVE = 2;
	public static final int OVERT = 4;
	public static final int TEF = 8;
	public static final int PLAYER = 16;
	public static final int ENEMY = 32;
	public static final int	GOING_OVERT = 64; // purple/blue blink
	public static final int GOING_COVERT = 128; // green blink
	public static final int DUEL = 256;

	private int flag = 16;
	private int playerFaction = 0;
	private long objId = 0;
	
	public UpdatePvpStatusMessage() {
		
	}
	
	public UpdatePvpStatusMessage(int playerType, int flag, long objId) {
		this.flag = playerType;
		this.playerFaction = flag;
		this.objId = objId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		flag = getInt(data);
		playerFaction = getInt(data);
		objId = getLong(data);
	}
	
	public ByteBuffer encode() {
		int length = 22;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 4);
		addInt(  data, CRC);
		addInt(  data, flag);
		addInt(  data, playerFaction);
		addLong( data, objId);
		return data;
	}
	
	public long getObjectId() { return objId; }
	public int getPlayerFaction() { return playerFaction; }
	public int getPlayerType() { return flag; }
	
}
