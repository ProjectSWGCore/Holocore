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

/**
 * Cancels all coroutines within this CoroutineScope and blocks until all coroutines have completed.
 * Exceptions during cancellation are caught and logged, preventing them from terminating the cancellation process.
 */
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

/**
 * Simplified function to launch a coroutine that executes a block after a specified delay in milliseconds within a CoroutineScope.
 *
 * @param periodMilliseconds The delay in milliseconds before the block is executed.
 * @param block The suspend function to be executed after the delay.
 * @return Returns a Job that can be used to control the execution (e.g., cancel or check status).
 */
fun CoroutineScope.launchAfter(periodMilliseconds: Long, block: suspend () -> Unit): Job = launchAfter(periodMilliseconds, TimeUnit.MILLISECONDS, block)

/**
 * Launches a coroutine that executes a block after a specified delay within a CoroutineScope.
 *
 * @param period The delay period before the block is executed.
 * @param unit The time unit for the delay period (e.g., TimeUnit.SECONDS).
 * @param block The suspend function to be executed after the delay.
 * @return Returns a Job that can be used to control the execution (e.g., cancel or check status).
 */
fun CoroutineScope.launchAfter(period: Long, unit: TimeUnit, block: suspend () -> Unit): Job {
	return launch {
		delay(unit.toMillis(period))
		block()
	}
}

/**
 * Simplified function to launch a coroutine that executes a block at a fixed rate in milliseconds.
 *
 * @param periodMilliseconds The period in milliseconds at which the block is executed.
 * @param block The suspend function to be executed at each interval.
 * @return Returns a Job that can be used to control the execution (e.g., cancel or check status).
 */
fun CoroutineScope.launchWithFixedRate(periodMilliseconds: Long, block: suspend () -> Unit): Job = launchWithFixedRate(periodMilliseconds, TimeUnit.MILLISECONDS, block)

/**
 * Launches a coroutine that executes a block at a fixed rate within a CoroutineScope.
 *
 * @param period The period at which the block is executed.
 * @param unit The time unit for the period (e.g., TimeUnit.SECONDS).
 * @param block The suspend function to be executed at each interval.
 * @return Returns a Job that can be used to control the execution (e.g., cancel or check status).
 */
fun CoroutineScope.launchWithFixedRate(period: Long, unit: TimeUnit, block: suspend () -> Unit): Job {
	val updateRateSleep = unit.toMillis(period)
	return launch {
		val anchorTime = System.nanoTime() / 1_000_000L
		while (isActive) {
			val sleepTime = updateRateSleep - ((System.nanoTime() / 1_000_000L + updateRateSleep - anchorTime) % updateRateSleep)
			delay(sleepTime)
			block()
		}
	}
}

/**
 * Simplified function to launch a coroutine that executes a block repeatedly with a fixed delay in milliseconds within a CoroutineScope.
 *
 * @param periodMilliseconds The delay in milliseconds between the end of the last execution and the start of the next.
 * @param block The suspend function to be executed repeatedly after each delay.
 * @return Returns a Job that can be used to control the execution (e.g., cancel or check status).
 */
fun CoroutineScope.launchWithFixedDelay(periodMilliseconds: Long, block: suspend () -> Unit): Job = launchWithFixedDelay(periodMilliseconds, TimeUnit.MILLISECONDS, block)

/**
 * Launches a coroutine that executes a block repeatedly with a fixed delay between executions within a CoroutineScope.
 *
 * @param period The delay period between the end of the last execution and the start of the next.
 * @param unit The time unit for the period (e.g., TimeUnit.SECONDS).
 * @param block The suspend function to be executed repeatedly after each delay.
 * @return Returns a Job that can be used to control the execution (e.g., cancel or check status).
 */
fun CoroutineScope.launchWithFixedDelay(period: Long, unit: TimeUnit, block: suspend () -> Unit): Job {
	return launch {
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

	/**
	 * Creates a new child CoroutineScope with an independent SupervisorJob for error handling.
	 * This child scope inherits the execution context from the parent but allows child coroutines
	 * to fail without affecting sibling coroutines or the parent scope.
	 *
	 * @return A new CoroutineScope instance with its own SupervisorJob, based on the primary scope.
	 */
	fun childScope(): CoroutineScope {
		return CoroutineScope(scope + SupervisorJob())
	}

	/**
	 * Terminates the coroutine scope and its associated resources.
	 * It cancels all coroutines running in the scope, waits for the supervisor job to complete,
	 * and shuts down the thread pool immediately.
	 */
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

		/**
		 * Retrieves and returns a new child CoroutineScope from the CoroutineManager instance.
		 * Throws a RuntimeException if the CoroutineManager has not been previously created.
		 *
		 * @return A new child CoroutineScope from the CoroutineManager instance.
		 * @throws RuntimeException if the CoroutineManager instance is not available.
		 */
		fun childScope(): CoroutineScope = INSTANCE.get()?.childScope() ?: throw RuntimeException("CoroutineManager not created")
	}
}
