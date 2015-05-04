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
package network.packets.swg;

import java.nio.ByteBuffer;


public class ErrorMessage extends SWGPacket {
	public static final int CRC = 0xB5ABF91A;
	private String type;
	private String message;
	private boolean fatal;
	
	public ErrorMessage() {
		
	}
	
	public ErrorMessage(String type, String message, boolean fatal) {
		this.type = type;
		this.message = message;
		this.fatal = fatal;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		type = getAscii(data);
		message = getAscii(data);
		fatal = getBoolean(data);
	}
	
	public ByteBuffer encode() {
		int length = 11 + type.length() + message.length();
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 3);
		addInt(  data, CRC);
		addAscii(data, type);
		addAscii(data, message);
		addBoolean(data, fatal);
		return data;
	}
}
