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
package com.projectswg.holocore.services.support.data

import com.projectswg.common.data.encodables.galaxy.Galaxy.GalaxyStatus
import com.projectswg.common.network.packets.swg.admin.AdminShutdownServer
import com.projectswg.holocore.ProjectSWG
import com.projectswg.holocore.intents.support.data.control.ServerStatusIntent
import com.projectswg.holocore.intents.support.data.control.ShutdownServerIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.resources.support.data.control.ServerStatus
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ServerStatusService : Service() {
	private val executor: ScheduledThreadPool = ScheduledThreadPool(1, Thread.MAX_PRIORITY, "shutdown-service")
	private val shutdownTime: AtomicLong = AtomicLong(0)
	private val operational: AtomicBoolean = AtomicBoolean(true)
	private val displayedShutdown: AtomicBoolean = AtomicBoolean(false)

	override fun initialize(): Boolean {
		executor.start()
		return true
	}

	override fun isOperational(): Boolean {
		return operational.get()
	}

	override fun terminate(): Boolean {
		executor.stop()
		executor.awaitTermination(1000)
		return super.terminate()
	}

	@IntentHandler
	private fun handleServerStatusIntent(ssi: ServerStatusIntent) {
		when (ssi.status) {
			ServerStatus.OFFLINE                          -> ProjectSWG.galaxy.status = GalaxyStatus.DOWN
			ServerStatus.INITIALIZING                     -> ProjectSWG.galaxy.status = GalaxyStatus.LOADING
			ServerStatus.OPEN                             -> ProjectSWG.galaxy.status = GalaxyStatus.UP
			ServerStatus.SHUTDOWN_REQUESTED               -> startShutdownClock(ssi.time, ssi.timeUnit)
			ServerStatus.LOCKED, ServerStatus.TERMINATING -> ProjectSWG.galaxy.status = GalaxyStatus.LOCKED
		}
	}

	@IntentHandler
	private fun handleShutdownServerIntent(ssi: ShutdownServerIntent) {
		initiateShutdownSequence(ssi.time, ssi.timeUnit)
	}

	@IntentHandler
	private fun handleInboundPacketIntent(ipi: InboundPacketIntent) {
		if (ipi.packet is AdminShutdownServer) initiateShutdownSequence((ipi.packet as AdminShutdownServer).shutdownTime.toLong(), TimeUnit.SECONDS)
	}

	private fun initiateShutdownSequence(time: Long, unit: TimeUnit) {
		Log.i("Beginning server shutdown sequence...")
		ServerStatusIntent(ServerStatus.SHUTDOWN_REQUESTED, time, unit).broadcast()
	}

	private fun startShutdownClock(time: Long, unit: TimeUnit) {
		shutdownTime.set(System.nanoTime() + unit.toNanos(time))
		executor.executeWithFixedRate(0, 1000) { updateShutdownClock() }
	}

	private fun updateShutdownClock() {
		val shutdownTime = shutdownTime.get()
		val currentTime = System.nanoTime()
		val remainingSeconds = ((shutdownTime - currentTime) / 1E9 + 0.5).toLong()
		assert(shutdownTime != 0L)
		if (remainingSeconds < 0) { // time's up
			operational.set(false)
		} else if (remainingSeconds == 0L) {
			if (!displayedShutdown.getAndSet(true)) SystemMessageIntent.broadcastGalaxy(SHUTDOWN_MESSAGE)
		} else {
			val message = buildShutdownMessage(remainingSeconds)
			if (message != null) SystemMessageIntent.broadcastGalaxy(message)
		}
	}

	companion object {
		private val TIME_SEARCH_PRIORITY = arrayOf(TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS)
		private val DISPLAY_COUNTERS = longArrayOf(1, 2, 3, 4, 5, 10, 15, 30, 45)
		private const val SHUTDOWN_PERIODIC = "The server will be shutting down in %d %s."
		private const val SHUTDOWN_MESSAGE = "The server will now be shutting down."
		private fun buildShutdownMessage(timeRemainingSeconds: Long): String? {
			val unit = getAppropriateTimeUnit(timeRemainingSeconds) ?: return null
			val remaining = unit.convert(timeRemainingSeconds, TimeUnit.SECONDS)
			var units = unit.name.lowercase()
			if (remaining == 1L) units = units.substring(0, units.length - 1) // drop the 's'
			return String.format(SHUTDOWN_PERIODIC, remaining, units)
		}

		private fun getAppropriateTimeUnit(timeRemainingSeconds: Long): TimeUnit? {
			for (unit in TIME_SEARCH_PRIORITY) {
				for (count in DISPLAY_COUNTERS) {
					if (unit.toSeconds(count) == timeRemainingSeconds) return unit
				}
			}
			return null
		}
	}
}
