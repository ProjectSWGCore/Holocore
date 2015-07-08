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

import resources.encodables.OutOfBandPackage;
import network.packets.swg.SWGPacket;

public class ChatSystemMessage extends SWGPacket {
	public static final int CRC = getCrc("ChatSystemMessage");

	private int type = 0;
	private String message = "";
	private OutOfBandPackage oob;
	
	public ChatSystemMessage() {
		
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
		this(type.ordinal(), message);
	}
	
	public ChatSystemMessage(SystemChatType type, OutOfBandPackage oob) {
		this(type.ordinal(), oob);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		type = getByte(data);
		message = getUnicode(data);
		getUnicode(data);
	}
	
	public ByteBuffer encode() {
		byte[] oobData = (oob != null ? oob.encode() : null);
		int length = 7;
		
		if (oobData == null) length+= 15 + message.length() * 2;
		else length+=  4 + oobData.length;
		
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(  data, 4);
		addInt(    data, CRC);
		addByte(   data, type);
		if (oobData == null) {
			addUnicode(data, message);
			addUnicode(data, "");
		} else {
			addInt(data, 0);
			addArray(data, oobData);
		}
		
		return data;
	}
	
	public SystemChatType getType() {
		for (SystemChatType t : SystemChatType.values()) {
			if (type == t.ordinal()) return t;
		}
		return SystemChatType.SCREEN_AND_CHAT;
	}
	public String getMessage() { return message; }
	
	public enum SystemChatType {
		SCREEN_AND_CHAT,
		SCREEN,
		CHAT
	}
}
