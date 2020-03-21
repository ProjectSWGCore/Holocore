/***********************************************************************************
 * Copyright (c) 2020 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.data.server_info.database

interface PswgGcwRegionDatabase {
	
	fun createZone(zoneName: String, basePoints: Long)
	fun setImperialPoints(zoneName: String, points: Long)
	fun setRebelPoints(zoneName: String, points: Long)
	fun getZone(zoneName: String): ZoneMetadata?
	
	companion object {
		
		fun createDefault(): PswgGcwRegionDatabase {
			return object : PswgGcwRegionDatabase {
				override fun createZone(zoneName: String, basePoints: Long) {}
				override fun setImperialPoints(zoneName: String, points: Long) {}
				override fun setRebelPoints(zoneName: String, points: Long) {}
				override fun getZone(zoneName: String): ZoneMetadata? = null
			}
		}
		
	}
	
}

class ZoneMetadata(val zone: String, val imperialPoints: Long, val rebelPoints: Long) {
	
	override fun toString(): String = "ZoneMetadata[zone=$zone imperialPoints=$imperialPoints rebelPoints=$rebelPoints]"
	override fun equals(other: Any?): Boolean = if (other is ZoneMetadata) zone == other.zone else false
	override fun hashCode(): Int = zone.hashCode()
	
}
