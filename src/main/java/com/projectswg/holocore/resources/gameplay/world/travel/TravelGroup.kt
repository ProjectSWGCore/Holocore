/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.world.travel

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class TravelGroup(private val landTime: Long, private val groundTime: Long, private val airTime: Long) {
	private val points: MutableList<TravelPoint> = CopyOnWriteArrayList()
	private val timeRemaining: AtomicLong = AtomicLong(airTime / 1000)
	var status: ShuttleStatus
		private set
	
	init {
		status = ShuttleStatus.GROUNDED
	}
	
	fun addTravelPoint(point: TravelPoint) {
		points.add(point)
	}
	
	fun getTimeRemaining(): Int {
		return timeRemaining.toInt()
	}
	
	fun launch(scope: CoroutineScope) {
		scope.launch {
			while (isActive) {
				// GROUNDED
				handleStatusGrounded()
				// LEAVING
				handleStatusLeaving()
				// AWAY
				handleStatusAway()
				// LANDING
				handleStatusLanding()
			}
		}
	}
	
	private suspend fun handleStatusGrounded() {
		status = ShuttleStatus.GROUNDED
		delay(groundTime * 1000L)
	}
	
	private suspend fun handleStatusLeaving() {
		status = ShuttleStatus.LEAVING
		updateShuttlePostures(false)
		delay(landTime * 1000L)
	}
	
	private suspend fun handleStatusAway() {
		status = ShuttleStatus.AWAY
		timeRemaining.set(airTime + landTime) // Reset the timer
		for (timeElapsed in 0 until airTime) {
			delay(1000L)
			timeRemaining.decrementAndGet()
		}
	}
	
	private suspend fun handleStatusLanding() {
		status = ShuttleStatus.LANDING
		updateShuttlePostures(true)
		timeRemaining.set(landTime) // Reset the timer
		for (timeElapsed in 0 until landTime) {
			delay(1000L)
			timeRemaining.decrementAndGet()
		}
	}
	
	private fun updateShuttlePostures(landed: Boolean) {
		for (tp in points) {
			val shuttle = tp.shuttle ?: continue // No associated shuttle
			val starport = tp.starport
			if (starport != null) {
				val soundFile = if (landed) "sound/sys_comm_generic.snd" else "sound/sys_comm_generic.snd"
				val effect = PlayMusicMessage(0, soundFile, 1, false)
				starport.runOnChildObjectsRecursively { obj -> obj.sendSelf(effect) }
			}
			shuttle.posture = if (landed) Posture.UPRIGHT else Posture.PRONE
		}
	}
	
	enum class ShuttleStatus {
		LANDING, GROUNDED, LEAVING, AWAY
	}
}
