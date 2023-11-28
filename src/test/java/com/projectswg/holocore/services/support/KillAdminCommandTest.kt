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
package com.projectswg.holocore.services.support

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.holocore.headless.*
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.services.gameplay.combat.CombatDeathblowService
import com.projectswg.holocore.services.gameplay.combat.CombatExperienceService
import com.projectswg.holocore.services.gameplay.player.experience.ExperiencePointService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.services.support.global.zone.LoginService
import com.projectswg.holocore.services.support.global.zone.ZoneService
import com.projectswg.holocore.services.support.global.zone.creation.CharacterCreationService
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KillAdminCommandTest : TestRunnerSimulatedWorld() {

	private val memoryUserDatabase = MemoryUserDatabase()

	@BeforeEach
	fun setUp() {
		registerService(LoginService(memoryUserDatabase))
		registerService(ZoneService())
		registerService(CharacterCreationService())
		registerService(SkillService())
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(CombatDeathblowService())
		registerService(ExperiencePointService())
		registerService(CombatExperienceService())
	}

	@AfterEach
	fun tearDown() {
		memoryUserDatabase.clear()
	}

	@Test
	fun killNpc() {
		memoryUserDatabase.addUser("username", "password", AccessLevel.DEV)
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")
		val npc = spawnNPCs("creature_bantha", character.player.creatureObject.location, 1, NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE).first()

		character.adminKill(npc)

		assertEquals(Posture.DEAD, npc.posture)
	}

	@Test
	fun killInvulnerableNpc() {
		memoryUserDatabase.addUser("username", "password", AccessLevel.DEV)
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")
		val npc = spawnNPCs("creature_bantha", character.player.creatureObject.location, 1, NpcStaticSpawnLoader.SpawnerFlag.INVULNERABLE).first()

		character.adminKill(npc)

		assertAll(
			{ assertEquals(Posture.DEAD, npc.posture, "admins should be able to kill invulnerable NPCs with /kill") },
			{ assertTrue(character.player.playerObject.experience.isEmpty(), "admins should not gain experience from killing invulnerable NPCs") }
		)
	}

	@Test
	fun killSelf() {
		memoryUserDatabase.addUser("username", "password", AccessLevel.DEV)
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")

		character.adminKill(character.player.creatureObject)

		assertNotEquals(Posture.DEAD, character.player.creatureObject)
	}

	private fun spawnNPCs(npcId: String, location: Location, amount: Int, spawnerFlag: NpcStaticSpawnLoader.SpawnerFlag): Collection<AIObject> {
		val egg = ObjectCreator.createObjectFromTemplate("object/tangible/ground_spawning/shared_patrol_spawner.iff")
		egg.moveToContainer(null, location)

		val spawnInfo = SimpleSpawnInfo.builder()
			.withNpcId(npcId)
			.withDifficulty(CreatureDifficulty.NORMAL)
			.withMinLevel(1)
			.withMaxLevel(1)
			.withLocation(location)
			.withAmount(amount)
			.withSpawnerFlag(spawnerFlag)
			.build()

		return NPCCreator.createAllNPCs(Spawner(spawnInfo, egg))
	}

}
