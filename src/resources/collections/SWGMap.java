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
package resources.collections;

import resources.encodables.Encodable;
import resources.network.NetBuffer;
import resources.objects.SWGObject;
import resources.server_info.Log;
import utilities.Encoder;
import utilities.Encoder.StringType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import network.packets.Packet;

public class SWGMap<K, V> extends HashMap<K, V> implements Encodable, Serializable {
	
	private static final long serialVersionUID = 2L;
	
	private final int view;
	private final int updateType;
	private final StringType strType;
	
	private transient Object updateMutex;
	private transient AtomicInteger updateCount;
	private transient Map<Object, byte[]> deltas;
	private transient Map<Object, byte[]> data;
	private transient int deltaSize;
	private transient int dataSize;
	
	public SWGMap(int view, int updateType) {
		this(view, updateType, StringType.UNSPECIFIED);
	}
	
	public SWGMap(int view, int updateType, StringType strType) {
		this.view = view;
		this.updateType = updateType;
		this.strType = strType;
		this.updateMutex = new Object();
		this.updateCount = new AtomicInteger(0);
		this.deltas = new HashMap<>();
		this.data = new HashMap<>();
		this.deltaSize = 0;
		this.dataSize = 0;
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		updateMutex = new Object();
		updateCount = new AtomicInteger(0);
		deltas = new HashMap<>();
		data = new HashMap<>();
		deltaSize = 0;
		dataSize = 0;
		for (Entry<K, V> e : entrySet()) {
			put(e.getKey(), e.getValue());
		}
		clearDeltaQueue();
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
		V old;
		synchronized (updateMutex) {
			old = super.put(key, value);
		}
		updateCount.incrementAndGet();
		if (old != null) {
			removeData(key);
		}
		addData(key, value, (byte) 0);
		
		return old;
	}
	
	@Override
	public V remove(Object key) {
		V old;
		synchronized (updateMutex) {
			old = super.remove(key);
		}
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
	public void decode(ByteBuffer data) {
		throw new UnsupportedOperationException("Use decode(ByteBuffer data, Class<K> kType, Class<V> vType) instead");
	}
	
	@SuppressWarnings("unchecked")
	// Unfortunately the exception is just caught
	public void decode(ByteBuffer data, StringType keyType, StringType valType) {
		int size = Packet.getInt(data);
		updateCount.set(Packet.getInt(data));
		NetBuffer buffer = NetBuffer.wrap(data);
		try {
			for (int i = 0; i < size; i++) {
				buffer.getByte();
				put((K) buffer.getString(keyType), (V) buffer.getString(valType));
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
		clearDeltaQueue();
	}
	
	@SuppressWarnings("unchecked")
	// Unfortunately the exception is just caught
	public void decode(ByteBuffer data, StringType keyType, Class<V> vType) {
		int size = Packet.getInt(data);
		updateCount.set(Packet.getInt(data));
		NetBuffer buffer = NetBuffer.wrap(data);
		try {
			for (int i = 0; i < size; i++) {
				buffer.getByte();
				String key = buffer.getString(keyType);
				Object value = buffer.getGeneric(vType);
				if (value != null && vType.isAssignableFrom(value.getClass()))
					put((K) key, (V) value);
				else
					Log.e("SWGMap", "Unable to parse: key=%s  value=%s", key, value);
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
		clearDeltaQueue();
	}
	
	@SuppressWarnings("unchecked")
	// There is type checking in the respective if's
	public void decode(ByteBuffer data, Class<K> kType, Class<V> vType) {
		int size = Packet.getInt(data);
		updateCount.set(Packet.getInt(data));
		
		NetBuffer buffer = NetBuffer.wrap(data);
		for (int i = 0; i < size; i++) {
			buffer.getByte();
			Object key = buffer.getGeneric(kType);
			if (key == null) {
				Log.e("SWGMap", "Failed to decode: " + kType.getSimpleName());
				break;
			}
			Object value = buffer.getGeneric(vType);
			if (value == null) {
				Log.e("SWGMap", "Failed to decode: " + vType.getSimpleName());
				break;
			}
			if (kType.isAssignableFrom(key.getClass()) && vType.isAssignableFrom(value.getClass()))
				put((K) key, (V) value);
			else
				Log.e("SWGMap", "Failed to insert key=" + key + "  value=" + value);
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
}
