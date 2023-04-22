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

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcDefaultAttackTest : TestRunnerSynchronousIntents() {

	@Test
	fun `Humanoid NPCs are granted the default attack commands of their weapons`() {
		val humanoidNpc = createNpc("humanoid_mos_eisley_police_officer")
		val defaultWeapons = humanoidNpc.defaultWeapons
		if (defaultWeapons.isEmpty()) {
			throw RuntimeException("Bad test setup, NPC has no default weapons at all")
		}

		for (defaultWeapon in defaultWeapons) {
			val defaultAttack = defaultWeapon.type.defaultAttack
			assertTrue(humanoidNpc.hasCommand(defaultAttack))
		}
	}

	@Test
	fun `Creature NPCs are granted the creature melee attack command`() {
		val creatureNpc = createNpc("creature_acklay")

		assertTrue(creatureNpc.hasCommand("creatureMeleeAttack"))
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

		return NPCCreator.createNPCs(Spawner(spawnInfo, egg)).first()
	}

}