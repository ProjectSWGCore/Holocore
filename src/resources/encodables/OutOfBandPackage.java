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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import resources.network.BaselineBuilder.Encodable;
import resources.objects.waypoint.WaypointObject;

public class OutOfBandPackage implements Encodable {
	private static final long serialVersionUID = 1L;

	private List<OutOfBandData> packages;
	private transient List<byte[]> data;
	private transient int dataSize;

	public OutOfBandPackage() {
		packages = new ArrayList<>(5);
	}

	public OutOfBandPackage(OutOfBandData outOfBandData) {
		this();
		packages.add(outOfBandData);
	}

	public static OutOfBandPackage createWithProse(Object stf, String key, Object prose) {
		return new OutOfBandPackage(new ProsePackage(stf, key, prose));
	}

	@Override
	public byte[] encode() {
		if (packages.size() == 0)
			return new byte[4];

		if (data == null) {
			data = new LinkedList<>();

			for (OutOfBandData outOfBandData : packages) {
				byte[] bytes = outOfBandData.encodeOutOfBandData();
				dataSize += bytes.length;
				data.add(bytes);
			}
		}

		ByteBuffer bb = ByteBuffer.allocate(4 + dataSize).order(ByteOrder.LITTLE_ENDIAN);

		bb.putInt(dataSize / 2);

		for (byte[] bytes : data) {
			bb.put(bytes);
		}

		return bb.array();
	}

	public void decode(ByteBuffer data) {
		int size = data.getInt() * 2;

		for (int read = 0; read < size; read+= 3) {
			boolean addedByte = data.getShort() > 0; // ?? Seen as 1

			Type type = Type.valueOf(data.get());
			switch(type) {
				case PROSE_PACKAGE:
					ProsePackage prose = new ProsePackage();
					read += prose.decodeOutOfBandData(data);
					packages.add(prose);
					break;
				case WAYPOINT:
					WaypointObject waypoint = new WaypointObject(-1);
					read += waypoint.decodeOutOfBandData(data);
					packages.add(waypoint);
					break;
				default:
					System.err.println("Tried to decode an unsupported OutOfBandData Type: " + type);
					break;
			}

			if (addedByte) {
				data.get();
				read+= 1;
			}
		}
	}

	public enum Type {
		NONE(-1),
		OBJECT(0),
		PROSE_PACKAGE(1),
		UNKNOWN(2),
		AUCTION_TOKEN(3),
		WAYPOINT(4),
		STRING_ID(5),
		STRING(6),
		UNKNOWN_2(7);

		byte type;

		Type(int type) {
			this.type = (byte) type;
		}

		public byte getType() {
			return type;
		}

		public static Type valueOf(byte typeByte) {
			for (Type type : Type.values()) {
				if (type.getType() == typeByte)
					return type;
			}
			return Type.NONE;
		}
	}

	public interface OutOfBandData extends Encodable {
		byte[] encodeOutOfBandData();
		int decodeOutOfBandData(ByteBuffer data);
	}
}
