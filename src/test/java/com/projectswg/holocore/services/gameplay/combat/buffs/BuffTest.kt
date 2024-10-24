/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.combat.buffs

import com.projectswg.holocore.intents.gameplay.combat.BuffIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.services.gameplay.player.character.PlayerPlayTimeService
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BuffTest : TestRunnerSynchronousIntents() {
	
	@BeforeEach
	fun setup() {
		registerService(BuffService())
		registerService(CharacterLookupService())
		registerService(PlayerPlayTimeService())
	}
	
	@Test
	fun `Buff removed when expires`() {
		val creatureObject = GenericCreatureObject(0)
		creatureObject.objectName = "Buffee"
		
		// Apply le buff (1s)
		val startTime = System.nanoTime()
		PlayerEventIntent(creatureObject.owner!!, PlayerEvent.PE_FIRST_ZONE).broadcast()
		BuffIntent("suppressionFire", creatureObject, creatureObject, false).broadcast()
		waitForIntents()
		
		while (System.nanoTime() - startTime < 2200e6) {
			if (!creatureObject.hasBuff("suppressionFire"))
				break
			Thread.sleep(50)
		}
		
		val endTime = System.nanoTime()
		assertTrue(endTime - startTime >= 2000e6)
		assertTrue(endTime - startTime < 2200e6)
		assertFalse(creatureObject.hasBuff("suppressionFire"))
	}
	
}
