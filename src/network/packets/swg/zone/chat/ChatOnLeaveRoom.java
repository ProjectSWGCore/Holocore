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

public class ChatOnLeaveRoom extends SWGPacket {
	
	public static final int CRC = 0x60B5098B;
	
	private ChatAvatar avatar;
	private int result;
	private int chatRoomId;
	private int sequence;

	public ChatOnLeaveRoom(ChatAvatar avatar, int errorCode, int chatRoomId, int sequence) {
		this.avatar = avatar;
		this.result = errorCode;
		this.chatRoomId = chatRoomId;
		this.sequence = sequence;
	}
	
	public ChatOnLeaveRoom(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		avatar = new ChatAvatar();
		avatar.decode(data);
		result = getInt(data);
		chatRoomId = getInt(data);
		sequence = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(18 + avatar.encode().length);
		addShort(data, 2);
		addInt(  data, CRC);
		data.put(avatar.encode());
		addInt(data, result);
		addInt  (data, chatRoomId);
		addInt  (data, sequence);
		return data;
	}
	
}
