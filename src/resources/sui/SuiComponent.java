/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.sui;

import network.packets.Packet;
import resources.encodables.Encodable;
import utilities.Encoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Waverunner
 */
public class SuiComponent implements Encodable {

	public SuiComponent() {}

	public SuiComponent(Type type) {
		this.type = type;
	}

	private Type type;
	private List <String> wideParams = new ArrayList<>();
	private List<String> narrowParams = new ArrayList<>();

	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public List <String> getNarrowParams() {
		return narrowParams;
	}
	public void setNarrowParams(List <String> narrowParams) {
		this.narrowParams = narrowParams;
	}
	public List <String> getWideParams() {
		return wideParams;
	}
	public void setWideParams(List <String> wideParams) {
		this.wideParams = wideParams;
	}
	public void addNarrowParam(String param) {
		narrowParams.add(param);
	}
	public void addWideParam(String param) {
		wideParams.add(param);
	}

	@Override
	public byte[] encode() {
		int size = 9;

		for (String param : wideParams) { size += 4 + (param.length() * 2); }
		for (String param : narrowParams) { size += 2 + param.length();}

		ByteBuffer bb = ByteBuffer.allocate(size);
		Packet.addByte(bb, type.getValue());
		Packet.addList(bb, wideParams, Encoder.StringType.UNICODE);
		Packet.addList(bb, narrowParams, Encoder.StringType.ASCII);

		return bb.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		type			= Type.valueOf(Packet.getByte(data));
		wideParams		= Packet.getList(data, Encoder.StringType.UNICODE);
		narrowParams	= Packet.getList(data, Encoder.StringType.ASCII);
	}

	public enum Type {
		NONE(0),
		CLEAR_DATA_SOURCE(1),
		ADD_CHILD_WIDGET(2),
		SET_PROPERTY(3),
		ADD_DATA_ITEM(4),
		SUBSCRIBE_TO_EVENT(5),
		ADD_DATA_SOURCE_CONTAINER(6),
		CLEAR_DATA_SOURCE_CONTAINER(7),
		ADD_DATA_SOURCE(8);

		private byte value;

		Type(int value) {
			this.value = (byte) value;
		}

		public byte getValue() {
			return value;
		}

		public static Type valueOf(byte value) {
			for (Type type : Type.values()) {
				if (type.getValue() == value)
					return type;
			}
			return NONE;
		}
	}
}
