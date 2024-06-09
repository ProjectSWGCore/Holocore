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

import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException
import java.util.*

class QuestLoader : DataLoader() {
	private val questListInfoMap: MutableMap<String, QuestListInfo> = Collections.synchronizedMap(HashMap())
	private val questTaskInfosMap: MutableMap<String, List<QuestTaskInfo>> = Collections.synchronizedMap(HashMap())

	val questNames: Collection<String>
		get() = questListInfoMap.keys

	fun getQuestListInfo(questName: String): QuestListInfo? {
		return questListInfoMap[questName]
	}

	fun getTaskListInfos(questName: String): List<QuestTaskInfo> {
		if (questTaskInfosMap.containsKey(questName)) {
			return questTaskInfosMap[questName]!!
		}

		val questTaskInfos: MutableList<QuestTaskInfo> = ArrayList()

		var index = 0
		try {
			SdbLoader.load(File("serverdata/quests/questtask/$questName.sdb")).use { set ->
				var file = set.file
				var columns = HashSet(set.columns)
				while (set.next()) {
					if (set.file != file) {
						file = set.file
						columns = HashSet(set.columns)
					}
					questTaskInfos.add(QuestTaskInfo(set, columns, index++))
				}
			}
		} catch (e: Exception) {
			throw RuntimeException("Failed to load quest task info for quest by name $questName", e)
		}

		questTaskInfosMap[questName] = questTaskInfos

		return questTaskInfos
	}

	@Throws(IOException::class)
	override fun load() {
		loadQuestListInfos()
	}

	@Throws(IOException::class)
	private fun loadQuestListInfos() {
		SdbLoader.load(File("serverdata/quests/questlist/questlist.msdb")).use { set ->
			while (set.next()) {
				val questName = set.getText("quest_name")
				if (questListInfoMap.containsKey(questName)) throw SdbLoaderException(set, RuntimeException("Duplicate quest list info for quest by name $questName"))

				questListInfoMap[questName] = QuestListInfo(set)
			}
		}
	}

	class QuestListInfo(set: SdbResultSet) {
		val journalEntryTitle: String = set.getText("journal_entry_title")
		val journalEntryDescription: String = set.getText("journal_entry_description")
		val isCompleteWhenTasksComplete: Boolean = set.getBoolean("complete_when_tasks_complete")
		val isRepeatable: Boolean = set.getBoolean("allow_repeats")
	}

	class QuestTaskGoToLocationInfo(set: SdbResultSet, columns: Set<String>) {
		val isCreateWaypoint: Boolean = set.getBooleanIfExists(columns, "create_waypoint")
		val planetName: String? = set.getTextIfExists(columns, "planet_name")
		val locationX: Double = set.getRealIfExists(columns, "location_x")
		val locationY: Double = set.getRealIfExists(columns, "location_y")
		val locationZ: Double = set.getRealIfExists(columns, "location_z")
		val waypointName: String? = set.getTextIfExists(columns, "waypoint_name")
		val radius: Double = set.getTextIfExists(columns, "radius")?.let { if (it.isBlank()) 0.0 else it.toDouble() } ?: 0.0
	}

	class QuestTaskRetrieveItemInfo(set: SdbResultSet, columns: Set<String>) {
		val serverTemplate: String? = set.getTextIfExists(columns, "server_template")?.let { if (it.isBlank()) null else ClientFactory.formatToSharedFile(it) }
		val numRequired: Int = set.getIntIfExists(columns, "num_required")
		val itemName: String = set.getTextIfExists(columns, "item_name") ?: ""
		val dropPercent: Int = set.getIntIfExists(columns, "drop_percent")
		val retrieveMenuText: String = set.getTextIfExists(columns, "retrieve_menu_text") ?: ""
	}

	class QuestTaskInfo(set: SdbResultSet, columns: Set<String>, val index: Int) {
		private val _nextTasksOnComplete: MutableCollection<Int> = ArrayList()
		val nextTasksOnComplete: Collection<Int>
			get() = Collections.unmodifiableCollection(_nextTasksOnComplete)

		val type: String = set.getText("attach_script")
		val name = set.getTextIfExists(columns, "task_name")
		val commMessageText = set.getTextIfExists(columns, "comm_message_text")
		val npcAppearanceServerTemplate = set.getTextIfExists(columns, "npc_appearance_server_template")
		val targetServerTemplate = set.getTextIfExists(columns, "target_server_template")
		val grantQuestOnComplete = set.getTextIfExists(columns, "grant_quest_on_complete")
		val count = set.getIntIfExists(columns, "count")
		val minTime = set.getIntIfExists(columns, "min_time")
		val maxTime = set.getIntIfExists(columns, "max_time")
		val messageBoxTitle = set.getTextIfExists(columns, "message_box_title")
		val messageBoxText = set.getTextIfExists(columns, "message_box_text")
		val messageBoxSound = set.getTextIfExists(columns, "message_box_sound")
		val experienceType = set.getTextIfExists(columns, "experience_type")
		val experienceAmount = set.getIntIfExists(columns, "experience_amount")
		val factionName = set.getTextIfExists(columns, "faction_name")?.lowercase()
		val factionAmount = set.getIntIfExists(columns, "faction_amount")
		val bankCredits = set.getIntIfExists(columns, "bank_credits")
		val lootCount = set.getIntIfExists(columns, "loot_count")
		val lootName = set.getTextIfExists(columns, "loot_name")
		val itemCount = set.getIntIfExists(columns, "count")  // is this correct?
		val itemTemplate = set.getTextIfExists(columns, "item")
		val isVisible = set.getBooleanIfExists(columns, "is_visible")
		val socialGroup = set.getTextIfExists(columns, "social_group")
		val lootItemName = set.getTextIfExists(columns, "loot_item_name") ?: ""
		val lootItemsRequired = set.getIntIfExists(columns, "loot_items_required")
		val lootDropPercent = set.getIntIfExists(columns, "loot_drop_percent")
		val musicOnActivate = set.getTextIfExists(columns, "music_on_activate")
		val musicOnComplete = set.getTextIfExists(columns, "music_on_complete")
		val musicOnFailure = set.getTextIfExists(columns, "music_on_failure")
		val signalName = set.getTextIfExists(columns, "signal_name")
		val gotoLocationInfo = if (type == "quest.task.ground.go_to_location") QuestTaskGoToLocationInfo(set, columns) else null
		val retrieveItemInfo = if (type == "quest.task.ground.retrieve_item") QuestTaskRetrieveItemInfo(set, columns) else null

		init {
			for (nextTaskOnComplete in set.getText("tasks_on_complete").split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
				if (nextTaskOnComplete.isNotBlank()) {
					_nextTasksOnComplete.add(nextTaskOnComplete.toInt())
				}
			}
		}

		override fun toString(): String {
			return "QuestTaskInfo{index=$index, type='$type', name='$name'}"
		}
	}

	companion object {
		private fun SdbResultSet.getTextIfExists(columns: Set<String>, column: String): String? {
			return if (columns.contains(column)) getText(column) else null
		}

		private fun SdbResultSet.getRealIfExists(columns: Set<String>, column: String): Double {
			return if (columns.contains(column)) getReal(column) else 0.0
		}

		private fun SdbResultSet.getIntIfExists(columns: Set<String>, column: String): Int {
			return if (columns.contains(column)) getInt(column).toInt() else 0
		}

		private fun SdbResultSet.getBooleanIfExists(columns: Set<String>, column: String): Boolean {
			return if (columns.contains(column)) getBoolean(column) else false
		}
	}
}
