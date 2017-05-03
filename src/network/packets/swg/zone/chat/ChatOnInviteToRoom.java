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
import resources.chat.ChatAvatar;

/**
 * @author Waverunner
 */
public class ChatOnInviteToRoom extends SWGPacket {
	public static final int CRC = getCrc("ChatOnInviteToRoom");

	private String room;
	private ChatAvatar sender;
	private ChatAvatar invited;
	private int result;

	public ChatOnInviteToRoom() {}

	public ChatOnInviteToRoom(String room, ChatAvatar sender, ChatAvatar invited, int result) {
		this.room = room;
		this.sender = sender;
		this.invited = invited;
		this.result = result;
	}

	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		room	= data.getAscii();
		sender 	= data.getEncodable(ChatAvatar.class);
		invited = data.getEncodable(ChatAvatar.class);
		result	= data.getInt();
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(12 + sender.getLength() + invited.getLength() + room.length());
		data.addShort(5);
		data.addInt(CRC);
		data.addAscii(room);
		data.addEncodable(sender);
		data.addEncodable(invited);
		data.addInt(result);
		return data;
	}
}
