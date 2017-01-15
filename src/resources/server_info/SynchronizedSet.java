/************************************************************************************
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
 * it under the terms of the GNU Affero General public synchronized License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General public synchronized License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General public synchronized License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.server_info;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SynchronizedSet<E> implements Set<E> {
	
	private final Set<E> set;
	
	public SynchronizedSet() {
		this.set = new HashSet<>();
	}
	
	public SynchronizedSet(Set<E> set) {
		this.set = set;
	}
	
	public synchronized void forEach(Consumer<? super E> action) {
		set.forEach(action);
	}
	
	public synchronized int size() {
		return set.size();
	}
	
	public synchronized boolean isEmpty() {
		return set.isEmpty();
	}
	
	public synchronized boolean contains(Object o) {
		return set.contains(o);
	}
	
	public synchronized Iterator<E> iterator() {
		return set.iterator();
	}
	
	public synchronized Object[] toArray() {
		return set.toArray();
	}
	
	public synchronized <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}
	
	public synchronized boolean add(E e) {
		return set.add(e);
	}
	
	public synchronized boolean remove(Object o) {
		return set.remove(o);
	}
	
	public synchronized boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}
	
	public synchronized boolean addAll(Collection<? extends E> c) {
		return set.addAll(c);
	}
	
	public synchronized boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}
	
	public synchronized boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}
	
	public synchronized void clear() {
		set.clear();
	}
	
	public synchronized boolean equals(Object o) {
		return set.equals(o);
	}
	
	public synchronized int hashCode() {
		return set.hashCode();
	}
	
	public synchronized Spliterator<E> spliterator() {
		return set.spliterator();
	}
	
	public synchronized boolean removeIf(Predicate<? super E> filter) {
		return set.removeIf(filter);
	}
	
	public synchronized Stream<E> stream() {
		return set.stream();
	}
	
	public synchronized Stream<E> parallelStream() {
		return set.parallelStream();
	}
	
}
