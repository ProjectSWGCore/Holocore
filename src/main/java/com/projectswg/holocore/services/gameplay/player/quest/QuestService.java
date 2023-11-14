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
package com.projectswg.holocore.services.gameplay.player.quest;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.network.packets.swg.zone.CommPlayerMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.quest.QuestCompletedMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.quest.QuestTaskCounterMessage;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.gameplay.player.quest.AbandonQuestIntent;
import com.projectswg.holocore.intents.gameplay.player.quest.AdvanceQuestIntent;
import com.projectswg.holocore.intents.gameplay.player.quest.CompleteQuestIntent;
import com.projectswg.holocore.intents.gameplay.player.quest.GrantQuestIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.QuestLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.StaticItemCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.player.Quest;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class QuestService extends Service {
	
	private final ScheduledThreadPool executor;
	private final QuestLoader questLoader;
	
	public QuestService() {
		this.executor = new ScheduledThreadPool(1, "quest-service-%d");
		this.questLoader = ServerData.INSTANCE.getQuestLoader();
	}
	
	@Override
	public boolean initialize() {
		executor.start();
		return true;
	}
	
	@Override
	public boolean terminate() {
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handleGrantQuestIntent(GrantQuestIntent intent) {
		Player player = intent.getPlayer();
		String questName = intent.getQuestName();
		PlayerObject playerObject = player.getPlayerObject();
		
		QuestLoader.QuestListInfo questListInfo = questLoader.getQuestListInfo(questName);
		
		boolean repeatable = Boolean.TRUE.equals(questListInfo.isRepeatable());
		
		if (!repeatable && playerObject.isQuestInJournal(questName)) {
			StandardLog.onPlayerError(this, player, "already had non-repeatable quest %s", questName);
			return;
		}
		
		playerObject.addQuest(questName);
		playerObject.addActiveQuestTask(questName, 0);
		
		StandardLog.onPlayerTrace(this, player, "received quest %s", questName);
		
		List<QuestLoader.QuestTaskInfo> currentTasks = getActiveTaskInfos(questName, playerObject);
		
		handleTaskEvents(player, questName, currentTasks);
		
		ProsePackage prose = new ProsePackage(new StringId("quest/ground/system_message", "quest_received"), "TO", questListInfo.getJournalEntryTitle());
		SystemMessageIntent.broadcastPersonal(player, prose, ChatSystemMessage.SystemChatType.QUEST);
		
		player.sendPacket(new PlayMusicMessage(0, "sound/ui_npe2_quest_received.snd", 1, false));
	}
	
	@IntentHandler
	private void handleAbandonQuestIntent(AbandonQuestIntent intent) {
		Player player = intent.getPlayer();
		String questName = intent.getQuestName();
		PlayerObject playerObject = player.getPlayerObject();
		
		if (playerObject.isQuestComplete(questName)) {
			StandardLog.onPlayerTrace(this, player, "attempted to abandon quest %s which they have completed", questName);
			return;
		}
		
		playerObject.removeQuest(questName);
	}
	
	@IntentHandler
	private void handleCompleteQuestIntent(CompleteQuestIntent intent) {
		Player player = intent.getPlayer();
		PlayerObject playerObject = player.getPlayerObject();
		String questName = intent.getQuestName();
		
		if (playerObject.isQuestRewardReceived(questName)) {
			StandardLog.onPlayerTrace(this, player, "attempted to claim reward for quest %s but they have already received it", questName);
			return;
		}
		
		// TODO award XP, load from tier+level matrix: datatables/quest/quest_experience.iff
		
		playerObject.setQuestRewardReceived(questName, true);
	}
	
	@IntentHandler
	private void handleAdvanceQuestIntent(AdvanceQuestIntent intent) {
		Player player = intent.getPlayer();
		String questName = intent.getQuestName();
		PlayerObject playerObject = player.getPlayerObject();
		
		if (!playerObject.isQuestInJournal(questName)) {
			StandardLog.onPlayerError(this, player, "advanced quest %s that was not in their quest journal", questName);
			return;
		}
		
		List<QuestLoader.QuestTaskInfo> currentTasks = getActiveTaskInfos(questName, playerObject);
		
		advanceQuest(questName, player, currentTasks);
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent intent) {
		CreatureObject corpse = intent.getCorpse();
		
		if (!(corpse instanceof AIObject)) {
			return;
		}
		
		AIObject npcCorpse = (AIObject) corpse;
		CreatureObject killer = intent.getKiller();
		Player owner = killer.getOwner();
		
		if (owner == null) {
			return;
		}
		
		PlayerObject playerObject = killer.getPlayerObject();
		
		Set<Map.Entry<CRC, Quest>> entries = playerObject.getQuests().entrySet();
		
		for (Map.Entry<CRC, Quest> entry : entries) {
			CRC key = entry.getKey();
			Quest quest = entry.getValue();
			
			if (quest.isComplete()) {
				continue;
			}
			
			String questName = key.getString();
			List<QuestLoader.QuestTaskInfo> activeTaskListInfos = getActiveTaskInfos(questName, playerObject);
			
			for (QuestLoader.QuestTaskInfo activeTaskListInfo : activeTaskListInfos) {
				String type = activeTaskListInfo.getType();
				
				if ("quest.task.ground.destroy_multi".equals(type)) {
					if (isKillPartOfTask(activeTaskListInfo, npcCorpse)) {
						int max = activeTaskListInfo.getCount();
						int counter = playerObject.incrementQuestCounter(questName);
						int remaining = max - counter;
						int task = activeTaskListInfo.getIndex();
						StandardLog.onPlayerTrace(this, owner, "%d remaining kills required on quest %s", remaining, questName);

						incrementKillCount(questName, task, owner, counter, max);
						
						if (remaining <= 0) {
							advanceQuest(questName, owner, activeTaskListInfos);
						}
					}
				}
			}
		}
	}
	
	private boolean isKillPartOfTask(QuestLoader.QuestTaskInfo swgQuestTask, AIObject npcCorpse) {
		String targetServerTemplate = swgQuestTask.getTargetServerTemplate();
		Spawner spawner = npcCorpse.getSpawner();
		String stfName = spawner.getStfName();
		
		return Objects.equals(targetServerTemplate, stfName);
	}
	
	private void incrementKillCount(String questName, int task, Player player, int counter, int max) {
		player.sendPacket(new QuestTaskCounterMessage(player.getCreatureObject().getObjectId(), questName, task, "@quest/groundquests:destroy_counter", counter, max));
		int remaining = max - counter;
		ProsePackage prose = new ProsePackage(new StringId("quest/groundquests", "destroy_multiple_success"), "DI", remaining);
		SystemMessageIntent.broadcastPersonal(player, prose);
	}
	
	private void handleTaskEvents(Player player, String questName, Collection<QuestLoader.QuestTaskInfo> currentTasks) {
		PlayerObject playerObject = player.getPlayerObject();
		
		for (QuestLoader.QuestTaskInfo currentTask : currentTasks) {
			String type = currentTask.getType();
			
			if (type == null) {
				continue;
			}
			
			switch (type) {
				case "quest.task.ground.comm_player": {
					handleCommPlayer(player, questName, playerObject, currentTask);
					break;
				}
				case "quest.task.ground.complete_quest": {
					completeQuest(player, questName);
					break;
				}
				case "quest.task.ground.timer": {
					handleTimer(player, questName, playerObject, currentTask);
					break;
				}
				case "quest.task.ground.show_message_box": {
					handleShowMessageBox(player, questName, playerObject, currentTask);
					break;
				}
				case "quest.task.ground.destroy_multi": {
					handleDestroyMulti(player, questName, currentTask);
					break;
				}
				case "quest.task.ground.reward": {
					handleReward(player, questName, playerObject, currentTask);
					break;
				}
			}
		}
	}

	private void handleReward(Player player, String questName, PlayerObject playerObject, QuestLoader.QuestTaskInfo currentTask) {
		// TODO should we provide the player with some feedback on what they received? Maybe the sauce code can reveal that.
		grantXPReward(player, currentTask);
		grantFactionPointsReward(playerObject, currentTask);
		grantCreditsReward(player, currentTask);
		grantLootRewards(player, currentTask);
		grantItemRewards(player, currentTask);

		playerObject.removeActiveQuestTask(questName, currentTask.getIndex());
		playerObject.addCompleteQuestTask(questName, currentTask.getIndex());

		for (Integer taskIndex : currentTask.getNextTasksOnComplete()) {
			playerObject.addActiveQuestTask(questName, taskIndex);
		}

		List<QuestLoader.QuestTaskInfo> taskListInfos = questLoader.getTaskListInfos(questName);
		Collection<Integer> nextTasksOnComplete = currentTask.getNextTasksOnComplete();
		List<QuestLoader.QuestTaskInfo> nextTasks = mapActiveTasks(nextTasksOnComplete, taskListInfos);

		handleTaskEvents(player, questName, nextTasks);
	}

	private static void grantXPReward(Player player, QuestLoader.QuestTaskInfo currentTask) {
		String experienceType = currentTask.getExperienceType();
		if (experienceType != null) {
			new ExperienceIntent(player.getCreatureObject(), experienceType, currentTask.getExperienceAmount()).broadcast();
		}
	}

	private static void grantFactionPointsReward(PlayerObject playerObject, QuestLoader.QuestTaskInfo currentTask) {
		String factionName = currentTask.getFactionName();
		if (factionName != null) {
			playerObject.adjustFactionPoints(factionName, currentTask.getFactionAmount());
		}
	}

	private static void grantCreditsReward(Player player, QuestLoader.QuestTaskInfo currentTask) {
		int bankCredits = currentTask.getBankCredits();
		if (bankCredits != 0) {
			player.getCreatureObject().addToBank(bankCredits);
		}
	}

	private static void grantLootRewards(Player player, QuestLoader.QuestTaskInfo currentTask) {
		int lootCount = currentTask.getLootCount();
		if (lootCount > 0) {
			SWGObject inventory = player.getCreatureObject().getInventory();
			for (int i = 0; i < lootCount; i++) {
				SWGObject item = StaticItemCreator.INSTANCE.createItem(currentTask.getLootName());
				if (item != null) {
					item.moveToContainer(inventory);
					ObjectCreatedIntent.broadcast(item);
				} else {
					// TODO warning..?
				}
			}
		}
	}

	private static void grantItemRewards(Player player, QuestLoader.QuestTaskInfo currentTask) {
		int itemCount = currentTask.getItemCount();
		if (itemCount > 0) {
			SWGObject inventory = player.getCreatureObject().getInventory();
			for (int i = 0; i < itemCount; i++) {
				SWGObject item = ObjectCreator.createObjectFromTemplate(currentTask.getItemTemplate());
				if (item != null) {
					item.moveToContainer(inventory);
					ObjectCreatedIntent.broadcast(item);
				} else {
					// TODO warning..?
				}
			}
		}
	}

	private static void handleDestroyMulti(Player player, String questName, QuestLoader.QuestTaskInfo currentTask) {
		int task = currentTask.getIndex();
		int max = currentTask.getCount();
		int counter = 0;
		player.sendPacket(new QuestTaskCounterMessage(player.getCreatureObject().getObjectId(), questName, task, "@quest/groundquests:destroy_counter", counter, max));
	}

	private void handleShowMessageBox(Player player, String questName, PlayerObject playerObject, QuestLoader.QuestTaskInfo currentTask) {
		String messageBoxTitle = currentTask.getMessageBoxTitle();
		String messageBoxText = currentTask.getMessageBoxText();
		SuiMessageBox sui = new SuiMessageBox(SuiButtons.OK, messageBoxTitle, messageBoxText);
		sui.setSize(384, 256);
		sui.setLocation(320, 256);
		sui.display(player);

		playerObject.removeActiveQuestTask(questName, currentTask.getIndex());
		playerObject.addCompleteQuestTask(questName, currentTask.getIndex());

		for (Integer taskIndex : currentTask.getNextTasksOnComplete()) {
			playerObject.addActiveQuestTask(questName, taskIndex);
		}

		List<QuestLoader.QuestTaskInfo> taskListInfos = questLoader.getTaskListInfos(questName);
		Collection<Integer> nextTasksOnComplete = currentTask.getNextTasksOnComplete();
		List<QuestLoader.QuestTaskInfo> nextTasks = mapActiveTasks(nextTasksOnComplete, taskListInfos);

		handleTaskEvents(player, questName, nextTasks);
	}

	private void handleCommPlayer(Player player, String questName, PlayerObject playerObject, QuestLoader.QuestTaskInfo currentTask) {
		String commMessageText = currentTask.getCommMessageText();
		OutOfBandPackage message = new OutOfBandPackage(new ProsePackage(new StringId(commMessageText)));
		long objectId = player.getCreatureObject().getObjectId();
		
		String sharedTemplate = ClientFactory.formatToSharedFile(currentTask.getNpcAppearanceServerTemplate());
		CRC modelCrc = new CRC(sharedTemplate);
		
		player.sendPacket(new CommPlayerMessage(objectId, message, modelCrc, "", 10));
		
		playerObject.removeActiveQuestTask(questName, currentTask.getIndex());
		playerObject.addCompleteQuestTask(questName, currentTask.getIndex());
		
		for (Integer taskIndex : currentTask.getNextTasksOnComplete()) {
			playerObject.addActiveQuestTask(questName, taskIndex);
		}
		
		List<QuestLoader.QuestTaskInfo> taskListInfos = questLoader.getTaskListInfos(questName);
		Collection<Integer> nextTasksOnComplete = currentTask.getNextTasksOnComplete();
		List<QuestLoader.QuestTaskInfo> nextTasks = mapActiveTasks(nextTasksOnComplete, taskListInfos);
		
		handleTaskEvents(player, questName, nextTasks);
	}
	
	private void handleTimer(Player player, String questName, PlayerObject playerObject, QuestLoader.QuestTaskInfo currentTask) {
		int minTime = currentTask.getMinTime();
		int maxTime = currentTask.getMaxTime();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int delay = random.nextInt(minTime, maxTime) * 1000;
		
		executor.execute(delay, () -> {
			playerObject.removeActiveQuestTask(questName, currentTask.getIndex());
			playerObject.addCompleteQuestTask(questName, currentTask.getIndex());
			
			List<QuestLoader.QuestTaskInfo> taskListInfos = questLoader.getTaskListInfos(questName);
			Collection<Integer> nextTasksOnComplete = currentTask.getNextTasksOnComplete();
			List<QuestLoader.QuestTaskInfo> nextTasks = mapActiveTasks(nextTasksOnComplete, taskListInfos);
			
			handleTaskEvents(player, questName, nextTasks);
		});
	}
	
	private void advanceQuest(String questName, Player player, List<QuestLoader.QuestTaskInfo> currentTasks) {
		PlayerObject playerObject = player.getPlayerObject();
		
		for (QuestLoader.QuestTaskInfo currentTask : currentTasks) {
			playerObject.removeActiveQuestTask(questName, currentTask.getIndex());
			playerObject.addCompleteQuestTask(questName, currentTask.getIndex());
			
			Collection<Integer> nextTasksOnComplete = currentTask.getNextTasksOnComplete();
			
			for (Integer nextTaskOnComplete : nextTasksOnComplete) {
				playerObject.addActiveQuestTask(questName, nextTaskOnComplete);
				StandardLog.onPlayerTrace(this, player, "advanced quest %s, activated task %d", questName, nextTaskOnComplete);
			}
		}
		
		List<QuestLoader.QuestTaskInfo> nextTasks = getActiveTaskInfos(questName, playerObject);
		
		if (nextTasks.isEmpty()) {
			QuestLoader.QuestListInfo questListInfo = questLoader.getQuestListInfo(questName);
			boolean completeWhenTasksComplete = questListInfo.isCompleteWhenTasksComplete();
			
			if (completeWhenTasksComplete) {
				completeQuest(player, questName);
				
				nextTasks = currentTasks;
			}
		} else {
			player.sendPacket(new PlayMusicMessage(0, "sound/ui_npe2_quest_step_completed.snd", 1, false));
		}
		
		handleTaskEvents(player, questName, nextTasks);
		
		for (QuestLoader.QuestTaskInfo nextTask : nextTasks) {
			String grantQuestOnComplete = nextTask.getGrantQuestOnComplete();
			
			if (grantQuestOnComplete != null && !grantQuestOnComplete.isBlank()) {
				GrantQuestIntent.broadcast(player, grantQuestOnComplete);
			}
		}
	}
	
	private void completeQuest(Player player, String questName) {
		PlayerObject playerObject = player.getPlayerObject();
		playerObject.completeQuest(questName);
		player.sendPacket(
				new QuestCompletedMessage(player.getCreatureObject().getObjectId(), new CRC(questName)),
				new PlayMusicMessage(0, "sound/ui_npe2_quest_completed.snd", 1, false)
		);
		StandardLog.onPlayerTrace(this, player, "completed quest %s", questName);
	}
	
	private List<QuestLoader.QuestTaskInfo> getActiveTaskInfos(String questName, PlayerObject playerObject) {
		List<QuestLoader.QuestTaskInfo> taskListInfos = questLoader.getTaskListInfos(questName);
		Collection<Integer> questActiveTasks = playerObject.getQuestActiveTasks(questName);
		
		return mapActiveTasks(questActiveTasks, taskListInfos);
	}
	
	private List<QuestLoader.QuestTaskInfo> mapActiveTasks(Collection<Integer> activeTaskIndices, List<QuestLoader.QuestTaskInfo> taskListInfos) {
		return activeTaskIndices.stream()
				.map(taskListInfos::get)
				.collect(Collectors.toList());
	}
	
}
