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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor
import com.projectswg.common.data.location.Point3D
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.SpecificObject
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject

class RequestWaypointCmdCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val cmd = args.split(" ".toRegex(), limit = 6).toTypedArray()
		if (cmd.size < 5) {
			broadcastPersonal(player, "Invalid number of arguments for waypoint! Expected 5 or 6")
			return
		}
		val color = WaypointColor.BLUE

		val terrain = Terrain.getTerrainFromName(cmd[0])

		val position = Point3D()
		position[cmd[1].toDouble(), cmd[2].toDouble()] = cmd[3].toDouble()

		val name = (if (cmd.size == 5 && cmd[4].isNotBlank()) cmd[4] else "@planet_n:" + terrain.getName())

		createWaypoint(player, terrain, position, color, name)
	}

	companion object {
		private fun createWaypoint(player: Player, terrain: Terrain, position: Point3D, color: WaypointColor, name: String) {
			val waypoint = ObjectCreator.createObjectFromTemplate(SpecificObject.SO_WAYPOINT.template) as WaypointObject
			waypoint.setPosition(terrain, position.x, position.y, position.z)
			waypoint.name = name
			waypoint.color = color
			if (!player.playerObject.addWaypoint(waypoint)) broadcastPersonal(player, "@base_player:too_many_waypoints")
			ObjectCreatedIntent(waypoint).broadcast()
		}
	}
}
