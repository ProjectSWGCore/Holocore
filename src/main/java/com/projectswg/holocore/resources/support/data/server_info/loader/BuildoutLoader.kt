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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.CrcDatabase
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.ObjectCreator.ObjectCreationException
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BuildoutLoader private constructor(events: Collection<String>) {
	private val objectMap: MutableMap<Long, SWGObject> = ConcurrentHashMap()
	private val buildingMap: MutableMap<String, BuildingObject> = ConcurrentHashMap()
	private val terrainMap = EnumMap<Terrain, Map<Long, SWGObject>>(Terrain::class.java)
	private val events: Set<String> = HashSet(events)

	init {
		for (terrain in Terrain.entries) {
			terrainMap[terrain] = HashMap()
		}
	}

	val objects: Map<Long, SWGObject>
		get() = Collections.unmodifiableMap(objectMap)

	val buildings: Map<String, BuildingObject>
		get() = Collections.unmodifiableMap(buildingMap)

	fun getObjects(terrain: Terrain): Map<Long, SWGObject> {
		return terrainMap[terrain]!! // Guaranteed in the init
	}

	private fun loadFromFile() {
		loadStandardBuildouts()
		loadAdditionalBuildouts()
	}

	private fun loadStandardBuildouts() {
		val events = this.events
		try {
			SdbLoader.load(File("serverdata/buildout/objects.sdb")).use { set ->
				while (set.next()) {
					try {
						loadStandardBuildout(events, set)
					} catch (e: Exception) {
						Log.e("Unable to load standard buildout with id %d, template_crc %d", set.getInt("id"), set.getInt("template_crc"))
					}
				}
			}
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	private fun loadStandardBuildout(events: Set<String>, set: SdbResultSet) {
		// "id", "template_crc", "container_id", "event", "terrain", "x", "y", "z", "orientation_x", "orientation_y", "orientation_z", "orientation_w", "cell_index", "tag"
		val event = set.getText(3)
		if (!event.isEmpty() && !events.contains(event)) return

		val obj = ObjectCreator.createObjectFromTemplate(set.getInt(0), CRC_DATABASE.getString(set.getInt(1).toInt()))
		obj.isGenerated = false
		obj.location = Location.builder().setPosition(set.getReal(5), set.getReal(6), set.getReal(7)).setOrientation(set.getReal(8), set.getReal(9), set.getReal(10), set.getReal(11)).setTerrain(Terrain.valueOf(set.getText(4))).build()
		obj.buildoutTag = set.getText(13)
		if (set.getInt(12) != 0L) {
			val building = objectMap[set.getInt(2)] as BuildingObject?
			val cell = building!!.getCellByNumber(set.getInt(12).toInt())
			obj.systemMove(cell)
		} else if (obj is BuildingObject) {
			obj.populateCells()
			for (cell in obj.getContainedObjects()) objectMap[cell.objectId] = cell
			if (!obj.getBuildoutTag().isEmpty()) buildingMap[obj.getBuildoutTag()] = obj
		}
		objectMap[set.getInt(0)] = obj
	}

	private fun loadAdditionalBuildouts() {
		try {
			SdbLoader.load(File("serverdata/buildout/additional_buildouts.msdb")).use { set ->
				while (set.next()) {
					if (!set.getBoolean("active")) continue
					createAdditionalObject(set)
				}
			}
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	private fun createAdditionalObject(set: SdbResultSet) {
		try {
			val obj = ObjectCreator.createObjectFromTemplate(set.getText("template"))
			obj.setPosition(set.getReal("x"), set.getReal("y"), set.getReal("z"))
			obj.terrain = Terrain.getTerrainFromName(set.getText("terrain"))
			obj.setHeading(set.getReal("heading"))
			obj.isGenerated = false
			checkParent(obj, set.getText("building_name"), set.getInt("cell_id").toInt())
			if (obj is BuildingObject) {
				obj.populateCells()
				for (cell in obj.getContainedObjects()) objectMap[cell.objectId] = cell
			}
			objectMap[obj.objectId] = obj
		} catch (e: ObjectCreationException) {
			Log.e("Invalid additional object: %s", set.getText("template"))
		}
	}

	private fun checkParent(obj: SWGObject, buildingName: String, cellId: Int) {
		if (buildingName.endsWith("_world")) return
		val building = buildingMap[buildingName]
		if (building == null) {
			Log.e("Building not found in map: %s", buildingName)
			return
		}

		val cell = building.getCellByNumber(cellId)
		if (cell == null) {
			Log.e("Cell is not found! Building: %s Cell: %d", buildingName, cellId)
			return
		}
		obj.systemMove(cell)
	}

	private class BuildoutInfo(set: SdbResultSet) {
		val id: Long = set.getInt("id")
		val crc: Int = set.getInt("template_crc").toInt()
		val containerId: Long = set.getInt("container_id")
		val event: String = set.getText("event")
		val terrain: Terrain = Terrain.valueOf(set.getText("terrain"))
		val x: Double = set.getReal("x")
		val y: Double = set.getReal("y")
		val z: Double = set.getReal("z")
		val orientationX: Double = set.getReal("orientation_x")
		val orientationY: Double = set.getReal("orientation_y")
		val orientationZ: Double = set.getReal("orientation_z")
		val orientationW: Double = set.getReal("orientation_w")
		val cellIndex: Int = set.getInt("cell_index").toInt()
		val tag: String = set.getText("tag")
	}

	companion object {
		private val CRC_DATABASE: CrcDatabase = CrcDatabase.getInstance()

		fun load(events: Collection<String>): BuildoutLoader {
			val loader = BuildoutLoader(events)
			loader.loadFromFile()
			return loader
		}
	}
}
