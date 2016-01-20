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

import network.packets.Packet;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.encodables.Encodable;
import resources.network.DeltaBuilder;
import resources.objects.SWGObject;
import resources.player.PlayerState;
import utilities.Encoder;
import utilities.Encoder.StringType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Supports a list of elements which automatically sends data as a delta when changed for baselines.
 * @author Waverunner
 *
 * @param <E> Element that implements {@link Encodable} in order for data to be sent, or a basic type.
 */
public class SWGList<E> extends AbstractList<E> implements Encodable, Serializable {
	private static final long serialVersionUID = 1L;

	private BaselineType baseline;
	
	private int view;
	private int updateType;	
	private transient int updateCount;
	private int dataSize;

	private StringType strType = StringType.UNSPECIFIED;

	// This list is a listing of all the data for the element list, this should always be in sync with list
	private final List<byte[]> data = new ArrayList<>();

	private final List<E> list = new ArrayList<>(); // thread-safe list
	
	private final LinkedList<byte[]> deltas = new LinkedList<>();
	private int deltaSize;

	/**
	 * Creates a new {@link SWGList} for the defined baseline with the given view and update. Note that this is an extension of {@link AbstractList} and makes use of {@link java.util.ArrayList}
	 * @param baseline {@link BaselineType} for this list, should be the same as the parent class this list resides in
	 * @param view The baseline number this list resides in
	 * @param updateType The update variable used for sending a delta, it's the operand count that this list resides at within the baseline
	 */
	public SWGList(BaselineType baseline, int view, int updateType) {
		this.baseline = baseline;
		this.view = view;
		this.updateType = updateType;
	}

	/**
	 * Creates a new {@link SWGList} with the given StringType to encode in. Note that this constructor must be used if the elements within the list is a String.
	 * @param baseline {@link BaselineType} for this list, should be the same as the parent class this list resides in
	 * @param view The baseline number this list resides in
	 * @param strType The {@link StringType} of the string, required only if the element in the list is a String as it's used for encoding either Unicode or ASCII characters
	 */
	public SWGList(BaselineType baseline, int view, int updateType, StringType strType) {
		this (baseline, view, updateType);
		this.strType = strType;
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		updateCount = 0;
	}

	/**
	 * Appends the specified element to the end of this list if it doesn't already exist. Once added, the updateCount is incremented by one
	 * and data for the object is encoded.
	 * <br><br>An <i>add delta</i> is then sent using {@link DeltaBuilder} if noUpdates = false (false by default)
	 * @param e element to be appended to this list
	 * @return true if the element was added
	 */
	@Override
	public boolean add(E e) {
		add(list.size(), e);
		return list.contains(e);
	}

	/**
	 * Inserts the specified element at the specified position in this list. Shifts the element currently
	 * at that position (if any) and any subsequent elements to the right (adds one to their indices).
	 * <br><br>An <i>add delta</i> is then sent using {@link DeltaBuilder} if noUpdates = false (false by default)
	 * @param index index at which the specified element is to be inserted
	 * @param e element to be inserted
	 */
	@Override
	public void add(int index, E e) {
		synchronized (list) {
			updateCount++;
			list.add(index, e);
			addObjectData(index, e, (byte) 1);
		}
	}

	/**
	 * Replaces the element at the specified position in this list with the specified element.
	 * <br><br>A <i>change delta</i> is then sent using {@link DeltaBuilder} if noUpdates = false (false by default). Since this
	 * sends a change delta, it should only be used for replacing an element, not for adding one.
	 * @param index index of the element to replace
	 * @param element element to be stored at the specified position
	 * @return The element that was replaced
	 */
	@Override
	public E set(int index, E element) {
		// Sends a "change" delta
		E previous;
		synchronized (list) {
			previous = list.set(index, element);
			if (previous != null) {
				updateCount++;
				removeDataSize(index);
				removeData(index);
			}
			addObjectData(index, element, (byte) 2);
		}
		return previous;
	}

	@Override
	public boolean remove(Object o) {
		//noinspection SuspiciousMethodCalls
		int index = list.indexOf(o); // No idea why this produces a suspicious method call..
		if (index != -1) {
			remove(index);
			return true;
		}
		return false;
	}

	/**
	 * Removes the element at the specified position in this list. Shifts any subsequent elements to the left
	 * (subtracts one from their indices). Returns the element that was removed from the list.
	 * @param index the index of the element to be removed
	 * @return the element previously at the specified position
	 */
	@Override
	public E remove(int index) {
		E element;

		synchronized (list) {
			element = list.remove(index);
			if (element != null) {
				updateCount++;
				removeObjectData(index, (byte) 0);
			}
		}

		return element;
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public E get(int index) {
		return list.get(index);
	}

	@Override
	public int size() {
		return list.size();
	}

	/**
	 * Creates an array of bytes based off of the elements within this list. Elements that are not of a standard type
	 * handled by {@link Encoder} should implement the {@link Encodable} interface.
	 * @return Array of bytes with the size, update count, and encoded elements
	 */
	@Override
	public byte[] encode() {
		int size = list.size();

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
/*		Not sure how to do decoding for an SWGList because of generics, won't know what specific type to decode as
		One possible workaround is to refactor decode to create new object instead of directly initializing the variables
		During compile time, a List<Type> is just a List due to type erasure.
		*/
		try {
			throw new Exception("This is not a supported operation. Use decode(ByteBuffer data, Class<T> elementType) instead");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void decode(ByteBuffer data, Class<E> elementType) {
		// TODO: Decode other instance types besides encodable for SWGList
		int size 	= Packet.getInt(data);
		updateCount = Packet.getInt(data);

		try {
			for (int i = 0; i < size; i++) {
				E instance = elementType.newInstance();
				if (instance instanceof Encodable)
					((Encodable) instance).decode(data);
				else {
					System.out.println("No decode support for the type " + elementType.getName());
					break;
				}
				list.add(instance);
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		clearDeltaQueue();
	}

	public void sendDeltaMessage(SWGObject target) {
		if (deltas.size() == 0)
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

	public void sendRefreshedListData(SWGObject target) {
		clearDeltaQueue();
		updateCount = 0;

		ByteBuffer bb = ByteBuffer.allocate(11 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(data.size() + 1);
		bb.putInt(updateCount += data.size() + 1);
		bb.put((byte) 3);
		bb.putShort((short) data.size());
		for (byte[] bytes : data) {
			bb.put(bytes);
		}

		DeltaBuilder builder = new DeltaBuilder(target, baseline, view, updateType, bb.array());
		builder.send();
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
	
	private void addObjectData(int index, E obj, byte update) {
		byte[] encodedData = Encoder.encode(obj, strType);
		if (encodedData == null) {
			System.err.println(toString() + " FATAL: Tried to encode an object that could not be encoded properly. Object: " + obj);
			return;
		}

		dataSize += encodedData.length;
		synchronized (data) {
			data.add(encodedData);
		}

		createIndexedDelta(encodedData, index, update);
	}

	private void createIndexedDelta(byte[] encodedData, int index, byte update) {
		ByteBuffer buffer = ByteBuffer.allocate(encodedData.length + 2).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) index);
		buffer.put(encodedData);

		byte[] indexedBytes = buffer.array();
		createDeltaData(indexedBytes, update);
	}

	private void removeObjectData(int index, byte update) {
		if (data.get(index) == null) {
			return;
		}

		synchronized (data) {
			dataSize -= data.remove(index).length;
		}

		// Only the index is sent for removing data
		ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) index);
		createDeltaData(buffer.array(), update);
	}

	// Removes obj size of data without removing it from the data map
	private void removeDataSize(int index) {
		dataSize -= data.get(index).length;
	}

	private void removeData(int index) {
		synchronized (data) {
			data.remove(index);
		}
	}

	public BaselineType getBaseline() { return baseline; }

	@Override
	public String toString() {
		return "SWGList[" + baseline + "0" + view + ":" + updateType + "]";
	}
}
