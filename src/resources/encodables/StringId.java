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

import network.packets.Packet;
import resources.network.NetBufferStream;
import resources.persistable.Persistable;

import com.projectswg.common.debug.Log;

public class StringId implements OutOfBandData, Persistable {
	
	private String key = "";
	private String file = "";

	public StringId() {}

	public StringId(String file, String key) {
		this.file = file;
		this.key = key;
	}
	
	public StringId(String stf) {
		if (!stf.contains(":")) {
			Log.e("Invalid stf format! Expected a semi-colon for " + stf);
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
		ByteBuffer buffer = ByteBuffer.allocate(8 + key.length() + file.length());

		Packet.addAscii(buffer, file);
		Packet.addInt(buffer, 0);
		Packet.addAscii(buffer, key);
		
		return buffer.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		file 	= Packet.getAscii(data);
		Packet.getInt(data);
		key 	= Packet.getAscii(data);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addAscii(file);
		stream.addAscii(key);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		file = stream.getAscii();
		key = stream.getAscii();
	}

	@Override
	public OutOfBandPackage.Type getOobType() {
		return OutOfBandPackage.Type.STRING_ID;
	}

	@Override
	public int getOobPosition() {
		return -1;
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
