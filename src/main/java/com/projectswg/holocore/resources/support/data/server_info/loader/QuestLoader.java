/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QuestLoader extends DataLoader {
	
	private final Map<String, QuestListInfo> questListInfoMap;
	private final Map<String, List<QuestLoader.QuestTaskInfo>> questTaskInfosMap;
	
	public QuestLoader() {
		questListInfoMap = Collections.synchronizedMap(new HashMap<>());
		questTaskInfosMap = Collections.synchronizedMap(new HashMap<>());
	}
	
	public QuestListInfo getQuestListInfo(String questName) {
		return questListInfoMap.get(questName);
	}
	
	public List<QuestTaskInfo> getTaskListInfos(String questName) {
		if (questTaskInfosMap.containsKey(questName)) {
			return questTaskInfosMap.get(questName);
		}
		
		List<QuestTaskInfo> questTaskInfos = new ArrayList<>();
		
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/nge/quests/questtask/" + questName + ".sdb"))) {
			while (set.next()) {
				int index = (int) set.getInt("index");
				String type = set.getText("type");
				String name = set.getText("name");
				String commMessageText = set.getText("comm_message_text");
				String npcAppearanceServerTemplate = set.getText("npc_appearance_server_template");
				String targetServerTemplate = set.getText("target_server_template");
				String grantQuestOnComplete = set.getText("grant_quest_on_complete");
				int count = (int) set.getInt("count");
				int minTime = (int) set.getInt("min_time");
				int maxTime = (int) set.getInt("max_time");
				String[] nextTasksOnComplete = set.getText("next_tasks_on_complete").split(";");
				
				QuestTaskInfo questTaskInfo = new QuestTaskInfo();
				
				questTaskInfo.setType(type);
				
				for (String nextTaskOnComplete : nextTasksOnComplete) {
					if (!nextTaskOnComplete.isBlank()) {
						int taskIdx = Integer.parseInt(nextTaskOnComplete);
						questTaskInfo.addTaskOnComplete(taskIdx);
					}
				}
				
				questTaskInfo.setIndex(index);
				questTaskInfo.setName(name);
				questTaskInfo.setCommMessageText(commMessageText);
				questTaskInfo.setNpcAppearanceServerTemplate(npcAppearanceServerTemplate);
				questTaskInfo.setTargetServerTemplate(targetServerTemplate);
				questTaskInfo.setGrantQuestOnComplete(grantQuestOnComplete);
				questTaskInfo.setCount(count);
				questTaskInfo.setMinTime(minTime);
				questTaskInfo.setMaxTime(maxTime);
				
				questTaskInfos.add(questTaskInfo);
			}
		} catch (Exception e) {
			Log.e(e);
		}
		
		questTaskInfosMap.put(questName, questTaskInfos);
		
		return questTaskInfos;
	}
	
	@Override
	public void load() throws IOException {
		loadQuestListInfos();
	}
	
	private void loadQuestListInfos() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/nge/quests/questlist/questlist.msdb"))) {
			while (set.next()) {
				String questName = set.getText("quest_name");
				
				if (questListInfoMap.containsKey(questName)) {
					throw new SdbLoaderException(set, new RuntimeException("Duplicate quest list info for quest by name " + questName));
				}
				
				int level = (int) set.getInt("level");
				int tier = (int) set.getInt("tier");
				String journalEntryTitle = set.getText("journal_entry_title");
				String journalEntryDescription = set.getText("journal_entry_description");
				String experienceType = set.getText("quest_reward_experience_type");
				int credits = (int) set.getInt("quest_reward_bank_credits");
				boolean completeWhenTasksComplete = set.getBoolean("complete_when_tasks_complete");
				boolean repeatable = set.getBoolean("allow_repeats");
				
				QuestListInfo listInfo = new QuestListInfo();
				listInfo.setLevel(level);
				listInfo.setTier(tier);
				listInfo.setJournalEntryTitle(journalEntryTitle);
				listInfo.setJournalEntryDescription(journalEntryDescription);
				listInfo.setExperienceType(experienceType);
				listInfo.setCredits(credits);
				listInfo.setCompleteWhenTasksComplete(completeWhenTasksComplete);
				listInfo.setRepeatable(repeatable);
				
				
				questListInfoMap.put(questName, listInfo);
			}
		}
	}
	
	public static class QuestListInfo {
		private int level;
		private int tier;
		private String journalEntryTitle;
		private String journalEntryDescription;
		private String experienceType;
		private int credits;
		private boolean completeWhenTasksComplete;
		private boolean repeatable;
		
		private QuestListInfo() {
		
		}
		
		public boolean isRepeatable() {
			return repeatable;
		}
		
		private void setRepeatable(boolean repeatable) {
			this.repeatable = repeatable;
		}
		
		public boolean isCompleteWhenTasksComplete() {
			return completeWhenTasksComplete;
		}
		
		public void setCompleteWhenTasksComplete(boolean completeWhenTasksComplete) {
			this.completeWhenTasksComplete = completeWhenTasksComplete;
		}
		
		public int getLevel() {
			return level;
		}
		
		private void setLevel(int level) {
			this.level = level;
		}
		
		public int getTier() {
			return tier;
		}
		
		private void setTier(int tier) {
			this.tier = tier;
		}
		
		public String getJournalEntryTitle() {
			return journalEntryTitle;
		}
		
		private void setJournalEntryTitle(String journalEntryTitle) {
			this.journalEntryTitle = journalEntryTitle;
		}
		
		public String getJournalEntryDescription() {
			return journalEntryDescription;
		}
		
		private void setJournalEntryDescription(String journalEntryDescription) {
			this.journalEntryDescription = journalEntryDescription;
		}
		
		public String getExperienceType() {
			return experienceType;
		}
		
		private void setExperienceType(String experienceType) {
			this.experienceType = experienceType;
		}
		
		public int getCredits() {
			return credits;
		}
		
		private void setCredits(int credits) {
			this.credits = credits;
		}
	}
	
	public static class QuestTaskInfo {
		private final Collection<Integer> nextTasksOnComplete;
		private int index;
		private String type;
		private String name;
		private String commMessageText;
		private String npcAppearanceServerTemplate;
		private String targetServerTemplate;
		private int count;
		private String grantQuestOnComplete;
		private int minTime;
		private int maxTime;
		
		private QuestTaskInfo() {
			nextTasksOnComplete = new ArrayList<>();
		}
		
		public int getMinTime() {
			return minTime;
		}
		
		private void setMinTime(int minTime) {
			this.minTime = minTime;
		}
		
		public int getMaxTime() {
			return maxTime;
		}
		
		private void setMaxTime(int maxTime) {
			this.maxTime = maxTime;
		}
		
		public int getIndex() {
			return index;
		}
		
		private void setIndex(int index) {
			this.index = index;
		}
		
		private void addTaskOnComplete(int taskIdx) {
			nextTasksOnComplete.add(taskIdx);
		}
		
		public Collection<Integer> getNextTasksOnComplete() {
			return new ArrayList<>(nextTasksOnComplete);
		}
		
		public String getType() {
			return type;
		}
		
		private void setType(String type) {
			this.type = type;
		}
		
		public String getName() {
			return name;
		}
		
		private void setName(String name) {
			this.name = name;
		}
		
		public String getCommMessageText() {
			return commMessageText;
		}
		
		private void setCommMessageText(String commMessageText) {
			this.commMessageText = commMessageText;
		}
		
		public String getNpcAppearanceServerTemplate() {
			return npcAppearanceServerTemplate;
		}
		
		private void setNpcAppearanceServerTemplate(String npcAppearanceServerTemplate) {
			this.npcAppearanceServerTemplate = npcAppearanceServerTemplate;
		}
		
		public String getTargetServerTemplate() {
			return targetServerTemplate;
		}
		
		private void setTargetServerTemplate(String targetServerTemplate) {
			this.targetServerTemplate = targetServerTemplate;
		}
		
		public int getCount() {
			return count;
		}
		
		private void setCount(int count) {
			this.count = count;
		}
		
		public String getGrantQuestOnComplete() {
			return grantQuestOnComplete;
		}
		
		public void setGrantQuestOnComplete(String grantQuestOnComplete) {
			this.grantQuestOnComplete = grantQuestOnComplete;
		}
	}
}
