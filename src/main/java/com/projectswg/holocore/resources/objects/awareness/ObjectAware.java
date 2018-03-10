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
package com.projectswg.holocore.resources.objects.awareness;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.player.Player;

public class ObjectAware {
	
	private final SWGObject object;
	private final Aware objectsAware;
	private final Aware customAware;
	private final AtomicReference<TerrainMapChunk> chunk;
	
	public ObjectAware(SWGObject obj) {
		this.object = obj;
		this.objectsAware = new Aware(obj);
		this.customAware = new Aware(obj);
		this.chunk = new AtomicReference<>(null);
	}
	
	protected TerrainMapChunk setTerrainMapChunk(TerrainMapChunk chunk) {
		return this.chunk.getAndSet(chunk);
	}
	
	protected TerrainMapChunk getTerrainMapChunk() {
		return chunk.get();
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
	
	public void addObjectAware(ObjectAware aware) {
		objectsAware.add(aware.getRawObjectsAware());
	}
	
	public void removeObjectAware(ObjectAware aware) {
		objectsAware.remove(aware.getRawObjectsAware());
	}
	
	public void addCustomAware(ObjectAware aware) {
		customAware.add(aware.getRawCustomAware());
	}
	
	public void removeCustomAware(ObjectAware aware) {
		customAware.remove(aware.getRawCustomAware());
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
		Set<SWGObject> aware = new HashSet<>();
		objectsAware.getAware(aware);
		customAware.getAware(aware);
		return aware;
	}
	
	public Set<Player> getObservers() {
		Set<Player> observers = new HashSet<>();
		objectsAware.getObservers(observers);
		customAware.getObservers(observers);
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
