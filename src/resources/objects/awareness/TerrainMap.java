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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.projectswg.common.callback.CallbackManager;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

public class TerrainMap {
	
	private static final int CHUNK_COUNT_ACROSS = 16;
	private static final int MIN_X = -8192;
	private static final int MIN_Z = -8192;
	private static final int MAP_WIDTH = 16384;
	private static final int CHUNK_WIDTH = MAP_WIDTH / CHUNK_COUNT_ACROSS;
	private static final int INDEX_FACTOR = (int) (Math.log(MAP_WIDTH / CHUNK_COUNT_ACROSS) / Math.log(2) + 1e-12);
	
	private final CallbackManager<TerrainMapCallback> callbackManager;
	private final TerrainMapChunk [][] chunks;
	
	public TerrainMap(Terrain t, TerrainMapCallback callback) {
		callbackManager = new CallbackManager<>("terrain-map-"+t.name(), 1);
		callbackManager.setCallback(callback);
		chunks = new TerrainMapChunk[CHUNK_COUNT_ACROSS][CHUNK_COUNT_ACROSS];
		for (int z = 0; z < CHUNK_COUNT_ACROSS; z++) {
			for (int x = 0; x < CHUNK_COUNT_ACROSS; x++) {
				int chunkStartX = MIN_X+x*CHUNK_WIDTH;
				int chunkStartZ = MIN_Z+z*CHUNK_WIDTH;
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
	
	public void removeFromMap(SWGObject obj) {
		if (removeWithoutUpdate(obj)) {
			update(obj);
			Assert.test(isInAwareness(obj));
		}
	}
	
	private void move(SWGObject obj) {
		TerrainMapChunk chunk = getChunk(obj);
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
	
	private void update(SWGObject obj) {
		final Collection<SWGObject> oldAware = obj.getObjectsAware();
		final Collection<SWGObject> newAware = getNearbyAware(obj);
		callbackManager.callOnEach(call -> updateAwareness(call, oldAware, newAware, obj));
	}
	
	private Set<SWGObject> getNearbyAware(SWGObject obj) {
		Set<SWGObject> aware = new HashSet<>(128);
		if (obj.getAwareness().getTerrainMapChunk() == null)
			return aware;
		
		int sX = calculateIndex(obj.getTruncX())-1;
		int sZ = calculateIndex(obj.getTruncZ())-1;
		int eX = sX + 2;
		int eZ = sZ + 2;
		if (sX < 0)
			sX = 0;
		if (sZ < 0)
			sZ = 0;
		if (eX >= CHUNK_COUNT_ACROSS)
			eX = CHUNK_COUNT_ACROSS-1;
		if (eZ >= CHUNK_COUNT_ACROSS)
			eZ = CHUNK_COUNT_ACROSS-1;
		
		for (int z = sZ; z <= eZ; ++z) {
			for (int x = sX; x <= eX; ++x) {
				chunks[z][x].getWithinAwareness(obj, aware);
			}
		}
		return aware;
	}
	
	private TerrainMapChunk getChunk(SWGObject obj) {
		int xInd = calculateIndex(obj.getTruncX());
		int zInd = calculateIndex(obj.getTruncZ());
		if (xInd < 0 || zInd < 0 || xInd >= CHUNK_COUNT_ACROSS || zInd >= CHUNK_COUNT_ACROSS)
			return null;
		return chunks[zInd][xInd];
	}
	
	public static boolean removeWithoutUpdate(SWGObject obj) {
		TerrainMapChunk chunk = obj.getAwareness().setTerrainMapChunk(null);
		if (chunk != null)
			chunk.removeObject(obj);
		return chunk != null;
	}
	
	private static void updateAwareness(TerrainMapCallback call, Collection<SWGObject> oldAware, Collection<SWGObject> newAware, SWGObject obj) {
		for (SWGObject n : newAware) {
			if (!oldAware.contains(n))
				call.onWithinRange(obj, n);
		}
		for (SWGObject p : oldAware) {
			if (!newAware.contains(p))
				call.onOutOfRange(obj, p);
		}
	}
	
	private static boolean isInAwareness(SWGObject obj) {
		if (obj.getParent() != null)
			return false;
		if (obj.getBaselineType() == BaselineType.WAYP)
			return false;
		if (obj.getBaselineType() != BaselineType.CREO)
			return true;
		return !((CreatureObject) obj).isPlayer() || ((CreatureObject) obj).isLoggedInPlayer();
	}
	
	private static int calculateIndex(int x) {
		return (x+8192) >> INDEX_FACTOR;
	}
	
	public interface TerrainMapCallback {
		void onWithinRange(SWGObject obj, SWGObject inRange);
		void onOutOfRange(SWGObject obj, SWGObject outRange);
		void onMoveSuccess(SWGObject obj);
		void onMoveFailure(SWGObject obj);
	}
	
}
