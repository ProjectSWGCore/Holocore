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

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author Waverunner
 */
public class ChatOnGetFriendsList extends SWGPacket {
	public static final int CRC = 0xE97AB594;

	private List<String> friends;
	private String galaxy;
	private long objectId;

	public ChatOnGetFriendsList(long objectId, String galaxy, List<String> friends) {
		this.objectId = objectId;
		this.galaxy = galaxy;
		this.friends = friends;
	}

	@Override
	public ByteBuffer encode() {
		int additional = galaxy.length() + 9;
		int length = 0;
		for (String friend : friends) {
			length += friend.length() + additional;
		}

		ByteBuffer bb = ByteBuffer.allocate(18 + length);
		addShort(bb, 3);
		addInt(bb, CRC);
		addLong(bb, objectId);

		addInt(bb, friends.size());
		for (String friend : friends) {
			addAscii(bb, "SWG");
			addAscii(bb, galaxy);
			addAscii(bb, friend);
		}

		return bb;
	}
}
