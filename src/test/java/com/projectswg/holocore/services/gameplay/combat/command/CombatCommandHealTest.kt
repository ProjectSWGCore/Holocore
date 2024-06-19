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
package com.projectswg.holocore.services.gameplay.combat.command

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.login.creation.ClientCreateCharacter
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandTimer
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.creation.CharacterCreation
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.gameplay.combat.CombatStatusService
import com.projectswg.holocore.services.gameplay.combat.buffs.BuffService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod.SkillModService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CombatCommandHealTest : TestRunnerSimulatedWorld() {

    @BeforeEach
    fun setup() {
        registerService(BuffService())
        registerService(CommandQueueService(5))
        registerService(CommandExecutionService())
        registerService(CombatStatusService())
        registerService(SkillService())
        registerService(SkillModService())
    }

    @Test
    fun `healing restores health`() {
        val startingHealth = 100
        val creatureObject = createCreatureObject(startingHealth)
        val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
        broadcastAndWait(GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "science_medic_novice", creatureObject, true))
        
        bactaShot(player)

        val resultingHealth = creatureObject.health
        assertTrue(resultingHealth > startingHealth)
    }

    @Test
    fun `Healing Efficiency skill modifier increases damage healed`() {
        val startingHealth = 100
        val creatureObject = createCreatureObject(startingHealth)
        val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
        broadcastAndWait(GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "science_medic_novice", creatureObject, true))
        
        bactaShot(player)

        val healed = creatureObject.health - startingHealth
        val bactaShotAddedDamage = getBactaShotAddedDamage()
        assertTrue(healed > bactaShotAddedDamage)
    }

    private fun getBactaShotAddedDamage(): Int {
        val combatCommand = ServerData.combatCommands.getCombatCommand("bactaShot", emptyList())
                ?: throw RuntimeException("Unable to find combat command")
        
        return combatCommand.addedDamage
    }

    private fun bactaShot(player: Player) {
        val crc = CRC.getCrc("bactashot")

        broadcastAndWait(InboundPacketIntent(player, CommandQueueEnqueue(player.creatureObject.objectId, 0, crc, 0, "")))
        (player as GenericPlayer).waitForNextPacket(CommandTimer::class.java)
    }

    private fun createCreatureObject(currentHealth: Int): CreatureObject {
        val player = GenericPlayer()
        val clientCreateCharacter = ClientCreateCharacter()
        clientCreateCharacter.biography = ""
        clientCreateCharacter.clothes = "combat_brawler"
        clientCreateCharacter.race = "object/creature/player/shared_human_male.iff"
        clientCreateCharacter.name = "Testing Character"
        val characterCreation = CharacterCreation(player, clientCreateCharacter)

        val mosEisley = DataLoader.zoneInsertions()["tat_moseisley"]!!
        val creatureObject = characterCreation.createCharacter(AccessLevel.PLAYER, mosEisley)
        creatureObject.owner = player
        creatureObject.health = currentHealth

        return creatureObject
    }
}