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


public class LoginClientId extends SWGPacket {
	public static final int CRC = getCrc("LoginClientId");
	
	private String username;
	private String password;
	private String version;
	
	public LoginClientId() {
		this("", "", "");
	}
	
	public LoginClientId(ByteBuffer data) {
		decode(data);
	}
	
	public LoginClientId(String username, String password, String version) {
		this.username = username;
		this.password = password;
		this.version  = version;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		username  = getAscii(data);
		password  = getAscii(data);
		version   = getAscii(data);
	}
	
	public ByteBuffer encode() {
		int length = 6 + 6 + username.length() * 2 + password.length() * 2 + version.length() * 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 4);
		addInt(  data, CRC);
		addAscii(data, username);
		addAscii(data, password);
		addAscii(data, version);
		return data;
	}
	
	public String getUsername()  { return username; }
	public void setUsername(String str) { this.username = str; }
	public String getPassword()  { return password; }
	public void setPassword(String str) { this.password = str; }
	public String getVersion()  { return version; }
}
