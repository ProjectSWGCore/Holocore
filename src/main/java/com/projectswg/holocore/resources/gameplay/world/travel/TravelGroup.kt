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
package com.projectswg.holocore.resources.gameplay.world.travel

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import me.joshlarson.jlcommon.concurrency.Delay
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class TravelGroup(landTime: Long, groundTime: Long, airTime: Long) : Runnable {
	private val points: MutableList<TravelPoint>
	private val timeRemaining: AtomicLong
	private val landTime: Long
	private val groundTime: Long
	private val airTime: Long
	var status: ShuttleStatus
		private set
	
	init {
		points = CopyOnWriteArrayList()
		timeRemaining = AtomicLong(airTime / 1000)
		this.landTime = landTime + 10000
		this.groundTime = groundTime
		this.airTime = airTime
		status = ShuttleStatus.GROUNDED
	}
	
	fun addTravelPoint(point: TravelPoint) {
		points.add(point)
	}
	
	fun getTimeRemaining(): Int {
		return timeRemaining.toInt()
	}
	
	override fun run() {
		try {
			while (!Delay.isInterrupted()) {
				// GROUNDED
				handleStatusGrounded()
				// LEAVING
				handleStatusLeaving()
				// AWAY
				handleStatusAway()
				// LANDING
				handleStatusLanding()
			}
		} catch (e: Exception) {
			Log.e(e)
		}
	}
	
	private fun handleStatusGrounded() {
		status = ShuttleStatus.GROUNDED
		Delay.sleepMilli(groundTime)
	}
	
	private fun handleStatusLeaving() {
		status = ShuttleStatus.LEAVING
		updateShuttlePostures(false)
		Delay.sleepMilli(landTime)
	}
	
	private fun handleStatusAway() {
		status = ShuttleStatus.AWAY
		for (timeElapsed in 0 until airTime / 1000) {
			if (Delay.sleepSeconds(1)) break
			timeRemaining.decrementAndGet()
		}
		timeRemaining.set(airTime / 1000) // Reset the timer
	}
	
	private fun handleStatusLanding() {
		status = ShuttleStatus.LANDING
		updateShuttlePostures(true)
		Delay.sleepMilli(landTime)
	}
	
	private fun updateShuttlePostures(landed: Boolean) {
		for (tp in points) {
			val shuttle = tp.shuttle ?: continue // No associated shuttle 
			val starport = tp.starport
			if (starport != null) {
				val effectFile = if (landed) "voice/sound/voice_starport_transport_arriving.snd" else "voice/sound/voice_starport_transport_away.snd"
				val effect = PlayClientEffectObjectMessage(effectFile, "", shuttle.objectId, "")
				starport.runOnChildObjectsRecursively { obj -> obj.sendSelf(effect) }
			}
			shuttle.posture = if (landed) Posture.UPRIGHT else Posture.PRONE
		}
	}
	
	enum class ShuttleStatus {
		LANDING, GROUNDED, LEAVING, AWAY
	}
}
