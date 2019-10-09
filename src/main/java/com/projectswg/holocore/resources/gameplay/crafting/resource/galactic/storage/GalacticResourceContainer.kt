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
package com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource
import java.lang.NullPointerException

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GalacticResourceContainer {
	
	val rawResources: List<RawResource>
		get() = rawResourceMap.values.toList()
	
	private val rawResourceMap = ConcurrentHashMap<Long, RawResource>()
	private val galacticResourceMap = ConcurrentHashMap<Long, GalacticResource>()
	private val galacticResourcesByName = ConcurrentHashMap<String, GalacticResource>()
	private val rawToGalactic = ConcurrentHashMap<RawResource, MutableList<GalacticResource>>()
	
	val allResources: List<GalacticResource>
		get() = galacticResourceMap.values.toList()
	
	fun getRawResource(resourceId: Long): RawResource? {
		return rawResourceMap[resourceId]
	}
	
	fun getGalacticResource(resourceId: Long): GalacticResource? {
		return galacticResourceMap[resourceId]
	}
	
	fun getGalacticResourceByName(resourceName: String): GalacticResource? {
		return galacticResourcesByName[resourceName]
	}
	
	fun getGalacticResources(rawResource: RawResource): List<GalacticResource> {
		return rawToGalactic[rawResource]?.toList() ?: ArrayList()
	}
	
	fun getSpawnedResources(terrain: Terrain): List<GalacticResource> {
		return galacticResourceMap.values.filter { r -> r.getSpawns(terrain).isNotEmpty() }
	}
	
	fun getSpawnedGalacticResources(rawResource: RawResource): Int {
		return galacticResourceMap.values.count { r -> r.rawResourceId == rawResource.id && r.spawns.isNotEmpty() }
	}
	
	fun addRawResource(resource: RawResource) {
		val replaced = rawResourceMap.put(resource.id, resource)
		assert(replaced == null) { "raw resource overwritten" }
	}
	
	fun addGalacticResource(resource: GalacticResource): Boolean {
		val raw = getRawResource(resource.rawResourceId) ?: throw NullPointerException("Invalid raw resource ID in GalacticResource")
		if (galacticResourceMap.putIfAbsent(resource.id, resource) == null) {
			if (galacticResourcesByName.putIfAbsent(resource.name, resource) == null) {
				rawToGalactic.computeIfAbsent(raw) { ArrayList() }.add(resource)
				return true
			} else {
				galacticResourceMap.remove(resource.id)
			}
		}
		return false
	}
	
}
