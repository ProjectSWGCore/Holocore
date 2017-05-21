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

import com.projectswg.common.network.NetBuffer;

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
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		avatar				= data.getEncodable(ChatAvatar.class);
		roomId 				= data.getInt();
		message 			= data.getUnicode();
		outOfBandPackage	= data.getEncodable(OutOfBandPackage.class);
	}
	
	@Override
	public NetBuffer encode() {
		byte[] oob = outOfBandPackage.encode();
		int length = 14 + avatar.getLength() + oob.length + message.length() * 2;
		NetBuffer data = NetBuffer.allocate(length);
		data.addShort(5);
		data.addInt(CRC);
		data.addEncodable(avatar);
		data.addInt(roomId);
		data.addUnicode(message);
		data.addRawArray(oob);
		return data;
	}
}
