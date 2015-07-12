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
package resources.client_info.visitors;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import resources.client_info.ClientData;
import utilities.ByteUtilities;

public class CrcStringTableData extends ClientData {
	
	private ArrayList<Integer> crcList = new ArrayList<>();
	private ArrayList<Integer> startList = new ArrayList<>();
	private ArrayList<String> stringList = new ArrayList<>();
	private Map <Integer, String> crcMap = new HashMap<>();
	private int count;
	
	@Override
	public void parse(String node, ByteBuffer data, int size) {
		switch (node) {
		
		case "0000DATA":
			count = data.getInt();
			break;
			
		case "CRCT":
			for(int i=0; i < count; ++i) {
				crcList.add(data.getInt());
			}
			break;
			
		case "STRT":
			for(int i=0; i < count; ++i) {
				startList.add(data.getInt());
			}
			break;
			
		case "STNG":
			for(int i=0; i < count; ++i) {
				data.position(startList.get(i));
				String str = ByteUtilities.nextString(data);
				crcMap.put(crcList.get(i), str);
				stringList.add(str);
			}
			break;
		}
		
	}

	public boolean isValidCrc(int crc) {
		
		if(!crcList.contains(crc))
			return false;
		return true;
		
	}
	
	public String getTemplateString(int crc) {
		return crcMap.get(crc);
	}
	
	public int getCrcForString(String str) {
		if (stringList.contains(str)) {
			return crcList.get(stringList.indexOf(str));
		} else {
			return 0;
		}
	}
}
