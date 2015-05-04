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
package resources.server_info;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CachedObjectDatabase<V extends Serializable> extends ObjectDatabase<V> {
	
	private final Map <Long, V> objects;
	
	public CachedObjectDatabase(String filename) {
		super(filename);
		objects = new HashMap<Long, V>();
	}
	
	public synchronized V put(String key, V value) {
		return put(hash(key), value);
	}
	
	public synchronized V put(long key, V value) {
		synchronized (objects) {
			return objects.put(key, value);
		}
	}
	
	public synchronized V get(String key) {
		return get(hash(key));
	}
	
	public synchronized V get(long key) {
		synchronized (objects) {
			return objects.get(key);
		}
	}
	
	public synchronized V remove(String key) {
		return get(hash(key));
	}
	
	public synchronized V remove(long key) {
		synchronized (objects) {
			return objects.remove(key);
		}
	}
	
	public synchronized int size() {
		synchronized (objects) {
			return objects.size();
		}
	}
	
	public synchronized boolean contains(long key) {
		synchronized (objects) {
			return objects.containsKey(key);
		}
	}
	
	public synchronized boolean contains(String key) {
		return contains(hash(key));
	}
	
	public synchronized boolean save() {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(getFile()));
			synchronized (objects) {
				for (Entry <Long, V> e : objects.entrySet()) {
					oos.writeLong(e.getKey());
					oos.writeObject(e.getValue());
				}
			}
			oos.close();
		} catch (IOException e) {
			System.err.println("CachedObjectDatabase: Error while saving file. IOException: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (Exception e) {
					System.err.println("CachedObjectDatabase: Failed to close stream while saving! " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	public synchronized void clearCache() {
		synchronized (objects) {
			objects.clear();
		}
	}
	
	public synchronized boolean load() {
		if (!fileExists())
			return false;
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(getFile()));
			synchronized (objects) {
				while (ois.available() >= 8) {
					long key = ois.readLong();
					@SuppressWarnings("unchecked")
					V val = (V) ois.readObject();
					objects.put(key, val);
				}
			}
		} catch (EOFException e) {
			
		} catch (IOException | ClassNotFoundException | ClassCastException e) {
			System.err.println("CachedObjectDatabase: Unable to load with error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
			return false;
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (Exception e) {
					System.err.println("CachedObjectDatabase: Failed to close stream when loading! " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	public synchronized void traverse(Traverser<V> traverser) {
		synchronized (objects) {
			for (V obj : objects.values())
				traverser.process(obj);
		}
	}
	
	private static final long hash(String string) {
		long h = 1125899906842597L;
		int len = string.length();
		
		for (int i = 0; i < len; i++) {
			h = 31*h + string.charAt(i);
		}
		return h;
	}
	
}
