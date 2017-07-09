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
package resources.objects.awareness;

import java.util.HashSet;
import java.util.Set;

import com.projectswg.common.callback.CallbackManager;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.waypoint.WaypointObject;

public class TerrainMap {
	
	private static final int CHUNK_COUNT_ACROSS = 16;
	private static final double MIN_X = -8192;
	private static final double MIN_Z = -8192;
	private static final double MAP_WIDTH = 16384;
	private static final double CHUNK_WIDTH = MAP_WIDTH / CHUNK_COUNT_ACROSS;
	
	private final CallbackManager<TerrainMapCallback> callbackManager;
	private final TerrainMapChunk [][] chunks;
	
	public TerrainMap(Terrain t) {
		callbackManager = new CallbackManager<>("terrain-map-"+t.name(), 1);
		chunks = new TerrainMapChunk[CHUNK_COUNT_ACROSS][CHUNK_COUNT_ACROSS];
		for (int z = 0; z < CHUNK_COUNT_ACROSS; z++) {
			for (int x = 0; x < CHUNK_COUNT_ACROSS; x++) {
				double chunkStartX = MIN_X+x*CHUNK_WIDTH;
				double chunkStartZ = MIN_Z+z*CHUNK_WIDTH;
				chunks[z][x] = new TerrainMapChunk(chunkStartX, chunkStartZ, chunkStartX+CHUNK_WIDTH, chunkStartZ+CHUNK_WIDTH);
			}
		}
	}
	
	public void start() {
		callbackManager.start();
	}
	
	public void stop() {
		callbackManager.stop();
	}
	
	public void setCallback(TerrainMapCallback callback) {
		callbackManager.setCallback(callback);
	}
	
	public boolean isCallbacksDone() {
		return callbackManager.isQueueEmpty();
	}
	
	public void moveWithinMap(SWGObject obj) {
		if (isInAwareness(obj)) {
			move(obj);
			update(obj);
			callbackManager.callOnEach((call) -> call.onMoveSuccess(obj));
		} else {
			callbackManager.callOnEach((call) -> call.onMoveFailure(obj));
		}
	}
	
	public void removeWithoutUpdate(SWGObject obj) {
		remove(obj);
	}
	
	public void removeFromMap(SWGObject obj) {
		if (remove(obj)) {
			update(obj);
			Assert.test(isInAwareness(obj));
		}
	}
	
	private void move(SWGObject obj) {
		TerrainMapChunk chunk = getChunk(obj.getX(), obj.getZ());
		TerrainMapChunk current = obj.getAwareness().setTerrainMapChunk(chunk);
		if (chunk == null) {
			Log.e("Null Chunk! Location: (%.3f, %.3f) Object: %s", obj.getX(), obj.getZ(), obj);
			return;
		}
		if (current == chunk)
			return; // Ignore if it doesn't change
		if (current != null)
			current.removeObject(obj);
		chunk.addObject(obj);
	}
	
	private boolean remove(SWGObject obj) {
		TerrainMapChunk chunk = obj.getAwareness().setTerrainMapChunk(null);
		if (chunk != null)
			chunk.removeObject(obj);
		return chunk != null;
	}
	
	private void update(SWGObject obj) {
		Set<SWGObject> prevAware = obj.getObjectsAware();
		Set<SWGObject> aware = getNearbyAware(obj);
		callbackManager.callOnEach(call -> {
			for (SWGObject n : aware) {
				if (!prevAware.contains(n))
					call.onWithinRange(obj, n);
			}
			for (SWGObject p : prevAware) {
				if (!aware.contains(p))
					call.onOutOfRange(obj, p);
			}
		});
	}
	
	private Set<SWGObject> getNearbyAware(SWGObject obj) {
		Set<SWGObject> aware = new HashSet<>();
		if (obj.getAwareness().getTerrainMapChunk() == null)
			return aware;
		int sX = calculateIndex(obj.getX())-1;
		int sZ = calculateIndex(obj.getZ())-1;
		for (int z = sZ; z <= sZ+2; ++z) {
			for (int x = sX; x <= sX+2; ++x) {
				getWithinAwareness(x, z, obj, aware);
			}
		}
		return aware;
	}
	
	private void getWithinAwareness(int x, int z, SWGObject obj, Set<SWGObject> aware) {
		if (x < 0 || z < 0 || x >= CHUNK_COUNT_ACROSS || z >= CHUNK_COUNT_ACROSS)
			return;
		chunks[z][x].getWithinAwareness(obj, aware);
	}
	
	private boolean isInAwareness(SWGObject obj) {
		if (obj.getParent() != null)
			return false;
		if (obj instanceof WaypointObject)
			return false;
		if (!(obj instanceof CreatureObject))
			return true;
		return ((CreatureObject) obj).isLoggedInPlayer() || !((CreatureObject) obj).isPlayer();
	}
	
	private TerrainMapChunk getChunk(double x, double z) {
		int xInd = calculateIndex(x);
		int zInd = calculateIndex(z);
		if (xInd < 0 || zInd < 0 || xInd >= CHUNK_COUNT_ACROSS || zInd >= CHUNK_COUNT_ACROSS)
			return null;
		return chunks[zInd][xInd];
	}
	
	private int calculateIndex(double x) {
		return (int) ((x+8192)/16384*CHUNK_COUNT_ACROSS);
	}
	
	public interface TerrainMapCallback {
		void onWithinRange(SWGObject obj, SWGObject inRange);
		void onOutOfRange(SWGObject obj, SWGObject outRange);
		void onMoveSuccess(SWGObject obj);
		void onMoveFailure(SWGObject obj);
	}
	
}
