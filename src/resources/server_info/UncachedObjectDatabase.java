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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class UncachedObjectDatabase<V extends Serializable> extends ObjectDatabase<V> {
	
	private final Map<Long, Long> objectIndex;
	private final Map<Long, V> objects;
	private boolean loaded;
	
	public UncachedObjectDatabase(String filename) {
		super(filename);
		objectIndex = new HashMap<Long, Long>();
		objects = new HashMap<Long, V>();
		loaded = false;
		loadPointers();
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
		V val = null;
		synchronized (objects) {
			val = objects.get(key);
		}
		if (val == null)
			return getFromDB(key);
		return val;
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
		synchronized (objectIndex) {
			return objectIndex.size();
		}
	}
	
	public synchronized boolean contains(long key) {
		synchronized (objectIndex) {
			return objectIndex.containsKey(key);
		}
	}
	
	public synchronized boolean contains(String key) {
		return contains(hash(key));
	}
	
	public synchronized boolean save() {
		if (!loaded) {
			System.err.println("Not saving '" + getFile() + "', file not loaded yet!");
			return false;
		}
		DataOutputStream dos = null;
		try {
			File file = getFile();
			dos = new DataOutputStream(new FileOutputStream(file + ".tmp"));
			loadCachedAndSave(dos, loadUncachedAndSave(dos, 0));
			moveFile(file+".tmp", file.getAbsolutePath());
		} catch (IOException e) {
			System.err.println("UncachedObjectDatabase: Error while saving file. IOException: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			safeClose(dos);
		}
		return true;
	}
	
	public synchronized void clearCache() {
		synchronized (objects) {
			objects.clear();
		}
	}
	
	public synchronized boolean load() {
		return loadToCache();
	}
	
	public synchronized boolean loadToCache() {
		if (!fileExists())
			return false;
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(getFile()));
			synchronized (objects) {
				while (dis.available() >= 12) {
					long key = dis.readLong();
					int length = dis.readInt();
					if (!objects.containsKey(key)) {
						objects.put(key, readObject(dis));
					} else {
						if (dis.skip(length) != length)
							throw new IOException("Failed to skip " + length + " bytes");
					}
				}
			}
			dis.close();
		} catch (IOException | ClassNotFoundException | ClassCastException e) {
			System.err.println("UncachedObjectDatabase: Unable to load cache with error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
			return false;
		} finally {
			safeClose(dis);
		}
		return true;
	}
	
	public synchronized void traverse(Traverser<V> traverser) {
		DataInputStream dis = null;
		try {
			save();
			dis = new DataInputStream(new FileInputStream(getFile()));
			synchronized (objects) {
				while (dis.available() >= 12) {
					V v = objects.get(dis.readLong());
					int length = dis.readInt();
					if (v != null) {
						if (dis.skip(length) != length)
							throw new IOException("Failed to skip " + length + " bytes");
					} else
						v = readObject(dis);
					traverser.process(v);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("UncachedObjectDatabase: Unable to traverse with error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		safeClose(dis);
	}
	
	public synchronized void traverseCache(Traverser<V> traverser) {
		synchronized (objects) {
			for (V obj : objects.values())
				traverser.process(obj);
		}
	}
	
	private void moveFile(String source, String destination) throws IOException {
		moveFile(source, destination, 4096);
	}
	
	private void moveFile(String source, String destination, int bufferSize) throws IOException {
		File sFile = new File(source);
		File dFile = new File(destination);
		
		InputStream inStream = null;
		OutputStream outStream = null;
		try {
			inStream = new FileInputStream(sFile);
			outStream = new FileOutputStream(dFile);
			
			byte [] buffer = new byte[bufferSize];
			int length = 0;
			while ((length = inStream.read(buffer)) > 0){
				outStream.write(buffer, 0, length);
			}
		} finally {
			safeClose(inStream);
			safeClose(outStream);
		}
		if (!sFile.delete())
			System.err.println("UncachedObjectDatabase: Failed to delete source file when moving "+sFile+" -> "+dFile);
	}
	
	private V getFromDB(long key) {
		long index = getObjectIndex(key);
		if (index == -1)
			return null;
		V val = readObject(index);
		synchronized (objects) {
			objects.put(key, val);
		}
		return val;
	}
	
	private long getObjectIndex(long key) {
		synchronized (objectIndex) {
			Long index = objectIndex.get(key);
			if (index == null)
				return -1;
			return index;
		}
	}
	
	private V readObject(long index) {
		FileInputStream fis = null;
		V val = null;
		try {
			fis = new FileInputStream(getFile());
			if (fis.skip(index+12) != index+12)
				throw new IOException("Failed to skip " + (index+12) + " bytes");
			val = readObject(fis);
		} catch (IOException | ClassNotFoundException e) {
			val = null;
		} finally {
			safeClose(fis);
		}
		return val;
	}
	
	@SuppressWarnings("unchecked")
	private V readObject(InputStream is) throws ClassNotFoundException, IOException {
		return (V) new ObjectInputStream(is).readObject();
	}
	
	private byte [] writeObject(Object o) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new ObjectOutputStream(bos).writeObject(o);
		return bos.toByteArray();
	}
	
	private long writeObject(DataOutputStream dos, long key, byte [] data) throws IOException {
		dos.writeLong(key);
		dos.writeInt(data.length);
		dos.write(data);
		return 12 + data.length;
	}
	
	private long loadUncachedAndSave(DataOutputStream dos, long index) {
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(getFile()));
			while (dis.available() >= 12) {
				long key = dis.readLong();
				int length = dis.readInt();
				synchronized (objects) {
					if (objects.containsKey(key)) {
						if (dis.skip(length) != length)
							throw new IOException("Failed to skip " + length + " bytes");
					} else {
						byte [] data = new byte[length];
						int len = 0;
						while (len < length) {
							len += dis.read(data, len, length-len);
						}
						index += writeObject(dos, key, data);
					}
				}
			}
		} catch (IOException e) {
			
		} finally {
			safeClose(dis);
		}
		return index;
	}
	
	private long loadCachedAndSave(DataOutputStream dos, long index) throws IOException {
		synchronized (objectIndex) {
			synchronized (objects) {
				for (Entry <Long, V> e : objects.entrySet()) {
					long key = e.getKey().longValue();
					objectIndex.put(key, index);
					index += writeObject(dos, key, writeObject(e.getValue()));
				}
			}
		}
		return index;
	}
	
	private boolean loadPointers() {
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(getFile()));
			long index = 0;
			while (index != -1) {
				index = loadObject(dis, index);
			}
			dis.close();
			loaded = true;
		} catch (IOException e) {
			return false;
		} finally {
			safeClose(dis);
		}
		return true;
	}
	
	private long loadObject(DataInputStream dis, long index) throws IOException {
		if (dis.available() < 12)
			return -1;
		long key = dis.readLong();
		int length = dis.readInt();
		if (length < 0)
			return -1;
		if (dis.skip(length) != length)
			throw new IOException("Failed to skip " + length + " bytes");
		synchronized (objectIndex) {
			objectIndex.put(key, index);
		}
		return index + 12 + length;
	}
	
	private boolean safeClose(OutputStream os) {
		if (os == null)
			return false;
		try {
			os.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	private boolean safeClose(InputStream is) {
		if (is == null)
			return false;
		try {
			is.close();
		} catch (IOException e) {
			return false;
		}
		return true;
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