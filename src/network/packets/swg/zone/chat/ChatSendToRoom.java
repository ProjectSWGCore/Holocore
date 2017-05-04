/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package network.packets.swg.zone.chat;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;
import resources.encodables.OutOfBandPackage;

public class ChatSendToRoom extends SWGPacket {
	public static final int CRC = getCrc("ChatSendToRoom");

	private String message;
	private OutOfBandPackage outOfBandPackage;
	private int roomId;
	private int sequence; // This seems to vary from room to room, client keeps track individually for each room?

	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;

		message 			= data.getUnicode();
		outOfBandPackage 	= data.getEncodable(OutOfBandPackage.class);
		roomId 				= data.getInt();
		sequence 			= data.getInt();
	}

	@Override
	public NetBuffer encode() {
		byte [] oobRaw = outOfBandPackage.encode();
		NetBuffer data = NetBuffer.allocate(12 + message.length()*2 + oobRaw.length);
		data.addUnicode(message);
		data.addRawArray(oobRaw);
		data.addInt(roomId);
		data.addInt(sequence);
		return data;
	}
	
	public String getMessage() {
		return message;
	}

	public OutOfBandPackage getOutOfBandPackage() {
		return outOfBandPackage;
	}

	public int getRoomId() {
		return roomId;
	}

	public int getSequence() {
		return sequence;
	}
}
