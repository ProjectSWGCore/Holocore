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
package com.projectswg.holocore.resources.gameplay.world.travel

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class TravelPoint // Not sure which effect this has on the client.
(val name: String, val location: Location, val group: TravelGroup?, val isStarport: Boolean) : Comparable<TravelPoint> {
	val isReachable = true
	var shuttle: CreatureObject? = null
	var collector: SWGObject? = null
	var starport: BuildingObject? = null
	
	val terrain: Terrain
		get() = location.terrain
	
	fun isWithinRange(`object`: SWGObject): Boolean {
		return collector!!.worldLocation.isWithinFlatDistance(`object`.worldLocation, MAX_USE_DISTANCE)
	}
	
	val suiFormat: String
		get() = String.format("@planet_n:%s -- %s", location.terrain.getName(), name)
	
	override fun compareTo(other: TravelPoint): Int {
		val comp = location.terrain.compareTo(other.location.terrain)
		return if (comp != 0) comp else name.compareTo(other.name)
	}
	
	override fun equals(other: Any?): Boolean {
		return if (other !is TravelPoint) false else name == other.name
	}
	
	override fun hashCode(): Int {
		return name.hashCode()
	}
	
	override fun toString(): String {
		return String.format("TravelPoint[name=%s location=%s %s", name, location.terrain, location.position)
	}
	
	companion object {
		private const val MAX_USE_DISTANCE = 8.0
	}
	
}
