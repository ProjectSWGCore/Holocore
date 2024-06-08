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
import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandTimer
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionIntent
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionStatusIntent
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.commands.State
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.gameplay.faction.FactionFlagService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FactionPvpTest : TestRunnerSimulatedWorld() {

	@BeforeEach
	fun setup() {
		registerService(SkillService())
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(FactionFlagService())
		registerService(CombatStatusService())
	}

	@Test
	fun `rebel and imperial characters can fight each other by becoming Special Forces`() {
		val rebelSpecialForcesCharacter = createCharacterInFaction("rebel", PvpStatus.SPECIALFORCES)
		val rebelSpecialForcesPlayer = rebelSpecialForcesCharacter.owner ?: throw RuntimeException("Unable to access player")
		val imperialSpecialForcesCharacter = createCharacterInFaction("imperial", PvpStatus.SPECIALFORCES)

		meleeHit(rebelSpecialForcesPlayer, imperialSpecialForcesCharacter)

		assertTrue(State.COMBAT.isActive(rebelSpecialForcesCharacter))
	}

	@Test
	fun `rebel and imperial characters must be Special Forces to fight each other`() {
		val rebelSpecialForcesCharacter = createCharacterInFaction("rebel", PvpStatus.COMBATANT)
		val rebelSpecialForcesPlayer = rebelSpecialForcesCharacter.owner ?: throw RuntimeException("Unable to access player")
		val imperialSpecialForcesCharacter = createCharacterInFaction("imperial", PvpStatus.SPECIALFORCES)

		meleeHit(rebelSpecialForcesPlayer, imperialSpecialForcesCharacter)

		assertFalse(State.COMBAT.isActive(rebelSpecialForcesCharacter))
	}

	private fun createCharacterInFaction(faction: String, pvpStatus: PvpStatus): GenericCreatureObject {
		val rebelSpecialForcesCreature = createCreatureObject()
		UpdateFactionIntent(rebelSpecialForcesCreature, ServerData.factions.getFaction(faction) ?: throw NullPointerException("invalid faction $faction")).broadcast()
		UpdateFactionStatusIntent(rebelSpecialForcesCreature, pvpStatus).broadcast()
		waitForIntents()
		return rebelSpecialForcesCreature
	}

	private fun meleeHit(player: GenericPlayer, target: SWGObject) {
		val crc = CRC.getCrc("meleehit")
		val targetObjectId = target.objectId

		broadcastAndWait(InboundPacketIntent(player, CommandQueueEnqueue(player.creatureObject.objectId, 0, crc, targetObjectId, "")))
		player.waitForNextPacket(CommandTimer::class.java)
		waitForIntents()
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "species_human", creatureObject, true).broadcast()
		ObjectCreatedIntent.broadcast(creatureObject)
		val defaultWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject
	}
}