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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import utilities.ThreadUtilities;

public abstract class ObjectDatabase<V extends Serializable> {
	
	private final File file;
	private final ScheduledExecutorService autosaveService;
	
	public ObjectDatabase(String filename) {
		this(filename, TimeUnit.MINUTES.toMillis(5));
	}
	
	public ObjectDatabase (String filename, long autosaveInterval, TimeUnit timeUnit) {
		this(filename, timeUnit.toMillis(autosaveInterval));
	}
	
	public ObjectDatabase(String filename, long autosaveInterval) {
		// Final variables
		this.file = new File(filename);
		if (autosaveInterval < 60000)
			autosaveInterval = 60000;
		this.autosaveService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("odb-autosave-"+file.getName()));
		// Setup
		autosaveService.scheduleAtFixedRate(() -> save(), autosaveInterval, autosaveInterval, TimeUnit.MILLISECONDS);
		try {
			createFilesAndDirectories();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createFilesAndDirectories() throws IOException {
		if (file.exists())
			return;
		String parentName = file.getParent();
		if (parentName != null && !parentName.isEmpty()) {
			File parent = new File(file.getParent());
			if (!parent.exists() && !parent.mkdirs())
				System.err.println(getClass().getSimpleName() + ": Failed to create parent directories for ODB: " + file.getCanonicalPath());
		}
		try {
			if (!file.createNewFile())
				System.err.println(getClass().getSimpleName() + ": Failed to create new ODB: " + file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		save();
		autosaveService.shutdownNow();
	}
	
	public final File getFile() {
		return file;
	}
	
	public final String getFilename() {
		return file.getPath();
	}
	
	public final boolean fileExists() {
		return file.isFile();
	}
	
	public abstract V put(String key, V value);
	public abstract V put(long key, V value);
	public abstract V get(String key);
	public abstract V get(long key);
	public abstract V remove(String key);
	public abstract V remove(long key);
	public abstract int size();
	public abstract boolean contains(long key);
	public abstract boolean contains(String key);
	public abstract boolean load();
	public abstract boolean save();
	public abstract void traverse(Traverser<V> traverser);
	
	public interface Traverser<V> {
		public void process(V element);
	}
	
}
