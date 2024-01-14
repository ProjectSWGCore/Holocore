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
package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import org.jetbrains.annotations.Nullable;

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
		
		int index = 0;
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/quests/questtask/" + questName + ".sdb"))) {
			while (set.next()) {
				Set<String> columns = new LinkedHashSet<>(set.getColumns());
				String type = set.getText("attach_script");
				String name = null;
				if (columns.contains("task_name")) {
					name = set.getText("task_name");
				}
				String commMessageText = null;
				if (columns.contains("comm_message_text")) {
					commMessageText = set.getText("comm_message_text");
				}
				String npcAppearanceServerTemplate = null;
				if (columns.contains("npc_appearance_server_template")) {
					npcAppearanceServerTemplate = set.getText("npc_appearance_server_template");
				}
				String targetServerTemplate = null;
				if (columns.contains("target_server_template")) {
					targetServerTemplate = set.getText("target_server_template");
				}
				String grantQuestOnComplete = null;
				if (columns.contains("grant_quest_on_complete")) {
					grantQuestOnComplete = set.getText("grant_quest_on_complete");
				}
				int count = 0;
				if (columns.contains("count")) {
					count = (int) set.getInt("count");
				}
				int minTime = 0;
				if (columns.contains("min_time")) {
					minTime = (int) set.getInt("min_time");
				}
				int maxTime = 0;
				if (columns.contains("max_time")) {
					maxTime = (int) set.getInt("max_time");
				}
				String[] nextTasksOnComplete = set.getText("tasks_on_complete").split(",");
				String messageBoxTitle = null;
				if (columns.contains("message_box_title")) {
					messageBoxTitle = set.getText("message_box_title");
				}
				String messageBoxText = null;
				if (columns.contains("message_box_text")) {
					messageBoxText = set.getText("message_box_text");
				}
				String experienceType = null;
				if (columns.contains("experience_type")) {
					experienceType = set.getText("experience_type");
				}
				int experienceAmount = 0;
				if (columns.contains("experience_amount")) {
					experienceAmount = (int) set.getInt("experience_amount");
				}
				String factionName = null;
				if (columns.contains("faction_name")) {
					factionName = set.getText("faction_name").toLowerCase(Locale.ROOT);
				}
				int factionAmount = 0;
				if (columns.contains("faction_amount")) {
					factionAmount = (int) set.getInt("faction_amount");
				}
				int bankCredits = 0;
				if (columns.contains("bank_credits")) {
					bankCredits = (int) set.getInt("bank_credits");
				}
				int lootCount = 0;
				if (columns.contains("loot_count")) {
					lootCount = (int) set.getInt("loot_count");
				}
				String lootName = null;
				if (columns.contains("loot_name")) {
					lootName = set.getText("loot_name");
				}
				int itemCount = 0;
				if (columns.contains("count")) {
					itemCount = (int) set.getInt("count");
				}
				String itemTemplate = null;
				if (columns.contains("item")) {
					itemTemplate = set.getText("item");
				}
				boolean visible = false;
				if (columns.contains("is_visible")) {
					visible = set.getBoolean("is_visible");
				}
				String socialGroup = null;
				if (columns.contains("social_group")) {
					socialGroup = set.getText("social_group");
				}
				String lootItemName = null;
				if (columns.contains("loot_item_name")) {
					lootItemName = set.getText("loot_item_name");
				}
				int lootItemsRequired = 0;
				if (columns.contains("loot_items_required")) {
					lootItemsRequired = (int) set.getInt("loot_items_required");
				}
				int lootDropPercent = 0;
				if (columns.contains("loot_drop_percent")) {
					lootDropPercent = (int) set.getInt("loot_drop_percent");
				}
				String musicOnActivate = null;
				if (columns.contains("music_on_activate")) {
					musicOnActivate = set.getText("music_on_activate");
				}
				String musicOnComplete = null;
				if (columns.contains("music_on_complete")) {
					musicOnComplete = set.getText("music_on_complete");
				}
				String musicOnFailure = null;
				if (columns.contains("music_on_failure")) {
					musicOnFailure = set.getText("music_on_failure");
				}
				boolean createWaypoint = false;
				if (columns.contains("create_waypoint")) {
					createWaypoint = set.getBoolean("create_waypoint");
				}
				String planetName = null;
				if (columns.contains("planet_name")) {
					planetName = set.getText("planet_name");
				}
				double locationX = 0;
				if (columns.contains("location_x")) {
					locationX = set.getReal("location_x");
				}
				double locationY = 0;
				if (columns.contains("location_y")) {
					locationY = set.getReal("location_y");
				}
				double locationZ = 0;
				if (columns.contains("location_z")) {
					locationZ = set.getReal("location_z");
				}
				String waypointName = null;
				if (columns.contains("waypoint_name")) {
					waypointName = set.getText("waypoint_name");
				}
				double radius = 0;
				if (columns.contains("radius")) {
					String radiusText = set.getText("radius");	// Is seen as empty string if not set
					if (!radiusText.isBlank()) {
						radius = Double.parseDouble(radiusText);
					}
				}
				
				QuestTaskInfo questTaskInfo = new QuestTaskInfo();
				
				questTaskInfo.setType(type);
				
				for (String nextTaskOnComplete : nextTasksOnComplete) {
					if (!nextTaskOnComplete.isBlank()) {
						int taskIdx = Integer.parseInt(nextTaskOnComplete);
						questTaskInfo.addTaskOnComplete(taskIdx);
					}
				}
				
				questTaskInfo.setIndex(index++);
				questTaskInfo.setName(name);
				questTaskInfo.setCommMessageText(commMessageText);
				questTaskInfo.setNpcAppearanceServerTemplate(npcAppearanceServerTemplate);
				questTaskInfo.setTargetServerTemplate(targetServerTemplate);
				questTaskInfo.setGrantQuestOnComplete(grantQuestOnComplete);
				questTaskInfo.setCount(count);
				questTaskInfo.setMinTime(minTime);
				questTaskInfo.setMaxTime(maxTime);
				questTaskInfo.setMessageBoxTitle(messageBoxTitle);
				questTaskInfo.setMessageBoxText(messageBoxText);
				questTaskInfo.setExperienceType(experienceType);
				questTaskInfo.setExperienceAmount(experienceAmount);
				questTaskInfo.setFactionName(factionName);
				questTaskInfo.setFactionAmount(factionAmount);
				questTaskInfo.setBankCredits(bankCredits);
				questTaskInfo.setLootCount(lootCount);
				questTaskInfo.setLootName(lootName);
				questTaskInfo.setItemCount(itemCount);
				questTaskInfo.setItemTemplate(itemTemplate);
				questTaskInfo.setVisible(visible);
				questTaskInfo.setSocialGroup(socialGroup);
				questTaskInfo.setLootItemName(lootItemName);
				questTaskInfo.setLootItemsRequired(lootItemsRequired);
				questTaskInfo.setLootDropPercent(lootDropPercent);
				questTaskInfo.setMusicOnActivate(musicOnActivate);
				questTaskInfo.setMusicOnComplete(musicOnComplete);
				questTaskInfo.setMusicOnFailure(musicOnFailure);
				questTaskInfo.setCreateWaypoint(createWaypoint);
				questTaskInfo.setPlanetName(planetName);
				questTaskInfo.setLocationX(locationX);
				questTaskInfo.setLocationY(locationY);
				questTaskInfo.setLocationZ(locationZ);
				questTaskInfo.setWaypointName(waypointName);
				questTaskInfo.setRadius(radius);

				questTaskInfos.add(questTaskInfo);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load quest task info for quest by name " + questName, e);
		}
		
		questTaskInfosMap.put(questName, questTaskInfos);
		
		return questTaskInfos;
	}
	
	@Override
	public void load() throws IOException {
		loadQuestListInfos();
	}
	
	private void loadQuestListInfos() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/quests/questlist/questlist.msdb"))) {
			while (set.next()) {
				String questName = set.getText("quest_name");
				
				if (questListInfoMap.containsKey(questName)) {
					throw new SdbLoaderException(set, new RuntimeException("Duplicate quest list info for quest by name " + questName));
				}
				
				String journalEntryTitle = set.getText("journal_entry_title");
				String journalEntryDescription = set.getText("journal_entry_description");
				boolean completeWhenTasksComplete = set.getBoolean("complete_when_tasks_complete");
				boolean repeatable = set.getBoolean("allow_repeats");
				
				QuestListInfo listInfo = new QuestListInfo();
				listInfo.setJournalEntryTitle(journalEntryTitle);
				listInfo.setJournalEntryDescription(journalEntryDescription);
				listInfo.setCompleteWhenTasksComplete(completeWhenTasksComplete);
				listInfo.setRepeatable(repeatable);
				
				
				questListInfoMap.put(questName, listInfo);
			}
		}
	}
	
	public static class QuestListInfo {
		private String journalEntryTitle;
		private String journalEntryDescription;
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
		private String messageBoxTitle;
		private String messageBoxText;
		private String experienceType;
		private int experienceAmount;
		private String factionName;
		private int factionAmount;
		private int bankCredits;
		private int lootCount;
		private String lootName;
		private int itemCount;
		private String itemTemplate;
		private boolean visible;
		private String socialGroup;
		private String lootItemName;
		private int lootItemsRequired;
		private int lootDropPercent;
		private String musicOnActivate;
		private String musicOnComplete;
		private String musicOnFailure;
		private boolean createWaypoint;
		private String planetName;
		private double locationX;
		private double locationY;
		private double locationZ;
		private String waypointName;
		private double radius;

		private QuestTaskInfo() {
			nextTasksOnComplete = new ArrayList<>();
		}
		
		public int getMinTime() {
			return minTime;
		}

		public String getMessageBoxTitle() {
			return messageBoxTitle;
		}

		public void setMessageBoxTitle(String messageBoxTitle) {
			this.messageBoxTitle = messageBoxTitle;
		}

		public String getMessageBoxText() {
			return messageBoxText;
		}

		public void setMessageBoxText(String messageBoxText) {
			this.messageBoxText = messageBoxText;
		}

		public String getExperienceType() {
			return experienceType;
		}

		public void setExperienceType(String experienceType) {
			this.experienceType = experienceType;
		}

		public int getExperienceAmount() {
			return experienceAmount;
		}

		public void setExperienceAmount(int experienceAmount) {
			this.experienceAmount = experienceAmount;
		}

		public String getFactionName() {
			return factionName;
		}

		public void setFactionName(String factionName) {
			this.factionName = factionName;
		}

		public int getFactionAmount() {
			return factionAmount;
		}

		public void setFactionAmount(int factionAmount) {
			this.factionAmount = factionAmount;
		}

		public int getBankCredits() {
			return bankCredits;
		}

		public void setBankCredits(int bankCredits) {
			this.bankCredits = bankCredits;
		}

		public int getLootCount() {
			return lootCount;
		}

		public void setLootCount(int lootCount) {
			this.lootCount = lootCount;
		}

		public String getLootName() {
			return lootName;
		}

		public void setLootName(String lootName) {
			this.lootName = lootName;
		}

		public int getItemCount() {
			return itemCount;
		}

		public void setItemCount(int itemCount) {
			this.itemCount = itemCount;
		}

		public String getItemTemplate() {
			return itemTemplate;
		}

		public void setItemTemplate(String itemTemplate) {
			this.itemTemplate = itemTemplate;
		}

		public boolean isVisible() {
			return visible;
		}

		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		@Nullable
		public String getSocialGroup() {
			return socialGroup;
		}

		public void setSocialGroup(String socialGroup) {
			this.socialGroup = socialGroup;
		}

		public String getLootItemName() {
			return lootItemName;
		}

		public void setLootItemName(String lootItemName) {
			this.lootItemName = lootItemName;
		}

		public int getLootItemsRequired() {
			return lootItemsRequired;
		}

		public void setLootItemsRequired(int lootItemsRequired) {
			this.lootItemsRequired = lootItemsRequired;
		}

		public int getLootDropPercent() {
			return lootDropPercent;
		}

		public void setLootDropPercent(int lootDropPercent) {
			this.lootDropPercent = lootDropPercent;
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

		public String getMusicOnActivate() {
			return musicOnActivate;
		}

		public void setMusicOnActivate(String musicOnActivate) {
			this.musicOnActivate = musicOnActivate;
		}

		public String getMusicOnComplete() {
			return musicOnComplete;
		}

		public void setMusicOnComplete(String musicOnComplete) {
			this.musicOnComplete = musicOnComplete;
		}

		public String getMusicOnFailure() {
			return musicOnFailure;
		}

		public void setMusicOnFailure(String musicOnFailure) {
			this.musicOnFailure = musicOnFailure;
		}

		public boolean isCreateWaypoint() {
			return createWaypoint;
		}

		public void setCreateWaypoint(boolean createWaypoint) {
			this.createWaypoint = createWaypoint;
		}

		public String getPlanetName() {
			return planetName;
		}

		public void setPlanetName(String planetName) {
			this.planetName = planetName;
		}

		public double getLocationX() {
			return locationX;
		}

		public void setLocationX(double locationX) {
			this.locationX = locationX;
		}

		public double getLocationY() {
			return locationY;
		}

		public void setLocationY(double locationY) {
			this.locationY = locationY;
		}

		public double getLocationZ() {
			return locationZ;
		}

		public void setLocationZ(double locationZ) {
			this.locationZ = locationZ;
		}

		public String getWaypointName() {
			return waypointName;
		}

		public void setWaypointName(String waypointName) {
			this.waypointName = waypointName;
		}

		public double getRadius() {
			return radius;
		}

		public void setRadius(double radius) {
			this.radius = radius;
		}

		@Override
		public String toString() {
			return "QuestTaskInfo{" + "index=" + index + ", type='" + type + '\'' + ", name='" + name + '\'' + '}';
		}
	}
}
