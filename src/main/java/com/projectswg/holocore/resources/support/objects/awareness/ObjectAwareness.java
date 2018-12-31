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

package com.projectswg.holocore.resources.support.objects.awareness;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.concurrency.ThreadPool;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class ObjectAwareness {
	
	private final TerrainMap[] terrains;
	private final ThreadPool threadPool;
	
	public ObjectAwareness() {
		this.terrains = new TerrainMap[Terrain.values().length];
		this.threadPool = new ThreadPool(terrains.length/2, "object-awareness-%d");
		for (int i = 0; i < terrains.length; i++) {
			terrains[i] = new TerrainMap();
		}
	}
	
	public void startThreadPool() {
		threadPool.start();
	}
	
	public boolean stopThreadPool() {
		threadPool.stop(false);
		return threadPool.awaitTermination(500);
	}
	
	/**
	 * Called when an object was created
	 *
	 * @param obj the object created
	 */
	public void createObject(@NotNull SWGObject obj) {
		terrains[obj.getTerrain().ordinal()].add(obj);
	}
	
	/**
	 * Called when an object is destroyed
	 *
	 * @param obj the object destroyed
	 */
	public void destroyObject(@NotNull SWGObject obj) {
		terrains[obj.getTerrain().ordinal()].remove(obj);
	}
	
	/**
	 * Called when an object needs an update
	 *
	 * @param obj the object to update
	 */
	public void updateObject(@NotNull SWGObject obj) {
		terrains[obj.getTerrain().ordinal()].move(obj);
	}
	
	/**
	 * Updates all affected chunks
	 */
	public void updateChunks() {
		if (!threadPool.isRunning()) {
			Arrays.stream(terrains).parallel().forEach(TerrainMap::updateChunks);
		} else {
			Semaphore semaphore = new Semaphore(0);
			for (TerrainMap terrain : terrains) {
				threadPool.execute(() -> {
					try {
						terrain.updateChunks();
					} finally {
						semaphore.release();
					}
				});
			}
			try {
				semaphore.acquire(terrains.length);
			} catch (InterruptedException e) {
				Log.w("ObjectAwareness interrupted while waiting for chunk updates");
			}
		}
	}
	
}
