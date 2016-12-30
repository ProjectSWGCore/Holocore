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
package resources.server_info;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SynchronizedHashMap<K, V> implements Map<K, V> {
	
	private final HashMap<K, V> map;
	
	public SynchronizedHashMap() {
		map = new HashMap<>();
	}
	
	public int hashCode() {
		synchronized (map) {
			return map.hashCode();
		}
	}
	
	public String toString() {
		synchronized (map) {
			return map.toString();
		}
	}
	
	public int size() {
		synchronized (map) {
			return map.size();
		}
	}
	
	public boolean isEmpty() {
		synchronized (map) {
			return map.isEmpty();
		}
	}
	
	public V get(Object key) {
		synchronized (map) {
			return map.get(key);
		}
	}
	
	public boolean containsKey(Object key) {
		synchronized (map) {
			return map.containsKey(key);
		}
	}
	
	public V put(K key, V value) {
		synchronized (map) {
			return map.put(key, value);
		}
	}
	
	public void putAll(Map<? extends K, ? extends V> m) {
		synchronized (map) {
			map.putAll(m);
		}
	}
	
	public V remove(Object key) {
		synchronized (map) {
			return map.remove(key);
		}
	}
	
	public void clear() {
		synchronized (map) {
			map.clear();
		}
	}
	
	public boolean containsValue(Object value) {
		synchronized (map) {
			return map.containsValue(value);
		}
	}
	
	public Set<K> keySet() {
		synchronized (map) {
			return map.keySet();
		}
	}
	
	public Collection<V> values() {
		synchronized (map) {
			return map.values();
		}
	}
	
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		synchronized (map) {
			return map.entrySet();
		}
	}
	
	public V replace(K key, V value) {
		synchronized (map) {
			return map.replace(key, value);
		}
	}
	
}
