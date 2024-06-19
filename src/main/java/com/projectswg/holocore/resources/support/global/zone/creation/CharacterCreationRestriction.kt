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
package com.projectswg.holocore.resources.support.global.zone.creation

import com.projectswg.holocore.resources.support.global.player.Player
import me.joshlarson.jlcommon.log.Log
import java.util.*
import java.util.concurrent.TimeUnit

class CharacterCreationRestriction(private var creationsPerPeriod: Int) {

	private val restrictions: MutableMap<String, PlayerRestriction> = HashMap()

	fun setCreationsPerPeriod(creationsPerPeriod: Int) {
		this.creationsPerPeriod = creationsPerPeriod
		synchronized(restrictions) {
			for (pr in restrictions.values) pr.setCreationsPerPeriod(creationsPerPeriod)
		}
	}

	fun isAbleToCreate(player: Player): Boolean {
		val pr = getRestriction(player)
		return pr.isAbleToCreate
	}

	fun createdCharacter(player: Player): Boolean {
		val pr = getRestriction(player)
		return pr.createdCharacter()
	}

	private fun getRestriction(player: Player): PlayerRestriction {
		var pr: PlayerRestriction?
		synchronized(restrictions) {
			pr = restrictions[player.username]
		}
		if (pr == null) {
			pr = PlayerRestriction(creationsPerPeriod)
			synchronized(restrictions) {
				restrictions.put(player.username, pr!!)
			}
		}
		return pr!!
	}

	private class PlayerRestriction(creationsPerPeriod: Int) {
		private val lastCreations: Deque<Long> = LinkedList()
		private var creationsPerPeriod = 0

		init {
			setCreationsPerPeriod(creationsPerPeriod)
		}

		fun setCreationsPerPeriod(creationsPerPeriod: Int) {
			this.creationsPerPeriod = creationsPerPeriod
		}

		val isAbleToCreate: Boolean
			get() {
				synchronized(lastCreations) {
					return lastCreations.size < creationsPerPeriod || creationsPerPeriod == 0 || !isWithinPeriod(lastCreations.last)
				}
			}

		fun createdCharacter(): Boolean {
			if (creationsPerPeriod == 0) return true
			synchronized(lastCreations) {
				val hitMax = lastCreations.size >= creationsPerPeriod
				val hackSuccess = hitMax && isWithinPeriod(lastCreations.last)
				val time = now()
				if (hackSuccess) {
					val state = lastCreations.toTypedArray<Long>().contentToString()
					Log.e("Character created when not allowed! Current time/state: %s/%s", time, state)
				}
				if (hitMax) lastCreations.pollLast()
				lastCreations.addFirst(time)
				return !hackSuccess
			}
		}
	}

	companion object {
		private val TIME_INCREMENT = TimeUnit.MINUTES.toMillis(15)

		private fun now(): Long {
			return System.currentTimeMillis()
		}

		private fun isWithinPeriod(time: Long): Boolean {
			val cur = now()
			return time > (cur - TIME_INCREMENT) && time <= cur
		}
	}
}
