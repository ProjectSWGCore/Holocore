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
package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ClientVerifyAndLockNameResponse extends SWGPacket {
	
	public static final int CRC = 0x9B2C6BA7;
	
	private String name = "";
	private ErrorMessage error = ErrorMessage.NAME_APPROVED;
	
	public ClientVerifyAndLockNameResponse() {
		
	}
	
	public ClientVerifyAndLockNameResponse(String name, ErrorMessage error) {
		this.name = name;
		this.error = error;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		name = getUnicode(data);
		error = ErrorMessage.valueOf(getAscii(data).toUpperCase());
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(20 + error.name().length() + name.length() * 2);
		addShort(  data, 9);
		addInt(    data, CRC);
		addUnicode(data, name);
		addAscii(  data, "ui");
		addInt(    data, 0);
		addAscii(  data, error.name().toLowerCase());
		return data;
	}
	
	public enum ErrorMessage {
		NAME_APPROVED,
		NAME_APPROVED_MODIFIED,
		NAME_DECLINED_SYNTAX,
		NAME_DECLINED_EMPTY,
		NAME_DECLINED_RACIALLY_INAPPROPRIATE,
		NAME_DECLINED_FICTIONALLY_INAPPROPRIATE,
		NAME_DECLINED_PROFANE,
		NAME_DECLINED_IN_USE,
		NAME_DECLINED_RESERVED,
		NAME_DECLINED_NO_TEMPLATE,
		NAME_DECLINED_NOT_CREATURE_TEMPLATE,
		NAME_DECLINED_NO_NAME_GENERATOR,
		NAME_DECLINED_CANT_CREATE_AVATAR,
		NAME_DECLINED_INTERNAL_ERROR,
		NAME_DECLINED_RETRY,
		NAME_DECLINED_TOO_FAST,
		NAME_DECLINED_NOT_AUTHORIZED_FOR_SPECIES,
		NAME_DECLINED_FICTIONALLY_RESERVED;
	}
	
}
