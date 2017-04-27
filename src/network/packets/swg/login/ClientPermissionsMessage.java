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

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;


public class ClientPermissionsMessage extends SWGPacket {
	public static final int CRC = getCrc("ClientPermissionsMessage");
	
	private boolean canLogin;
	private boolean canCreateRegularCharacter;
	private boolean canCreateJediCharacter;
	private boolean canSkipTutorial;
	
	public ClientPermissionsMessage() {
		canLogin = true;
		canCreateRegularCharacter = true;
		canCreateJediCharacter = true;
		canSkipTutorial = true;
	}
	
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		canLogin = data.getBoolean();
		canCreateRegularCharacter = data.getBoolean();
		canCreateJediCharacter = data.getBoolean();
		canSkipTutorial = data.getBoolean();
	}
	
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(10);
		data.addShort(5);
		data.addInt(CRC);
		data.addBoolean(canLogin);
		data.addBoolean(canCreateRegularCharacter);
		data.addBoolean(canCreateJediCharacter);
		data.addBoolean(canSkipTutorial);
		return data;
	}
	
	public void setCanLogin(boolean can) { this.canLogin = can; }
	public void setCanCreateRegularCharacter(boolean can) { this.canCreateRegularCharacter = can; }
	public void setCanCreateJediCharacter(boolean can) { this.canCreateJediCharacter = can; }
	public void setCanSkipTutorial(boolean can) { this.canSkipTutorial = can; }
	
	public boolean canLogin() { return canLogin; }
	public boolean canCreateRegularCharacter() { return canCreateRegularCharacter; }
	public boolean canCreateJediCharacter() { return canCreateJediCharacter; }
	public boolean canSkipTutorial() { return canSkipTutorial; }
}
