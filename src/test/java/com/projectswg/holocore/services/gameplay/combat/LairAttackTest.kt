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
package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.tangible.PvpFlag
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandTimer
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.gameplay.faction.FactionFlagService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class LairAttackTest : TestRunnerSimulatedWorld() {

	@BeforeEach
	fun setup() {
		registerService(SkillService())
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(FactionFlagService())
		registerService(CombatStatusService())
	}

	@Test
	fun `lairs can be attacked`() {
		val character = createCharacter()
		val player = character.owner ?: throw RuntimeException("Unable to access player")
		val lair = createLair()

		meleeHit(player, lair)

		assertTrue(lair.conditionDamage > 0)
	}

	@Test
	fun `lairs are destroyed when they take too much damage`() {
		val character = createCharacter()
		val player = character.owner ?: throw RuntimeException("Unable to access player")
		val lair = createLair()
		val destroyedLair = AtomicBoolean(false)
		lair.maxHitPoints = 1

		registerIntentHandler(DestroyObjectIntent::class.java) {
			if (it.`object` == lair)
				destroyedLair.set(true)
		}
		meleeHit(player, lair)

		assertTrue(destroyedLair.get())
	}

	private fun meleeHit(player: GenericPlayer, target: SWGObject) {
		val crc = CRC.getCrc("meleehit")
		val targetObjectId = target.objectId

		broadcastAndWait(InboundPacketIntent(player, CommandQueueEnqueue(player.creatureObject.objectId, 0, crc, targetObjectId, "")))
		player.waitForNextPacket(CommandTimer::class.java)
		waitForIntents()
	}

	private fun createCharacter(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		GrantSkillIntent.broadcast(GrantSkillIntent.IntentType.GRANT, "species_human", creatureObject, true)
		ObjectCreatedIntent.broadcast(creatureObject)
		val defaultWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject
	}

	private fun createLair(): TangibleObject {
		val lair = ObjectCreator.createObjectFromTemplate(ClientFactory.formatToSharedFile("object/tangible/lair/bantha/lair_bantha.iff")) as TangibleObject
		lair.removeOptionFlags(OptionFlag.INVULNERABLE)
		lair.setPvpFlags(PvpFlag.YOU_CAN_ATTACK)
		ObjectCreatedIntent.broadcast(lair)
		return lair
	}
}