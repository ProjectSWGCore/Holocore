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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.projectswg.common.data.EnumLookup;
import com.projectswg.common.debug.Log;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

import resources.objects.SpecificObject;
import resources.objects.waypoint.WaypointObject;
import services.objects.ObjectCreator;

public class OutOfBandPackage implements Encodable, Persistable {
	
	private final List<OutOfBandData> packages;
	
	public OutOfBandPackage() {
		packages = new ArrayList<>();
	}
	
	public OutOfBandPackage(OutOfBandData ... outOfBandData) {
		this();
		Collections.addAll(packages, outOfBandData);
	}
	
	public List<OutOfBandData> getPackages() {
		return packages;
	}
	
	@Override
	public byte[] encode() {
		if (packages.isEmpty())
			return new byte[4];
		
		int length = getLength();
		NetBuffer data = NetBuffer.allocate(length);
		data.addInt((length-4) / 2); // Client treats this like a unicode string, so it's half the actual size of the array
		for (OutOfBandData oob : packages) {
			data.addRawArray(packOutOfBandData(oob));
		}
		return data.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		int remaining = data.getInt() * 2;
		while (remaining > 0) {
			int start = data.position();
			int padding = data.getShort();
			Type type = Type.getTypeForByte(data.getByte());
			unpackOutOfBandData(data, type);
			data.position(data.position() + padding);
			remaining -= data.position() - start;
		}
	}
	
	@Override
	public int getLength() {
		int size = 4;
		for (OutOfBandData oob : packages) {
			size += getOOBLength(oob);
		}
		return size;
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addList(packages, (p) -> OutOfBandFactory.save(p, stream));
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getList((i) -> packages.add(OutOfBandFactory.create(stream)));
	}
	
	private void unpackOutOfBandData(NetBuffer data, Type type) {
		// Position doesn't seem to be reflective of it's spot in the package list, not sure if this can be automated
		// as the client will send -3 for multiple waypoints in a mail, so could be static for each OutOfBandData
		// If that's the case, then we can move the position variable to the Type enum instead of a method return statement
		data.getInt(); // position
		OutOfBandData oob;
		switch (type) {
			case PROSE_PACKAGE:
				oob = data.getEncodable(ProsePackage.class);
				break;
			case WAYPOINT:
				oob = (WaypointObject) ObjectCreator.createObjectFromTemplate(SpecificObject.SO_WORLD_WAYPOINT.getTemplate());
				oob.decode(data);
				break;
			case STRING_ID:
				oob = data.getEncodable(StringId.class);
				break;
			default:
				Log.e("Tried to decode an unsupported OutOfBandData Type: " + type);
				return;
		}
		packages.add(oob);
	}
	
	private byte[] packOutOfBandData(OutOfBandData oob) {
		// Type and position is included in the padding size
		int paddingSize = getOOBPaddingSize(oob);
		NetBuffer data = NetBuffer.allocate(getOOBLength(oob));
		data.addShort(paddingSize); // Number of bytes for decoding to skip over when reading
		data.addByte(oob.getOobType().getType());
		data.addInt(oob.getOobPosition());
		data.addEncodable(oob);
		for (int i = 0; i < paddingSize; i++) {
			data.addByte(0);
		}
		
		return data.array();
	}
	
	private int getOOBLength(OutOfBandData oob) {
		return 7 + oob.getLength() + getOOBPaddingSize(oob);
	}
	
	private int getOOBPaddingSize(OutOfBandData oob) {
		return (oob.getLength() + 5) % 2;
	}
	
	@Override
	public String toString() {
		return "OutOfBandPackage[packages=" + packages + "]";
	}
	
	public enum Type {
		UNDEFINED			(Byte.MIN_VALUE),
		OBJECT				(0),
		PROSE_PACKAGE		(1),
		AUCTION_TOKEN		(2),
		OBJECT_ATTRIBUTES	(3),
		WAYPOINT			(4),
		STRING_ID			(5),
		STRING				(6),
		NUM_TYPES			(7);
		
		private static final EnumLookup<Byte, Type> LOOKUP = new EnumLookup<>(Type.class, t -> t.getType());
		
		byte type;
		
		Type(int type) {
			this.type = (byte) type;
		}
		
		public byte getType() {
			return type;
		}
		
		public static Type getTypeForByte(byte typeByte) {
			return LOOKUP.getEnum(typeByte, UNDEFINED);
		}
	}
}
