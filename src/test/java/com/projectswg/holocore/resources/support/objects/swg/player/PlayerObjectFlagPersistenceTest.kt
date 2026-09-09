/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects.swg.player

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.holocore.resources.support.global.player.PlayerFlags
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerObjectFlagPersistenceTest {

	@Test
	fun `flags survive a save and load cycle`() {
		val saved = PlayerObject(0)
		saved.flags.set(PlayerFlags.FACTIONRANK)

		assertTrue(roundTrip(saved).flags[PlayerFlags.FACTIONRANK])
	}

	@Test
	fun `profile flags survive a save and load cycle`() {
		val saved = PlayerObject(0)
		saved.profileFlags.set(PlayerFlags.HELPER)

		assertTrue(roundTrip(saved).profileFlags[PlayerFlags.HELPER])
	}

	@Test
	fun `unset flags remain unset after a save and load cycle`() {
		val saved = PlayerObject(0)
		saved.flags.set(PlayerFlags.FACTIONRANK)

		assertFalse(roundTrip(saved).flags[PlayerFlags.ANONYMOUS])
	}

	private fun roundTrip(obj: PlayerObject): PlayerObject {
		val data = MongoData()
		obj.saveMongo(data)

		val loaded = PlayerObject(0)
		loaded.readMongo(data)
		return loaded
	}
}
