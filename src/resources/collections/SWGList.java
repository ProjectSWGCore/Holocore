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
import resources.network.NetBuffer;
import resources.objects.SWGObject;
import resources.server_info.Log;
import resources.server_info.SynchronizedList;
import utilities.Encoder;
import utilities.Encoder.StringType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supports a list of elements which automatically sends data as a delta when changed for baselines.
 * 
 * @param <E> Element that implements {@link Encodable} in order for data to be sent, or a basic
 *            type.
 */
public class SWGList<E> extends SynchronizedList<E> implements Encodable {
	
	private final StringType strType;
	private final int view;
	private final int updateType;
	private final AtomicInteger updateCount;
	private final List<byte[]> deltas;
	private final List<byte[]> data;
	
	private int deltaSize;
	private int dataSize;
	
	/**
	 * Creates a new {@link SWGList} for the defined baseline with the given view and update. Note
	 * that this is an extension of {@link AbstractList} and makes use of
	 * {@link java.util.ArrayList}
	 * 
	 * @param baseline {@link BaselineType} for this list, should be the same as the parent class
	 *            this list resides in
	 * @param view The baseline number this list resides in
	 * @param updateType The update variable used for sending a delta, it's the operand count that
	 *            this list resides at within the baseline
	 */
	public SWGList(int view, int updateType) {
		this(view, updateType, StringType.UNSPECIFIED);
	}
	
	/**
	 * Creates a new {@link SWGList} with the given StringType to encode in. Note that this
	 * constructor must be used if the elements within the list is a String.
	 * 
	 * @param baseline {@link BaselineType} for this list, should be the same as the parent class
	 *            this list resides in
	 * @param view The baseline number this list resides in
	 * @param strType The {@link StringType} of the string, required only if the element in the list
	 *            is a String as it's used for encoding either Unicode or ASCII characters
	 */
	public SWGList(int view, int updateType, StringType strType) {
		this.view = view;
		this.updateType = updateType;
		this.strType = strType;
		this.dataSize = 0;
		this.updateCount = new AtomicInteger(0);
		this.deltas = new SynchronizedList<>(new LinkedList<>());
		this.data = new SynchronizedList<>();
		this.deltaSize = 0;
	}
	
	public void resetUpdateCount() {
		updateCount.set(0);
	}
	
	@Override
	public boolean add(E e) {
		add(size(), e);
		return get(size() - 1).equals(e);
	}
	
	@Override
	public void add(int index, E e) {
		super.add(index, e);
		updateCount.incrementAndGet();
		addObjectData(index, e, (byte) 1);
	}
	
	@Override
	public E set(int index, E element) {
		// Sends a "change" delta
		E previous = super.set(index, element);
		if (previous != null) {
			removeData(index);
		}
		updateCount.incrementAndGet();
		addObjectData(index, element, (byte) 2);
		return previous;
	}
	
	@Override
	public boolean remove(Object o) {
		int index = indexOf(o);
		if (index != -1) {
			remove(index);
			return true;
		}
		return false;
	}
	
	@Override
	public E remove(int index) {
		E element = super.remove(index);
		if (element != null) {
			updateCount.incrementAndGet();
			removeObjectData(index);
		}
		
		return element;
	}
	
	@Override
	public E get(int index) {
		return super.get(index);
	}
	
	/**
	 * Creates an array of bytes based off of the elements within this Elements that are not of a
	 * standard type handled by {@link Encoder} should implement the {@link Encodable} interface.
	 * 
	 * @return Array of bytes with the size, update count, and encoded elements
	 */
	@Override
	public byte[] encode() {
		ByteBuffer buffer;
		synchronized (data) {
			if (dataSize == 0)
				return new byte[8];
			buffer = ByteBuffer.allocate(8 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
			
			buffer.putInt(data.size());
			buffer.putInt(updateCount.get());
			data.forEach(buffer::put);
		}
		
		return buffer.array();
	}
	
	@Override
	public void decode(ByteBuffer data) {
		// We need specific type information
		throw new UnsupportedOperationException("Use decode(ByteBuffer data, Class<E> elementType) instead");
	}
	
	public void decode(ByteBuffer data, StringType type) {
		int size = Packet.getInt(data);
		updateCount.set(Packet.getInt(data));
		NetBuffer buffer = NetBuffer.wrap(data);
		for (int i = 0; i < size; i++) {
			@SuppressWarnings("unchecked")
			E obj = (E) buffer.getString(type);
			add(obj);
		}
		clearDeltaQueue();
	}
	
	public void decode(ByteBuffer data, Class<E> elementType) {
		int size = Packet.getInt(data);
		updateCount.set(Packet.getInt(data));
		
		boolean encodable = Encodable.class.isAssignableFrom(elementType);
		NetBuffer wrap = NetBuffer.wrap(data);
		for (int i = 0; i < size; i++) {
			if (!decodeElement(wrap, elementType, encodable))
				break;
		}
		clearDeltaQueue();
	}
	
	private boolean decodeElement(NetBuffer wrap, Class<E> elementType, boolean encodable) {
		if (encodable) {
			try {
				E instance = elementType.newInstance();
				if (instance instanceof Encodable) {
					((Encodable) instance).decode(wrap.getBuffer());
					add(instance);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				Log.e("SWGList", e);
				return false;
			}
		} else {
			Object o = wrap.getGeneric(elementType);
			if (o != null && elementType.isAssignableFrom(o.getClass())) {
				// Shouldn't be possible to get an exception with the isAssignableFrom check
				@SuppressWarnings("unchecked")
				E obj = (E) o;
				add(obj);
			} else
				return false;
		}
		return true;
	}
	
	public void sendRefreshedListData(SWGObject target) {
		clearDeltaQueue();
		ByteBuffer buffer;
		synchronized (data) {
			updateCount.addAndGet(data.size());
			
			buffer = ByteBuffer.allocate(11 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(data.size() + 1);
			buffer.putInt(updateCount.get());
			buffer.put((byte) 3);
			buffer.putShort((short) data.size());
			for (byte[] bytes : data) {
				buffer.put(bytes);
			}
		}
		
		target.sendDelta(view, updateType, buffer.array());
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
			for (byte[] data : deltas) {
				buffer.put(data);
			}
		}
		
		return buffer.array();
	}
	
	private void createDeltaData(byte[] delta) {
		synchronized (deltas) {
			deltaSize += delta.length;
			deltas.add(delta);
		}
	}
	
	private void addObjectData(int index, E obj, byte update) {
		byte[] encodedData = Encoder.encode(obj, strType);
		if (encodedData == null) {
			Log.e(toString(), "Tried to encode an object that could not be encoded properly. Object: " + obj);
			return;
		}
		
		synchronized (data) {
			dataSize += encodedData.length;
			data.add(index, encodedData);
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(encodedData.length + 3).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(update);
		buffer.putShort((short) index);
		buffer.put(encodedData);
		createDeltaData(buffer.array());
	}
	
	private void removeObjectData(int index) {
		removeData(index);
		
		// Only the index is sent for removing data
		ByteBuffer buffer = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) 0);
		buffer.putShort((short) index);
		createDeltaData(buffer.array());
	}
	
	private void removeData(int index) {
		synchronized (data) {
			dataSize -= data.remove(index).length;
		}
	}
	
	@Override
	public String toString() {
		return "SWGList[0" + view + ":" + updateType + "]";
	}
}
