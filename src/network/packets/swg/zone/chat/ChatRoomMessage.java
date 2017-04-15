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
package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;
import resources.chat.ChatAvatar;
import resources.encodables.OutOfBandPackage;

public class ChatRoomMessage extends SWGPacket {
	public static final int CRC = getCrc("ChatRoomMessage");

	private ChatAvatar avatar;
	private int roomId = 0;
	private String message = "";
	private OutOfBandPackage outOfBandPackage;

	public ChatRoomMessage(ChatAvatar avatar, int roomId, String message, OutOfBandPackage oob) {
		this.avatar = avatar;
		this.roomId = roomId;
		this.message = message;
		this.outOfBandPackage = oob;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		avatar				= getEncodable(data, ChatAvatar.class);
		roomId 				= getInt(data);
		message 			= getUnicode(data);
		outOfBandPackage	= getEncodable(data, OutOfBandPackage.class);
	}
	
	public ByteBuffer encode() {
		byte[] oob = outOfBandPackage.encode();
		int length = 14 + avatar.encode().length + oob.length + message.length() * 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 5);
		addInt(data, CRC);
		addEncodable(data, avatar);
		addInt(data, roomId);
		addUnicode(data, message);
		addData(data, oob);
		return data;
	}
}
