/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.resources.support.objects.swg.custom

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.holocore.intents.support.npc.ai.CompileNpcMovementIntent
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent
import com.projectswg.holocore.resources.support.npc.ai.NavigationPoint
import com.projectswg.holocore.resources.support.npc.ai.NavigationRouteType
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

abstract class NpcMode(val ai: AIObject) {
	abstract fun act()

	fun onPlayerEnterAware(player: CreatureObject, distance: Double) {
	}

	open fun onPlayerMoveInAware(player: CreatureObject, distance: Double) {
	}

	open fun onPlayerExitAware(player: CreatureObject) {
	}

	open fun onModeStart() {
	}

	open fun onModeEnd() {
	}

	val nearbyPlayers: Collection<CreatureObject>
		get() = ai.nearbyPlayers

	val isRooted: Boolean
		get() = when (ai.posture) {
			Posture.DEAD, Posture.INCAPACITATED, Posture.INVALID, Posture.KNOCKED_DOWN, Posture.LYING_DOWN, Posture.SITTING                                                                                   -> true
			Posture.BLOCKING, Posture.CLIMBING, Posture.CROUCHED, Posture.DRIVING_VEHICLE, Posture.FLYING, Posture.PRONE, Posture.RIDING_CREATURE, Posture.SKILL_ANIMATING, Posture.SNEAKING, Posture.UPRIGHT ->                // Rooted if there are no nearby players
				nearbyPlayers.isEmpty()

			else                                                                                                                                                                                              -> nearbyPlayers.isEmpty()
		}

	val spawner: Spawner?
		get() = ai.spawner

	fun queueNextLoop(delay: Long) {
		ai.queueNextLoop(delay)
	}

	val walkSpeed: Double
		get() = (ai.movementPercent * ai.movementScale * ai.walkSpeed).toDouble()

	val runSpeed: Double
		get() = (ai.movementPercent * ai.movementScale * ai.runSpeed).toDouble()

	fun moveTo(parent: SWGObject?, location: Location?) {
		MoveObjectIntent(ai, parent, location!!, walkSpeed).broadcast()
	}

	fun moveTo(location: Location?) {
		MoveObjectIntent(ai, location!!, walkSpeed).broadcast()
	}

	fun walkTo(parent: SWGObject?, location: Location?) {
		CompileNpcMovementIntent(ai, NavigationPoint.from(ai.parent, ai.location, parent, location!!, walkSpeed), NavigationRouteType.TERMINATE, walkSpeed, null).broadcast()
	}

	fun walkTo(location: Location?) {
		CompileNpcMovementIntent(ai, NavigationPoint.from(ai.parent, ai.location, location!!, walkSpeed), NavigationRouteType.TERMINATE, walkSpeed, null).broadcast()
	}

	fun runTo(parent: SWGObject?, location: Location?) {
		CompileNpcMovementIntent(ai, NavigationPoint.from(ai.parent, ai.location, parent, location!!, runSpeed), NavigationRouteType.TERMINATE, runSpeed, null).broadcast()
	}

	fun runTo(location: Location?) {
		CompileNpcMovementIntent(ai, NavigationPoint.from(ai.parent, ai.location, location!!, runSpeed), NavigationRouteType.TERMINATE, runSpeed, null).broadcast()
	}
}
