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
package com.projectswg.holocore.services.gameplay.combat.missions

import com.projectswg.common.network.packets.swg.login.creation.ClientCreateCharacter
import com.projectswg.common.network.packets.swg.zone.SceneDestroyObject
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionAcceptRequest
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionAcceptResponse
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionListRequest
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.creation.CharacterCreation
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.mission.MissionObject
import com.projectswg.holocore.services.gameplay.missions.DestroyMissionService
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DestroyMissionListTest : TestRunnerSimulatedWorld() {

    @BeforeEach
    fun setUp() {
        registerService(DestroyMissionService())
    }

    @Test
    fun `listed missions can be accepted`() {
        val character = createCharacter()
        val player = character.owner ?: throw RuntimeException("Unable to access player")
        val missionTerminal = createMissionTerminal()
        sendMissionListRequest(player, missionTerminal)
        
        sendMissionAcceptRequest(player, missionTerminal)

        val missionAcceptResponse = (player as GenericPlayer).waitForNextPacket(MissionAcceptResponse::class.java)
        assertEquals(1, missionAcceptResponse?.success)
    }

    @Test
    fun `amount of accepted missions is limited to 2`() {
        val character = createCharacter()
        val player = character.owner ?: throw RuntimeException("Unable to access player")
        val missionTerminal = createMissionTerminal()
        sendMissionListRequest(player, missionTerminal)
        sendMissionAcceptRequest(player, missionTerminal)
        (player as GenericPlayer).waitForNextPacket(MissionAcceptResponse::class.java)
        sendMissionListRequest(player, missionTerminal)
        sendMissionAcceptRequest(player, missionTerminal)
        player.waitForNextPacket(MissionAcceptResponse::class.java)
        
        sendMissionListRequest(player, missionTerminal)
        sendMissionAcceptRequest(player, missionTerminal)
        
        val missionAcceptResponse = player.waitForNextPacket(MissionAcceptResponse::class.java)
        assertEquals(0, missionAcceptResponse?.success)
    }
    
    @Test
    fun `MissionListRequest is responded to by populating the mission bag`() {
        val character = createCharacter()
        val player = character.owner ?: throw RuntimeException("Unable to access player")
        val missionTerminal = createMissionTerminal()
        
        sendMissionListRequest(player, missionTerminal)

        val missionBag = character.missionBag
        val containedObjects = missionBag.containedObjects
        assertEquals(5, containedObjects.size)
    }

    @Test
    fun `mission bag remains at the desired size of MissionObjects`() {
        val character = createCharacter()
        val player = character.owner ?: throw RuntimeException("Unable to access player")
        val missionTerminal = createMissionTerminal()
        
        sendMissionListRequest(player, missionTerminal)
        sendMissionListRequest(player, missionTerminal)  // Extra request, to ensure we don't end up with 10, 15, 20, 25... MissionObjects

        val missionBag = character.missionBag
        val containedObjects = missionBag.containedObjects
        assertEquals(5, containedObjects.size)
    }

    @Test
    fun `excess MissionObjects are destroyed`() {
        val character = createCharacter()
        val player = character.owner ?: throw RuntimeException("Unable to access player")
        val missionBag = character.missionBag
        val missionTerminal = createMissionTerminal()
        for (i in 1..10) {
            createMissionObject().moveToContainer(missionBag)
        }

        sendMissionListRequest(player, missionTerminal)

        (player as GenericPlayer).waitForNextPacket(SceneDestroyObject::class.java)
        val containedObjects = missionBag.containedObjects
        assertEquals(5, containedObjects.size)
    }

    @Test
    fun `the mission bag contains MissionObjects`() {
        val character = createCharacter()
        val player = character.owner ?: throw RuntimeException("Unable to access player")
        val missionTerminal = createMissionTerminal()
        
        sendMissionListRequest(player, missionTerminal)

        val missionBag = character.missionBag
        val containedObjects = missionBag.containedObjects
        assertEquals(MissionObject::class, containedObjects.iterator().next()::class)
    }

    private fun sendMissionListRequest(player: Player, missionTerminal: SWGObject) {
        val creatureObject = player.creatureObject
        val objectId = creatureObject.objectId
        val missionListRequest = MissionListRequest(objectId)
        missionListRequest.terminalId = missionTerminal.objectId
        broadcastAndWait(InboundPacketIntent(player, missionListRequest))
    }

    private fun createMissionTerminal(): SWGObject {
        val missionTerminal =
            ObjectCreator.createObjectFromTemplate("object/tangible/terminal/shared_terminal_mission.iff")
        ObjectCreatedIntent(missionTerminal).broadcast()
        return missionTerminal
    }

    private fun sendMissionAcceptRequest(player: Player, missionTerminal: SWGObject) {
        val creatureObject = player.creatureObject
        val objectId = creatureObject.objectId
        val missionAcceptRequest = MissionAcceptRequest(objectId)
        missionAcceptRequest.terminalId = missionTerminal.objectId
        missionAcceptRequest.missionId = creatureObject.missionBag.containedObjects.iterator().next().objectId
        broadcastAndWait(InboundPacketIntent(player, missionAcceptRequest))
    }

    private fun createCharacter(): CreatureObject {
        val player = GenericPlayer()
        val clientCreateCharacter = ClientCreateCharacter()
        clientCreateCharacter.biography = ""
        clientCreateCharacter.clothes = "combat_brawler"
        clientCreateCharacter.race = "object/creature/player/shared_human_male.iff"
        clientCreateCharacter.name = "Testing Character"
        val characterCreation = CharacterCreation(player, clientCreateCharacter)

        val mosEisley = DataLoader.zoneInsertions().getInsertion("tat_moseisley")
        val creatureObject = characterCreation.createCharacter(AccessLevel.PLAYER, mosEisley)
        creatureObject.owner = player

        return creatureObject
    }

    private fun createMissionObject(): MissionObject {
        val missionObject =
            ObjectCreator.createObjectFromTemplate("object/mission/shared_mission_object.iff") as MissionObject
        ObjectCreatedIntent(missionObject).broadcast()

        return missionObject
    }
}