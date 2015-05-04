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
package resources.encodables;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import resources.network.BaselineBuilder.Encodable;

public class Stf implements Encodable {
	private static final long serialVersionUID = 1L;
	
	private String key = "";
	private String file = "";
	
	public Stf(String file, String key) {
		this.file = file;
		this.key = key;
	}
	
	public Stf(String stf) {
		if (!stf.contains(":")) {
			System.err.println("Stf: Invalid stf format! Expected a semi-colon for " + stf);
			return;
		}
		
		if (stf.startsWith("@")) stf = stf.replaceFirst("@", "");
		
		String[] split = stf.split(":");
		file = split[0];
		
		if (split.length == 2)
			key = split[1];
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer = ByteBuffer.allocate(8 + key.length() + file.length()).order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putShort((short) file.length());
		buffer.put(file.getBytes(Charset.forName("UTF-8")));
		buffer.putInt(0);
		buffer.putShort((short) key.length());
		buffer.put(key.getBytes(Charset.forName("UTF-8")));
		
		return buffer.array();
	}

	public String getKey() { return key; }
	public void setKey(String key) { this.key = key; }

	public String getFile() { return file; }
	public void setFile(String file) { this.file = file; }

	@Override
	public String toString() {
		return "@" + file + ":" + key;
	}
}
