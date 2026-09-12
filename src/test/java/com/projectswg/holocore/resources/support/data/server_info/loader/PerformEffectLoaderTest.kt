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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.test.runners.TestRunnerNoIntents
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PerformEffectLoaderTest : TestRunnerNoIntents() {

	@Test
	fun effectsLoad() {
		assertEquals(27, ServerData.performEffects.effects.size)
	}

	@Test
	fun effectSharedByMusicAndDance() {
		val dazzle = ServerData.performEffects.getEffect("Dazzle1")!!

		assertEquals(listOf("music", "dance"), dazzle.performanceTypes)
		assertEquals(10, dazzle.requiredSkillModValue)
		assertTrue(dazzle.requiredPerforming)
		assertEquals(3, dazzle.targetType)
		assertEquals(5.0, dazzle.effectDuration)
		assertEquals(60, dazzle.effectActionCost)
	}

	@Test
	fun danceOnlyEffect() {
		val smokeBomb = ServerData.performEffects.getEffect("SmokeBomb3")!!

		assertEquals(listOf("dance"), smokeBomb.performanceTypes)
		assertEquals(75, smokeBomb.requiredSkillModValue)
		assertEquals(1, smokeBomb.targetType)
		assertEquals(120, smokeBomb.effectActionCost)
	}

	@Test
	fun musicOnlyEffect() {
		val ventriloquism = ServerData.performEffects.getEffect("Ventriloquism1")!!

		assertEquals(listOf("music"), ventriloquism.performanceTypes)
		assertEquals(30, ventriloquism.requiredSkillModValue)
		assertEquals(2, ventriloquism.targetType)
		assertEquals(10.0, ventriloquism.effectDuration)
	}

	@Test
	fun unknownEffectIsNull() {
		assertNull(ServerData.performEffects.getEffect("Dazzle4"))
	}
}
