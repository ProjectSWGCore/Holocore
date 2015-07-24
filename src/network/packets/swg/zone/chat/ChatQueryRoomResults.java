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
import resources.chat.ChatRoom;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author Waverunner
 */
public class ChatQueryRoomResults extends SWGPacket {
	public static final int CRC = getCrc("ChatQueryRoomResults");

	private ChatRoom room;
	private int sequence;

	public ChatQueryRoomResults(ChatRoom room, int sequence) {
		this.room = room;
		this.sequence = sequence;
	}

	@Override
	public void decode(ByteBuffer data) {
		super.decode(data, CRC);
	}

	@Override
	public ByteBuffer encode() {
		byte[] data = room.encode();
		int size = data.length;

		List<ChatAvatar> members = room.getMembers();
		List<ChatAvatar> invited = room.getInvited();
		List<ChatAvatar> moderators = room.getModerators();
		List<ChatAvatar> banned = room.getBanned();

		for (ChatAvatar avatar : members) { size += avatar.getSize(); }
		for (ChatAvatar avatar : invited) { size += avatar.getSize(); }
		for (ChatAvatar avatar : moderators) { size += avatar.getSize(); }
		for (ChatAvatar avatar : banned) { size += avatar.getSize(); }

		ByteBuffer bb = ByteBuffer.allocate(20 + size);
		addList(bb, members);
		addList(bb, invited);
		addList(bb, moderators);
		addList(bb, banned);
		addInt(bb, sequence);
		addData(bb, data);

		return bb;
	}
}
