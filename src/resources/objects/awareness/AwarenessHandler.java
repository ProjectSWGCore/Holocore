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
import java.util.Set;

import resources.Location;
import resources.Terrain;
import resources.buildout.BuildoutArea;
import resources.objects.SWGObject;
import resources.objects.awareness.TerrainMap.TerrainMapCallback;
import resources.player.Player;
import resources.server_info.Log;

public class AwarenessHandler {
	
	private static final Location GONE_LOCATION = new Location(0, 0, 0, null);
	
	private final Map<Terrain, TerrainMap> terrains;
	
	public AwarenessHandler(TerrainMapCallback callback) {
		terrains = new HashMap<>(Terrain.getTerrainCount());
		loadTerrainMaps(callback);
	}
	
	public void close() {
		for (TerrainMap map : terrains.values()) {
			map.stop();
		}
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
		if (obj.getParent() != null)
			obj.moveToContainer(null);
		// Adjust to server coordinates
		BuildoutArea area = obj.getBuildoutArea();
		if (area != null)
			requestedLocation = area.adjustLocation(requestedLocation);
		// Remove from previous awareness
		Terrain oldTerrain = obj.getTerrain();
		Terrain newTerrain = requestedLocation.getTerrain();
		if (oldTerrain != newTerrain && oldTerrain != null) {
			obj.clearObjectsAware(); // Moving to GONE
			TerrainMap oldTerrainMap = getTerrainMap(oldTerrain);
			if (oldTerrainMap != null)
				oldTerrainMap.removeFromMap(obj);
		}
		// Add to new awareness
		TerrainMap map = getTerrainMap(newTerrain);
		if (map != null) {
			map.moveWithinMap(obj, requestedLocation);
		} else if (!requestedLocation.equals(GONE_LOCATION)) {
			Log.e(this, "Unknown terrain: %s", newTerrain);
		}
	}
	
	public void moveObject(SWGObject obj, SWGObject parent, Location requestedLocation) {
		Set<SWGObject> oldAware = obj.getObjectsAware();
		Set<Player> oldObservers = obj.getObservers();
		if (obj.getParent() != parent)
			obj.moveToContainer(parent);
		// Remove from previous awareness
		TerrainMap oldTerrain = getTerrainMap(obj);
		if (oldTerrain != null)
			oldTerrain.removeWithoutUpdate(obj);
		// Update location
		obj.setLocation(requestedLocation);
		// Update awareness
		TerrainMap map = getTerrainMap(parent);
		if (map != null) {
			map.moveToParent(obj, parent);
			handleUpdateAwarenessManual(obj, oldAware, oldObservers, obj.getObjectsAware(), obj.getObservers());
		} else if (!requestedLocation.equals(GONE_LOCATION)) {
			Log.e(this, "Unknown terrain: %s", requestedLocation.getTerrain());
		}
	}
	
	public void disappearObject(SWGObject obj, boolean disappearObjects, boolean disappearCustom) {
		moveObject(obj, GONE_LOCATION);
		if (disappearObjects)
			obj.clearObjectsAware();
		if (disappearCustom)
			obj.clearCustomAware(true);
	}
	
	private void handleUpdateAwarenessManual(SWGObject obj, Set<SWGObject> oldAware, Set<Player> oldObservers, Set<SWGObject> newAware, Set<Player> newObservers) {
		// Create obj for all new observers
		for (Player nowObserver : newObservers) {
			if (!oldObservers.remove(nowObserver))
				obj.createObject(nowObserver);
		}
		// Destroy obj for all old observers
		for (Player oldObserver : oldObservers)
			obj.destroyObject(oldObserver);
		
		Player owner = obj.getOwner();
		if (owner == null)
			return;
		// Create new aware objects for obj
		for (SWGObject nowAware : newAware) {
			if (!oldAware.remove(nowAware))
				nowAware.createObject(owner);
		}
		// Destroy old aware objects for obj
		for (SWGObject old : oldAware)
			old.destroyObject(owner);
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
