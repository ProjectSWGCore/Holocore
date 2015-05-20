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
package utilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import resources.network.BaselineBuilder.Encodable;

public class Encoder {

	public static byte[] encode(Object object) {
		return encode(object, StringType.UNSPECIFIED);
	}
	
	public static byte[] encode(Object object, StringType strType) {
		if (strType != StringType.UNSPECIFIED) {
			switch (strType) {
			case ASCII: return encodeAscii((String) object);
			case UNICODE: return encodeUnicode((String) object);
			default: break;
			}
		} else {
			if (object instanceof Encodable) {
				return encodeObject((Encodable) object);
			} else if (object instanceof Integer) {
				return encodeInteger((Integer) object);
			} else if (object instanceof Long) {
				return encodeLong((Long) object);
			} else if (object instanceof Short) { 
				return encodeShort((Short) object);
			} else if (object instanceof Byte) { 
				return encodeByte((Byte) object);
			} else if (object instanceof Boolean) {
				return encodeBoolean((boolean) object);
			} else if (object instanceof Float || object instanceof Double) {
				return encodeFloat(object);
			} else if (object instanceof String){
				throw new UnsupportedOperationException("You must specify a String type!");
			} else {
				System.err.println("[Encoder] Do not know how to encode instance type " + object.getClass().getName());
			}
		}
		return null;
	}

	public static byte[] encodeObject(Encodable encodable) {
		return encodable.encode();
	}

	private static byte[] encodeFloat(Object object) {
		ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putFloat((object instanceof Float ? (Float) object : ((Double) object).floatValue()));
		return buffer.array();
	}

	private static byte[] encodeByte(Byte object) {
		return new byte[]{(object.byteValue())};
	}
	
	private static byte[] encodeBoolean(boolean object) {
		return encodeByte((byte) (object ? 1 : 0));
	}

	public static byte[] encodeShort(Short object) {
		ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort(object);
		return buffer.array();
	}

	public static byte[] encodeInteger(int integer) {
		ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(integer);
		return buffer.array();
	}
	
	public static byte[] encodeLong(long l) {
		ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(l);
		return buffer.array();
	}
	
	public static byte[] encodeAscii(String string) {
		ByteBuffer buffer = ByteBuffer.allocate(2 + string.length()).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) string.length());
		buffer.put(string.getBytes(Charset.forName("UTF-8")));
		return buffer.array();
	}
	
	public static byte[] encodeUnicode(String string) {
		ByteBuffer buffer = ByteBuffer.allocate(4 + (string.length() * 2)).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(string.length());
		buffer.put(string.getBytes(Charset.forName("UTF-16LE")));
		return buffer.array();
	}
	
	public enum StringType {
		UNSPECIFIED, ASCII, UNICODE
	}
}
