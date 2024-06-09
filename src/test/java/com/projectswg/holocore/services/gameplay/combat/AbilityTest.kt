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
package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandTimer
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AbilityTest : TestRunnerSimulatedWorld() {

	@BeforeEach
	fun setup() {
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(SkillService())
	}

	@Test
	fun `you can execute an ability when you have the skill that grants it`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		grantRequiredSkillForParryRiposte(creatureObject)

		val success = parryRiposte(player)

		assertTrue(success)
	}

	@Test
	fun `you cannot execute an ability when you are missing the skill that grants it`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")

		val success = parryRiposte(player)

		assertFalse(success)
	}

	private fun grantRequiredSkillForParryRiposte(creatureObject: GenericCreatureObject) {
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_brawler_master", creatureObject, true).broadcast()
		waitForIntents()
	}

	private fun parryRiposte(player: GenericPlayer): Boolean {
		val crc = CRC.getCrc("parryriposte")
		InboundPacketIntent(player, CommandQueueEnqueue(player.creatureObject.objectId, 0, crc, 0, "")).broadcast()

		val commandTimer = player.waitForNextPacket(CommandTimer::class.java)

		if (commandTimer != null) {
			return commandTimer.flags.contains(CommandTimer.CommandTimerFlag.EXECUTE)
		} else {
			throw RuntimeException("No CommandTimer packet was sent at all")
		}
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		ObjectCreatedIntent.broadcast(creatureObject)
		val defaultWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject
	}
}