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
package com.projectswg.holocore.resources.support.data.server_info.loader

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SchematicGroupLoaderTest {
	@Test
	fun groupsByGroupIdCorrectly() {
		val groupId = "craftAdvancedCreatureGroup"
		val expected = setOf(
			"object/draft_schematic/bio_engineer/creature/creature_torton.iff",
			"object/draft_schematic/bio_engineer/creature/creature_kimogila.iff",
			"object/draft_schematic/bio_engineer/creature/creature_rancor.iff",
			"object/draft_schematic/bio_engineer/creature/creature_fambaa.iff",
			"object/draft_schematic/bio_engineer/creature/creature_veermok.iff",
			"object/draft_schematic/bio_engineer/creature/creature_graul.iff",
			"object/draft_schematic/bio_engineer/creature/creature_huf_dun.iff",
			"object/draft_schematic/bio_engineer/creature/creature_malkloc.iff",
			"object/draft_schematic/bio_engineer/creature/creature_sharnaff.iff",
			"object/draft_schematic/bio_engineer/creature/creature_woolamander.iff",
		)

		val schematicsInGroup = ServerData.schematicGroups.getSchematicsInGroup(groupId)
		
		assertEquals(expected.size, schematicsInGroup.size)
		assertTrue(schematicsInGroup.containsAll(expected))
	}

	@Test
	fun unknownGroupIdGivesEmptyCollection() {
		val schematicsInGroup = ServerData.schematicGroups.getSchematicsInGroup("thisGroupDefinitelyDoesNotExist")
		assertTrue(schematicsInGroup.isEmpty())
	}

	@Test
	fun endGroupIdIsIgnored() {
		// There's a group called "end" in the client info file, but it's not a real group
		val schematicsInGroup = ServerData.schematicGroups.getSchematicsInGroup("end")
		assertTrue(schematicsInGroup.isEmpty())
	}
}