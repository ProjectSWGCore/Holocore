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

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

/**
 * @author Waverunner
 */
public class ChatCreateRoom extends SWGPacket {
	public static final int CRC = getCrc("ChatCreateRoom");

	private boolean isPublic;
	private boolean isModerated;
	private String owner;
	private String roomName;
	private String roomTitle;
	private int sequence;

	public ChatCreateRoom() {}

	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		isPublic 	= getBoolean(data);
		isModerated	= getBoolean(data);
		owner		= getAscii(data);
		roomName	= getAscii(data);
		roomTitle	= getAscii(data);
		sequence	= getInt(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer bb = ByteBuffer.allocate(18 + owner.length() + roomName.length() + roomTitle.length());
		addShort(bb, 7);
		addInt(bb, CRC);
		addBoolean(bb, isPublic);
		addBoolean(bb, isModerated);
		addAscii(bb, owner);
		addAscii(bb, roomName);
		addAscii(bb, roomTitle);
		addInt(bb, sequence);
		return bb;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public boolean isModerated() {
		return isModerated;
	}

	public String getOwner() {
		return owner;
	}

	public String getRoomName() {
		return roomName;
	}

	public String getRoomTitle() {
		return roomTitle;
	}

	public int getSequence() {
		return sequence;
	}

	@Override
	public String toString() {
		return "ChatCreateRoom[isPublic=" + isPublic + ", isModerated=" + isModerated +
				", owner='" + owner + "'," + "roomName='" + roomName + "'," + "roomTitle='" + roomTitle + '\'' +
				", sequence=" + sequence + "]";
	}
}
