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
package com.projectswg.holocore.resources.support.data.location

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Point3D
import com.projectswg.common.data.location.Quaternion
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class InstanceLocation : MongoPersistable {
	var location: Location = Location(0.0, 0.0, 0.0, Terrain.GONE)
	var instanceType: InstanceType = InstanceType.NONE
	var instanceNumber: Int = 0

	override fun readMongo(data: MongoData) {
		instanceNumber = data.getInteger("number", 0)
		instanceType = InstanceType.valueOf(data.getString("type", "NONE"))
		location = data.getDocument("location", Location())!!
	}

	override fun saveMongo(data: MongoData) {
		data.putInteger("number", instanceNumber)
		data.putString("type", instanceType.name)
		data.putDocument("location", location)
	}

	fun setPosition(terrain: Terrain?, x: Double, y: Double, z: Double) {
		checkNotNull(terrain) { "terrain is null" }
		this.location = Location.builder(this.location).setTerrain(terrain).setPosition(x, y, z).build()
	}

	fun setPosition(x: Double, y: Double, z: Double) {
		location = Location.builder(location).setPosition(x, y, z).build()
	}

	fun setOrientation(x: Double, y: Double, z: Double, w: Double) {
		location = Location.builder(location).setOrientation(x, y, z, w).build()
	}

	fun setHeading(heading: Double) {
		location = Location.builder(location).setHeading(heading).build()
	}

	val position: Point3D
		get() = location.position

	var terrain: Terrain
		get() = location.terrain
		set(terrain) {
			if (location.terrain == terrain) return
			location = Location.builder(location).setTerrain(terrain).build()
		}

	val orientation: Quaternion
		get() = location.orientation

	val positionX: Double
		get() = location.x

	val positionY: Double
		get() = location.y

	val positionZ: Double
		get() = location.z

	val orientationX: Double
		get() = location.orientationX

	val orientationY: Double
		get() = location.orientationY

	val orientationZ: Double
		get() = location.orientationZ

	val orientationW: Double
		get() = location.orientationW

	fun getHeadingTo(target: Location?): Double {
		return location.getHeadingTo(target)
	}

	fun getHeadingTo(target: Point3D?): Double {
		return location.getHeadingTo(target)
	}

	fun getWorldLocation(self: SWGObject): Location {
		val parent = self.superParent ?: return location
		if (self.slotArrangement != -1) return parent.worldLocation
		return Location.builder(location).translateLocation(parent.location).setTerrain(parent.terrain).build()
	}
}
