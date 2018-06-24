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
package com.projectswg.holocore.resources.gameplay.crafting.resource.galactic;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResourceContainer;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.log.log_wrapper.ConsoleLogWrapper;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class GalacticResourceSpawner {
	
	private static final Terrain [] ALL_PLANETS = new Terrain[] {
		Terrain.CORELLIA,	Terrain.DANTOOINE,		Terrain.DATHOMIR,
		Terrain.ENDOR,		Terrain.KASHYYYK_MAIN,	Terrain.LOK,
		Terrain.NABOO,		Terrain.RORI,			Terrain.TALUS,
		Terrain.TATOOINE,	Terrain.YAVIN4,			Terrain.MUSTAFAR
	};
	
	private static final Terrain [] BASE_PLANETS = new Terrain[] {
		Terrain.CORELLIA,	Terrain.DANTOOINE,		Terrain.DATHOMIR,
		Terrain.ENDOR,		Terrain.LOK,			Terrain.NABOO,
		Terrain.RORI,			Terrain.TALUS,		Terrain.TATOOINE,
		Terrain.YAVIN4
	};
	
	private final Random random;
	private final AtomicLong resourceIdMax;
	
	public GalacticResourceSpawner() {
		this.random = new Random();
		this.resourceIdMax = new AtomicLong(0);
	}
	
	public static void main(String [] args) {
		Log.addWrapper(new ConsoleLogWrapper());
		DataManager.initialize();
		RawResourceContainer container = new RawResourceContainer();
		container.loadResources();
		for (RawResource rawResource : container.getResources()) {
			GalacticResourceContainer.getContainer().addRawResource(rawResource);
		}
		GalacticResourceSpawner spawner = new GalacticResourceSpawner();
		spawner.initialize();
		spawner.terminate();
		DataManager.terminate();
	}
	
	public void initialize() {
		loadOldResources();
		updateAllResources();
	}
	
	public void terminate() {
		saveResources();
	}
	
	public void updateAllResources() {
		updateSpawns();
		updateUnusedPools();
	}
	
	private void loadOldResources() {
		loadOldGalacticResources();
		loadOldGalacticResourceSpawns();
	}
	
	private void loadOldGalacticResources() {
		GalacticResourceLoader loader = new GalacticResourceLoader();
		long startTime = StandardLog.onStartLoad("galactic resources");
		List<GalacticResource> resources = loader.loadResources();
		for (GalacticResource resource : resources) {
			resource.setRawResource(GalacticResourceContainer.getContainer().getRawResource(resource.getRawResourceId()));
			GalacticResourceContainer.getContainer().addGalacticResource(resource);
			if (resource.getId() > resourceIdMax.get())
				resourceIdMax.set(resource.getId());
		}
		StandardLog.onEndLoad(resources.size(), "galactic resources", startTime);
	}
	
	private void loadOldGalacticResourceSpawns() {
		GalacticResourceLoader loader = new GalacticResourceLoader();
		long startTime = StandardLog.onStartLoad("galactic spawns");
		List<GalacticResourceSpawn> spawns = loader.loadSpawns();
		for (GalacticResourceSpawn spawn: spawns) {
			if (!spawn.isExpired())
				GalacticResourceContainer.getContainer().addResourceSpawn(spawn);
		}
		StandardLog.onEndLoad(spawns.size(), "galactic spawns", startTime);
	}
	
	private void saveResources() {
		GalacticResourceLoader loader = new GalacticResourceLoader();
		loader.saveResources(GalacticResourceContainer.getContainer().getAllResources());
		loader.saveSpawns(GalacticResourceContainer.getContainer().getAllResourceSpawns());
	}
	
	private void updateUnusedPools() {
		List<RawResource> rawResources = GalacticResourceContainer.getContainer().getRawResources();
		for (RawResource raw : rawResources) {
			if (raw.getMaxPools() == 0)
				continue;
			updateUnusedResourcePool(raw);
		}
	}
	
	private void updateUnusedResourcePool(RawResource raw) {
		int spawned = GalacticResourceContainer.getContainer().getSpawnedGalacticResources(raw);
		int minTypes = raw.getMinTypes();
		int maxTypes = raw.getMaxTypes();
		if (spawned >= minTypes)
			return; // Only respawn once total number spawned in goes below minTypes
		int targetTypes = calculateTargetTypes(minTypes, maxTypes);
		for (int i = spawned; i < targetTypes; i++) {
			createNewResourceWithSpawns(raw);
		}
	}
	
	private int calculateTargetTypes(int minTypes, int maxTypes) {
		double x = random.nextDouble();
		return (int) (x * x * (maxTypes - minTypes)) + minTypes;
	}
	
	private void createNewResourceWithSpawns(RawResource raw) {
		int targetSpawns = random.nextInt(raw.getMaxPools() - raw.getMinPools() + 1) + raw.getMinPools();
		GalacticResource resource = createNewResource(raw);
		Terrain restricted = getRestrictedResource(raw);
		if (restricted == null) {
			for (Terrain terrain : BASE_PLANETS) {
				for (int i = 0; i < targetSpawns; i++) {
					createNewSpawn(resource, terrain);
				}
			}
		} else {
			for (int i = 0; i < targetSpawns; i++) {
				createNewSpawn(resource, restricted);
			}
		}
	}
	
	private GalacticResource createNewResource(RawResource raw) {
		long newId = resourceIdMax.incrementAndGet();
		String newName = createRandomName(raw) + ' ' + newId;
		GalacticResource resource = new GalacticResource(newId, newName, raw.getId());
		resource.setRawResource(raw);
		GalacticResourceContainer.getContainer().addGalacticResource(resource);
		return resource;
	}
	
	private void createNewSpawn(GalacticResource resource, Terrain terrain) {
		GalacticResourceSpawn spawn = new GalacticResourceSpawn(resource.getId());
		spawn.setRandomValues(terrain);
		GalacticResourceContainer.getContainer().addResourceSpawn(spawn);
	}
	
	private String createRandomName(RawResource raw) {
		return raw.getName().getKey();
	}
	
	private void updateSpawns() {
		List<GalacticResource> resources = GalacticResourceContainer.getContainer().getSpawnedResources();
		for (GalacticResource resource : resources) {
			updateResourceSpawns(resource);
		}
	}
	
	private void updateResourceSpawns(GalacticResource resource) {
		for (Terrain terrain : BASE_PLANETS) {
			updateResourceTerrainSpawns(resource, terrain);
		}
	}
	
	private void updateResourceTerrainSpawns(GalacticResource resource, Terrain terrain) {
		List<GalacticResourceSpawn> spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, terrain);
		// Remove expired spawns
		List<GalacticResourceSpawn> expired = spawns.stream().filter(GalacticResourceSpawn::isExpired).collect(Collectors.toList());
		for (GalacticResourceSpawn spawn : expired) {
			GalacticResourceContainer.getContainer().removeResourceSpawn(spawn);
		}
	}
	
	private Terrain getRestrictedResource(RawResource resource) {
		String key = resource.getName().getKey();
		if (key.length() >= 2 && Character.isDigit(key.charAt(key.length()-1)) && key.charAt(key.length()-2) == '_')
			key = key.substring(0, key.lastIndexOf('_'));
		for (Terrain test : ALL_PLANETS) {
			if (key.endsWith(test.getName()))
				return test;
		}
		if (key.endsWith("kashyyyk")) // special case because kashyyyk = kashyyyk_main
			return Terrain.KASHYYYK_MAIN;
		return null;
	}
	
}
