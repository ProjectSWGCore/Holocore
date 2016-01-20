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

import java.util.HashMap;
import java.util.Map;

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class CrcStringTableData extends ClientData {
	
	private final Map<Integer, String> crcMap = new HashMap<>();
	private final Map<String, Integer> reverseCrcMap = new HashMap<>();
	
	@Override
	public void readIff(SWGFile iff) {
		iff.enterNextForm(); // Version
		int [] crcList = null;
		int count = 0;
		
		IffNode chunk;
		while ((chunk = iff.enterNextChunk()) != null) {
			switch (chunk.getTag()) {
				case "DATA":
					count = chunk.readInt();
					crcList = new int[count];
					break;
				case "CRCT":
					for (int i = 0; i < count; ++i) {
						crcList[i] = chunk.readUInt();
					}
					break;
				case "STRT":
					chunk.skip(count * 4); // Start List -- not needed
					break;
				case "STNG":
					for (int i = 0; i < count; ++i) {
						String str = chunk.readString();
						crcMap.put(crcList[i], str);
						reverseCrcMap.put(str, crcList[i]);
					}
					break;
				default:
					break;
			}
		}
	}
	
	public boolean isValidCrc(int crc) {
		return reverseCrcMap.containsValue(crc);
	}
	
	public String getTemplateString(int crc) {
		return crcMap.get(crc);
	}
	
	public int getCrcForString(String str) {
		Integer crc = reverseCrcMap.get(str);
		return crc == null ? 0 : crc.intValue();
	}
}
