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
import network.packets.swg.zone.insertion.ChatRoomList.ChatRoom.User;

public class ChatRoomMessage extends SWGPacket {
	
	public static final int CRC = 0xCD4CE444;
	private User user = new User();
	private int roomId = 0;
	private String message = "";
	
	public ChatRoomMessage() {
		
	}
	
	public ChatRoomMessage(String game, String server, String name, int roomId, String message) {
		this.user.game = game;
		this.user.server = server;
		this.user.name = name;
		this.roomId = roomId;
		this.message = message;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		user.game = getAscii(data);
		user.server = getAscii(data);
		user.name = getAscii(data);
		roomId = getInt(data);
		message = getUnicode(data);
		getUnicode(data);
	}
	
	public ByteBuffer encode() {
		int length = 24 + user.game.length() + user.server.length() + user.name.length() + message.length() * 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(  data, 5);
		addInt(    data, CRC);
		addAscii(  data, user.game);
		addAscii(  data, user.server);
		addAscii(  data, user.name);
		addInt(    data, roomId);
		addUnicode(data, message);
		addUnicode(data, "");
		return data;
	}
}
