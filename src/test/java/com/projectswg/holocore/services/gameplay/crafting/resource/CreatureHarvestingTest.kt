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
package com.projectswg.holocore.services.gameplay.crafting.resource

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.intents.gameplay.combat.RequestCreatureDeathIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.gameplay.combat.CombatDeathblowService
import com.projectswg.holocore.services.gameplay.player.experience.ExperiencePointService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreatureHarvestingTest : TestRunnerSynchronousIntents() {

	@BeforeEach
	fun setup() {
		registerService(CombatDeathblowService())
		registerService(CreatureHarvestingService())
		registerService(ResourceService())
		registerService(ExperiencePointService())
	}

	@Test
	fun `harvested resources are placed in the inventory of the player`() {
		val player = createPlayer()
		val previousInventorySize = inventorySize(player)
		val bantha = createNpc("creature_bantha")

		broadcastAndWait(RequestCreatureDeathIntent(player.creatureObject, bantha))
		broadcastAndWait(HarvestHideIntent(player, bantha))

		val nextInventorySize = inventorySize(player)
		assertTrue(nextInventorySize > previousInventorySize, "Inventory contents should have increased")
	}

	@Test
	fun `you get Scouting XP by harvesting creatures`() {
		val player = createPlayer()
		val previousScoutXp = scoutXp(player)
		val bantha = createNpc("creature_bantha")

		broadcastAndWait(RequestCreatureDeathIntent(player.creatureObject, bantha))
		broadcastAndWait(HarvestHideIntent(player, bantha))

		val nextScoutXp = scoutXp(player)
		assertTrue(nextScoutXp > previousScoutXp, "Scout XP should have increased")
	}

	private fun inventorySize(player: Player): Int {
		return player.creatureObject.inventory.childObjects.size
	}

	private fun scoutXp(player: Player): Int {
		return player.playerObject.experience.getOrElse("scout") { 0 }
	}

	private fun createPlayer(): Player {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		val inFrontOfMosEisleyStarport = Location.builder()
			.setTerrain(Terrain.TATOOINE)
			.setX(-3521.0)
			.setY(5.0)
			.setZ(-4807.0)
			.build()
		creatureObject.location = inFrontOfMosEisleyStarport
		ObjectCreatedIntent(creatureObject).broadcast()
		val defaultWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject.owner ?: throw RuntimeException("Unable to access player")
	}

	private fun createNpc(npcId: String): AIObject {
		val inFrontOfMosEisleyStarport = Location.builder()
			.setTerrain(Terrain.TATOOINE)
			.setX(-3521.0)
			.setY(5.0)
			.setZ(-4807.0)
			.build()

		val egg = ObjectCreator.createObjectFromTemplate("object/tangible/ground_spawning/shared_patrol_spawner.iff")
		egg.moveToContainer(null, inFrontOfMosEisleyStarport)

		val spawnInfo = SimpleSpawnInfo.builder()
			.withNpcId(npcId)
			.withDifficulty(CreatureDifficulty.ELITE)
			.withMinLevel(19)
			.withMaxLevel(19)
			.withLocation(inFrontOfMosEisleyStarport)
			.build()

		return NPCCreator.createAllNPCs(Spawner(spawnInfo, egg)).first()
	}
}