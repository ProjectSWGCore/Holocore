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

import network.packets.Packet;
import resources.network.NetBufferStream;
import resources.objects.waypoint.WaypointObject;
import resources.persistable.Persistable;
import resources.server_info.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class OutOfBandPackage implements Encodable, Persistable {
	
	private List<OutOfBandData> packages;
	private transient List<byte[]> data;
	private transient int dataSize;

	public OutOfBandPackage() {
		packages = new ArrayList<>(5);
		data = null;
		dataSize = 0;
	}

	public OutOfBandPackage(OutOfBandData... outOfBandData) {
		this();
		Collections.addAll(packages, outOfBandData);
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		data = null;
		dataSize = 0;
	}

	public List<OutOfBandData> getPackages() {
		return packages;
	}

	@Override
	public byte[] encode() {
		if (packages.size() == 0)
			return new byte[4];

		if (data == null) {
			data = new LinkedList<>();

			for (OutOfBandData outOfBandData : packages) {
				byte[] bytes = packOutOfBandData(outOfBandData);
				dataSize += bytes.length;
				data.add(bytes);
			}
		}

		ByteBuffer bb = ByteBuffer.allocate(4 + dataSize).order(ByteOrder.LITTLE_ENDIAN);

		bb.putInt(dataSize / 2);

		data.forEach(bb::put);

		return bb.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		int size = data.getInt() * 2;

		int padding;
		int start;

		for (int read = 0; read < size; read++) {
			start = data.position();
			padding = data.getShort();
			unpackOutOfBandData(data, Type.valueOf(data.get()));
			data.position(data.position() + padding);
			read += data.position() - start;
		}
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addList(packages, (p) -> p.save(stream));
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getList((i) -> packages.add(OutOfBandFactory.create(stream)));
	}

	private void unpackOutOfBandData(ByteBuffer data, Type type) {
		// Position doesn't seem to be reflective of it's spot in the package list, not sure if this can be automated
		// as the client will send -3 for multiple waypoints in a mail, so could be static for each OutOfBandData
		// If that's the case, then we can move the position variable to the Type enum instead of a method return statement
/*		int position =*/ data.getInt();
		switch(type) {
			case PROSE_PACKAGE:
				ProsePackage prose = Packet.getEncodable(data, ProsePackage.class);
				packages.add(prose);
				break;
			case WAYPOINT:
				WaypointObject waypoint = new WaypointObject(-1);
				waypoint.decode(data);
				packages.add(waypoint);
				break;
			case STRING_ID:
				StringId stringId = Packet.getEncodable(data, StringId.class);
				packages.add(stringId);
				break;
			default:
				Log.e("OutOfBandPackage", "Tried to decode an unsupported OutOfBandData Type: " + type);
				break;
		}
	}

	private byte[] packOutOfBandData(OutOfBandData data) {
		byte[] base = data.encode();

		// Type and position is included in the padding size
		int paddingSize = (base.length + 5) % 2;

		ByteBuffer bb = ByteBuffer.allocate(7 + paddingSize + base.length);

		Packet.addShort(bb, paddingSize); // Number of bytes for decoding to skip over when reading
		Packet.addByte(bb, data.getOobType().getType());
		Packet.addInt(bb, data.getOobPosition());
		Packet.addData(bb, base);

		for (int i = 0; i < paddingSize; i++) {
			Packet.addByte(bb, 0);
		}

		return bb.array();
	}

	@Override
	public String toString() {
		return "OutOfBandPackage[packages=" + packages + "]";
	}

	public enum Type {
		UNDEFINED(Byte.MIN_VALUE),
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
			return Type.UNDEFINED;
		}
	}
}
