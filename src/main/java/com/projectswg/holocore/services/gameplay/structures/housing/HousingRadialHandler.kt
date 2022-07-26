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

package com.projectswg.holocore.services.gameplay.structures.housing

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject

class HousingRadialHandler : RadialHandlerInterface {
	
	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		if (target !is TangibleObject)
			return
		
		if (canDrop(player.creatureObject, target))
			getInventoryOptions(options, player, target)
		if (canPickup(player.creatureObject, target))
			options.add(RadialOption.create(RadialItem.ITEM_PICKUP))
	}
	
	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		val creature = player.creatureObject ?: return
		when (selection) {
			RadialItem.ITEM_DROP -> {
				if (canDrop(creature, target))
					target.moveToContainer(creature.parent, creature.location)
			}
			RadialItem.ITEM_PICKUP -> {
				if (canPickup(creature, target))
					target.moveToContainer(creature.inventory)
			}
			else -> {}
		}
	}
	
	private fun getInventoryOptions(options: MutableCollection<RadialOption>, player: Player, target: TangibleObject) {
		options.add(RadialOption.create(RadialItem.ITEM_DROP))
	}
	
	private fun canPickup(creature: CreatureObject?, target: SWGObject): Boolean {
		return !isInInventory(creature, target) && isInOwnedBuilding(creature, target.superParent)
	}
	
	private fun canDrop(creature: CreatureObject?, target: SWGObject): Boolean {
		return isInInventory(creature, target) && isInOwnedBuilding(creature, creature?.superParent)
	}
	
	private fun isInInventory(creature: CreatureObject?, target: SWGObject): Boolean {
		return target.parent == creature?.inventory
	}
	
	private fun isInOwnedBuilding(creature: CreatureObject?, superParent: SWGObject?): Boolean {
		val superParentAsBuilding = superParent as? BuildingObject ?: return false
		return superParentAsBuilding.playerStructureInfo?.owner == creature
	}
	
}