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
package com.projectswg.holocore.resources.support.data.server_info.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestQuestLoader {
	
	@Nested
	public class TestQuestInfo {
		
		private static QuestLoader.QuestListInfo questInfo;
		
		@BeforeEach
		public void setup() throws IOException {
			questInfo = ServerData.INSTANCE.getQuestLoader().getQuestListInfo("quest/c_newbie_quest4");
		}
		
		@Test
		public void canLoadQuestListInfo() {
			assertNotNull(questInfo);
		}
		
		@Test
		public void canLoadJournalEntryTitle() {
			assertEquals("@quest/ground/c_newbie_quest4:journal_entry_title", questInfo.getJournalEntryTitle());
		}
		
		@Test
		public void canLoadJournalEntryDescription() {
			assertEquals("@quest/ground/c_newbie_quest4:journal_entry_description", questInfo.getJournalEntryDescription());
		}
		
		@Test
		public void canloadCompleteWhenTasksCompleteFlag() {
			assertTrue(questInfo.isCompleteWhenTasksComplete());
		}
		
		@Test
		public void canLoadRepeatableFlag() {
			assertTrue(questInfo.isRepeatable());
		}
	
	}
	
	@Nested
	public class TestQuestTask {
		
		private static List<QuestLoader.QuestTaskInfo> taskListInfos;
		
		@BeforeEach
		public void setup() throws IOException {
			taskListInfos = ServerData.INSTANCE.getQuestLoader().getTaskListInfos("quest/c_syren5");
		}
		
		@Test
		public void canLoadRightAmountOfTasks() {
			assertEquals(8, taskListInfos.size());
		}
		
		@Test
		public void canLoadMinTime() {
			assertEquals(0, taskListInfos.get(5).getMinTime());
		}
		
		@Test
		public void canLoadMaxTime() {
			assertEquals(0, taskListInfos.get(5).getMaxTime());
		}
		
		@Test
		public void canLoadIndex() {
			assertEquals(5, taskListInfos.get(5).getIndex());
		}
		
		@Test
		public void canLoadType() {
			assertEquals("quest.task.ground.destroy_multi_and_loot", taskListInfos.get(1).getType());
		}
		
		@Test
		public void canLoadName() {
			assertEquals("encounterWithCalHandro", taskListInfos.get(3).getName());
		}
		
		@Test
		public void canLoadTargetServerTemplate() {
			List<QuestLoader.QuestTaskInfo> taskListInfos = ServerData.INSTANCE.getQuestLoader().getTaskListInfos("quest/yavin_fallenstar_pt_2");
			assertEquals("imperial_major", taskListInfos.get(1).getTargetServerTemplate());
		}
		
		@Test
		public void canLoadNpcAppearanceServerTemplate() {
			assertEquals("object/mobile/boba_fett.iff", taskListInfos.get(5).getNpcAppearanceServerTemplate());
		}
		
		@Test
		public void canLoadCommMessageText() {
			assertEquals("@quest/ground/c_syren5:task05_comm_message_text", taskListInfos.get(5).getCommMessageText());
		}
		
		@Test
		public void canLoadCount() {
			assertEquals(1, taskListInfos.get(4).getCount());
		}
		
		@Test
		public void canLoadNextTasksOnCompleteMultipleTasks() {
			Collection<Integer> nextTasksOnComplete = taskListInfos.get(3).getNextTasksOnComplete();
			Collection<Integer> expected = Arrays.asList(4, 5, 6);
			
			assertEquals(expected, nextTasksOnComplete);
		}
		
		@Test
		public void canLoadNextTasksOnCompleteSingleTask() {
			Collection<Integer> nextTasksOnComplete = taskListInfos.get(1).getNextTasksOnComplete();
			Collection<Integer> expected = Collections.singletonList(2);
			
			assertEquals(expected, nextTasksOnComplete);
		}
		
		@Test
		public void canLoadNextTasksOnCompleteEmpty() {
			Collection<Integer> nextTasksOnComplete = taskListInfos.get(6).getNextTasksOnComplete();
			Collection<Integer> expected = Collections.emptyList();
			
			assertEquals(expected, nextTasksOnComplete);
		}
	}
}
