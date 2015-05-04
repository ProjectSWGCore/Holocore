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


public class CreateCharacterFailure extends SWGPacket {
	
	public static final int CRC = 0xDF333C6E;
	private NameFailureReason reason;
	
	public CreateCharacterFailure() {
		reason = NameFailureReason.NAME_RETRY;
	}
	
	public CreateCharacterFailure(NameFailureReason reason) {
		this.reason = reason;
	}
	
	public void decode(ByteBuffer data) {
		
	}
	
	public ByteBuffer encode() {
		String errorString = nameFailureTranslation(reason);
		ByteBuffer data = ByteBuffer.allocate(20 + errorString.length());
		addShort(  data, 3);
		addInt(    data, CRC);
		addUnicode(data, "");
		addAscii(  data, "ui");
		addInt(    data, 0);
		addAscii(  data, errorString);
		return data;
	}
	
	private String nameFailureTranslation(NameFailureReason reason) {
		switch (reason) {
			case NAME_DECLINED_EMPTY:
				return "name_declined_empty";
			case NAME_IN_USE:
				return "name_declined_in_use";
			case NAME_RETRY:
				return "name_declined_retry";
			case NAME_SYNTAX:
				return "name_declined_syntax";
			case NAME_TOO_FAST:
				return "name_declined_too_fast";
		}
		return "name_declined_retry";
	}
	
	public enum NameFailureReason {
		NAME_DECLINED_EMPTY,
		NAME_TOO_FAST,
		NAME_RETRY,
		NAME_SYNTAX,
		NAME_IN_USE
	}
}
