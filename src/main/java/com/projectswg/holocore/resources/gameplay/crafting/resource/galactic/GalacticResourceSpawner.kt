/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.gameplay.crafting.resource.galactic

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource
import com.projectswg.holocore.resources.support.data.namegen.SWGNameGenerator
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.stream.Collectors.toList

class GalacticResourceSpawner {
	
	private val resourceIdMax = AtomicLong(0)
	private val resourceNameGenerator = SWGNameGenerator()
	
	fun initialize() {
		loadResources()
		updateAllResources()
	}
	
	fun terminate() {
		saveResources()
	}
	
	fun updateAllResources() {
		updateSpawns()
		updateUnusedPools()
	}
	
	private fun loadResources() {
		val startTime = StandardLog.onStartLoad("galactic resources")
		var resourceCount = 0
		for (resource in PswgDatabase.resources.resources) {
			GalacticResourceContainer.getContainer().addGalacticResource(resource)
			if (resource.id > resourceIdMax.get())
				resourceIdMax.set(resource.id)
			resourceCount++
		}
		StandardLog.onEndLoad(resourceCount, "galactic resources", startTime)
	}
	
	private fun saveResources() {
		val loader = GalacticResourceLoader()
		loader.saveResources(GalacticResourceContainer.getContainer().allResources)
		PswgDatabase.resources.resources = GalacticResourceContainer.getContainer().allResources
	}
	
	private fun updateUnusedPools() {
		val rawResources = GalacticResourceContainer.getContainer().rawResources
		for (raw in rawResources) {
			if (raw.maxPools == 0)
				continue
			updateUnusedResourcePool(raw)
		}
	}
	
	private fun updateUnusedResourcePool(raw: RawResource) {
		val spawned = GalacticResourceContainer.getContainer().getSpawnedGalacticResources(raw)
		val minTypes = raw.minTypes
		val maxTypes = raw.maxTypes
		if (spawned >= minTypes)
			return  // Only respawn once total number spawned in goes below minTypes
		val targetTypes = calculateTargetTypes(minTypes, maxTypes)
		for (i in spawned until targetTypes) {
			createNewResourceWithSpawns(raw)
		}
	}
	
	private fun calculateTargetTypes(minTypes: Int, maxTypes: Int): Int {
		val x = ThreadLocalRandom.current().nextDouble()
		return (x * x * (maxTypes - minTypes).toDouble()).toInt() + minTypes
	}
	
	private fun createNewResourceWithSpawns(raw: RawResource) {
		val targetSpawns = ThreadLocalRandom.current().nextInt(raw.minPools, raw.maxPools+1)
		val resource = createNewResource(raw)
		val restricted = getRestrictedResource(raw)
		if (restricted == null) {
			for (terrain in BASE_PLANETS) {
				for (i in 0 until targetSpawns) {
					createNewSpawn(resource, terrain)
				}
			}
		} else {
			for (i in 0 until targetSpawns) {
				createNewSpawn(resource, restricted)
			}
		}
	}
	
	private fun createNewResource(raw: RawResource): GalacticResource {
		val newId = resourceIdMax.incrementAndGet()
		var resource: GalacticResource
		do {
			val newName = resourceNameGenerator.generateName("resources")
			resource = GalacticResource(newId, newName, raw.id)
			resource.rawResource = raw
			Log.t("Generating new resource: $resource  $raw")
		} while (!GalacticResourceContainer.getContainer().addGalacticResource(resource))
		return resource
	}
	
	private fun createNewSpawn(resource: GalacticResource, terrain: Terrain) {
		val spawn = GalacticResourceSpawn(resource.id)
		spawn.setRandomValues(terrain)
		resource.addSpawn(spawn)
	}
	
	private fun updateSpawns() {
		val resources = GalacticResourceContainer.getContainer().allResources
		for (resource in resources) {
			val spawns = resource.spawns
			if (spawns.isEmpty())
				continue
			val expired = spawns.stream().filter { it.isExpired }.collect(toList())
			expired.forEach(Consumer<GalacticResourceSpawn> { resource.removeSpawn(it) })
		}
	}
	
	private fun getRestrictedResource(resource: RawResource): Terrain? {
		var key = resource.name.key
		if (key.length >= 2 && Character.isDigit(key[key.length - 1]) && key[key.length - 2] == '_')
			key = key.substring(0, key.lastIndexOf('_'))
		for (test in ALL_PLANETS) {
			if (key.endsWith(test.getName()))
				return test
		}
		return if (key.endsWith("kashyyyk")) Terrain.KASHYYYK_MAIN else null
	}
	
	companion object {
		
		private val ALL_PLANETS = arrayOf(Terrain.CORELLIA, Terrain.DANTOOINE, Terrain.DATHOMIR, Terrain.ENDOR, Terrain.KASHYYYK_MAIN, Terrain.LOK, Terrain.NABOO, Terrain.RORI, Terrain.TALUS, Terrain.TATOOINE, Terrain.YAVIN4, Terrain.MUSTAFAR)
		
		private val BASE_PLANETS = arrayOf(Terrain.CORELLIA, Terrain.DANTOOINE, Terrain.DATHOMIR, Terrain.ENDOR, Terrain.LOK, Terrain.NABOO, Terrain.RORI, Terrain.TALUS, Terrain.TATOOINE, Terrain.YAVIN4)
	}
	
}
