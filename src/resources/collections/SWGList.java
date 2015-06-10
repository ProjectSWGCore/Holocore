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
import resources.network.BaselineBuilder.Encodable;
import resources.network.DeltaBuilder;
import resources.objects.SWGObject;
import resources.player.PlayerState;
import utilities.Encoder;
import utilities.Encoder.StringType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Supports a list of elements which automatically sends data as a delta when changed for baselines.
 * @author Waverunner
 *
 * @param <E> Element that implements {@link Encodable} in order for data to be sent, or a basic type.
 */
public class SWGList<E> extends AbstractList<E> implements Encodable {
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
	private Map<Integer, byte[]> data = new ConcurrentHashMap<>();
	private List<E> list = new CopyOnWriteArrayList<>(); // thread-safe list
	
	private LinkedList<byte[]> deltas = new LinkedList<>();
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
	 * @param baseline
	 * @param view
	 * @param updateType
	 * @param strType
	 */
	public SWGList(BaselineType baseline, int view, int updateType, StringType strType) {
		this (baseline, view, updateType);
		this.strType = strType;
	}

	/**
	 * Appends the specified element to the end of this list if it doesn't already exist. Once added, the updateCount is incremented by one
	 * and data for the object is encoded. A add delta is then sent using {@link DeltaBuilder} if noUpdates = false (false by default)
	 * @param e element to be appended to this list
	 * @return true if the element was added
	 */
	@Override
	public boolean add(E e) {
		updateCount++;
		
		boolean added = list.add(e);
		
		if (added) {
			addObjectData(list.lastIndexOf(e), e);
		}
		
		return added;
	}

	/**
	 * Inserts the specified element at the specified position in this list. Shifts the element currently
	 * at that position (if any) and any subsequent elements to the right (adds one to their indices).
	 * A add delta is then sent using {@link DeltaBuilder} if noUpdates = false (false by default)
	 * @param index index at which the specified element is to be inserted
	 * @param e element to be inserted
	 */
	@Override
	public void add(int index, E e) {
		updateCount++;
		list.add(index, e);
		
		addObjectData(index, e);
	}

	/**
	 * Replaces the element at the specified position in this list with the specified element.
	 * A change delta is then sent using {@link DeltaBuilder} if noUpdates = false (false by default). Since this
	 * sends a change delta, it should only be used for replacing an element, not for adding one.
	 * @param index index of the element to replace
	 * @param element
	 * @return
	 */
	@Override
	public E set(int index, E element) {
		// Sends a "change" delta
		updateCount++;
		E previous = list.set(index, element);
		if (previous != null) {
			removeDataSize(index);
			removeData(index);
		}
		addObjectData(index, element, (byte) 2);
		return previous;
	}


	@Override
	public E remove(int index) {
		// Method is also called for remove(E element), just replaced by the index
		updateCount++;
		E element = list.remove(index);

		if (element != null)
			removeObjectData(index, element, (byte) 0);

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
	
	@Override
	public byte[] encode() {
		int size = list.size();

		if (size == 0) {
			return new byte[8];
		}

		if (size != data.size()) {
			// Data got out of sync with the list, so lets clean that up!
			clearAllData();
			for (int i = 0; i < size; i++) {
				addObjectData(i, list.get(i), (byte) 0);
			}
			clearDeltaQueue();
		}

		ByteBuffer buffer = ByteBuffer.allocate(8 + dataSize).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt(size);
		buffer.putInt(updateCount);

		for (byte[] bytes : data.values()) {
			buffer.put(bytes);
		}
		
		return buffer.array();
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
	
	public void clearDeltaQueue() {
		deltas.clear();
		deltaSize = 0;
	}

	private void clearAllData() {
		data.clear();
		dataSize = 0;
		clearDeltaQueue();
	}

	public void setUpdateCount(int count) {
		this.updateCount = count;
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
	
	private void addObjectData(int index, Object obj, byte update) {
		byte[] encodedData = Encoder.encode(obj, strType);
		
		data.put(index, encodedData);
		
		dataSize += encodedData.length;

		ByteBuffer buffer = ByteBuffer.allocate(encodedData.length + 2).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) index);
		buffer.put(encodedData);

		byte[] indexedBytes = buffer.array();
		createDeltaData(indexedBytes, update);
	}
	
	private void addObjectData(int index, Object obj) {
		addObjectData(index, obj, (byte) 1);
	}

	private void removeObjectData(int index, Object object, byte update) {
		if (data.get(index) != null) {
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
		data.remove(index);
	}

	public BaselineType getBaseline() { return baseline; }
	public int getViewType() { return view; }
	public int getUpdateType() { return updateType; }

	@Override
	public String toString() {
		return "SWGList[" + baseline + "0" + view + ":" + updateType + "]";
	}
}
