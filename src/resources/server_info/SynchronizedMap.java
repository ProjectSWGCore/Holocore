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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SynchronizedMap<K, V> implements Map<K, V> {
	
	private final Map<K, V> map;
	
	public SynchronizedMap() {
		this.map = new HashMap<>();
	}
	
	public SynchronizedMap(Map<K, V> map) {
		this.map = map;
	}
	
	public synchronized int hashCode() {
		return map.hashCode();
	}
	
	public synchronized String toString() {
		return map.toString();
	}
	
	public synchronized int size() {
		return map.size();
	}
	
	public synchronized boolean isEmpty() {
		return map.isEmpty();
	}
	
	public synchronized V get(Object key) {
		return map.get(key);
	}
	
	public synchronized boolean containsKey(Object key) {
		return map.containsKey(key);
	}
	
	public synchronized V put(K key, V value) {
		return map.put(key, value);
	}
	
	public synchronized void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}
	
	public synchronized V remove(Object key) {
		return map.remove(key);
	}
	
	public synchronized void clear() {
		map.clear();
	}
	
	public synchronized boolean containsValue(Object value) {
		return map.containsValue(value);
	}
	
	public synchronized Set<K> keySet() {
		return map.keySet();
	}
	
	public synchronized Collection<V> values() {
		return map.values();
	}
	
	public synchronized Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}
	
	public synchronized V replace(K key, V value) {
		return map.replace(key, value);
	}
	
}
