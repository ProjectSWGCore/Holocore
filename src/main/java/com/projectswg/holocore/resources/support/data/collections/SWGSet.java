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
package com.projectswg.holocore.resources.support.data.collections;

import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.encoding.Encoder;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class SWGSet<E> extends CopyOnWriteArraySet<E> implements Encodable {
	
	private static final long serialVersionUID = 1L;
	
	private final int view;
	private final int updateType;
	private final StringType strType;
	private final AtomicInteger updateCount;
	private final List<byte[]> deltas;
	private final Set<ByteBuffer> data;
	
	private int deltaSize;
	private int dataSize;
	
	/**
	 * Creates a new {@link SWGSet} for the defined baseline with the given view and update. Note
	 * that this is an extension of {@link AbstractSet} and makes use of {@link HashSet}
	 * 
	 * @param view The baseline number this list resides in
	 * @param updateType The update variable used for sending a delta, it's the operand count that
	 *            this list resides at within the baseline
	 */
	public SWGSet(int view, int updateType) {
		this(view, updateType, StringType.UNSPECIFIED);
	}
	
	/**
	 * Creates a new {@link SWGSet} with the given StringType to encode in. Note that this
	 * constructor must be used if the elements within the list is a String.
	 * 
	 * @param view The baseline number this set resides in
	 * @param updateType The update number for this variable
	 * @param strType The {@link StringType} of the string, required only if the element in the set
	 *            is a String as it's used for encoding either Unicode or ASCII characters
	 */
	public SWGSet(int view, int updateType, StringType strType) {
		this.view = view;
		this.updateType = updateType;
		this.strType = strType;
		this.dataSize = 0;
		this.updateCount = new AtomicInteger(0);
		this.deltas = new LinkedList<>();
		this.data = new HashSet<>();
		this.deltaSize = 0;
	}
	
	public void resetUpdateCount() {
		updateCount.set(0);
	}
	
	@Override
	public boolean add(E e) {
		if (!super.add(e))
			return false;
		updateCount.incrementAndGet();
		addObjectData(e, (byte) 1);
		return true;
	}
	
	@Override
	public boolean remove(Object o) {
		if (!super.remove(o))
			return false;
		updateCount.incrementAndGet();
		removeObjectData(o, (byte) 0);
		return true;
	}
	
	@Override
	public void clear() {
		super.clear();
		updateCount.incrementAndGet();
		clearAllObjectData();
	}
	
	public void sendDeltaMessage(SWGObject target) {
		if (deltas.size() == 0)
			return;
		
		target.sendDelta(view, updateType, getDeltaData());
		// Clear the queue since the delta has been sent to observers through the builder
		clearDeltaQueue();
	}
	
	private void addObjectData(E obj, byte update) {
		byte[] encodedData = Encoder.encode(obj, strType);
		if (encodedData == null) {
			Log.e("Tried to encode an object that could not be encoded properly. Object: " + obj);
			return;
		}
		
		synchronized (data) {
			dataSize += encodedData.length;
			data.add(ByteBuffer.wrap(encodedData));
		}
		createDeltaData(encodedData, update);
	}
	
	private void removeObjectData(Object obj, byte update) {
		byte[] encodedData = Encoder.encode(obj, strType);
		if (encodedData == null) {
			Log.e("Tried to encode an object that could not be encoded properly. Object: " + obj);
			return;
		}
		
		synchronized (data) {
			dataSize -= encodedData.length;
			data.remove(ByteBuffer.wrap(encodedData));
		}
		
		createDeltaData(encodedData, update);
	}
	
	private void clearAllObjectData() {
		clearAllData();
		synchronized (deltas) {
			deltaSize = 1;
			deltas.add(new byte[] {(byte) 2});
		}
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
	
	private void createDeltaData(byte[] delta, byte update) {
		byte[] combinedUpdate = new byte[delta.length + 1];
		combinedUpdate[0] = update;
		System.arraycopy(delta, 0, combinedUpdate, 1, delta.length);
		synchronized (deltas) {
			deltaSize += delta.length + 1;
			deltas.add(combinedUpdate);
		}
	}
	
	private void clearAllData() {
		synchronized (data) {
			dataSize = 0;
			data.clear();
		}
		
		clearDeltaQueue();
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer;
		synchronized (data) {
			if (dataSize == 0)
				return new byte[8];
			buffer = ByteBuffer.allocate(getLength()).order(ByteOrder.LITTLE_ENDIAN);
			
			buffer.putInt(data.size());
			buffer.putInt(updateCount.get());
			data.forEach(storedBuffer -> {
				buffer.put(storedBuffer);
				storedBuffer.flip();
				});
		}
		
		return buffer.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		throw new UnsupportedOperationException("Use decode(ByteBuffer data, Class<E> elementType) instead");
	}
	
	@Override
	public int getLength() {
		return 8 + dataSize;
	}
	
	public void decode(ByteBuffer data, StringType type) {
		int size = data.getInt();
		updateCount.set(data.getInt());
		NetBuffer buffer = NetBuffer.wrap(data);
		for (int i = 0; i < size; i++) {
			@SuppressWarnings("unchecked")
			E obj = (E) buffer.getString(type);
			add(obj);
		}
		clearDeltaQueue();
	}
	
	public void decode(ByteBuffer data, Class<E> elementType) {
		int size = data.getInt();
		updateCount.set(data.getInt());
		
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
				E instance = elementType.getConstructor().newInstance();
				if (instance instanceof Encodable) {
					((Encodable) instance).decode(wrap);
					add(instance);
				}
			} catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
				Log.e(e);
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
	
	public static SWGSet<String> getSwgSet(NetBuffer buffer, int num, int var, StringType type) {
		SWGSet<String> set = new SWGSet<>(num, var, type);
		set.decode(buffer.getBuffer(), type);
		return set;
	}
	
	public static <T> SWGSet<T> getSwgSet(NetBuffer buffer, int num, int var, Class<T> c) {
		SWGSet<T> set = new SWGSet<>(num, var);
		set.decode(buffer.getBuffer(), c);
		return set;
	}
	
}
