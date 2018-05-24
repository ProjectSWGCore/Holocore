/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.scripts.commands.admin

import com.projectswg.holocore.resources.objects.SWGObject
import com.projectswg.holocore.resources.objects.building.BuildingObject
import com.projectswg.holocore.resources.objects.cell.CellObject
import com.projectswg.holocore.resources.player.Player
import com.projectswg.holocore.services.galaxy.GalacticManager
import com.projectswg.holocore.utilities.IntentFactory

static def execute(Player player, SWGObject target, String args) {
	args = args.trim()
	def split = args.split(" ")
	if (split.size().intValue() == 0 || split[0].isEmpty()) {
		def creature = player.getCreatureObject()
		def worldLocation = creature.getWorldLocation()
		def cellLocation = creature.getLocation()
		def parent = creature.getParent()
		IntentFactory.sendSystemMessage(player, "Position: " + worldLocation.getPosition())
		IntentFactory.sendSystemMessage(player, "Orientation: " + worldLocation.getOrientation())
		if (parent != null) {
			IntentFactory.sendSystemMessage(player, "  Cell Position: " + cellLocation.getPosition())
			IntentFactory.sendSystemMessage(player, "  Cell Orientation: " + cellLocation.getOrientation())
			if (parent instanceof CellObject) {
				IntentFactory.sendSystemMessage(player, "  Cell ID/Name: " + parent.getNumber() + " / " + parent.getCellName())
			} else {
				IntentFactory.sendSystemMessage(player, "  Parent ID/Type: " + parent.getObjectId() + " / " + parent.getClass().getSimpleName())
				IntentFactory.sendSystemMessage(player, "  Parent Template: " + parent.getTemplate())
			}
			def grandparent = parent.getParent()
			if (grandparent != null) {
				IntentFactory.sendSystemMessage(player, "    Grandparent ID/Type: " + grandparent.getObjectId() + " / " + grandparent.getClass().getSimpleName())
				IntentFactory.sendSystemMessage(player, "    Grandparent Template: " + grandparent.getTemplate())
			}
		}
	} else if (split[0].equalsIgnoreCase("all_cells")) {
		def creature = player.getCreatureObject()
		def parent = creature.getParent()
		if (parent != null) {
			def grandparent = parent.getParent()
			if (grandparent != null) {
				if (grandparent instanceof BuildingObject) {
					def cells = grandparent.getCells()
					IntentFactory.sendSystemMessage(player, "Cell Count: " + cells.size())
					for (int i = 0; i < cells.size(); i++) {
						def cell = cells.get(i)
						IntentFactory.sendSystemMessage(player, "    " + cell.getNumber() + " / " + cell.getCellName())
					}
				} else {
					IntentFactory.sendSystemMessage(player, "Duuuude, you gotta be in a building")
				}
			} else {
				IntentFactory.sendSystemMessage(player, "No grandfather fo u")
			}
		} else {
			IntentFactory.sendSystemMessage(player, "Get in a container bro")
		}
	}
}
