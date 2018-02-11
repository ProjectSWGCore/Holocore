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
package com.projectswg.holocore.resources.server_info;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.projectswg.common.debug.Log;
import com.projectswg.common.persistable.InputPersistenceStream;
import com.projectswg.common.persistable.InputPersistenceStream.PersistableCreator;
import com.projectswg.common.persistable.OutputPersistenceStream;
import com.projectswg.common.persistable.OutputPersistenceStream.PersistableSaver;
import com.projectswg.common.persistable.Persistable;

public class CachedObjectDatabase<V extends Persistable> extends ObjectDatabase<V> {
	
	private final PersistableCreator<V> creator;
	private final PersistableSaver<V> saver;
	private final Set <V> objects;
	private boolean loaded;
	
	public CachedObjectDatabase(String filename, PersistableCreator<V> creator, PersistableSaver<V> saver) {
		super(filename);
		this.creator = creator;
		this.saver = saver;
		objects = new HashSet<>();
		loaded = false;
	}
	
	public synchronized boolean add(V value) {
		synchronized (objects) {
			return objects.add(value);
		}
	}
	
	public synchronized boolean remove(V obj) {
		synchronized (objects) {
			return objects.remove(obj);
		}
	}
	
	public synchronized int size() {
		synchronized (objects) {
			return objects.size();
		}
	}
	
	public synchronized boolean contains(V obj) {
		synchronized (objects) {
			return objects.contains(obj);
		}
	}
	
	public synchronized boolean save() {
		if (!loaded) {
			Log.e("Not saving '" + getFile() + "', file not loaded yet!");
			return false;
		}
		try (OutputPersistenceStream os = new OutputPersistenceStream(new FileOutputStream(getFile()))) {
			synchronized (objects) {
				for (V obj : objects)
					os.write(obj, saver);
			}
		} catch (IOException e) {
			Log.e("Error while saving file. IOException: " + e.getMessage());
			Log.e(e);
			return false;
		}
		return true;
	}
	
	public synchronized boolean load() {
		if (!fileExists()) {
			Log.e("load() - file '%s' does not exist!", getFile());
			loaded = true;
			return false;
		}
		try (InputPersistenceStream is = new InputPersistenceStream(new FileInputStream(getFile()))) {
			synchronized (objects) {
				while (is.available() > 0)
					objects.add(is.read(creator));
				loaded = true;
			}
			if (is.available() > 0) {
				loaded = false;
				clearObjects();
				return false;
			}
		} catch (Exception e) {
			Log.e("Error while loading file. %s: %s", e.getClass().getSimpleName(), e.getMessage());
			Log.e(e);
			clearObjects();
			return false;
		}
		return true;
	}
	
	public synchronized void clearObjects() {
		synchronized (objects) {
			objects.clear();
		}
	}
	
	public synchronized void traverse(Traverser<V> traverser) {
		synchronized (objects) {
			for (V obj : objects)
				traverser.process(obj);
		}
	}
	
	public synchronized void traverseInterruptable(InterruptableTraverser<V> traverser) {
		synchronized (objects) {
			for (V obj : objects) {
				if (!traverser.process(obj))
					return;
			}
		}
	}
	
}
