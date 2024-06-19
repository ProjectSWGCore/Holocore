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

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.player.PlayerState
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup

/**
 * @author Waverunner
 */
class FindFriendCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val ghost = player.playerObject

		if (ghost == null || args.isEmpty()) return

		val friendName = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].lowercase()

		if (!ghost.isFriend(friendName)) {
			SystemMessageIntent(player, "@ui_cmnty:friend_location_failed_noname").broadcast()
			return
		}

		val friend = PlayerLookup.getPlayerByFirstName(friendName)
		if (friend == null || friend.playerState != PlayerState.ZONED_IN) {
			SystemMessageIntent(player, ProsePackage(StringId("ui_cmnty", "friend_location_failed"), "TU", friendName)).broadcast()
			return
		}

		val friendGhost = friend.playerObject
		if (friendGhost == null || !friendGhost.isFriend(player.characterName.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].lowercase())) {
			SystemMessageIntent(player, ProsePackage(StringId("ui_cmnty", "friend_location_failed"), "TU", friendName)).broadcast()
			return
		}

		val location = friend.creatureObject.worldLocation

		var waypoint: WaypointObject? = null
		for ((_, waypointEntry) in ghost.waypoints) {
			if (waypointEntry.objectName != friendName) continue

			waypoint = waypointEntry
			break
		}

		if (waypoint == null) {
			waypoint = ObjectCreator.createObjectFromTemplate("object/waypoint/shared_waypoint.iff") as WaypointObject
			waypoint.setPosition(location.terrain, location.x, location.y, location.z)
			waypoint.color = WaypointColor.PURPLE
			waypoint.name = friendName
			if (!ghost.addWaypoint(waypoint)) broadcastPersonal(player, "@base_player:too_many_waypoints")
			ObjectCreatedIntent(waypoint).broadcast()
			SystemMessageIntent(player, ProsePackage(StringId("ui_cmnty", "friend_location_create_new_wp"), "TU", friendName)).broadcast()
		} else {
			waypoint.setPosition(location.terrain, location.x, location.y, location.z)
			ghost.updateWaypoint(waypoint)
			SystemMessageIntent(player, ProsePackage(StringId("ui_cmnty", "friend_location"), "TU", friendName)).broadcast()
		}
	}
}
