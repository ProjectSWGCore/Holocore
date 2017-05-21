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
public class ChatOnUnbanAvatarFromRoom extends SWGPacket {
	public static final int CRC = getCrc("ChatOnUnbanAvatarFromRoom");

	private String room;
	private ChatAvatar moderator;
	private ChatAvatar target;
	private int result;
	private int sequence;

	public ChatOnUnbanAvatarFromRoom(String room, ChatAvatar moderator, ChatAvatar target, int result, int sequence) {
		this.room = room;
		this.moderator = moderator;
		this.target = target;
		this.result = result;
		this.sequence = sequence;
	}

	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		room		= data.getAscii();
		moderator	= data.getEncodable(ChatAvatar.class);
		target		= data.getEncodable(ChatAvatar.class);
		result		= data.getInt();
		sequence	= data.getInt();
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(16 + moderator.encode().length + target.encode().length + room.length());
		data.addShort(6);
		data.addInt(CRC);
		data.addAscii(room);
		data.addEncodable(moderator);
		data.addEncodable(target);
		data.addInt(result);
		data.addInt(sequence);
		return data;
	}
}
