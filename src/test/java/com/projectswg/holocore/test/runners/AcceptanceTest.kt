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
package com.projectswg.holocore.test.runners

import com.projectswg.common.data.encodables.galaxy.Galaxy
import com.projectswg.common.data.location.Location
import com.projectswg.holocore.headless.MemoryUserDatabase
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
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
import com.projectswg.holocore.services.gameplay.combat.CombatStateService
import com.projectswg.holocore.services.gameplay.combat.buffs.BuffService
import com.projectswg.holocore.services.gameplay.combat.duel.DuelService
import com.projectswg.holocore.services.gameplay.combat.loot.GrantLootService
import com.projectswg.holocore.services.gameplay.combat.loot.LootGenerationService
import com.projectswg.holocore.services.gameplay.faction.FactionFlagService
import com.projectswg.holocore.services.gameplay.player.badge.BadgeManager
import com.projectswg.holocore.services.gameplay.player.character.TippingService
import com.projectswg.holocore.services.gameplay.player.experience.ExperiencePointService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.gameplay.player.group.GroupService
import com.projectswg.holocore.services.support.global.chat.ChatManager
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService
import com.projectswg.holocore.services.support.global.zone.LoginService
import com.projectswg.holocore.services.support.global.zone.ZoneService
import com.projectswg.holocore.services.support.global.zone.creation.CharacterCreationService
import com.projectswg.holocore.services.support.global.zone.sui.SuiService
import com.projectswg.holocore.services.support.objects.SimulatedObjectStorage
import com.projectswg.holocore.services.support.objects.awareness.AwarenessService
import com.projectswg.holocore.services.support.objects.awareness.ClientAwarenessService
import com.projectswg.holocore.services.support.objects.items.ContainerService
import com.projectswg.holocore.services.support.objects.radials.RadialService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.time.ZoneOffset
import java.util.*

/**
 * Acceptance test runner that sets up all the services required for an acceptance test.
 * This includes all the services required for a character to be able to log in and perform actions.
 *
 * Read more about acceptance test-driven development here:
 * https://en.wikipedia.org/wiki/Acceptance_test-driven_development
 *
 * Acceptance tests should be using [com.projectswg.holocore.headless.HeadlessSWGClient] to perform actions.
 */
abstract class AcceptanceTest : TestRunnerSynchronousIntents() {

	private val memoryUserDatabase = MemoryUserDatabase()

	@BeforeEach
	fun setUpServices() {
		val galaxy = Galaxy()
		galaxy.setZoneOffset(ZoneOffset.UTC)
		val galaxies = setOf(galaxy)

		registerService(ClientAwarenessService())
		registerService(CharacterLookupService())
		registerService(SimulatedObjectStorage())
		registerService(AwarenessService(5L))
		registerService(LoginService(galaxies, memoryUserDatabase))
		registerService(ZoneService())
		registerService(CommandQueueService(5, DeterministicDie(0), DeterministicDie(0), DeterministicDie(0), skipWarmup = true))
		registerService(CommandExecutionService())
		registerService(CharacterCreationService())
		registerService(ExperiencePointService())
		registerService(CombatExperienceService())
		registerService(CombatDeathblowService())
		registerService(CombatStateService())
		registerService(SkillService())
		registerService(ChatManager())
		registerService(SuiService())
		registerService(RadialService())
		registerService(TippingService())
		registerService(GroupService())
		registerService(LootGenerationService(DeterministicDie(1), DeterministicDie(1)))
		registerService(GrantLootService())
		registerService(ContainerService())
		registerService(BadgeManager())
		registerService(BuffService())
		registerService(DuelService())
		registerService(FactionFlagService())
	}

	@AfterEach
	fun wipeUserDatabase() {
		memoryUserDatabase.clear()
	}

	companion object {
		@JvmStatic
		@BeforeAll
		fun setUpAll() {
			ServerData.terrains // Eagerly load, so even the very first command is fast in the 'swimming' state check
		}
	}

	/**
	 * Generates a user with a random username and password.
	 */
	fun generateUser(accessLevel: AccessLevel = AccessLevel.PLAYER, banned: Boolean = false): UserCredentials {
		val username = UUID.randomUUID().toString().substringBefore("-")    // UUIDs are a bit long for usernames
		val password = UUID.randomUUID().toString()
		memoryUserDatabase.addUser(username, password, accessLevel, banned)
		return UserCredentials(username, password)
	}

	/**
	 * Spawns a single NPC at the given location.
	 *
	 * @param npcId the ID of the NPC to spawn - e.g. "creature_bantha"
	 * @param location the location to spawn the NPCs at
	 * @param spawnerFlag the spawner flag to use - defaults to [NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE]
	 * @param combatLevelRange the range defining the minimum and maximum possible combat level for the creatures - defaults to 1..1
	 */
	fun spawnNPC(npcId: String, location: Location, spawnerFlag: NpcStaticSpawnLoader.SpawnerFlag = NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE, combatLevelRange: IntRange = 1..1): AIObject {
		val egg = ObjectCreator.createObjectFromTemplate("object/tangible/ground_spawning/shared_patrol_spawner.iff")
		egg.moveToContainer(null, location)

		val spawnInfo = SimpleSpawnInfo.builder()
			.withNpcId(npcId)
			.withDifficulty(CreatureDifficulty.NORMAL)
			.withMinLevel(combatLevelRange.first)
			.withMaxLevel(combatLevelRange.last)
			.withLocation(location)
			.withAmount(1)
			.withSpawnerFlag(spawnerFlag)
			.build()

		return NPCCreator.createAllNPCs(Spawner(spawnInfo, egg)).first()
	}
}
