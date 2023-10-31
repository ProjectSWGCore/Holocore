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

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.schematic.SlotType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DraftSchematicLoaderTest {

	@Test
	fun craftedTemplateIsTransformedIntoSharedTemplate() {
		val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")
		
		assertEquals("object/tangible/deed/camp_deed/shared_camp_luxury_deed.iff", draftSchematic.craftedSharedTemplate)
	}
	
	@Test
	fun itemsPerContainer() {
		val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")
		
		assertEquals(10, draftSchematic.itemsPerContainer)
	}
	
	@Test
	fun volume() {
		val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")
		
		assertEquals(1, draftSchematic.volume)
	}
	
	@Test
	fun complexity() {
		val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")
		
		assertEquals(17, draftSchematic.complexity)
	}

	@Test
	fun combinedCrc() {
		val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/instrument/shared_instrument_slitherhorn.iff") ?: fail("Could not find draft schematic")

		assertEquals(8706505225174593761, draftSchematic.combinedCrc)
	}
	
	@Nested
	inner class Slots {

		@Test
		fun allSlotsAreLoaded() {
			val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")

			assertEquals(10, draftSchematic.ingridientSlot.size)
		}
		
		@Test
		fun optional() {
			val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")

			assertFalse(draftSchematic.ingridientSlot[0].isOptional)
		}

		@Test
		fun name() {
			val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")

			assertEquals(StringId("craft_item_ingredients_n", "shelter_panels"), draftSchematic.ingridientSlot[0].name)
		}

		@Nested
		inner class Ingredients {
			
			@Test
			fun ingredientType() {
				val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")

				assertEquals(SlotType.RESOURCES, draftSchematic.ingridientSlot[0].fromSlotDataOption[0].slotType)
			}
			
			@Test
			fun allIngredientsAreLoaded() {
				val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")

				assertEquals(1, draftSchematic.ingridientSlot[0].fromSlotDataOption.size)
			}

			@Test
			fun name() {
				val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")

				assertEquals(StringId("craft_item_ingredients_n", "shelter_panels"), draftSchematic.ingridientSlot[0].fromSlotDataOption[0].stfName)
			}

			@Test
			fun ingredient() {
				val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")

				assertEquals("aluminum", draftSchematic.ingridientSlot[0].fromSlotDataOption[0].ingredientName)
			}

			@Test
			fun ingredientIffResolved() {
				val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/weapon/shared_bowcaster_assault.iff") ?: fail("Could not find draft schematic")

				assertEquals("@craft_weapon_ingredients_n:blaster_power_handler_advanced", draftSchematic.ingridientSlot[4].fromSlotDataOption[0].ingredientName)
			}

			@Test
			fun count() {
				val draftSchematic = ServerData.draftSchematics.getDraftSchematic("object/draft_schematic/camp/shared_camp_luxury.iff") ?: fail("Could not find draft schematic")

				assertEquals(250, draftSchematic.ingridientSlot[0].fromSlotDataOption[0].amount)
			}
		}
	}
}