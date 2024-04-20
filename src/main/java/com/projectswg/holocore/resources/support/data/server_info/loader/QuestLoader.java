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

import com.projectswg.common.data.swgfile.ClientFactory;
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

	public Collection<String> getQuestNames() {
		return questListInfoMap.keySet();
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
				QuestTaskInfo questTaskInfo = new QuestTaskInfo();
				Set<String> columns = new LinkedHashSet<>(set.getColumns());
				String type = set.getText("attach_script");
				if (type.equals("quest.task.ground.go_to_location")) {
					if (columns.contains("create_waypoint")) {
						questTaskInfo.setCreateWaypoint(set.getBoolean("create_waypoint"));
					}
					if (columns.contains("planet_name")) {
						questTaskInfo.setPlanetName(set.getText("planet_name"));
					}
					if (columns.contains("location_x")) {
						questTaskInfo.setLocationX(set.getReal("location_x"));
					}
					if (columns.contains("location_y")) {
						questTaskInfo.setLocationY(set.getReal("location_y"));
					}
					if (columns.contains("location_z")) {
						questTaskInfo.setLocationZ(set.getReal("location_z"));
					}
					if (columns.contains("waypoint_name")) {
						questTaskInfo.setWaypointName(set.getText("waypoint_name"));
					}
					if (columns.contains("radius")) {
						String radiusText = set.getText("radius");    // Is seen as empty string if not set
						if (!radiusText.isBlank()) {
							questTaskInfo.setRadius(Double.parseDouble(radiusText));
						}
					}
				}
				if (type.equals("quest.task.ground.retrieve_item")) {
					if (columns.contains("server_template")) {
						String rawServerTemplate = set.getText("server_template");
						if (!rawServerTemplate.isBlank()) {
							questTaskInfo.setServerTemplate(ClientFactory.formatToSharedFile(rawServerTemplate));
						}
					}
					if (columns.contains("num_required")) {
						questTaskInfo.setNumRequired((int) set.getInt("num_required"));
					}
					if (columns.contains("item_name")) {
						questTaskInfo.setItemName(set.getText("item_name"));
					}
					if (columns.contains("drop_percent")) {
						questTaskInfo.setDropPercent((int) set.getInt("drop_percent"));
					}
					if (columns.contains("retrieve_menu_text")) {
						questTaskInfo.setRetrieveMenuText(set.getText("retrieve_menu_text"));
					}
				}

				questTaskInfo.setType(type);

				for (String nextTaskOnComplete : set.getText("tasks_on_complete").split(",")) {
					if (!nextTaskOnComplete.isBlank()) {
						int taskIdx = Integer.parseInt(nextTaskOnComplete);
						questTaskInfo.addTaskOnComplete(taskIdx);
					}
				}

				questTaskInfo.setIndex(index++);
				questTaskInfo.setName(getTaskName(columns, set));
				questTaskInfo.setCommMessageText(getCommMessageText(columns, set));
				questTaskInfo.setNpcAppearanceServerTemplate(getNpcAppearanceServerTemplate(columns, set));
				questTaskInfo.setTargetServerTemplate(getTargetServerTemplate(columns, set));
				questTaskInfo.setGrantQuestOnComplete(getGrantQuestOnComplete(columns, set));
				questTaskInfo.setCount(getCount(columns, set));
				questTaskInfo.setMinTime(getMinTime(columns, set));
				questTaskInfo.setMaxTime(getMaxTime(columns, set));
				questTaskInfo.setMessageBoxTitle(getMessageBoxTitle(columns, set));
				questTaskInfo.setMessageBoxText(getMessageBoxText(columns, set));
				questTaskInfo.setExperienceType(getExperienceType(columns, set));
				questTaskInfo.setExperienceAmount(getExperienceAmount(columns, set));
				questTaskInfo.setFactionName(getFactionName(columns, set));
				questTaskInfo.setFactionAmount(getFactionAmount(columns, set));
				questTaskInfo.setBankCredits(getBankCredits(columns, set));
				questTaskInfo.setLootCount(getLootCount(columns, set));
				questTaskInfo.setLootName(getLootName(columns, set));
				questTaskInfo.setItemCount(getCount(columns, set));
				questTaskInfo.setItemTemplate(getItemTemplate(columns, set));
				questTaskInfo.setVisible(isVisible(columns, set));
				questTaskInfo.setSocialGroup(getSocialGroup(columns, set));
				questTaskInfo.setLootItemName(getLootItemName(columns, set));
				questTaskInfo.setLootItemsRequired(getLootItemsRequired(columns, set));
				questTaskInfo.setLootDropPercent(getLootDropPercent(columns, set));
				questTaskInfo.setMusicOnActivate(getMusicOnActivate(columns, set));
				questTaskInfo.setMusicOnComplete(getMusicOnComplete(columns, set));
				questTaskInfo.setMusicOnFailure(getMusicOnFailure(columns, set));

				questTaskInfos.add(questTaskInfo);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load quest task info for quest by name " + questName, e);
		}

		questTaskInfosMap.put(questName, questTaskInfos);

		return questTaskInfos;
	}

	private static @Nullable String getMusicOnFailure(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("music_on_failure")) {
			return set.getText("music_on_failure");
		}
		return null;
	}

	private static @Nullable String getMusicOnComplete(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("music_on_complete")) {
			return set.getText("music_on_complete");
		}
		return null;
	}

	private static @Nullable String getMusicOnActivate(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("music_on_activate")) {
			return set.getText("music_on_activate");
		}
		return null;
	}

	private static int getLootDropPercent(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("loot_drop_percent")) {
			return (int) set.getInt("loot_drop_percent");
		}
		return 0;
	}

	private static int getLootItemsRequired(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("loot_items_required")) {
			return (int) set.getInt("loot_items_required");
		}
		return 0;
	}

	private static @Nullable String getLootItemName(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("loot_item_name")) {
			return set.getText("loot_item_name");
		}
		return null;
	}

	private static @Nullable String getSocialGroup(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("social_group")) {
			return set.getText("social_group");
		}
		return null;
	}

	private static boolean isVisible(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("is_visible")) {
			return set.getBoolean("is_visible");
		}
		return false;
	}

	private static @Nullable String getItemTemplate(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("item")) {
			return set.getText("item");
		}
		return null;
	}

	private static @Nullable String getLootName(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("loot_name")) {
			return set.getText("loot_name");
		}
		return null;
	}

	private static int getLootCount(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("loot_count")) {
			return (int) set.getInt("loot_count");
		}
		return 0;
	}

	private static int getBankCredits(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("bank_credits")) {
			return (int) set.getInt("bank_credits");
		}
		return 0;
	}

	private static int getFactionAmount(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("faction_amount")) {
			return (int) set.getInt("faction_amount");
		}
		return 0;
	}

	private static @Nullable String getFactionName(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("faction_name")) {
			return set.getText("faction_name").toLowerCase(Locale.ROOT);
		}
		return null;
	}

	private static int getExperienceAmount(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("experience_amount")) {
			return (int) set.getInt("experience_amount");
		}
		return 0;
	}

	private static @Nullable String getExperienceType(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("experience_type")) {
			return set.getText("experience_type");
		}
		return null;
	}

	private static @Nullable String getMessageBoxText(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("message_box_text")) {
			return set.getText("message_box_text");
		}
		return null;
	}

	private static @Nullable String getMessageBoxTitle(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("message_box_title")) {
			return set.getText("message_box_title");
		}
		return null;
	}

	private static int getMaxTime(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("max_time")) {
			return (int) set.getInt("max_time");
		}
		return 0;
	}

	private static int getMinTime(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("min_time")) {
			return (int) set.getInt("min_time");
		}
		return 0;
	}

	private static int getCount(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("count")) {
			return (int) set.getInt("count");
		}
		return 0;
	}

	private static @Nullable String getGrantQuestOnComplete(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("grant_quest_on_complete")) {
			return set.getText("grant_quest_on_complete");
		}
		return null;
	}

	private static @Nullable String getTargetServerTemplate(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("target_server_template")) {
			return set.getText("target_server_template");
		}
		return null;
	}

	private static @Nullable String getNpcAppearanceServerTemplate(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("npc_appearance_server_template")) {
			return set.getText("npc_appearance_server_template");
		}
		return null;
	}

	private static @Nullable String getCommMessageText(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("comm_message_text")) {
			return set.getText("comm_message_text");
		}
		return null;
	}

	private static @Nullable String getTaskName(Set<String> columns, SdbLoader.SdbResultSet set) {
		if (columns.contains("task_name")) {
			return set.getText("task_name");
		}
		return null;
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
		private String serverTemplate;
		private int numRequired;
		private String itemName;
		private int dropPercent;
		private String retrieveMenuText;

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

		public String getServerTemplate() {
			return serverTemplate;
		}

		public void setServerTemplate(String serverTemplate) {
			this.serverTemplate = serverTemplate;
		}

		public int getNumRequired() {
			return numRequired;
		}

		public void setNumRequired(int numRequired) {
			this.numRequired = numRequired;
		}

		public String getItemName() {
			return itemName;
		}

		public void setItemName(String itemName) {
			this.itemName = itemName;
		}

		public int getDropPercent() {
			return dropPercent;
		}

		public void setDropPercent(int dropPercent) {
			this.dropPercent = dropPercent;
		}

		public String getRetrieveMenuText() {
			return retrieveMenuText;
		}

		public void setRetrieveMenuText(String retrieveMenuText) {
			this.retrieveMenuText = retrieveMenuText;
		}

		@Override
		public String toString() {
			return "QuestTaskInfo{" + "index=" + index + ", type='" + type + '\'' + ", name='" + name + '\'' + '}';
		}
	}
}
