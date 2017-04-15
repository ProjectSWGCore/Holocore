/************************************************************************************
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
package network.packets.swg.holo;

import java.nio.ByteBuffer;

public class HoloConnectionStopped extends HoloPacket {
	
	public static final int CRC = getCrc("HoloConnectionStopped");
	
	private ConnectionStoppedReason reason;
	
	public HoloConnectionStopped() {
		this(ConnectionStoppedReason.UNKNOWN);
	}
	
	public HoloConnectionStopped(ConnectionStoppedReason reason) {
		this.reason = reason;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		try {
			reason = ConnectionStoppedReason.valueOf(getAscii(data));
		} catch (IllegalArgumentException e) {
			reason = ConnectionStoppedReason.UNKNOWN;
		}
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8+reason.name().length());
		addShort(data, 1);
		addInt(data, CRC);
		addAscii(data, reason.name());
		return data;
	}
	
	public void setReason(ConnectionStoppedReason reason) {
		this.reason = reason;
	}
	
	public ConnectionStoppedReason getReason() {
		return reason;
	}
	
	public static enum ConnectionStoppedReason {
		APPLICATION,
		INVALID_PROTOCOL,
		OTHER_SIDE_TERMINATED,
		NETWORK,
		SERVER_ERROR,
		UNKNOWN
	}
	
}
