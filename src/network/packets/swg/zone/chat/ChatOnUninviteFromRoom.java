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
import resources.chat.ChatAvatar;

import java.nio.ByteBuffer;

/**
 * @author Waverunner
 */
public class ChatOnUninviteFromRoom extends SWGPacket {
	public static final int CRC = getCrc("ChatOnUninviteFromRoom");

	private String room;
	private ChatAvatar sender;
	private ChatAvatar invitee;
	private int result;
	private int sequence;

	public ChatOnUninviteFromRoom() {}

	public ChatOnUninviteFromRoom(String room, ChatAvatar sender, ChatAvatar invitee, int result, int sequence) {
		this.room = room;
		this.sender = sender;
		this.invitee = invitee;
		this.result = result;
		this.sequence = sequence;
	}

	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;

		room		= getAscii(data);
		sender		= getEncodable(data, ChatAvatar.class);
		invitee 	= getEncodable(data, ChatAvatar.class);
		result		= getInt(data);
		sequence	= getInt(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer bb = ByteBuffer.allocate(16 + room.length() + sender.encode().length + invitee.encode().length);
		addShort(bb, 6);
		addInt(bb, CRC);
		addAscii(bb, room);
		addEncodable(bb, sender);
		addEncodable(bb, invitee);
		addInt(bb, result);
		addInt(bb, sequence);
		return bb;
	}
}
