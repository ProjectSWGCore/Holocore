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
package com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

public enum GalacticResourceContainer {
	INSTANCE;
	
	private final Map<Long, RawResource> rawResources;
	private final Map<Long, GalacticResource> galacticResources;
	private final Map<String, GalacticResource> galacticResourcesByName;
	private final Map<RawResource, List<GalacticResource>> rawToGalactic;
	
	GalacticResourceContainer() {
		this.rawResources = new ConcurrentHashMap<>();
		this.galacticResources = new ConcurrentHashMap<>();
		this.galacticResourcesByName = new ConcurrentHashMap<>();
		this.rawToGalactic = new ConcurrentHashMap<>();
	}
	
	public RawResource getRawResource(long resourceId) {
		return rawResources.get(resourceId);
	}
	
	public GalacticResource getGalacticResource(long resourceId) {
		return galacticResources.get(resourceId);
	}
	
	public GalacticResource getGalacticResourceByName(String resourceName) {
		return galacticResourcesByName.get(resourceName);
	}
	
	public List<RawResource> getRawResources() {
		return copyImmutable(rawResources.values());
	}
	
	public List<GalacticResource> getGalacticResources(RawResource rawResource) {
		return copyImmutable(rawToGalactic.get(rawResource));
	}
	
	public List<GalacticResource> getSpawnedResources(Terrain terrain) {
		return galacticResources.values().stream()
				.filter(r -> !r.getSpawns(terrain).isEmpty())
				.collect(toList());
	}
	
	public int getSpawnedGalacticResources(RawResource rawResource) {
		return (int) galacticResources.values().stream()
				.filter(r -> !r.getSpawns().isEmpty())
				.map(r -> r.getRawResourceId() == rawResource.getId())
				.count();
	}
	
	public void addRawResource(RawResource resource) {
		RawResource replaced = rawResources.put(resource.getId(), resource);
		assert replaced == null : "raw resource overwritten";
	}
	
	public boolean addGalacticResource(GalacticResource resource) {
		RawResource raw = getRawResource(resource.getRawResourceId());
		Objects.requireNonNull(raw, "Invalid raw resource ID in GalacticResource!");
		if (galacticResources.putIfAbsent(resource.getId(), resource) == null) {
			if (galacticResourcesByName.putIfAbsent(resource.getName(), resource) == null) {
				rawToGalactic.computeIfAbsent(raw, k -> new ArrayList<>()).add(resource);
				return true;
			} else {
				galacticResources.remove(resource.getId());
			}
		}
		return false;
	}
	
	public List<GalacticResource> getAllResources() {
		return copyImmutable(galacticResources.values());
	}
	
	public static GalacticResourceContainer getContainer() {
		return INSTANCE;
	}
	
	private static <T> List<T> copyImmutable(Collection<T> list) {
		if (list == null)
			return Collections.unmodifiableList(new ArrayList<>());
		return List.copyOf(list);
	}
	
}
