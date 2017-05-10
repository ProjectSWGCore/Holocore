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
import java.util.Map;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;

import resources.objects.SWGObject;
import resources.objects.awareness.TerrainMap.TerrainMapCallback;

public class AwarenessHandler implements AutoCloseable {
	
	private final Map<Terrain, TerrainMap> terrains;
	
	public AwarenessHandler(TerrainMapCallback callback) {
		terrains = new HashMap<>(Terrain.getTerrainCount());
		loadTerrainMaps(callback);
	}
	
	@Override
	public void close() {
		for (TerrainMap map : terrains.values()) {
			map.stop();
		}
	}
	
	public boolean isCallbacksDone() {
		for (TerrainMap map : terrains.values()) {
			if (!map.isCallbacksDone())
				return false;
		}
		return true;
	}
	
	private void loadTerrainMaps(TerrainMapCallback callback) {
		for (Terrain t : Terrain.values()) {
			TerrainMap map = new TerrainMap(t);
			map.start();
			map.setCallback(callback);
			terrains.put(t, map);
		}
	}
	
	public void moveObject(SWGObject obj, Location requestedLocation) {
		// Remove from previous awareness
		if (obj.getTerrain() != requestedLocation.getTerrain()) {
			TerrainMap oldTerrainMap = getTerrainMap(obj.getTerrain());
			if (oldTerrainMap != null)
				oldTerrainMap.removeWithoutUpdate(obj);
		}
		// Update location
		obj.setLocation(requestedLocation);
		if (obj.getParent() != null)
			obj.moveToContainer(null);
		// Update awareness
		if (obj.getTerrain() != Terrain.GONE) {
			TerrainMap map = getTerrainMap(requestedLocation.getTerrain());
			if (map != null) {
				map.moveWithinMap(obj);
			} else {
				Log.e("Unknown terrain: %s", requestedLocation.getTerrain());
			}
		}
	}
	
	public void moveObject(SWGObject obj, SWGObject parent, Location requestedLocation) {
		Assert.notNull(parent);
		// Remove from previous awareness
		TerrainMap oldMap = getTerrainMap(obj.getTerrain());
		if (oldMap != null)
			oldMap.removeWithoutUpdate(obj);
		// Update location
		obj.setLocation(requestedLocation);
		// Update awareness
		if (obj.getParent() != parent)
			obj.moveToContainer(parent);
		obj.resetAwareness();
	}
	
	public void disappearObject(SWGObject obj, boolean disappearObjects, boolean disappearCustom) {
		if (obj.getTerrain() != Terrain.GONE) {
			TerrainMap map = getTerrainMap(obj);
			Assert.notNull(map);
			if (disappearObjects)
				map.removeFromMap(obj);
			else
				map.removeWithoutUpdate(obj);
		}
		if (disappearCustom)
			obj.clearCustomAware(true);
	}
	
	private TerrainMap getTerrainMap(SWGObject object) {
		Terrain t = object.getTerrain();
		if (t == null)
			return null;
		return getTerrainMap(t);
	}
	
	private TerrainMap getTerrainMap(Terrain t) {
		return terrains.get(t);
	}
	
}
