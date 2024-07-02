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
package com.projectswg.holocore.resources.support.global.zone.creation

import com.projectswg.holocore.headless.HeadlessSWGClient
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CharacterCreationTest : AcceptanceTest() {

	@Test
	fun `new characters receive a Slitherhorn`() {
		val user = generateUser()
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")

		assertTrue(inventoryContainsSlitherhorn(character.player.creatureObject))
	}

	@Test
	fun `new characters become Novice in all basic professions`() {
		val user = generateUser()
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")

		val skills = character.player.creatureObject.skills
		assertAll(
			{ assertTrue(skills.contains("combat_brawler_novice")) },
			{ assertTrue(skills.contains("combat_marksman_novice")) },
			{ assertTrue(skills.contains("crafting_artisan_novice")) },
			{ assertTrue(skills.contains("science_medic_novice")) },
			{ assertTrue(skills.contains("outdoors_scout_novice")) },
			{ assertTrue(skills.contains("social_entertainer_novice")) },
		)
	}

	@Test
	fun `new characters receive their species skill`() {
		val user = generateUser()
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")

		assertTrue(character.player.creatureObject.skills.contains("species_human"))
	}

	private fun inventoryContainsSlitherhorn(character: CreatureObject): Boolean {
		val inventory = character.inventory
		val containedObjects = inventory.containedObjects

		for (containedObject in containedObjects) {
			if (isSlitherhorn(containedObject)) {
				return true
			}
		}

		return false
	}

	private fun isSlitherhorn(containedObject: SWGObject) = containedObject.template == "object/tangible/instrument/shared_slitherhorn.iff"

}