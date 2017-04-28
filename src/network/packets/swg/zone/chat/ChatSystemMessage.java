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
import resources.encodables.OutOfBandPackage;

public class ChatSystemMessage extends SWGPacket {
	
	public static final int CRC = getCrc("ChatSystemMessage");
	
	private OutOfBandPackage oob;
	private String message;
	private int type;
	
	public ChatSystemMessage() {
		this(0, "");
	}
	
	public ChatSystemMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	public ChatSystemMessage(int type, OutOfBandPackage oob) {
		this.type = type;
		this.oob = oob;
	}
	
	public ChatSystemMessage(SystemChatType type, String message) {
		this(type.getType(), message);
	}
	
	public ChatSystemMessage(SystemChatType type, OutOfBandPackage oob) {
		this(type.getType(), oob);
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		type = data.getByte();
		message = data.getUnicode();
		data.getUnicode();
	}
	
	@Override
	public NetBuffer encode() {
		boolean oobExists = (oob != null);
		int length = 7;
		
		if (oobExists)
			length += 4 + oob.getLength();
		else
			length += 8 + message.length() * 2;
		
		NetBuffer data = NetBuffer.allocate(length);
		data.addShort(4);
		data.addInt(CRC);
		data.addByte(type);
		if (oobExists) {
			data.addInt(0);
			data.addEncodable(oob);
		} else {
			data.addUnicode(message);
			data.addInt(0);
		}
		
		return data;
	}
	
	public SystemChatType getType() {
		switch (type) {
			case 0:
			default:
				return SystemChatType.SCREEN_AND_CHAT;
			case 1:
				return SystemChatType.SCREEN;
			case 2:
				return SystemChatType.CHAT;
		}
	}
	
	public String getMessage() {
		return message;
	}
	
	public enum SystemChatType {
		SCREEN_AND_CHAT	(0x00),
		SCREEN			(0x01),
		CHAT			(0x02);
		
		int type;
		
		SystemChatType(int type) {
			this.type = type;
		}
		
		public int getType() {
			return type;
		}
	}
	
}
