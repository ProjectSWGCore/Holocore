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
package com.projectswg.holocore.resources.objects.awareness;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.player.PlayerState;

class Aware {
	
	private final SWGObject object;
	private final List<Aware> awareness;
	private final AtomicReference<Aware> parent;
	
	public Aware(SWGObject obj) {
		this.object = Objects.requireNonNull(obj, "Object cannot be null!");
		this.awareness = new ReadWriteArrayList<>(128, false);
		this.parent = new AtomicReference<>(null);
	}
	
	public SWGObject getObject() {
		return object;
	}
	
	public void setParent(Aware parent) {
		this.parent.set(parent);
	}
	
	public void add(Aware a) {
		awareness.add(a);
		a.awareness.add(this);
	}
	
	public void remove(Aware a) {
		awareness.remove(a);
		a.awareness.remove(this);
	}
	
	public boolean contains(SWGObject obj) {
		AtomicBoolean contained = new AtomicBoolean(false);
		awareness.forEach(a -> {
			if (a.getObject().equals(obj))
				contained.set(true);
		});
		return contained.get();
	}
	
	public void clear() {
		for (Aware a : new ArrayList<>(awareness))
			remove(a);
	}
	
	public Set<SWGObject> getAware() {
		Set<SWGObject> aware = new HashSet<>(awareness.size());
		getAware(aware);
		return aware;
	}
	
	public void getAware(Collection<SWGObject> aware) {
		awareness.forEach(a -> aware.add(a.getObject()));
		Aware parent = getParent();
		if (parent != null)
			parent.getAware(aware);
	}
	
	public Set<Player> getObservers() {
		Set<Player> observers = new HashSet<>();
		getObservers(observers);
		return observers;
	}
	
	public void getObservers(Set<Player> observers) {
		Player owner = object.getOwner();
		Aware superParent = getSuperParent();
		if (superParent == null)
			getObservers(observers, owner, getObject());
		else
			superParent.getObservers(observers, owner, getObject());
	}
	
	private Aware getParent() {
		return parent.get();
	}
	
	private Aware getSuperParent() {
		Aware tmpParent = getParent();
		Aware ret = tmpParent;
		if (tmpParent == null)
			return null;
		while ((tmpParent = tmpParent.getParent()) != null) {
			ret = tmpParent;
		}
		return ret;
	}
	
	private void getObservers(Set<Player> observers, Player owner, SWGObject original) {
		addObserversToSet(getObject().getContainedObjects(), observers, owner, original);
		addObserversToSet(getObject().getSlottedObjects(), observers, owner, original);
		addObserversToSet(getAware(), observers, owner, original);
	}
	
	public static void addObserversToSet(Collection<SWGObject> nearby, Collection<Player> observers, Player owner, SWGObject original) {
		for (SWGObject aware : nearby) {
			if (!aware.isVisible(original))
				continue;
			if (isObserver(aware, owner, original))
				observers.add(aware.getOwnerShallow());
			else
				addObserversToSet(aware.getContainedObjects(), observers, owner, original);
		}
	}
	
	public static boolean isObserver(SWGObject aware, Player owner, SWGObject original) {
		if (!(aware instanceof CreatureObject))
			return false;
		Player awareOwner = aware.getOwner();
		if (awareOwner == null || awareOwner.equals(owner))
			return false;
		if (awareOwner.getPlayerState() != PlayerState.ZONED_IN)
			return false;
		return ((CreatureObject) aware).isLoggedInPlayer();
	}
	
	private static class ReadWriteArrayList<T> implements List<T> {
		
		private final ArrayList<T> list;
		private final ReadWriteLock lock;
		
		public ReadWriteArrayList() {
			this(true);
		}
		
		public ReadWriteArrayList(boolean fair) {
			this(16, fair);
		}
		
		public ReadWriteArrayList(int capacity) {
			this(capacity, true);
		}
		
		public ReadWriteArrayList(int capacity, boolean fair) {
			this.list = new ArrayList<>(capacity);
			this.lock = new ReentrantReadWriteLock(fair);
		}
		
		public void trimToSize() {
			lock.writeLock().lock();
			try {
				list.trimToSize();
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public void ensureCapacity(int minCapacity) {
			lock.writeLock().lock();
			try {
				list.ensureCapacity(minCapacity);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		@Override
		public int size() {
			return list.size();
		}
		
		@Override
		public boolean isEmpty() {
			return list.isEmpty();
		}
		
		@Override
		public boolean contains(Object o) {
			lock.readLock().lock();
			try {
				return list.contains(o);
			} finally {
				lock.readLock().unlock();
			}
		}
		
		@Override
		public boolean containsAll(Collection<?> c) {
			lock.readLock().lock();
			try {
				return list.containsAll(c);
			} finally {
				lock.readLock().unlock();
			}
		}
		
		@Override
		public int indexOf(Object o) {
			lock.readLock().lock();
			try {
				return list.indexOf(o);
			} finally {
				lock.readLock().unlock();
			}
		}
		
		@Override
		public int lastIndexOf(Object o) {
			lock.readLock().lock();
			try {
				return list.lastIndexOf(o);
			} finally {
				lock.readLock().unlock();
			}
		}
		
		@Override
		public Object clone() {
			lock.readLock().lock();
			try {
				return list.clone();
			} finally {
				lock.readLock().unlock();
			}
		}
		
		@Override
		public Object[] toArray() {
			lock.readLock().lock();
			try {
				return list.toArray();
			} finally {
				lock.readLock().unlock();
			}
		}
		
		public <T1> T1[] toArray(T1[] a) {
			lock.readLock().lock();
			try {
				return list.toArray(a);
			} finally {
				lock.readLock().unlock();
			}
		}
		
		@Override
		public T get(int index) {
			lock.readLock().lock();
			try {
				return list.get(index);
			} finally {
				lock.readLock().unlock();
			}
		}
		
		public T set(int index, T element) {
			lock.writeLock().lock();
			try {
				return list.set(index, element);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public boolean add(T t) {
			lock.writeLock().lock();
			try {
				return list.add(t);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public void add(int index, T element) {
			lock.writeLock().lock();
			try {
				list.add(index, element);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		@Override
		public T remove(int index) {
			lock.writeLock().lock();
			try {
				return list.remove(index);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		@Override
		public boolean remove(Object o) {
			lock.writeLock().lock();
			try {
				return list.remove(o);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		@Override
		public void clear() {
			lock.writeLock().lock();
			try {
				list.clear();
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public boolean addAll(Collection<? extends T> c) {
			lock.writeLock().lock();
			try {
				return list.addAll(c);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public boolean addAll(int index, Collection<? extends T> c) {
			lock.writeLock().lock();
			try {
				return list.addAll(index, c);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public boolean removeAll(Collection<?> c) {
			lock.writeLock().lock();
			try {
				return list.removeAll(c);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public boolean retainAll(Collection<?> c) {
			lock.writeLock().lock();
			try {
				return list.retainAll(c);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		@Override
		public ListIterator<T> listIterator(int index) {
			return list.listIterator(index);
		}
		
		@Override
		public ListIterator<T> listIterator() {
			return list.listIterator();
		}
		
		@Override
		public Iterator<T> iterator() {
			return list.iterator();
		}
		
		@Override
		public List<T> subList(int fromIndex, int toIndex) {
			lock.readLock().lock();
			try {
				return list.subList(fromIndex, toIndex);
			} finally {
				lock.readLock().unlock();
			}
		}
		
		public void forEach(Consumer<? super T> action) {
			lock.readLock().lock();
			try {
				list.forEach(action);
			} finally {
				lock.readLock().unlock();
			}
		}
		
		@Override
		public Spliterator<T> spliterator() {
			return list.spliterator();
		}
		
		public boolean removeIf(Predicate<? super T> filter) {
			lock.writeLock().lock();
			try {
				return list.removeIf(filter);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public void replaceAll(UnaryOperator<T> operator) {
			lock.writeLock().lock();
			try {
				list.replaceAll(operator);
			} finally {
				lock.writeLock().unlock();
			}
		}
		
		public void sort(Comparator<? super T> c) {
			lock.writeLock().lock();
			try {
				list.sort(c);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
}
