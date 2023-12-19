/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.player.badge

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Point3D
import com.projectswg.holocore.intents.gameplay.player.badge.GrantBadgeIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectTeleportIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.ExplorationBadgeLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.explorationBadges
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class ExplorationBadgeService : Service() {
	@IntentHandler
	private fun handlePlayerTransformedIntent(pti: PlayerTransformedIntent) {
		val creatureObject = pti.player
		val explorationBadgeInfo = checkExplorationRegions(creatureObject) ?: return
		
		if (!hasBadge(creatureObject, explorationBadgeInfo)) {
			GrantBadgeIntent.broadcast(creatureObject, explorationBadgeInfo.badgeName)
		}
	}
	@IntentHandler
	private fun handleObjectTeleportedIntent(oti: ObjectTeleportIntent) {
		val creatureObject = oti.`object` as? CreatureObject ?: return
		val explorationBadgeInfo = checkExplorationRegions(creatureObject) ?: return
		
		if (!hasBadge(creatureObject, explorationBadgeInfo)) {
			GrantBadgeIntent.broadcast(creatureObject, explorationBadgeInfo.badgeName)
		}
	}

	private fun hasBadge(creatureObject: CreatureObject, explorationBadgeInfo: ExplorationBadgeLoader.ExplorationBadgeInfo): Boolean {
		val playerObject = creatureObject.playerObject
		val badgeSlot = explorationBadgeInfo.badgeSlot.toInt()

		return playerObject.badges.hasBadge(badgeSlot)
	}

	private fun checkExplorationRegions(creature: CreatureObject): ExplorationBadgeLoader.ExplorationBadgeInfo? {
		val worldLocation = creature.worldLocation
		val explorationBadges = explorationBadges.getExplorationBadges(worldLocation.terrain)

		for (explorationBadge in explorationBadges) {
			if (closeEnough(worldLocation, explorationBadge)) {
				return explorationBadge
			}
		}

		return null
	}

	private fun closeEnough(worldLocation: Location, explorationBadge: ExplorationBadgeLoader.ExplorationBadgeInfo): Boolean {
		val point3D = Point3D(explorationBadge.x.toDouble(), 0.0, explorationBadge.y.toDouble())
		val radius = explorationBadge.radius.toDouble()
		return worldLocation.isWithinFlatDistance(point3D, radius)
	}
}
