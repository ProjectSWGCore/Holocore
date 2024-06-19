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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject

class CmdDumpZoneInformation : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		var args = args
		args = args.trim { it <= ' ' }
		val split = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		if (split.isEmpty() || split[0].isEmpty()) {
			val creature = player.creatureObject
			val worldLocation = creature.worldLocation
			val cellLocation = creature.location
			val parent = creature.parent
			broadcastPersonal(player, "Position: " + worldLocation.position)
			broadcastPersonal(player, "Orientation: " + worldLocation.orientation)
			if (parent != null) {
				broadcastPersonal(player, "  Cell Position: " + cellLocation.position)
				broadcastPersonal(player, "  Cell Orientation: " + cellLocation.orientation)
				if (parent is CellObject) {
					broadcastPersonal(player, "  Cell ID/Name: " + parent.number + " / " + parent.cellName)
				} else {
					broadcastPersonal(player, "  Parent ID/Type: " + parent.objectId + " / " + parent.javaClass.simpleName)
					broadcastPersonal(player, "  Parent Template: " + parent.template)
				}
				val grandparent = parent.parent
				if (grandparent != null) {
					broadcastPersonal(player, "    Grandparent ID/Type: " + grandparent.objectId + " / " + grandparent.javaClass.simpleName)
					broadcastPersonal(player, "    Grandparent Template: " + grandparent.template)
				}
			}
		} else if (split[0].equals("all_cells", ignoreCase = true)) {
			val creature = player.creatureObject
			val parent = creature.parent
			if (parent != null) {
				val grandparent = parent.parent
				if (grandparent != null) {
					if (grandparent is BuildingObject) {
						val cells = grandparent.cells
						broadcastPersonal(player, "Cell Count: " + cells.size)
						for (cell in cells) {
							broadcastPersonal(player, "    " + cell.number + " / " + cell.cellName)
						}
					} else {
						broadcastPersonal(player, "Duuuude, you gotta be in a building")
					}
				} else {
					broadcastPersonal(player, "No grandfather fo u")
				}
			} else {
				broadcastPersonal(player, "Get in a container bro")
			}
		}
	}
}
