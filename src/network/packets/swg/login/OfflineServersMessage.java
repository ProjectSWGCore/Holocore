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
import java.util.ArrayList;
import java.util.List;

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
	
	public OfflineServersMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int listCount = getInt(data);
		offlineServers = new ArrayList<String>(listCount);
		for (int i = 0 ; i < listCount; i++)
			offlineServers.add(getAscii(data));
	}
	
	public ByteBuffer encode() {
		int strLength = 0;
		for (String str : offlineServers)
			strLength += 2 + str.length();
		ByteBuffer data = ByteBuffer.allocate(10 + strLength);
		addShort(data, 2);
		addInt(  data, CRC);
		for (String str : offlineServers)
			addAscii(data, str);
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
