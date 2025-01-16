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
package com.projectswg.holocore.utilities

import com.projectswg.common.utilities.ThreadUtilities
import kotlinx.coroutines.*
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

fun CoroutineScope.cancelAndWait() {
	val scope = this
	runBlocking(HolocoreCoroutine.INSTANCE.get()!!.dispatcher) {
		try {
			scope.coroutineContext[Job]?.cancelAndJoin()
		} catch (e: Throwable) {
			Log.w(e)
		}
	}
}

fun CoroutineScope.launchAfter(periodMilliseconds: Long, block: suspend () -> Unit) = launchAfter(periodMilliseconds, TimeUnit.MILLISECONDS, block)

fun CoroutineScope.launchAfter(period: Long, unit: TimeUnit, block: suspend () -> Unit) {
	launch {
		delay(unit.toMillis(period))
		block()
	}
}

fun CoroutineScope.launchWithFixedRate(periodMilliseconds: Long, block: suspend () -> Unit) = launchWithFixedRate(periodMilliseconds, TimeUnit.MILLISECONDS, block)

fun CoroutineScope.launchWithFixedRate(period: Long, unit: TimeUnit, block: suspend () -> Unit) {
	val updateRateSleep = unit.toMillis(period)
	launch {
		val anchorTime = System.nanoTime() / 1_000_000L
		while (isActive) {
			val sleepTime = updateRateSleep - ((System.nanoTime() / 1_000_000L + updateRateSleep - anchorTime) % updateRateSleep)
			delay(sleepTime)
			block()
		}
	}
}

fun CoroutineScope.launchWithFixedDelay(periodMilliseconds: Long, block: suspend () -> Unit) = launchWithFixedDelay(periodMilliseconds, TimeUnit.MILLISECONDS, block)

fun CoroutineScope.launchWithFixedDelay(period: Long, unit: TimeUnit, block: suspend () -> Unit) {
	launch {
		val sleepTime = unit.toMillis(period)
		while (isActive) {
			delay(sleepTime)
			block()
		}
	}
}

class HolocoreCoroutine : AutoCloseable {

	private val threadPool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), ThreadUtilities.newThreadFactory("coroutine-%d"))
	val dispatcher = threadPool.asCoroutineDispatcher()
	private val supervisor = SupervisorJob()
	private val scope = supervisor + dispatcher

	fun childScope(): CoroutineScope {
		return CoroutineScope(scope + SupervisorJob())
	}

	fun terminate() {
		scope.cancel()
		runBlocking(dispatcher) {
			supervisor.join()
		}
		threadPool.shutdownNow()
	}

	override fun close() {
		terminate()
	}

	companion object {

		val INSTANCE = AtomicReference<HolocoreCoroutine?>(null)

		fun childScope(): CoroutineScope = INSTANCE.get()?.childScope() ?: throw RuntimeException("CoroutineManager not created")
	}
}
