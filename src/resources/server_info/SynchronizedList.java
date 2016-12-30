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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class SynchronizedList<E> implements List<E> {
	
	private final List<E> list;
	
	public SynchronizedList() {
		this.list = new ArrayList<>();
	}
	
	public SynchronizedList(List<E> list) {
		this.list = list;
	}

	public synchronized void forEach(Consumer<? super E> action) {
		list.forEach(action);
	}

	public synchronized int size() {
		return list.size();
	}

	public synchronized boolean isEmpty() {
		return list.isEmpty();
	}

	public synchronized boolean contains(Object o) {
		return list.contains(o);
	}

	public synchronized Iterator<E> iterator() {
		return list.iterator();
	}

	public synchronized Object[] toArray() {
		return list.toArray();
	}

	public synchronized <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	public synchronized boolean add(E e) {
		return list.add(e);
	}

	public synchronized boolean remove(Object o) {
		return list.remove(o);
	}

	public synchronized boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public synchronized boolean addAll(Collection<? extends E> c) {
		return list.addAll(c);
	}

	public synchronized boolean addAll(int index, Collection<? extends E> c) {
		return list.addAll(index, c);
	}

	public synchronized boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	public synchronized boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	public synchronized void replaceAll(UnaryOperator<E> operator) {
		list.replaceAll(operator);
	}

	public synchronized boolean removeIf(Predicate<? super E> filter) {
		return list.removeIf(filter);
	}

	public synchronized void sort(Comparator<? super E> c) {
		list.sort(c);
	}

	public synchronized void clear() {
		list.clear();
	}

	public synchronized boolean equals(Object o) {
		return list.equals(o);
	}

	public synchronized int hashCode() {
		return list.hashCode();
	}

	public synchronized E get(int index) {
		return list.get(index);
	}

	public synchronized E set(int index, E element) {
		return list.set(index, element);
	}

	public synchronized void add(int index, E element) {
		list.add(index, element);
	}

	public synchronized Stream<E> stream() {
		return list.stream();
	}

	public synchronized E remove(int index) {
		return list.remove(index);
	}

	public synchronized Stream<E> parallelStream() {
		return list.parallelStream();
	}

	public synchronized int indexOf(Object o) {
		return list.indexOf(o);
	}

	public synchronized int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public synchronized ListIterator<E> listIterator() {
		return list.listIterator();
	}

	public synchronized ListIterator<E> listIterator(int index) {
		return list.listIterator(index);
	}

	public synchronized List<E> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	public synchronized Spliterator<E> spliterator() {
		return list.spliterator();
	}
	
}
