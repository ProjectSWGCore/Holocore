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
package network.packets.swg.login;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class LoginClientToken extends SWGPacket {
	
	public static final int CRC = 0xAAB296C6;
	private byte [] sessionKey;
	private int userId;
	private String username;
	
	public LoginClientToken() {
		
	}
	
	public LoginClientToken(ByteBuffer data) {
		decode(data);
	}
	
	public LoginClientToken(byte [] sessionKey, int userId, String username) {
		this.sessionKey = sessionKey;
		this.userId = userId;
		this.username = username;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int sessionKeyLength = getInt(data);
		if (sessionKeyLength > data.remaining())
			return;
		sessionKey = new byte[sessionKeyLength];
		data.get(sessionKey);
		userId = getInt(data);
		username = getAscii(data);
	}
	
	public ByteBuffer encode() {
		int length = 16 + sessionKey.length + username.length();
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 4);
		addInt(  data, CRC);
		addInt(  data, sessionKey.length);
		data.put(sessionKey);
		addInt(  data, userId);
		addAscii(data, username);
		return data;
	}
	
	public byte [] getSessionKey() {
		return sessionKey;
	}
	
	public int getUserId() {
		return userId;
	}
	
	public String getUsername() {
		return username;
	}
	
}
