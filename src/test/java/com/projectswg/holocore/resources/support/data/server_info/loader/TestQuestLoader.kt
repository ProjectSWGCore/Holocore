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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.questLoader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TestQuestLoader {
	@Nested
	inner class TestQuestListInfo {

		private val questListInfo = questLoader.getQuestListInfo("quest/c_newbie_quest4") ?: throw IllegalStateException("Quest info not found")

		@Test
		fun canLoadJournalEntryTitle() {
			assertEquals("@quest/ground/c_newbie_quest4:journal_entry_title", questListInfo.journalEntryTitle)
		}

		@Test
		fun canLoadJournalEntryDescription() {
			assertEquals("@quest/ground/c_newbie_quest4:journal_entry_description", questListInfo.journalEntryDescription)
		}

		@Test
		fun canLoadCategory() {
			assertEquals("@quest/ground/c_newbie_quest4:category", questListInfo.category)
		}

		@Test
		fun canloadCompleteWhenTasksCompleteFlag() {
			assertTrue(questListInfo.isCompleteWhenTasksComplete)
		}

		@Test
		fun canLoadRepeatableFlag() {
			assertTrue(questListInfo.isRepeatable)
		}
	}

	@Nested
	inner class TestQuestTaskInfo {

		private val taskListInfos = questLoader.getTaskListInfos("quest/c_syren5")

		@Test
		fun canLoadRightAmountOfTasks() {
			assertEquals(8, taskListInfos.size)
		}

		@Test
		fun canLoadMinTime() {
			assertEquals(0, taskListInfos[5].minTime)
		}

		@Test
		fun canLoadMaxTime() {
			assertEquals(0, taskListInfos[5].maxTime)
		}

		@Test
		fun canLoadIndex() {
			assertEquals(5, taskListInfos[5].index)
		}

		@Test
		fun canLoadType() {
			assertEquals("quest.task.ground.destroy_multi_and_loot", taskListInfos[1].type)
		}

		@Test
		fun canLoadName() {
			assertEquals("encounterWithCalHandro", taskListInfos[3].name)
		}

		@Test
		fun canLoadTargetServerTemplate() {
			val taskListInfos = questLoader.getTaskListInfos("quest/yavin_fallenstar_pt_2")
			assertEquals("imperial_major", taskListInfos[1].targetServerTemplate)
		}

		@Test
		fun canLoadMessageBoxTitle() {
			val taskListInfos = questLoader.getTaskListInfos("quest/c_newbie_start")
			assertEquals("@quest/ground/c_newbie_start:task00_message_box_title", taskListInfos.first().messageBoxTitle)
		}

		@Test
		fun canLoadMessageBoxText() {
			val taskListInfos = questLoader.getTaskListInfos("quest/c_newbie_start")
			assertEquals("@quest/ground/c_newbie_start:task00_message_box_text", taskListInfos.first().messageBoxText)
		}

		@Test
		fun canLoadNpcAppearanceServerTemplate() {
			assertEquals("object/mobile/boba_fett.iff", taskListInfos[5].npcAppearanceServerTemplate)
		}

		@Test
		fun canLoadCommMessageText() {
			assertEquals("@quest/ground/c_syren5:task05_comm_message_text", taskListInfos[5].commMessageText)
		}

		@Test
		fun canLoadCount() {
			assertEquals(1, taskListInfos[4].count)
		}

		@Test
		fun canLoadNextTasksOnCompleteMultipleTasks() {
			val nextTasksOnComplete = taskListInfos[3].nextTasksOnComplete
			val expected = listOf(4, 5, 6)

			assertIterableEquals(expected, nextTasksOnComplete)
		}

		@Test
		fun canLoadNextTasksOnCompleteSingleTask() {
			val nextTasksOnComplete = taskListInfos[1].nextTasksOnComplete
			val expected = listOf(2)

			assertIterableEquals(expected, nextTasksOnComplete)
		}

		@Test
		fun canLoadNextTasksOnCompleteEmpty() {
			val nextTasksOnComplete = taskListInfos[6].nextTasksOnComplete
			val expected = emptyList<Int>()

			assertIterableEquals(expected, nextTasksOnComplete)
		}
	}
}
