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

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class OfflineServersMessage extends SWGPacket {

	public static final int CRC = 0xF41A5265;
	
	private List <String> offlineServers;
	
	public OfflineServersMessage() {
		offlineServers = new ArrayList<String>();
	}
	
	public OfflineServersMessage(List <String> offline) {
		this.offlineServers = offline;
	}
	
	public OfflineServersMessage(NetBuffer data) {
		decode(data);
	}
	
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		int listCount = data.getInt();
		offlineServers = new ArrayList<String>(listCount);
		for (int i = 0 ; i < listCount; i++)
			offlineServers.add(data.getAscii());
	}
	
	public NetBuffer encode() {
		int strLength = 0;
		for (String str : offlineServers)
			strLength += 2 + str.length();
		NetBuffer data = NetBuffer.allocate(10 + strLength);
		data.addShort(2);
		data.addInt(CRC);
		data.addInt(offlineServers.size());
		for (String str : offlineServers)
			data.addAscii(str);
		return data;
	}
	
	public List <String> getOfflineServers() {
		return offlineServers;
	}
	
	public void setOfflineServers(List <String> offline) {
		offlineServers = offline;
	}
	
	public void addOflineServer(String offline) {
		offlineServers.add(offline);
	}
	
}
