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

import resources.objects.SWGObject;
import resources.player.Player;

public class ObjectAware {
	
	private final SWGObject object;
	private final Aware objectsAware;
	private final Aware customAware;
	private final Object chunkMutex;
	private TerrainMapChunk chunk;
	
	public ObjectAware(SWGObject obj) {
		this.object = obj;
		this.objectsAware = new Aware(obj);
		this.customAware = new Aware(obj);
		this.chunkMutex = new Object();
		this.chunk = null;
	}
	
	protected TerrainMapChunk setTerrainMapChunk(TerrainMapChunk chunk) {
		synchronized (chunkMutex) {
			TerrainMapChunk old = this.chunk;
			this.chunk = chunk;
			return old;
		}
	}
	
	protected TerrainMapChunk getTerrainMapChunk() {
		synchronized (chunkMutex) {
			return chunk;
		}
	}
	
	public void setParent(ObjectAware parent) {
		if (parent == null) {
			objectsAware.setParent(null);
			customAware.setParent(null);
		} else {
			objectsAware.setParent(parent.getRawObjectsAware());
			customAware.setParent(parent.getRawCustomAware());
		}
	}
	
	public boolean addObjectAware(ObjectAware aware) {
		return objectsAware.add(aware.getRawObjectsAware());
	}
	
	public boolean removeObjectAware(ObjectAware aware) {
		return objectsAware.remove(aware.getRawObjectsAware());
	}
	
	public boolean addCustomAware(ObjectAware aware) {
		return customAware.add(aware.getRawCustomAware());
	}
	
	public boolean removeCustomAware(ObjectAware aware) {
		return customAware.remove(aware.getRawCustomAware());
	}
	
	public boolean isObjectAware(SWGObject aware) {
		return objectsAware.contains(aware);
	}
	
	public boolean isCustomAware(SWGObject aware) {
		return customAware.contains(aware);
	}
	
	public void clearObjectsAware() {
		objectsAware.clear();
	}
	
	public void clearCustomAware() {
		customAware.clear();
	}
	
	public Set<SWGObject> getAware() {
		Set<SWGObject> aware = objectsAware.getAware();
		aware.addAll(customAware.getAware());
		return aware;
	}
	
	public Set<Player> getObservers() {
		Set<Player> observers = objectsAware.getObservers();
		observers.addAll(customAware.getObservers());
		return observers;
	}
	
	public Set<Player> getChildObservers() {
		Set<Player> observers = new HashSet<>();
		Aware.addObserversToSet(object.getContainedObjects(), observers, object.getOwner(), object);
		return observers;
	}
	
	public Set<SWGObject> getObjectsAware() {
		return objectsAware.getAware();
	}
	
	public Set<SWGObject> getCustomAware() {
		return customAware.getAware();
	}
	
	public Set<Player> getObjectObservers() {
		return objectsAware.getObservers();
	}
	
	public Set<Player> getCustomObservers() {
		return customAware.getObservers();
	}
	
	private Aware getRawObjectsAware() {
		return objectsAware;
	}
	
	private Aware getRawCustomAware() {
		return customAware;
	}
	
}
