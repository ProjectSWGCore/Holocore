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
package com.projectswg.holocore.services.gameplay.player.quest

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.oob.OutOfBandPackage
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.common.network.packets.swg.zone.CommPlayerMessage
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.quest.QuestCompletedMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.quest.QuestTaskCounterMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.quest.QuestTaskTimerData
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent
import com.projectswg.holocore.intents.gameplay.player.quest.AbandonQuestIntent
import com.projectswg.holocore.intents.gameplay.player.quest.CompleteQuestIntent
import com.projectswg.holocore.intents.gameplay.player.quest.GrantQuestIntent
import com.projectswg.holocore.intents.gameplay.player.quest.GrantQuestIntent.Companion.broadcast
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.QuestLoader.QuestTaskInfo
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.StaticItemCreator.createItem
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject
import com.projectswg.holocore.resources.support.random.Die
import com.projectswg.holocore.resources.support.random.RandomDie
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ThreadLocalRandom

class QuestService(private val destroyMultiAndLootDie: Die = RandomDie()) : Service() {
	private val executor = ScheduledThreadPool(1, "quest-service-%d")
	private val questLoader = ServerData.questLoader

	override fun initialize(): Boolean {
		executor.start()
		return true
	}

	override fun terminate(): Boolean {
		executor.stop()
		return executor.awaitTermination(1000)
	}

	@IntentHandler
	private fun handleGrantQuestIntent(intent: GrantQuestIntent) {
		val player = intent.player
		val questName = intent.questName
		val playerObject = player.getPlayerObject()
		val questListInfo = questLoader.getQuestListInfo(questName)
		val repeatable = java.lang.Boolean.TRUE == questListInfo.isRepeatable
		if (!repeatable && playerObject.isQuestInJournal(questName)) {
			StandardLog.onPlayerError(this, player, "already had non-repeatable quest %s", questName)
			return
		}
		playerObject.addQuest(questName)
		playerObject.addActiveQuestTask(questName, 0)
		StandardLog.onPlayerTrace(this, player, "received quest %s", questName)
		handleTaskEvents(player, questName)
		val prose = ProsePackage(StringId("quest/ground/system_message", "quest_received"), "TO", questListInfo.journalEntryTitle)
		SystemMessageIntent.broadcastPersonal(player, prose, ChatSystemMessage.SystemChatType.QUEST)
	}

	@IntentHandler
	private fun handleAbandonQuestIntent(intent: AbandonQuestIntent) {
		val player = intent.player
		val questName = intent.questName
		val playerObject = player.getPlayerObject()
		if (playerObject.isQuestComplete(questName)) {
			StandardLog.onPlayerTrace(this, player, "attempted to abandon quest %s which they have completed", questName)
			return
		}
		playerObject.removeQuest(questName)
	}

	@IntentHandler
	private fun handleCompleteQuestIntent(intent: CompleteQuestIntent) {
		val player = intent.player
		val playerObject = player.getPlayerObject()
		val questName = intent.questName
		if (playerObject.isQuestRewardReceived(questName)) {
			StandardLog.onPlayerTrace(this, player, "attempted to claim reward for quest %s but they have already received it", questName)
			return
		}

		playerObject.setQuestRewardReceived(questName, true)
	}

	@IntentHandler
	private fun handleCreatureKilledIntent(intent: CreatureKilledIntent) {
		val corpse = intent.corpse as? AIObject ?: return
		val killer = intent.killer
		val owner = killer.owner ?: return
		val playerObject = killer.playerObject
		val entries = playerObject.quests.entries
		for ((key, quest) in entries) {
			if (quest.isComplete) {
				continue
			}
			val questName = key.string
			val activeTaskListInfos = getActiveTaskInfos(questName, playerObject)
			for (activeTaskListInfo in activeTaskListInfos) {
				val type = activeTaskListInfo.type
				when (type) {
					"quest.task.ground.destroy_multi"          -> handleKillDestroyMulti(activeTaskListInfo, questName, owner, corpse)
					"quest.task.ground.destroy_multi_and_loot" -> handleKillDestroyMultiAndLoot(activeTaskListInfo, questName, owner, corpse)
				}
			}
		}
	}

	private fun handleKillDestroyMultiAndLoot(activeTaskListInfo: QuestTaskInfo, questName: String, owner: Player, corpse: AIObject) {
		if (!isKillPartOfTask(activeTaskListInfo, corpse)) {
			return
		}
		
		val playerObject = owner.playerObject
		val roll = destroyMultiAndLootDie.roll(1..100)
		val itemFound = roll <= activeTaskListInfo.lootDropPercent

		if (itemFound) {
			val max = activeTaskListInfo.lootItemsRequired
			val counter = playerObject.incrementQuestCounter(questName)
			val remaining = max - counter
			val task = activeTaskListInfo.index
			StandardLog.onPlayerTrace(this, owner, "acquired %d/%d %s on task %d of quest %s", counter, max, activeTaskListInfo.lootItemName, task, questName)
			incrementKillItemCount(questName, task, owner, counter, max, activeTaskListInfo.lootItemName)
			if (remaining <= 0) {
				advanceQuest(questName, owner, activeTaskListInfo)
			}
		} else {
			StandardLog.onPlayerTrace(this, owner, "failed to acquire '%s' on task %d of quest %s", activeTaskListInfo.lootItemName, activeTaskListInfo.index, questName)
			SystemMessageIntent.broadcastPersonal(owner, ProsePackage(StringId("quest/groundquests", "destroy_multiple_and_loot_fail"), "TO", activeTaskListInfo.lootItemName))
		}
	}

	private fun handleKillDestroyMulti(activeTaskListInfo: QuestTaskInfo, questName: String, owner: Player, corpse: AIObject) {
		if (!isKillPartOfTask(activeTaskListInfo, corpse)) {
			return
		}
		
		val playerObject = owner.playerObject
		val max = activeTaskListInfo.count
		val counter = playerObject.incrementQuestCounter(questName)
		val remaining = max - counter
		val task = activeTaskListInfo.index
		StandardLog.onPlayerTrace(this, owner, "acquired %d/%d kills on task %d of quest %s", counter, max, task, questName)
		incrementKillCount(questName, task, owner, counter, max)
		if (remaining <= 0) {
			advanceQuest(questName, owner, activeTaskListInfo)
		}
	}

	private fun isKillPartOfTask(swgQuestTask: QuestTaskInfo, npcCorpse: AIObject): Boolean {
		val targetServerTemplate = swgQuestTask.targetServerTemplate
		val spawner = npcCorpse.spawner
		val stfName = spawner.stfName
		val questSocialGroup = swgQuestTask.socialGroup
		val npcSocialGroup = npcCorpse.spawner.socialGroup
		return isMatchingSocialGroup(questSocialGroup, npcSocialGroup) || isMatchingServerTemplate(targetServerTemplate, stfName)
	}

	private fun incrementKillCount(questName: String, task: Int, player: Player, counter: Int, max: Int) {
		player.sendPacket(
			QuestTaskCounterMessage(
				player.creatureObject.objectId, questName, task, "@quest/groundquests:destroy_counter", counter, max
			)
		)
		val remaining = max - counter
		val prose = ProsePackage(StringId("quest/groundquests", "destroy_multiple_success"), "DI", remaining)
		SystemMessageIntent.broadcastPersonal(player, prose)
	}

	private fun incrementKillItemCount(questName: String, task: Int, player: Player, counter: Int, max: Int, itemName: String) {
		player.sendPacket(
			QuestTaskCounterMessage(
				player.creatureObject.objectId, questName, task, "@quest/groundquests:destroy_and_loot_counter", counter, max
			)
		)
		val remaining = max - counter
		val prose = ProsePackage(StringId("quest/groundquests", "destroy_multiple_and_loot_success"), "TO", itemName, "DI", remaining)
		SystemMessageIntent.broadcastPersonal(player, prose)
	}

	private fun handleTaskEvents(player: Player, questName: String) {
		val playerObject = player.getPlayerObject()
		val currentTasks = getActiveTaskInfos(questName, playerObject)
		for (currentTask in currentTasks) {
			val type = currentTask.type ?: continue
			if (currentTask.isVisible) {
				player.sendPacket(PlayMusicMessage(0, "sound/ui_journal_updated.snd", 1, false))
			}

			when (type) {
				"quest.task.ground.comm_player"            -> handleCommPlayer(player, questName, currentTask)
				"quest.task.ground.complete_quest"         -> completeQuest(player, questName)
				"quest.task.ground.timer"                  -> handleTimer(player, questName, currentTask)
				"quest.task.ground.show_message_box"       -> handleShowMessageBox(player, questName, currentTask)
				"quest.task.ground.destroy_multi"          -> handleDestroyMulti(player, questName, currentTask)
				"quest.task.ground.destroy_multi_and_loot" -> handleDestroyMultiAndLoot(player, questName, currentTask)
				"quest.task.ground.reward"                 -> handleReward(player, questName, playerObject, currentTask)
				"quest.task.ground.nothing"                -> handleNothing(player, questName, currentTask)
			}
		}
	}

	private fun handleNothing(player: Player, questName: String, currentTask: QuestTaskInfo) {
		advanceQuest(questName, player, currentTask)
	}

	private fun handleReward(player: Player, questName: String, playerObject: PlayerObject, currentTask: QuestTaskInfo) {
		grantXPReward(player, currentTask)
		grantFactionPointsReward(playerObject, currentTask)
		grantCreditsReward(player, currentTask)
		grantLootRewards(player, currentTask)
		grantItemRewards(player, currentTask)
		// weapon stuff: weapon	count_weapon	speed	damage	efficiency	elemental_value
		// armor stuff: armor	count_armor	quality
		// speed etc. are percentages, the absolute values we must define somewhere else before we can hand out weapons and armor rewards
		advanceQuest(questName, player, currentTask)
	}

	private fun handleShowMessageBox(player: Player, questName: String, currentTask: QuestTaskInfo) {
		val messageBoxTitle = currentTask.messageBoxTitle
		val messageBoxText = currentTask.messageBoxText
		val sui = SuiMessageBox(SuiButtons.OK, messageBoxTitle, messageBoxText)
		sui.setSize(384, 256)
		sui.setLocation(320, 256)
		sui.display(player)
		advanceQuest(questName, player, currentTask)
	}

	private fun handleCommPlayer(player: Player, questName: String, currentTask: QuestTaskInfo) {
		val commMessageText = currentTask.commMessageText
		val message = OutOfBandPackage(ProsePackage(StringId(commMessageText)))
		val objectId = player.creatureObject.objectId
		val modelCrc = getModelCrc(currentTask)
		player.sendPacket(CommPlayerMessage(objectId, message, modelCrc, "", 10f))
		advanceQuest(questName, player, currentTask)
	}

	private fun handleTimer(player: Player, questName: String, currentTask: QuestTaskInfo) {
		val minTime = currentTask.minTime
		val maxTime = currentTask.maxTime
		val random = ThreadLocalRandom.current()
		val delaySeconds = random.nextInt(minTime, maxTime + 1)
		val delayMilliseconds = delaySeconds * 1000
		executor.execute(delayMilliseconds.toLong()) {	// TODO if the server is restarted, the timer will be lost and the quest will be stuck
			if (player.playerObject.isQuestInJournal(questName)) {
				StandardLog.onPlayerTrace(this, player, "timer for task %d of quest %s expired", currentTask.index, questName)
				advanceQuest(questName, player, currentTask)
			}
		}

		if (currentTask.isVisible) {
			player.playerObject.updatePlayTime()	// So the client can calculate the correct time remaining after we send QuestTaskTimerData
			val task = currentTask.index
			val timerPacket = QuestTaskTimerData(player.creatureObject.objectId)
			timerPacket.questName = questName
			timerPacket.taskId = task
			timerPacket.counterText = "@quest/groundquests:timer_timertext"
			timerPacket.duration = player.playerObject.playTime + delaySeconds
			player.sendPacket(timerPacket)
		}
	}

	private fun advanceQuest(questName: String, player: Player, currentTask: QuestTaskInfo) {
		val playerObject = player.getPlayerObject()
		playerObject.removeActiveQuestTask(questName, currentTask.index)
		playerObject.addCompleteQuestTask(questName, currentTask.index)
		val grantQuestOnComplete = currentTask.grantQuestOnComplete
		if (grantQuestOnComplete != null && grantQuestOnComplete.isNotBlank()) {
			broadcast(player, grantQuestOnComplete)
		}

		val nextTasksOnComplete = currentTask.nextTasksOnComplete
		StandardLog.onPlayerTrace(this, player, "completed task %d of quest %s, activating next tasks [%s]", currentTask.index, questName, nextTasksOnComplete.joinToString(separator = ","))
		nextTasksOnComplete.forEach { playerObject.addActiveQuestTask(questName, it) }

		handleTaskEvents(player, questName)

		if (nextTasksOnComplete.isEmpty()) {
			if (questLoader.getQuestListInfo(questName).isCompleteWhenTasksComplete) {
				completeQuest(player, questName)
			}
		}
	}

	private fun completeQuest(player: Player, questName: String) {
		val playerObject = player.getPlayerObject()
		playerObject.completeQuest(questName)
		player.sendPacket(QuestCompletedMessage(player.creatureObject.objectId, CRC(questName)))
		StandardLog.onPlayerTrace(this, player, "completed quest %s", questName)
	}

	private fun getActiveTaskInfos(questName: String, playerObject: PlayerObject): List<QuestTaskInfo> {
		val taskListInfos = questLoader.getTaskListInfos(questName)
		val questActiveTasks = playerObject.getQuestActiveTasks(questName)
		return mapActiveTasks(questActiveTasks, taskListInfos)
	}

	private fun mapActiveTasks(activeTaskIndices: Collection<Int>, taskListInfos: List<QuestTaskInfo>): List<QuestTaskInfo> {
		return activeTaskIndices.map { taskListInfos[it] }
	}

	private fun isMatchingServerTemplate(targetServerTemplate: String?, stfName: String): Boolean {
		return targetServerTemplate != null && targetServerTemplate == stfName
	}

	private fun isMatchingSocialGroup(questSocialGroup: String?, npcSocialGroup: String?): Boolean {
		return questSocialGroup != null && questSocialGroup.equals(npcSocialGroup, ignoreCase = true)
	}

	private fun grantXPReward(player: Player, currentTask: QuestTaskInfo) {
		val experienceType = currentTask.experienceType
		if (experienceType != null) {
			ExperienceIntent(player.creatureObject, experienceType, currentTask.experienceAmount).broadcast()
		}
	}

	private fun grantFactionPointsReward(playerObject: PlayerObject, currentTask: QuestTaskInfo) {
		val factionName = currentTask.factionName
		if (factionName != null) {
			playerObject.adjustFactionPoints(factionName, currentTask.factionAmount)
		}
	}

	private fun grantCreditsReward(player: Player, currentTask: QuestTaskInfo) {
		val bankCredits = currentTask.bankCredits
		if (bankCredits != 0) {
			player.creatureObject.addToBank(bankCredits.toLong())
		}
	}

	private fun grantLootRewards(player: Player, currentTask: QuestTaskInfo) {
		val lootCount = currentTask.lootCount
		if (lootCount > 0) {
			for (i in 0 until lootCount) {
				val lootName = currentTask.lootName
				if (lootName != null && lootName.isNotBlank()) {
					val item = createItem(lootName)
					if (item != null) {
						transferItemToInventory(player, item)
					}
				}
			}
		}
	}

	private fun grantItemRewards(player: Player, currentTask: QuestTaskInfo) {
		val itemCount = currentTask.itemCount
		if (itemCount > 0) {
			for (i in 0 until itemCount) {
				val itemTemplate = currentTask.itemTemplate
				if (itemTemplate != null && itemTemplate.isNotBlank()) {
					val item = ObjectCreator.createObjectFromTemplate(itemTemplate)
					transferItemToInventory(player, item)
				}
			}
		}
	}

	private fun transferItemToInventory(player: Player, item: SWGObject) {
		val inventory = player.creatureObject.getInventory()
		item.moveToContainer(inventory)
		ObjectCreatedIntent.broadcast(item)
		SystemMessageIntent.broadcastPersonal(
			player, ProsePackage(StringId("quest/ground/system_message", "placed_in_inventory"), "TO", item.stringId)
		)
	}

	private fun handleDestroyMulti(player: Player, questName: String, currentTask: QuestTaskInfo) {
		val task = currentTask.index
		val max = currentTask.count
		val counter = 0
		player.sendPacket(
			QuestTaskCounterMessage(
				player.creatureObject.objectId, questName, task, "@quest/groundquests:destroy_counter", counter, max
			)
		)
	}

	private fun handleDestroyMultiAndLoot(player: Player, questName: String, currentTask: QuestTaskInfo) {
		val task = currentTask.index
		val max = currentTask.lootItemsRequired
		val counter = 0
		player.sendPacket(
			QuestTaskCounterMessage(
				player.creatureObject.objectId, questName, task, "@quest/groundquests:destroy_and_loot_counter", counter, max
			)
		)
	}

	private fun getModelCrc(currentTask: QuestTaskInfo): CRC {
		val npcAppearanceServerTemplate = currentTask.npcAppearanceServerTemplate
		if (npcAppearanceServerTemplate != null && npcAppearanceServerTemplate.isNotBlank()) {
			val sharedTemplate = ClientFactory.formatToSharedFile(npcAppearanceServerTemplate)
			return CRC(sharedTemplate)
		}
		return CRC(0) // Fallback case, as some tasks don't have an appearance set. The player sees their own character in the comm window.
	}
}
