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

import java.nio.ByteBuffer;
import java.util.EnumSet;

import network.packets.swg.SWGPacket;
import resources.PvpFaction;
import resources.PvpFlag;

public class UpdatePvpStatusMessage extends SWGPacket {
	public static final int CRC = getCrc("UpdatePvpStatusMessage");

	private PvpFlag[] pvpFlags;
	private PvpFaction pvpFaction;
	private long objId;
	
	public UpdatePvpStatusMessage() {
		pvpFlags = new PvpFlag[]{PvpFlag.PLAYER};
		pvpFaction = PvpFaction.NEUTRAL;
		objId = 0;
	}
	
	public UpdatePvpStatusMessage(PvpFaction pvpFaction, long objId, PvpFlag... pvpFlags) {
		this.pvpFlags = pvpFlags;
		this.pvpFaction = pvpFaction;
		this.objId = objId;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		EnumSet<PvpFlag> enumFlags = PvpFlag.getFlags(getInt(data));
		
		pvpFlags = enumFlags.toArray(new PvpFlag[enumFlags.size()]);
		pvpFaction = PvpFaction.getFactionForCrc(getInt(data));
		objId = getLong(data);
	}
	
	public ByteBuffer encode() {
		int length = 22;
		int flagBitmask = 0;
		ByteBuffer data = ByteBuffer.allocate(length);
		
		for(PvpFlag pvpFlag : pvpFlags)
			flagBitmask |= pvpFlag.getBitmask();
		
		addShort(data, 4);
		addInt(  data, CRC);
		addInt(  data, flagBitmask);
		addInt(  data, pvpFaction.getCrc());
		addLong( data, objId);
		return data;
	}
	
	public long getObjectId() { return objId; }
	public PvpFaction getPlayerFaction() { return pvpFaction; }
	public PvpFlag[] getPvpFlags() { return pvpFlags; }
	
}
