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
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawn;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class GalacticResourceContainer {
	
	private static final GalacticResourceContainer CONTAINER = new GalacticResourceContainer();
	
	private final Map<Long, RawResource> rawResources;
	private final Map<Long, GalacticResource> galacticResources;
	private final Map<String, GalacticResource> galacticResourcesByName;
	private final Map<RawResource, List<GalacticResource>> rawToGalactic;
	private final ResourceSpawnTreeGlobal resourceSpawns;
	
	public GalacticResourceContainer() {
		this.rawResources = new ConcurrentHashMap<>();
		this.galacticResources = new ConcurrentHashMap<>();
		this.galacticResourcesByName = new ConcurrentHashMap<>();
		this.rawToGalactic = new ConcurrentHashMap<>();
		this.resourceSpawns = new ResourceSpawnTreeGlobal();
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
	
	public int getSpawnedGalacticResources(RawResource rawResource) {
		return (int) resourceSpawns.getSpawnedGalacticResourceIds().stream()
				.map(this::getGalacticResource)
				.filter(r -> r.getRawResourceId() == rawResource.getId())
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
	
	public List<GalacticResourceSpawn> getAllResourceSpawns() {
		return resourceSpawns.getResourceSpawns();
	}
	
	public List<GalacticResource> getSpawnedResources() {
		return resourceSpawns.getSpawnedGalacticResourceIds().stream().map(this::getGalacticResource).collect(Collectors.toList());
	}
	
	public List<GalacticResource> getSpawnedResources(Terrain terrain) {
		return resourceSpawns.getSpawnedGalacticResourceIds(terrain).stream().map(this::getGalacticResource).collect(Collectors.toList());
	}
	
	public List<GalacticResourceSpawn> getTerrainResourceSpawns(GalacticResource resource, Terrain terrain) {
		return resourceSpawns.getResourceSpawns(resource, terrain);
	}
	
	public boolean addResourceSpawn(GalacticResourceSpawn spawn) {
		assert getGalacticResource(spawn.getResourceId()) != null : "Invalid resourceId for GalacticResourceSpawn!";
		return resourceSpawns.addSpawn(spawn);
	}
	
	public boolean removeResourceSpawn(GalacticResourceSpawn spawn) {
		assert getGalacticResource(spawn.getResourceId()) != null : "Invalid resourceId for GalacticResourceSpawn!";
		return resourceSpawns.removeSpawn(spawn);
	}
	
	public static GalacticResourceContainer getContainer() {
		return CONTAINER;
	}
	
	private static <T> List<T> copyImmutable(Collection<T> list) {
		if (list == null)
			return Collections.unmodifiableList(new ArrayList<>());
		return List.copyOf(list);
	}
	
	private static <T> List<T> createImmutable() {
		return List.of();
	}
	
	private static class ResourceSpawnTreeGlobal {
		
		private final Map<Long, ResourceSpawnTreeResource> resourceSpawnTree; // Maps GalacticResource IDs to it's spawn tree
		
		public ResourceSpawnTreeGlobal() {
			this.resourceSpawnTree = new HashMap<>();
		}
		
		public boolean addSpawn(GalacticResourceSpawn spawn) {
			ResourceSpawnTreeResource resource;
			synchronized (resourceSpawnTree) {
				resource = resourceSpawnTree.computeIfAbsent(spawn.getResourceId(), k -> new ResourceSpawnTreeResource());
			}
			return resource.addSpawn(spawn);
		}
		
		public boolean removeSpawn(GalacticResourceSpawn spawn) {
			ResourceSpawnTreeResource resource;
			synchronized (resourceSpawnTree) {
				resource = resourceSpawnTree.get(spawn.getResourceId());
			}
			boolean success = resource.removeSpawn(spawn);
			if (resource.isDepleted()) {
				synchronized (resourceSpawnTree) {
					resourceSpawnTree.remove(spawn.getResourceId());
				}
			}
			return success;
		}
		
		public List<Long> getSpawnedGalacticResourceIds() {
			synchronized (resourceSpawnTree) {
				return copyImmutable(resourceSpawnTree.keySet());
			}
		}
		
		public List<Long> getSpawnedGalacticResourceIds(Terrain terrain) {
			synchronized (resourceSpawnTree) {
				List<Long> spawned = new ArrayList<>(resourceSpawnTree.size());
				for (Entry<Long, ResourceSpawnTreeResource> entry : resourceSpawnTree.entrySet()) {
					if (entry.getValue().isSpawnedOn(terrain))
						spawned.add(entry.getKey());
				}
				return Collections.unmodifiableList(spawned);
			}
		}
		
		public List<GalacticResourceSpawn> getResourceSpawns(GalacticResource galacticResource, Terrain terrain) {
			ResourceSpawnTreeResource resource;
			synchronized (resourceSpawnTree) {
				resource = resourceSpawnTree.get(galacticResource.getId());
			}
			if (resource == null)
				return createImmutable();
			return resource.getSpawns(terrain);
		}
		
		public List<GalacticResourceSpawn> getResourceSpawns() {
			synchronized (resourceSpawnTree) {
				List<GalacticResourceSpawn> spawns = new ArrayList<>();
				for (ResourceSpawnTreeResource resource : resourceSpawnTree.values()) {
					spawns.addAll(resource.getSpawns());
				}
				return spawns;
			}
		}
		
	}
	
	private static class ResourceSpawnTreeResource {
		
		private final Map<Terrain, ResourceSpawnTreePlanet> planetSpawnTree;
		
		public ResourceSpawnTreeResource() {
			this.planetSpawnTree = new EnumMap<>(Terrain.class);
		}
		
		public boolean addSpawn(GalacticResourceSpawn spawn) {
			ResourceSpawnTreePlanet planet;
			synchronized (planetSpawnTree) {
				planet = planetSpawnTree.computeIfAbsent(spawn.getTerrain(), k -> new ResourceSpawnTreePlanet());
			}
			return planet.addSpawn(spawn);
		}
		
		public boolean removeSpawn(GalacticResourceSpawn spawn) {
			ResourceSpawnTreePlanet planet;
			synchronized (planetSpawnTree) {
				planet = planetSpawnTree.get(spawn.getTerrain());
			}
			boolean success = planet.removeSpawn(spawn);
			if (planet.isDepleted()) {
				synchronized (planetSpawnTree) {
					planetSpawnTree.remove(spawn.getTerrain());
				}
			}
			return success;
		}
		
		public List<GalacticResourceSpawn> getSpawns() {
			synchronized (planetSpawnTree) {
				List<GalacticResourceSpawn> spawns = new ArrayList<>();
				for (ResourceSpawnTreePlanet planet : planetSpawnTree.values()) {
					spawns.addAll(planet.getSpawns());
				}
				return spawns;
			}
		}
		
		public List<GalacticResourceSpawn> getSpawns(Terrain terrain) {
			ResourceSpawnTreePlanet planet;
			synchronized (planetSpawnTree) {
				planet = planetSpawnTree.get(terrain);
			}
			if (planet == null)
				return createImmutable();
			return planet.getSpawns();
		}
		
		public boolean isSpawnedOn(Terrain terrain) {
			synchronized (planetSpawnTree) {
				return planetSpawnTree.containsKey(terrain);
			}
		}
		
		public boolean isDepleted() {
			synchronized (planetSpawnTree) {
				return planetSpawnTree.isEmpty();
			}
		}
		
	}
	
	private static class ResourceSpawnTreePlanet {
		
		private final List<GalacticResourceSpawn> spawns;
		private final AtomicBoolean depleted;				// TRUE once all spawns have been removed
		
		public ResourceSpawnTreePlanet() {
			this.spawns = new ArrayList<>();
			this.depleted = new AtomicBoolean(false);
		}
		
		public boolean addSpawn(GalacticResourceSpawn spawn) {
			synchronized (spawns) {
				if (depleted.get())
					throw new IllegalStateException("Cannot add resource spawn when depleted!");
				if (depleted.get())
					return false;
				return spawns.add(spawn);
			}
		}
		
		public boolean removeSpawn(GalacticResourceSpawn spawn) {
			synchronized (spawns) {
				boolean success = spawns.remove(spawn);
				if (spawns.isEmpty())
					depleted.set(true); // When the last resource spawn is removed - this is now invalid
				return success;
			}
		}
		
		public List<GalacticResourceSpawn> getSpawns() {
			synchronized (spawns) {
				return copyImmutable(spawns);
			}
		}
		
		public boolean isDepleted() {
			return depleted.get();
		}
		
	}
	
}
