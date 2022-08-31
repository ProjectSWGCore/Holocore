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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import java.util.*

class WaypointCmdCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val ghost = player.playerObject ?: return
		val waypointParameters = EnumMap<Parameter, Any>(Parameter::class.java)
		var currentState = Parameter.INITIAL
		var argumentIndex = -1
		while (argumentIndex < args.length) {
			// If we're the last argument (NAME), parse the remainder of the args
			val nextArgumentIndex = if (currentState == Parameter.COLOR) args.length else args.indexOf(' ', startIndex=argumentIndex+1)
			// Grab the argument
			val nextArgument = args.substring(argumentIndex+1, if (nextArgumentIndex == -1) args.length else nextArgumentIndex)
			if (nextArgument.isEmpty()) {
				argumentIndex = nextArgumentIndex
				if (nextArgumentIndex == -1)
					break
				continue
			}
			
			val nextArgumentParsed = nextArgument.toDoubleOrNull()
			if (nextArgumentParsed == null) {
				// Find the next state that is a string
				val nextState = currentState.nextStates.firstOrNull { !it.float && it != Parameter.END } ?: break
				waypointParameters[nextState] = nextArgument
				currentState = nextState
			} else {
				// Find the next state that is a float
				val nextState = currentState.nextStates.firstOrNull { it.float && it != Parameter.END } ?: break
				waypointParameters[nextState] = nextArgumentParsed
				currentState = nextState
			}
			if (nextArgumentIndex == -1)
				break
			argumentIndex = nextArgumentIndex
		}
		
		if (!currentState.nextStates.contains(Parameter.END)) {
			SystemMessageIntent.broadcastPersonal(player, "Failed to parse waypoint command!")
			return
		}
		val color = when((waypointParameters[Parameter.COLOR] as String?)?.lowercase(Locale.US)) {
			"white" -> WaypointColor.WHITE
			"purple" -> WaypointColor.PURPLE
			"green" -> WaypointColor.GREEN
			"yellow" -> WaypointColor.YELLOW
			"orange" -> WaypointColor.ORANGE
			else -> WaypointColor.BLUE
		}
		val name = waypointParameters.getOrDefault(Parameter.NAME, "Waypoint") as String
		val location = if (waypointParameters.containsKey(Parameter.Z))
				Location.builder()
						.setTerrain(Terrain.getTerrainFromName(waypointParameters[Parameter.TERRAIN] as String))
						.setX(waypointParameters[Parameter.X] as Double)
						.setZ(waypointParameters[Parameter.Z] as Double)
						.setY(waypointParameters.getOrDefault(Parameter.Y, 0.0) as Double)
						.build()
			else
				player.creatureObject.worldLocation
		
		if (!ghost.addWaypoint(createWaypoint(color, name, location))) SystemMessageIntent.broadcastPersonal(player, "@base_player:too_many_waypoints")
		if (player.creatureObject.terrain != location.terrain) {
			SystemMessageIntent(player, "Waypoint: New waypoint \"" + name + "\" created for location "
					+ location.terrain.getName() + " (" + String.format("%.0f", location.x) + ", "
					+ String.format("%.0f", location.y) + ", " + String.format("%.0f", location.z) + ')').broadcast()
		} else {
			SystemMessageIntent(player, "Waypoint: New waypoint \"" + name + "\" created for location ("
					+ String.format("%.0f", location.x) + ", " + String.format("%.0f", location.y) + ", " + String.format("%.0f", location.z) + ')').broadcast()
		}
	}
	
	private fun createWaypoint(color: WaypointColor, name: String, location: Location): WaypointObject {
		val waypoint = ObjectCreator.createObjectFromTemplate("object/waypoint/shared_waypoint.iff") as WaypointObject
		waypoint.setPosition(location.terrain, location.x, location.y, location.z)
		waypoint.color = color
		waypoint.name = name
		ObjectCreatedIntent(waypoint).broadcast()
		return waypoint
	}
	
	private enum class Parameter(val float: Boolean, val nextStates: List<Parameter>) {
		END     (false, listOf()),
		NAME    (false, listOf(END)),
		COLOR   (false, listOf(NAME, END)),
		Y       (true, listOf(COLOR, END)),
		Z       (true, listOf(COLOR, Y, END)),
		X       (true, listOf(Z)),
		TERRAIN (false, listOf(X)),
		INITIAL (false, listOf(TERRAIN, X, END));
	}
	
}
