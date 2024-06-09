/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.crafting.resource.galactic

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.data.location.Terrain
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Supplier
import java.util.stream.Collectors

class GalacticResource(id: Long = 0, name: String = "", rawResourceId: Long = 0) : MongoPersistable {
	val stats: GalacticResourceStats = GalacticResourceStats()
	private val _spawns: MutableList<GalacticResourceSpawn> = CopyOnWriteArrayList()
	val spawns: List<GalacticResourceSpawn>
		get() { return Collections.unmodifiableList(_spawns) }
	private val terrainSpawns: MutableMap<Terrain, MutableList<GalacticResourceSpawn>> = ConcurrentHashMap()

	var id: Long
		private set
	var name: String
		private set
	var rawResourceId: Long

	init {
		this.id = id
		this.name = name
		this.rawResourceId = rawResourceId
	}

	fun getSpawns(terrain: Terrain?): List<GalacticResourceSpawn> {
		val spawns: List<GalacticResourceSpawn>? = terrainSpawns[terrain]
		return if (spawns == null) listOf() else Collections.unmodifiableList(spawns)
	}

	fun addSpawn(spawn: GalacticResourceSpawn) {
		_spawns.add(spawn)
		terrainSpawns.computeIfAbsent(spawn.terrain) { CopyOnWriteArrayList() }.add(spawn)
	}

	fun removeSpawn(spawn: GalacticResourceSpawn) {
		_spawns.remove(spawn)
		terrainSpawns.compute(spawn.terrain) { _: Terrain, spawns: MutableList<GalacticResourceSpawn>? ->
			if (spawns == null) return@compute null
			spawns.remove(spawn)
			if (spawns.isEmpty()) null else spawns
		}
	}

	override fun readMongo(data: MongoData) {
		_spawns.clear()
		terrainSpawns.clear()

		id = data.getLong("id", id)
		name = data.getString("name", name)
		rawResourceId = data.getLong("rawId", rawResourceId)
		data.getDocument("stats", stats)
		_spawns.addAll(data.getArray("spawns", Supplier { GalacticResourceSpawn() }))
		terrainSpawns.putAll(_spawns.stream().collect(Collectors.groupingBy { obj: GalacticResourceSpawn -> obj.terrain }))
	}

	override fun saveMongo(data: MongoData) {
		data.putLong("id", id)
		data.putString("name", name)
		data.putLong("rawId", rawResourceId)
		data.putDocument("stats", stats)
		data.putArray("spawns", _spawns)
	}

	override fun toString(): String {
		return "GalacticResource[ID=$id  NAME='$name']"
	}

	override fun equals(other: Any?): Boolean {
		if (other !is GalacticResource) return false
		return other.id == id && (other.name == name)
	}

	override fun hashCode(): Int {
		return java.lang.Long.hashCode(id)
	}
}
