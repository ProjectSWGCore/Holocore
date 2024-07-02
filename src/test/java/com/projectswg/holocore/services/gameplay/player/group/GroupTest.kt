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
package com.projectswg.holocore.services.gameplay.player.group

import com.projectswg.holocore.headless.*
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GroupTest : AcceptanceTest() {

	@Test
	fun formGroup() {
		val zonedInCharacter1 = createZonedInCharacter("Charone")
		val zonedInCharacter2 = createZonedInCharacter("Chartwo")

		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter2)
		zonedInCharacter2.acceptCurrentGroupInvitation()

		assertTrue(zonedInCharacter1.isInGroupWith(zonedInCharacter2))
	}

	@Test
	fun makeLeader() {
		val zonedInCharacter1 = createZonedInCharacter("Charone")
		val zonedInCharacter2 = createZonedInCharacter("Chartwo")
		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter2)
		zonedInCharacter2.acceptCurrentGroupInvitation()

		zonedInCharacter1.makeGroupLeader(zonedInCharacter2)

		assertAll(
			{ assertFalse(zonedInCharacter1.isGroupLeader()) },
			{ assertTrue(zonedInCharacter2.isGroupLeader()) },
		)
	}

	@Test
	fun makeLeaderOnlyWorksForCurrentLeader() {
		val zonedInCharacter1 = createZonedInCharacter("Charone")
		val zonedInCharacter2 = createZonedInCharacter("Chartwo")
		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter2)
		zonedInCharacter2.acceptCurrentGroupInvitation()

		zonedInCharacter2.makeGroupLeader(zonedInCharacter2)

		assertAll(
			{ assertFalse(zonedInCharacter2.isGroupLeader()) },
			{ assertTrue(zonedInCharacter1.isGroupLeader()) },
		)
	}

	@Test
	fun memberLeavesGroup() {
		val zonedInCharacter1 = createZonedInCharacter("Charone")
		val zonedInCharacter2 = createZonedInCharacter("Chartwo")
		val zonedInCharacter3 = createZonedInCharacter("Charthree")

		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter2)
		zonedInCharacter2.acceptCurrentGroupInvitation()
		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter3)
		zonedInCharacter3.acceptCurrentGroupInvitation()

		zonedInCharacter2.leaveCurrentGroup()

		assertAll(
			{ assertTrue(zonedInCharacter1.isInGroupWith(zonedInCharacter3)) },
			{ assertFalse(zonedInCharacter1.isInGroupWith(zonedInCharacter2)) },
			{ assertFalse(zonedInCharacter3.isInGroupWith(zonedInCharacter2)) },
		)
	}

	@Test
	fun leaderKicksMember() {
		val zonedInCharacter1 = createZonedInCharacter("Charone")
		val zonedInCharacter2 = createZonedInCharacter("Chartwo")
		val zonedInCharacter3 = createZonedInCharacter("Charthree")

		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter2)
		zonedInCharacter2.acceptCurrentGroupInvitation()
		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter3)
		zonedInCharacter3.acceptCurrentGroupInvitation()

		zonedInCharacter1.kickFromGroup(zonedInCharacter2)

		assertAll(
			{ assertTrue(zonedInCharacter1.isInGroupWith(zonedInCharacter3)) },
			{ assertFalse(zonedInCharacter1.isInGroupWith(zonedInCharacter2)) },
			{ assertFalse(zonedInCharacter3.isInGroupWith(zonedInCharacter2)) },
		)
	}

	@Test
	fun leaderLeavesGroup() {
		val zonedInCharacter1 = createZonedInCharacter("Charone")
		val zonedInCharacter2 = createZonedInCharacter("Chartwo")
		val zonedInCharacter3 = createZonedInCharacter("Charthree")

		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter2)
		zonedInCharacter2.acceptCurrentGroupInvitation()
		zonedInCharacter1.invitePlayerToGroup(zonedInCharacter3)
		zonedInCharacter3.acceptCurrentGroupInvitation()

		zonedInCharacter1.leaveCurrentGroup()

		assertAll(
			{ assertFalse(zonedInCharacter1.isInGroupWith(zonedInCharacter2)) },
			{ assertFalse(zonedInCharacter1.isInGroupWith(zonedInCharacter3)) },
			{ assertTrue(zonedInCharacter2.isGroupLeader() || zonedInCharacter3.isGroupLeader(), "one of the remaining members should be the new leader") },
		)
	}

	private fun createZonedInCharacter(characterName: String): ZonedInCharacter {
		val user = generateUser()
		return HeadlessSWGClient.createZonedInCharacter(user.username, user.password, characterName)
	}

}