/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.collections;

import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.encoding.Encoder;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.holocore.resources.objects.SWGObject;
import me.joshlarson.jlcommon.concurrency.SynchronizedMap;
import me.joshlarson.jlcommon.log.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SWGMap<K, V> extends ConcurrentHashMap<K, V> implements Encodable {
	
	private static final long serialVersionUID = 1L;
	
	private final int view;
	private final int updateType;
	private final StringType strType;
	private final AtomicInteger updateCount;
	private final Map<Object, byte[]> deltas;
	private final Map<Object, byte[]> data;
	
	private int deltaSize;
	private int dataSize;
	
	public SWGMap(int view, int updateType) {
		this(view, updateType, StringType.UNSPECIFIED);
	}
	
	public SWGMap(int view, int updateType, StringType strType) {
		this.view = view;
		this.updateType = updateType;
		this.strType = strType;
		this.updateCount = new AtomicInteger(0);
		this.deltas = new SynchronizedMap<>();
		this.data = new SynchronizedMap<>();
		this.deltaSize = 0;
		this.dataSize = 0;
	}
	
	public void resetUpdateCount() {
		updateCount.set(0);
	}
	
	@Override
	public V get(Object key) {
		return super.get(key);
	}
	
	@Override
	public V put(K key, V value) {
		V old = super.put(key, value);
		updateCount.incrementAndGet();
		if (old != null) {
			removeData(key);
		}
		addData(key, value, (byte) 0);
		
		return old;
	}
	
	@Override
	public V remove(Object key) {
		V old = super.remove(key);
		updateCount.incrementAndGet();
		removeData(key);
		
		return old;
	}
	
	/**
	 * Sends a delta and updates the data, notifying clients of the changed item in this map. Use
	 * this if you are not adding or removing the element and just simply changing it. Should be
	 * used after changing values for an item in this super.
	 * 
	 * @param key Associated key with the value that was modified.
	 * @param parent The parent of this map, who the map is bound to in order to send the delta
	 */
	public void update(Object key, SWGObject parent) {
		updateCount.incrementAndGet();
		removeDataSize(key); // remove the size of the prior data
		addData(key, super.get(key), (byte) 2);
		sendDeltaMessage(parent);
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer;
		synchronized (data) {
			if (dataSize == 0)
				return new byte[8];
			buffer = ByteBuffer.allocate(8 + dataSize + data.size()).order(ByteOrder.LITTLE_ENDIAN);
			
			buffer.putInt(data.size());
			buffer.putInt(updateCount.get());
			
			for (byte[] bytes : data.values()) {
				buffer.put((byte) 0);
				buffer.put(bytes);
			}
		}
		
		return buffer.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		throw new UnsupportedOperationException("Use decode(ByteBuffer data, Class<K> kType, Class<V> vType) instead");
	}
	
	@Override
	public int getLength() {
		return 8 + dataSize + data.size();
	}
	
	@SuppressWarnings("unchecked")
	// Unfortunately the exception is just caught
	public void decode(ByteBuffer data, StringType keyType, StringType valType) {
		int size = data.getInt();
		updateCount.set(data.getInt());
		NetBuffer buffer = NetBuffer.wrap(data);
		try {
			for (int i = 0; i < size; i++) {
				buffer.getByte();
				put((K) buffer.getString(keyType), (V) buffer.getString(valType));
			}
		} catch (ClassCastException e) {
			Log.e(e);
		}
		clearDeltaQueue();
	}
	
	@SuppressWarnings("unchecked")
	// Unfortunately the exception is just caught
	public void decode(ByteBuffer data, StringType keyType, Class<V> vType) {
		int size = data.getInt();
		updateCount.set(data.getInt());
		NetBuffer buffer = NetBuffer.wrap(data);
		try {
			for (int i = 0; i < size; i++) {
				buffer.getByte();
				String key = buffer.getString(keyType);
				Object value = buffer.getGeneric(vType);
				if (value != null && vType.isAssignableFrom(value.getClass()))
					put((K) key, (V) value);
				else
					Log.e("Unable to parse: key=%s  value=%s", key, value);
			}
		} catch (ClassCastException e) {
			Log.e(e);
		}
		clearDeltaQueue();
	}
	
	@SuppressWarnings("unchecked")
	// There is type checking in the respective if's
	public void decode(ByteBuffer data, Class<K> kType, Class<V> vType) {
		int size = data.getInt();
		updateCount.set(data.getInt());
		
		NetBuffer buffer = NetBuffer.wrap(data);
		for (int i = 0; i < size; i++) {
			buffer.getByte();
			Object key = buffer.getGeneric(kType);
			if (key == null) {
				Log.e("Failed to decode: " + kType.getSimpleName());
				break;
			}
			Object value = buffer.getGeneric(vType);
			if (value == null) {
				Log.e("Failed to decode: " + vType.getSimpleName());
				break;
			}
			if (kType.isAssignableFrom(key.getClass()) && vType.isAssignableFrom(value.getClass()))
				put((K) key, (V) value);
			else
				Log.e("Failed to insert key=" + key + "  value=" + value);
		}
		clearDeltaQueue();
	}
	
	public void sendDeltaMessage(SWGObject target) {
		if (deltas.size() == 0)
			return;
		
		target.sendDelta(view, updateType, getDeltaData());
		// Clear the queue since the delta has been sent to observers through the builder
		clearDeltaQueue();
	}
	
	public void clearDeltaQueue() {
		synchronized (deltas) {
			deltas.clear();
			deltaSize = 0;
		}
	}
	
	private byte[] getDeltaData() {
		ByteBuffer buffer;
		synchronized (deltas) {
			buffer = ByteBuffer.allocate(8 + deltaSize).order(ByteOrder.LITTLE_ENDIAN);
			
			buffer.putInt(deltas.size());
			buffer.putInt(updateCount.get());
			for (byte[] data : deltas.values()) {
				buffer.put(data);
			}
		}
		
		return buffer.array();
	}
	
	private void createDeltaData(Object key, byte[] delta, byte update) {
		byte[] combinedUpdate = new byte[delta.length + 1];
		combinedUpdate[0] = update;
		System.arraycopy(delta, 0, combinedUpdate, 1, delta.length);
		synchronized (deltas) {
			if (deltas.containsKey(key)) {
				deltaSize -= deltas.remove(key).length;
			}
			deltaSize += combinedUpdate.length;
			deltas.put(key, combinedUpdate);
		}
	}
	
	private void addData(Object key, Object value, byte update) {
		byte[] encodedKey = Encoder.encode(key, strType);
		byte[] encodedValue = Encoder.encode(value, strType);
		byte[] encodedData = new byte[encodedKey.length + encodedValue.length];
		
		System.arraycopy(encodedKey, 0, encodedData, 0, encodedKey.length);
		System.arraycopy(encodedValue, 0, encodedData, encodedKey.length, encodedValue.length);
		
		synchronized (data) {
			data.put(key, encodedData);
			dataSize += encodedData.length;
		}
		
		createDeltaData(key, encodedData, update);
	}
	
	private void removeData(Object key) {
		byte [] value;
		synchronized (data) {
			value = data.remove(key);
			dataSize -= value.length;
		}
		
		createDeltaData(key, value, (byte) 1);
	}
	
	private void removeDataSize(Object key) {
		synchronized (data) {
			dataSize -= data.get(key).length;
		}
	}
	
	public static SWGMap<String, String> getSwgMap(NetBuffer buffer, int num, int var, StringType type) {
		SWGMap<String, String> set = new SWGMap<>(num, var, type);
		set.decode(buffer.getBuffer(), type, type);
		return set;
	}
	
	public static <T> SWGMap<String, T> getSwgMap(NetBuffer buffer, int num, int var, StringType keyType, Class<T> c) {
		SWGMap<String, T> set = new SWGMap<>(num, var, keyType);
		set.decode(buffer.getBuffer(), keyType, c);
		return set;
	}
	
	public static <K, V> SWGMap<K, V> getSwgMap(NetBuffer buffer, int num, int var, Class<K> keyClass, Class<V> valClass) {
		SWGMap<K, V> set = new SWGMap<>(num, var);
		set.decode(buffer.getBuffer(), keyClass, valClass);
		return set;
	}
	
}
