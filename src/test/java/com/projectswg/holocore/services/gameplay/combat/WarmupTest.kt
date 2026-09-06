/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandTimer
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * A command with a warmup time is used, so there is a window where the warmup can be interrupted.
 */
class WarmupTest : TestRunnerSimulatedWorld() {

	@BeforeEach
	fun setup() {
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
	}

	@Test
	fun `moving interrupts the warmup`() {
		val creatureObject = createCreatureObject()
		startWarmup(creatureObject)

		MoveObjectIntent(creatureObject, creatureObject.location, RUN_SPEED).broadcast()
		waitForIntents()

		assertNotNull(waitForCommandTimerWithFlag(creatureObject, CommandTimer.CommandTimerFlag.FAILED), "Warmup was not interrupted")
	}

	@Test
	fun `turning in place does not interrupt the warmup`() {
		val creatureObject = createCreatureObject()
		startWarmup(creatureObject)

		MoveObjectIntent(creatureObject, creatureObject.location, 0.0).broadcast()
		waitForIntents()

		assertNull(waitForCommandTimerWithFlag(creatureObject, CommandTimer.CommandTimerFlag.FAILED), "Warmup was interrupted")
	}

	private fun startWarmup(creatureObject: GenericCreatureObject) {
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val crc = CRC.getCrc("coupdegrace")
		InboundPacketIntent(player, CommandQueueEnqueue(creatureObject.objectId, 0, crc, 0, "")).broadcast()

		assertNotNull(waitForCommandTimerWithFlag(creatureObject, CommandTimer.CommandTimerFlag.WARMUP), "Warmup never started")
	}

	private fun waitForCommandTimerWithFlag(creatureObject: GenericCreatureObject, flag: CommandTimer.CommandTimerFlag): CommandTimer? {
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		return player.waitForNextPacket(CommandTimer::class.java, 500, TimeUnit.MILLISECONDS) { it.flags.contains(flag) }
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		ObjectCreatedIntent(creatureObject).broadcast()
		val defaultWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject
	}

	private companion object {
		private const val RUN_SPEED = 5.376
	}
}
