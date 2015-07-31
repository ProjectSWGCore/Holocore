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

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.encodables.Encodable;
import resources.network.DeltaBuilder;
import resources.objects.SWGObject;
import resources.player.PlayerState;
import utilities.Encoder;
import utilities.Encoder.StringType;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SWGMap<K, V> extends AbstractMap<K, V> implements Encodable, Serializable {
	private static final long serialVersionUID = 1L;

	private BaselineType baseline;
	
	private int view;
	private int updateType;	
	private transient int updateCount;
	private int dataSize;
	
	private StringType strType = StringType.UNSPECIFIED;
	
	/*
	 * Map which will contain all the byte data. A map is used here because it allows the encode method to not have to guess the ByteBuffer size. Doing it this way will
	 * also allow all the data to be pre-compiled for the list, so it can have a positive impact on large SWGList's. This means that only 1 ByteBuffer is being created,
	 * and that is to just take the data from this map and put it all together!
	 */
	private Map<Object, byte[]> data = new ConcurrentHashMap<>();
	private Map<K, V> map = new ConcurrentHashMap<K, V>();
	
	private Map<Object, byte[]> deltas = new HashMap<>();
	private int deltaSize;
	
	public SWGMap(BaselineType baseline, int view, int updateType) {
		this.baseline = baseline;
		this.view = view;
		this.updateType = updateType;
	}
	
	public SWGMap(BaselineType baseline, int view, int updateType, StringType strType) {
		this.baseline = baseline;
		this.view = view;
		this.updateType = updateType;
		this.strType = strType;
	}
	
	@Override
	public V get(Object key) {
		return map.get(key);
	}

	@Override
	public V put(K key, V value) {
		updateCount++;
		
		V old = map.put(key, value);
		
		if (old != null) {
			removeData(key);
		}
		addData(key, value, (byte) 0);
		
		return old;
	}
	
	@Override
	public V remove(Object key) {
		updateCount++;
		
		V old = map.remove(key);
		
		removeData(key);
		
		return old;
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	/**
	 * Sends a delta and updates the data, notifying clients of the changed item in this map. Use this if you are not adding or removing the element
	 * and just simply changing it. Should be used after changing values for an item in this map.
	 * @param key Associated key with the value that was modified.
	 * @param parent The parent of this map, who the map is bound to in order to send the delta
	 */
	public void update(Object key, SWGObject parent) {
		updateCount++;
		
		removeDataSize(key); // remove the size of the prior data because not all encodables are fixed sizes (ie some have strings inside them)
		addData(key, map.get(key), (byte) 2);
		sendDeltaMessage(parent);
	}
	
	@Override
	public byte[] encode() {
		int size = map.size();

		if (size == 0) {
			return new byte[8];
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(8 + dataSize + data.size()).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt(size);
		buffer.putInt(updateCount);

		if (data.size() != size) {
			// Data got out of sync with the map, so lets clean that up!
			clearAllData();
			for (Entry<K, V> entry : map.entrySet()) {
				addData(entry.getKey(), entry.getValue(), (byte) 0);
			}
			clearDeltaQueue();
		}

		for (byte[] bytes : data.values()) {
			buffer.put((byte) 0);
			buffer.put(bytes);
		}

		return buffer.array();
	}

	@Override
	public void decode(ByteBuffer data) {

		//throw new NotImplementedException();
/*		Not sure how to do decoding for an SWGMap because of generics, won't know what specific type to decode as
		One possible workaround is to refactor decode to create new object instead of directly initializing the variables

		int size 	= Packet.getInt(data);
		updateCount	= Packet.getInt(data);

		for (int i = 0; i < size; i++) {

		}

		return 0;*/
	}

	public void sendDeltaMessage(SWGObject target) {
		if (!(deltas.size() > 0))
			return;

		if (target.getOwner() == null || target.getOwner().getPlayerState() != PlayerState.ZONED_IN) {
			clearDeltaQueue();
			return;
		}
		
		DeltaBuilder builder = new DeltaBuilder(target, baseline, view, updateType, getDeltaData());
		builder.send();
		// Clear the queue since the delta has been sent to observers through the builder
		clearDeltaQueue();
	}
	
	public void clearDeltaQueue() {
		deltas.clear();
		deltaSize = 0;
	}
	
	private byte[] getDeltaData() {
		ByteBuffer buffer = ByteBuffer.allocate(8 + deltaSize).order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putInt(deltas.size());
		buffer.putInt(updateCount);
		for (byte[] data : deltas.values()) {
			buffer.put(data);
		}
		
		return buffer.array();
	}
	
	private void createDeltaData(Object key, byte[] delta, byte update) {
		synchronized(deltas) {
			byte[] combinedUpdate = new byte[delta.length + 1];
			combinedUpdate[0] = update;
			System.arraycopy(delta, 0, combinedUpdate, 1, delta.length);

			if (deltas.containsKey(key)) {
				deltaSize -= deltas.remove(key).length;
			}
			deltaSize += delta.length + 1;
			deltas.put(key, combinedUpdate);
		}
	}

	private void clearAllData() {
		dataSize = 0;
		data.clear();

		clearDeltaQueue();
	}

	private void addData(Object key, Object value, byte update) {
		byte[] encodedKey = Encoder.encode(key, strType);
		byte[] encodedValue;
		byte[] encodedData;
		
		if(value instanceof String)
			encodedValue = Encoder.encode(value, strType);
		else
			encodedValue = Encoder.encode(value);
		
		encodedData = new byte[encodedKey.length + encodedValue.length];
		
		System.arraycopy(encodedKey, 0, encodedData, 0, encodedKey.length);
		System.arraycopy(encodedValue, 0, encodedData, encodedKey.length, encodedValue.length);
		
		data.put(key, encodedData);
		
		dataSize += encodedData.length;
		
		createDeltaData(key, encodedData, update);
	}
	
	
	// Removes Key and its Value size of data and removes it from the data map
	private void removeData(Object key) {
		byte[] bytes = data.remove(key);
		if (bytes == null) {
			System.err.println("[SWGMap] Could not remove key as it wasn't in the data map: " + key);
			return;
		}
		
		dataSize -= bytes.length;
		
		createDeltaData(key, bytes, (byte) 1);
	}
	
	// Removes Key and its Value size of data without removing it from the data map
	private void removeDataSize(Object key) {
		dataSize -= data.get(key).length;
	}
}
