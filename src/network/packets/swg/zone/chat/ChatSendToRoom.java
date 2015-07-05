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

import network.packets.swg.SWGPacket;
import resources.encodables.OutOfBandPackage;

import java.nio.ByteBuffer;

/**
 * @author Waverunner
 */
public class ChatSendToRoom extends SWGPacket {
	public static final int CRC = resources.common.CRC.getCrc("ChatSendToRoom");

	private String message;
	private OutOfBandPackage outOfBandPackage;
	private int roomId;
	private int sequence; // This seems to vary from room to room, client keeps track individually for each room?

	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;

		message 			= getUnicode(data);
		outOfBandPackage 	= getEncodable(data, OutOfBandPackage.class);
		roomId 				= getInt(data);
		sequence 			= getInt(data);
	}

	@Override
	public ByteBuffer encode() {
		return super.encode();
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
