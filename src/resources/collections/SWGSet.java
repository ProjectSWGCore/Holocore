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

package resources.collections;

import network.packets.Packet;
import network.packets.swg.zone.baselines.Baseline;
import resources.encodables.Encodable;
import resources.network.NetBuffer;
import resources.objects.SWGObject;
import utilities.Encoder;
import utilities.Encoder.StringType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by Waverunner on 8/18/2015
 */
public class SWGSet<E> extends AbstractSet<E> implements Encodable, Serializable {
	private static final long serialVersionUID = 1L;

	private int view;
	private int updateType;
	private transient int updateCount;
	private int dataSize;

	private Encoder.StringType strType = StringType.UNSPECIFIED;

	// Data set for this set, should be in sync (same number of elements if encode data is not modified) with set.
	private final Set<byte[]> data = new HashSet<>();

	private final Set<E> set = new HashSet<>();

	private final LinkedList<byte[]> deltas = new LinkedList<>();
	private int deltaSize;

	/**
	 * Creates a new {@link SWGSet} for the defined baseline with the given view and update. Note that this is an extension of {@link AbstractSet} and makes use of {@link HashSet}
	 * @param baseline {@link Baseline.BaselineType} for this set, should be the same as the parent class this list resides in
	 * @param view The baseline number this list resides in
	 * @param updateType The update variable used for sending a delta, it's the operand count that this list resides at within the baseline
	 */
	public SWGSet(int view, int updateType) {
		this.view = view;
		this.updateType = updateType;
	}

	/**
	 * Creates a new {@link SWGSet} with the given StringType to encode in. Note that this constructor must be used if the elements within the list is a String.
	 * @param baseline {@link Baseline.BaselineType} for this set, should be the same as the parent class this list resides in
	 * @param view The baseline number this set resides in
	 * @param strType The {@link StringType} of the string, required only if the element in the set is a String as it's used for encoding either Unicode or ASCII characters
	 */
	public SWGSet(int view, int updateType, StringType strType) {
		this(view, updateType);
		this.strType = strType;
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		updateCount = 0;
	}
	
	public void resetUpdateCount() {
		updateCount = 0;
	}
	
	@Override
	public boolean add(E e) {
		if (!set.add(e))
			return false;
		updateCount++;
		addObjectData(e, (byte) 1);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		if (!set.remove(o))
			return false;
		updateCount++;
		removeObjectData(o, (byte) 0);
		return set.remove(o);
	}

	@Override
	public void clear() {
		set.clear();
		updateCount++;
		clearAllObjectData();
	}

	@Override
	public Iterator<E> iterator() {
		return set.iterator();
	}

	@Override
	public int size() {
		return set.size();
	}

	public void sendDeltaMessage(SWGObject target) {
		if (!(deltas.size() > 0))
			return;
		
		target.sendDelta(view, updateType, getDeltaData());
		// Clear the queue since the delta has been sent to observers through the builder
		clearDeltaQueue();
	}

	private void addObjectData(E obj, byte update) {
		byte[] encodedData = Encoder.encode(obj, strType);
		if (encodedData == null) {
			System.err.println(toString() + " FATAL: Tried to encode an object that could not be encoded properly. Object: " + obj);
			return;
		}

		dataSize += encodedData.length;
		synchronized (data) {
			data.add(encodedData);
		}
		createDeltaData(encodedData, update);
	}

	private void removeObjectData(Object obj, byte update) {
		byte[] encodedData = Encoder.encode(obj, strType);
		if (encodedData == null) {
			System.err.println(toString() + " FATAL: Tried to encode an object that could not be encoded properly. Object: " + obj);
			return;
		}

		synchronized (data) {
			dataSize -= encodedData.length;
		}

		createDeltaData(encodedData, update);
	}

	private void clearAllObjectData() {
		clearAllData();
		deltaSize = 1;
		deltas.add(new byte[]{(byte) 2});
	}

	public void clearDeltaQueue() {
		deltas.clear();
		deltaSize = 0;
	}

	private byte[] getDeltaData() {
		ByteBuffer buffer = ByteBuffer.allocate(8 + deltaSize).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt(deltas.size());
		buffer.putInt(updateCount);
		for (byte[] data : deltas) {
			buffer.put(data);
		}

		return buffer.array();
	}

	private void createDeltaData(byte[] delta, byte update) {
		synchronized(deltas) {
			byte[] combinedUpdate = new byte[delta.length + 1];
			combinedUpdate[0] = update;
			System.arraycopy(delta, 0, combinedUpdate, 1, delta.length);
			deltaSize += delta.length + 1;
			deltas.add(combinedUpdate);
		}
	}

	private void clearAllData() {
		dataSize = 0;
		data.clear();

		clearDeltaQueue();
	}

	@Override
	public byte[] encode() {
		int size = data.size();

		if (size == 0) {
			return new byte[8];
		}

		ByteBuffer buffer = ByteBuffer.allocate(8 + dataSize).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt(size);
		buffer.putInt(updateCount);

		data.forEach(buffer::put);

		return buffer.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		/*Not sure how to do decoding for an SWGSet because of generics, won't know what specific type to decode as
		One possible workaround is to refactor decode to create new object instead of directly initializing the variables
		During compile time, a Set<Type> is just a Set due to type erasure.
		*/
		try {
			throw new Exception("This is not a supported operation. Use decode(ByteBuffer data, Class<T> elementType) instead");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked") // Unfortunately the exception is just caught
	public void decode(ByteBuffer data, StringType type) {
		int size	= Packet.getInt(data);
		updateCount	= Packet.getInt(data);
		NetBuffer buffer = NetBuffer.wrap(data);
		try {
			for (int i = 0; i < size; i++)
				set.add((E) buffer.getString(type));
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked") // There is type checking in the respective if's
	public void decode(ByteBuffer data, Class<E> elementType) {
		int size 	= Packet.getInt(data);
		updateCount = Packet.getInt(data);

		try {
			boolean encodable = Encodable.class.isAssignableFrom(elementType);
			NetBuffer wrap = NetBuffer.wrap(data);
			for (int i = 0; i < size; i++) {
				if (encodable) {
					E instance = elementType.newInstance();
					if (instance instanceof Encodable) {
						((Encodable) instance).decode(data);
						set.add(instance);
					}
				} else {
					Object o = wrap.getGeneric(elementType);
					if (o != null && elementType.isAssignableFrom(o.getClass()))
						set.add((E) o);
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		clearDeltaQueue();
	}
}
