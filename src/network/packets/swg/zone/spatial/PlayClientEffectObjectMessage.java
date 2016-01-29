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
package network.packets.swg.zone.spatial;

import java.nio.ByteBuffer;
import static network.packets.Packet.addAscii;
import static network.packets.Packet.addInt;
import static network.packets.Packet.addLong;
import static network.packets.Packet.addShort;
import static network.packets.Packet.getAscii;
import static network.packets.Packet.getLong;
import network.packets.swg.SWGPacket;

/**
 *
 * @author Mads
 */
public class PlayClientEffectObjectMessage extends SWGPacket {
	
	public static final int CRC = getCrc("PlayClientEffectObjectMessage");
	
	private String effectFileName;
	private long objectId;
	private String hardpoint;
	private String label;
	
	public PlayClientEffectObjectMessage(String effectFile, long objectId, String hardpoint, String label) {
		this.effectFileName = effectFile;
		this.objectId = objectId;
		this.hardpoint = hardpoint;
		this.label = label;
	}

	public String getEffectFileName() {
		return effectFileName;
	}

	public long getObjectId() {
		return objectId;
	}

	public String getHardpoint() {
		return hardpoint;
	}

	public String getLabel() {
		return label;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC)) {
			return;
		}
		
		effectFileName = getAscii(data);
		hardpoint = getAscii(data);
		objectId = getLong(data);
		label = getAscii(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(Short.BYTES * 4 + Integer.BYTES + Long.BYTES + effectFileName.length() + hardpoint.length() + label.length());
		addShort(data, 5);
		addInt(data, CRC);
		addAscii(data, effectFileName);
		addAscii(data, hardpoint);	// TODO unknown - some sort of extra parameter string
		addLong(data, objectId);
		addAscii(data, label);
		return data;
	}
}
