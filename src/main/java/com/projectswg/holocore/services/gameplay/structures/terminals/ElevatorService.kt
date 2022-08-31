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

package com.projectswg.holocore.services.gameplay.structures.terminals

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.ElevatorLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

class ElevatorService : Service() {
	
	private val playersInElevators = HashMap<CreatureObject, SuiListBox?>()
	private val playersInElevatorsLock = ReentrantLock()
	
	@IntentHandler
	private fun handleMoveObjectIntent(moi: MoveObjectIntent) {
		val creature = moi.`object` as? CreatureObject ?: return
		if (!creature.isPlayer)
			return
		// Now dealing with players only
		
		// Not going to be in a cell
		val cell = if (moi.parent == null) null else moi.parent as? CellObject
		if (cell == null) {
			removeFromElevatorMap(creature)
			return
		}
		
		// Not going to be in a building
		val building = moi.parent.parent as? BuildingObject
		if (building == null) {
			removeFromElevatorMap(creature)
			return
		}
		
		// Not in an elevator
		val floors = ServerData.elevators.getFloors(building.template, cell.number)
		if (floors == null) {
			removeFromElevatorMap(creature)
			return
		}
		
		// We're going to be in an elevator o_O
		if (addToElevatorMap(creature)) {
			if (floors.size < 2) {
				return // ?!
			} else if (floors.size == 2) {
				// Just jump straight to the other floor if there's only one option
				if (isCurrentFloor(creature, floors[0]))
					teleportToFloor(creature, building, floors, 1)
				else
					teleportToFloor(creature, building, floors, 0)
			} else {
				provideFloorSelection(creature, building, floors)
			}
		}
	}
	
	private fun removeFromElevatorMap(creature: CreatureObject) {
		playersInElevatorsLock.withLock {
			val previousElevatorPrompt = playersInElevators.remove(creature)
			previousElevatorPrompt?.close(creature.owner ?: return)
		}
	}
	
	private fun addToElevatorMap(creature: CreatureObject): Boolean {
		playersInElevatorsLock.withLock {
			if (playersInElevators.containsKey(creature))
				return false
			playersInElevators[creature] = null
			return true
		}
	}
	
	private fun updateElevatorMap(creature: CreatureObject, listBox: SuiListBox) {
		val player = creature.owner
		playersInElevatorsLock.withLock {
			if (player != null)
				playersInElevators[creature]?.close(player)
			playersInElevators[creature] = listBox
		}
	}
	
	private fun removeListBoxFromElevatorMap(creature: CreatureObject) {
		playersInElevatorsLock.withLock {
			if (creature in playersInElevators)
				playersInElevators[creature] = null
		}
	}
	
	private fun provideFloorSelection(creature: CreatureObject, building: BuildingObject, floors: List<ElevatorLoader.ElevatorInfo>) {
		val listBox = SuiListBox(SuiButtons.OK_CANCEL, "Elevator", "Select your desired floor.")
		
		for (floor in floors) {
			listBox.addListItem(getFloorString(creature, floor))
		}
		
		listBox.addCancelButtonCallback("handleFloorCancel") { _, _ -> removeListBoxFromElevatorMap(creature) }
		listBox.addCallback(SuiEvent.OK_PRESSED, "handleFloorSelection") { _, parameters: Map<String?, String?>? ->
			val selection = SuiListBox.getSelectedRow(parameters)
			if (selection < 0 || selection >= floors.size)
				return@addCallback
			
			teleportToFloor(creature, building, floors, selection)
			removeListBoxFromElevatorMap(creature)
		}
		
		listBox.display(creature.owner ?: return)
		updateElevatorMap(creature, listBox)
	}
	
	private fun teleportToFloor(creature: CreatureObject, building: BuildingObject, floors: List<ElevatorLoader.ElevatorInfo>, targetFloor: Int) {
		val currentFloor = floors.indexOfFirst { isCurrentFloor(creature, it) }
		if (currentFloor == -1)
			return // We weren't in the elevator at all... I guess
		
		if (targetFloor < 0 || targetFloor >= floors.size)
			return // Internal error?
		
		if (currentFloor == targetFloor)
			return // Well that was quick
		
		if (targetFloor > currentFloor) // These are actually inverted for list purposes
			creature.sendSelf(PlayClientEffectObjectMessage("clienteffect/elevator_descend.cef", "", creature.objectId, ""))
		else
			creature.sendSelf(PlayClientEffectObjectMessage("clienteffect/elevator_rise.cef", "", creature.objectId, ""))
		
		val newLocation = Location.builder(creature.location)
			.setY(floors[targetFloor].locationY)
			.build()
		
		creature.moveToContainer(building.getCellByNumber(floors[targetFloor].cell), newLocation)
		SystemMessageIntent.broadcastPersonal(creature.owner ?: return, "Now on floor ${floors[targetFloor].floor}")
	}
	
	companion object {
		
		private fun getFloorString(creature: CreatureObject, floor: ElevatorLoader.ElevatorInfo): String {
			val currentFloorString = if (isCurrentFloor(creature, floor)) " (current)" else ""
			return "Floor ${floor.floor}$currentFloorString"
		}
		
		private fun isCurrentFloor(creature: CreatureObject, floor: ElevatorLoader.ElevatorInfo): Boolean {
			return abs(creature.location.y - floor.locationY) < 0.1
		}
		
	}

}
