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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import resources.Location;
import resources.Terrain;
import resources.callback.CallbackManager;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.waypoint.WaypointObject;
import resources.server_info.Log;
import utilities.AwarenessUtilities;

public class TerrainMap {
	
	private static final int CHUNK_COUNT_ACROSS = 16;
	private static final double MIN_X = -8192;
	private static final double MIN_Z = -8192;
	private static final double MAP_WIDTH = 16384;
	private static final double CHUNK_WIDTH = MAP_WIDTH / CHUNK_COUNT_ACROSS;
	
	private final CallbackManager<TerrainMapCallback> callbackManager;
	private final TerrainMapChunk [][] chunks;
	private final Map<Long, TerrainMapChunk> objectChunk;
	
	public TerrainMap(Terrain t) {
		callbackManager = new CallbackManager<>("terrain-map-"+t.name(), 1);
		chunks = new TerrainMapChunk[CHUNK_COUNT_ACROSS][CHUNK_COUNT_ACROSS];
		objectChunk = new HashMap<>();
		for (int z = 0; z < chunks.length; z++) {
			for (int x = 0; x < chunks[z].length; x++) {
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
	
	public void moveWithinMap(SWGObject obj, Location loc) {
		obj.setLocation(loc);
		if (isInAwareness(obj)) {
			move(obj);
			update(obj);
			callbackManager.callOnEach((call) -> call.onMoveSuccess(obj));
		} else {
			callbackManager.callOnEach((call) -> call.onMoveFailure(obj));
		}
	}
	
	public void moveToParent(SWGObject obj, SWGObject parent) {
		obj.resetAwareness();
	}
	
	public void removeWithoutUpdate(SWGObject obj) {
		remove(obj);
	}
	
	public void removeFromMap(SWGObject obj) {
		remove(obj);
		update(obj);
	}
	
	private void move(SWGObject obj) {
		TerrainMapChunk chunk = objectChunk.get(obj.getObjectId());
		if (chunk != null) {
			if (!chunk.isWithinBounds(obj))
				chunk.removeObject(obj);
			else
				return;
		}
		chunk = getChunk(obj.getX(), obj.getZ());
		if (chunk == null) {
			Log.e("TerrainMap", "Null Chunk! Location: (%.3f, %.3f) Object: %s", obj.getX(), obj.getZ(), obj);
			return;
		}
		chunk.addObject(obj);
		objectChunk.put(obj.getObjectId(), chunk);
	}
	
	private void remove(SWGObject obj) {
		TerrainMapChunk chunk = objectChunk.remove(obj.getObjectId());
		if (chunk != null)
			chunk.removeObject(obj);
	}
	
	private void update(SWGObject obj) {
		Set<SWGObject> prevAware = obj.getObjectsAware();
		Set<SWGObject> aware = getNearbyAware(obj);
		AwarenessUtilities.callForNewAware(prevAware, aware, (inRange)  -> callbackManager.callOnEach((call) -> call.onWithinRange(obj, inRange)));
		AwarenessUtilities.callForOldAware(prevAware, aware, (outRange) -> callbackManager.callOnEach((call) -> call.onOutOfRange(obj, outRange)));
	}
	
	private Set<SWGObject> getNearbyAware(SWGObject obj) {
		Set<SWGObject> aware = new HashSet<>();
		int startX = Math.max(calculateIndex(obj.getX()) - 1, 0);
		int startZ = Math.max(calculateIndex(obj.getZ()) - 1, 0);
		int endX = Math.min(startX+3, CHUNK_COUNT_ACROSS);
		int endZ = Math.min(startZ+3, CHUNK_COUNT_ACROSS);
		for (int z = startZ; z < endZ; z++) {
			for (int x = startX; x < endX; x++) {
				aware.addAll(chunks[z][x].getWithinAwareness(obj));
			}
		}
		return aware;
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
