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
			QuestLoader questLoader = new QuestLoader();
			questLoader.load();	// May throw an exception
			
			questInfo = questLoader.getQuestListInfo("quest/npe_rebel_2");
		}
		
		@Test
		public void canLoadQuestListInfo() {
			assertNotNull(questInfo);
		}
		
		@Test
		public void canLoadLevel() {
			assertEquals(10, questInfo.getLevel());
		}
		
		@Test
		public void canLoadTier() {
			assertEquals(3, questInfo.getTier());
		}
		
		@Test
		public void canLoadJournalEntryTitle() {
			assertEquals("@quest/ground/npe_rebel_2:journal_entry_title", questInfo.getJournalEntryTitle());
		}
		
		@Test
		public void canLoadJournalEntryDescription() {
			assertEquals("@quest/ground/npe_rebel_2:journal_entry_description", questInfo.getJournalEntryDescription());
		}
		
		@Test
		public void canLoadExperienceType() {
			assertEquals("quest_combat", questInfo.getExperienceType());
		}
		
		@Test
		public void canLoadCreditReward() {
			assertEquals(250, questInfo.getCredits());
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
			QuestLoader questLoader = new QuestLoader();
			
			taskListInfos = questLoader.getTaskListInfos("quest/purvis_kill_warriors");
		}
		
		@Test
		public void canLoadRightAmountOfTasks() {
			assertEquals(7, taskListInfos.size());
		}
		
		@Test
		public void canLoadMinTime() {
			assertEquals(20, taskListInfos.get(5).getMinTime());
		}
		
		@Test
		public void canLoadMaxTime() {
			assertEquals(25, taskListInfos.get(5).getMaxTime());
		}
		
		@Test
		public void canLoadIndex() {
			assertEquals(5, taskListInfos.get(5).getIndex());
		}
		
		@Test
		public void canLoadType() {
			assertEquals("quest.task.ground.comm_player", taskListInfos.get(6).getType());
		}
		
		@Test
		public void canLoadName() {
			assertEquals("kill_tusken_raider_warriors", taskListInfos.get(0).getName());
		}
		
		@Test
		public void canLoadTargetServerTemplate() {
			assertEquals("tusken_raider_warrior", taskListInfos.get(1).getTargetServerTemplate());
		}
		
		@Test
		public void canLoadNpcAppearanceServerTemplate() {
			assertEquals("object/mobile/bib_fortuna.iff", taskListInfos.get(6).getNpcAppearanceServerTemplate());
		}
		
		@Test
		public void canLoadCommMessageText() {
			assertEquals("@quest/ground/purvis_kill_warriors:task06_comm_message_text", taskListInfos.get(6).getCommMessageText());
		}
		
		@Test
		public void canLoadCount() {
			assertEquals(5, taskListInfos.get(1).getCount());
		}
		
		@Test
		public void canLoadNextTasksOnCompleteMultipleTasks() {
			Collection<Integer> nextTasksOnComplete = taskListInfos.get(0).getNextTasksOnComplete();
			Collection<Integer> expected = Arrays.asList(1, 5);
			
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
