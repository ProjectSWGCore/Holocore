/***********************************************************************************
 * Copyright (c) 2022 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ElevatorLoader : DataLoader() {
	
	private val elevators = HashMap<String, HashMap<Int, ArrayList<ElevatorInfo>>>()
	
	fun getFloors(buildingTemplate: String, cellNumber: Int): List<ElevatorInfo>? {
		return Collections.unmodifiableList(elevators[buildingTemplate]?.get(cellNumber) ?: return null)
	}
	
	override fun load() {
		elevators.clear()
		SdbLoader.load(File("serverdata/elevator/elevator.sdb")).use { set ->
			set.stream { ElevatorInfo(it) }.forEach {
				if (it.buildingTemplate !in elevators)
					elevators[it.buildingTemplate] = HashMap()
				val buildingEntry = elevators[it.buildingTemplate]!!
				if (it.cell !in buildingEntry)
					buildingEntry[it.cell] = ArrayList()
				buildingEntry[it.cell]!!.add(it)
			}
		}
	}
	
	class ElevatorInfo(set: SdbLoader.SdbResultSet) {
		
		val buildingTemplate = set.getText("template")
		val cell = set.getInt("cell").toInt()
		val floor = set.getText("floor")
		val locationY = set.getReal("location")
		
	}
	
}