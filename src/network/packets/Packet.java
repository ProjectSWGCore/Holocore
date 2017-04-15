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
package network.packets;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.projectswg.common.data.CRC;
import com.projectswg.common.debug.Log;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.encoding.StringType;


public class Packet {
	
	public static final Charset ascii   = Charset.forName("UTF-8");
	public static final Charset unicode = Charset.forName("UTF-16LE");
	
	private SocketAddress socketAddress;
	private ByteBuffer    data;
	private int           opcode;
	
	public Packet() {
		socketAddress = null;
		data = ByteBuffer.allocate(2);
		opcode = 0;
	}
	
	public Packet(ByteBuffer data) {
		decode(data);
	}
	
	public void setSocketAddress(SocketAddress socketAddress) {
		this.socketAddress = socketAddress;
	}
	
	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}
	
	public SocketAddress getSocketAddress() {
		return socketAddress;
	}
	
	public int getOpcode() {
		return opcode;
	}
	
	protected static int getCrc(String str) {
		return CRC.getCrc(str);
	}

	public static void addList(ByteBuffer bb, Collection<? extends Encodable> list) {
		if (list == null) {
			addInt(bb, 0);
			return;
		}

		addInt(bb, list.size());
		for (Encodable encodable : list) {
			addData(bb, encodable.encode());
		}
	}

	public static void addList(ByteBuffer bb, List<byte[]> list) {
		addInt(bb, list.size());
		for (byte[] bytes : list) {
			addData(bb, bytes);
		}
	}

	public static void addList(ByteBuffer bb, List<String> list, StringType type) {
		addInt(bb, list.size());

		switch(type) {
			case ASCII: for (String s : list) { addAscii(bb, s); }
				break;
			case UNICODE: for (String s : list) { addUnicode(bb, s); }
				break;
			default: Log.e("Cannot encode StringType " + type);
				break;
		}
	}

	public static void addBoolean(ByteBuffer bb, boolean b) {
		bb.put(b ? (byte) 1 : (byte) 0);
	}
	
	public static void addAscii(ByteBuffer bb, String s) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort((short) s.length());
		bb.put(s.getBytes(ascii));
	}
	
	public static void addUnicode(ByteBuffer bb, String s) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(s.length());
		bb.put(s.getBytes(unicode));
	}
	
	public static void addLong(ByteBuffer bb, long l) {
		bb.order(ByteOrder.LITTLE_ENDIAN).putLong(l);
	}
	
	public static void addInt(ByteBuffer bb, int i) {
		bb.order(ByteOrder.LITTLE_ENDIAN).putInt(i);
	}
	
	public static void addFloat(ByteBuffer bb, float f) {
		bb.putFloat(f);
	}
	
	public static void addShort(ByteBuffer bb, int i) {
		bb.order(ByteOrder.LITTLE_ENDIAN).putShort((short) i);
	}
	
	public static void addNetLong(ByteBuffer bb, long l) {
		bb.order(ByteOrder.BIG_ENDIAN).putLong(l);
	}
	
	public static void addNetInt(ByteBuffer bb, int i) {
		bb.order(ByteOrder.BIG_ENDIAN).putInt(i);
	}
	
	public static void addNetShort(ByteBuffer bb, int i) {
		bb.order(ByteOrder.BIG_ENDIAN).putShort((short) i);
	}
	
	public static void addByte(ByteBuffer bb, int b) {
		bb.put((byte) b);
	}

	public static void addData(ByteBuffer bb, byte[] data) {
		bb.put(data);
	}
	
	public static void addArray(ByteBuffer bb, byte[] data) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		addShort(bb, data.length);
		addData(bb, data);
	}

	public static void addArrayList(ByteBuffer bb, byte[] b) {
		addShort(bb, b.length);
		bb.put(b);
	}

	public static void addEncodable(ByteBuffer data, Encodable encodable) {
		data.put(encodable.encode());
	}

	public static void addCrc(ByteBuffer bb, String crcString) {
		addInt(bb, CRC.getCrc(crcString));
	}

	public static boolean getBoolean(ByteBuffer bb) {
		return getByte(bb) == 1;
	}
	
	public static String getAscii(ByteBuffer bb) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		short length = bb.getShort();
		if (length > bb.remaining())
			return "";
		byte [] str = new byte[length];
		bb.get(str);
		return new String(str, ascii);
	}
	
	public static String getUnicode(ByteBuffer bb) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int length = bb.getInt() * 2;
		if (length > bb.remaining())
			return "";
		byte [] str = new byte[length];
		bb.get(str);
		return new String(str, unicode);
	}

	public static byte getByte(ByteBuffer bb) {
		return bb.get();
	}
	
	public static short getShort(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).getShort();
	}
	
	public static int getInt(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).getInt();
	}
	
	public static float getFloat(ByteBuffer bb) {
		return bb.getFloat();
	}
	
	public static long getLong(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).getLong();
	}
	
	public static short getNetShort(ByteBuffer bb) {
		return bb.order(ByteOrder.BIG_ENDIAN).getShort();
	}
	
	public static int getNetInt(ByteBuffer bb) {
		return bb.order(ByteOrder.BIG_ENDIAN).getInt();
	}
	
	public static long getNetLong(ByteBuffer bb) {
		return bb.order(ByteOrder.BIG_ENDIAN).getLong();
	}
	
	public static byte [] getArray(ByteBuffer bb) {
		byte [] data = new byte[getShort(bb)];
		bb.get(data);
		return data;
	}
	
	public static byte [] getArray(ByteBuffer bb, int length) {
		byte [] data = new byte[length];
		bb.get(data);
		return data;
	}
	
	public static int [] getIntArray(ByteBuffer bb) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int [] ints = new int[bb.getInt()];
		for (int i = 0; i < ints.length; i++)
			ints[i] = bb.getInt();
		return ints;
	}
	
	public static int [] getIntArray(ByteBuffer bb, int size) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int [] ints = new int[size];
		for (int i = 0; i < ints.length; i++)
			ints[i] = bb.getInt();
		return ints;
	}
	
	public static boolean[] getBooleanArray(ByteBuffer bb) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		boolean[] booleans = new boolean[bb.getInt()];
		for(int i = 0; i < booleans.length; i++)
			booleans[i] = getBoolean(bb);
		return booleans;
	}
	
	/**
	 * Decodes the ByteBuffer stream into the passed List using the given listType instance for creating elements
	 * for the list from the buffer.
	 * @param bb ByteBuffer
	 * @param type Top-most class for the elements that are to be added to the list
	 * @param <T> Class type that implements {@link Encodable}
	 * @return Amount of bytes that are read
	 */
	public static <T extends Encodable> List<T> getList(ByteBuffer bb, Class<T> type) {
		int size = getInt(bb);

		if (size < 0) {
			Log.e("Read list with size less than zero!");
			return null;
		} else if (size == 0) {
			return new ArrayList<>();
		}

		List<T> list = new ArrayList<>();

		try {
			for (int i = 0; i < size; i++) {
				T instance = type.newInstance();
				instance.decode(bb);
				list.add(instance);
			}
		} catch (InstantiationException | IllegalAccessException e) {
			Log.e(e);
		}

		if (size != list.size())
			Log.e("Expected list size %d but only have %d elements in the list", size, list.size());
		return list;
	}

	public static <T extends Encodable> T getEncodable(ByteBuffer bb, Class<T> type) {
		T instance = null;
		try {
			instance = type.newInstance();
			instance.decode(bb);
		} catch (InstantiationException | IllegalAccessException e) {
			Log.e(e);
		}

		return instance;
	}

	public static List<String> getList(ByteBuffer bb, StringType type) {
		int size = getInt(bb);

		if (size < 0) {
			Log.e("Read list with size less than zero!");
			return null;
		} else if (size == 0) {
			return new ArrayList<>();
		}

		List<String> list = new ArrayList<>();

		switch(type) {
			case ASCII: for (int i = 0; i < size; i++) { list.add(getAscii(bb)); }
				break;
			case UNICODE: for (int i = 0; i < size; i++) { list.add(getUnicode(bb)); }
				break;
			default: Log.e("Do not know how to read list of StringType " + type);
				break;
		}

		return list;
	}

	public void decode(ByteBuffer data) {
		this.data = data;
		opcode = getNetShort(data);
	}
	
	public ByteBuffer getData() {
		return data;
	}
	
	public ByteBuffer encode() {
		return data;
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
	
}
