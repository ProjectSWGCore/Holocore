/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.commands;

import com.projectswg.common.concurrency.PswgScheduledThreadPool;
import com.projectswg.common.debug.Log;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import intents.chat.ChatBroadcastIntent;
import intents.chat.ChatCommandIntent;
import network.packets.swg.zone.object_controller.CommandQueueDequeue;
import network.packets.swg.zone.object_controller.CommandQueueEnqueue;
import resources.commands.Command;
import resources.config.ConfigFile;
import resources.encodables.ProsePackage;
import resources.encodables.StringId;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.server_info.DataManager;
import services.galaxy.GalacticManager;
import utilities.Scripts;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class CommandLauncher {
	
	private final Map<Player, Queue<EnqueuedCommand>>	combatQueueMap;
	private final PswgScheduledThreadPool				combatQueueThread;
	private final CommandCooldownHandler				cooldownHandler;
	
	public CommandLauncher() {
		this.combatQueueMap = new HashMap<>();
		this.combatQueueThread = new PswgScheduledThreadPool(1, "command-combat-queue");
		this.cooldownHandler = new CommandCooldownHandler();
	}
	
	public void start() {
		cooldownHandler.start();
		combatQueueThread.start();
		combatQueueThread.executeWithFixedRate(1000, 1000, () -> pollQueues());
	}
	
	public void stop() {
		combatQueueThread.stop();
		cooldownHandler.stop();
	}
	
	public void removePlayerFromQueue(Player player) {
		synchronized (combatQueueMap) {
			combatQueueMap.remove(player);
		}
	}
	
	public void addToQueue(Player player, EnqueuedCommand c) {
		Queue<EnqueuedCommand> combatQueue;
		synchronized (combatQueueMap) {
			combatQueue = combatQueueMap.get(player);
			if (combatQueue == null) {
				combatQueue = new PriorityQueue<>();
				combatQueueMap.put(player, combatQueue);
			}
		}
		
		// Schedule for later execution
		synchronized (combatQueue) {
			if (!combatQueue.offer(c)) {
				Log.e("Unable to enqueue command %s from %s because the combat queue is full", c.getCommand().getName(), player.getCreatureObject());
			}
		}
	}
	
	public void doCommand(Player player, EnqueuedCommand enqueued) {
		CreatureObject creature = player.getCreatureObject();
		Command command = enqueued.getCommand();
		// TODO implement locomotion and state checks up here. See action and error in CommandQueueDequeue!
		// TODO target and targetType checks
		
		if (DataManager.getConfig(ConfigFile.DEBUG).getBoolean("DEBUG-LOG-COMMAND", false))
			Log.d("doCommand %s", command.getName());
		
		sendCommandDequeue(player, command, enqueued.getRequest(), 0, 0);
		
		if (!cooldownHandler.startCooldowns(creature, enqueued)) {
			Log.w("Not starting command %s - cooldown not ready yet!", command.getName());
			return; // This ability is currently on cooldown
		}
		
		executeCommand(player, enqueued);
		new ChatCommandIntent(creature, enqueued.getTarget(), command, enqueued.getRequest().getArguments().split(" ")).broadcast();
	}
	
	private void pollQueues() {
		// Takes the head of each queue and executes the command
		synchronized (combatQueueMap) {
			combatQueueMap.forEach((player, combatQueue) -> {
				EnqueuedCommand queueHead;
				synchronized (combatQueue) {
					queueHead = combatQueue.poll();
				}
				if (queueHead == null)
					return;
				
				doCommand(player, queueHead);
			});
		}
	}
	
	private void sendCommandDequeue(Player player, Command command, CommandQueueEnqueue request, int action, int error) {
		CommandQueueDequeue dequeue = new CommandQueueDequeue(player.getCreatureObject().getObjectId());
		dequeue.setCounter(request.getCounter());
		dequeue.setAction(action);
		dequeue.setError(error);
		dequeue.setTimer(command.getExecuteTime());
		player.sendPacket(dequeue);
	}
	
	private void executeCommand(Player player, EnqueuedCommand enqueued) {
		Command command = enqueued.getCommand();
		if (player.getCreatureObject() == null) {
			Log.e("No creature object associated with the player '%s'!", player.getUsername());
			return;
		}
		
		if (player.getAccessLevel().getValue() < command.getGodLevel()) {
			String commandAccessLevel = AccessLevel.getFromValue(command.getGodLevel()).toString();
			String playerAccessLevel = player.getAccessLevel().toString();
			Log.i("[%s] attempted to use the command \"%s\", but did not have the minimum access level. Access Level Required: %s, Player Access Level: %s", player.getCharacterName(), command.getName(), commandAccessLevel, playerAccessLevel);
			String errorProseString1 = "use that command";
			new ChatBroadcastIntent(player, new ProsePackage("StringId", new StringId("cmd_err", "state_must_have_prose"), "TO", errorProseString1, "TU", commandAccessLevel)).broadcast();
			return;
		}
		
		if (!command.getCharacterAbility().isEmpty() && !player.getCreatureObject().hasAbility(command.getCharacterAbility())) {
			Log.i("[%s] attempted to use the command \"%s\", but did not have the required ability. Ability Required: %s", player.getCharacterName(), command.getName(), command.getCharacterAbility());
			String errorProseString = String.format("use the %s command", command.getName());
			new ChatBroadcastIntent(player, new ProsePackage("StringId", new StringId("cmd_err", "ability_prose"), "TO", errorProseString)).broadcast();
			return;
		}
		
		// TODO: Check if the player has the ability
		// TODO: Cool-down checks
		// TODO: Handle for different target
		// TODO: Handle for different targetType
		
		if (command.hasJavaCallback()) {
			try {
				command.getJavaCallback().newInstance().execute(enqueued.getGalacticManager(), player, enqueued.getTarget(), enqueued.getRequest().getArguments());
			} catch (InstantiationException | IllegalAccessException e) {
				Log.e(e);
			}
		} else {
			try {
				Scripts.invoke("commands/generic/" + command.getDefaultScriptCallback(), "execute", enqueued.getGalacticManager(), player, enqueued.getTarget(), enqueued.getRequest().getArguments());
			} catch (ResourceException e) {
				// Script doesn't exist
			} catch (ScriptException e) {
				Log.a(e);
			}
		}
	}
	
	public static class EnqueuedCommand implements Comparable<EnqueuedCommand> {
		
		private final Command command;
		private final GalacticManager galacticManager;
		private final SWGObject target;
		private final CommandQueueEnqueue request;
		
		public EnqueuedCommand(Command command, GalacticManager galacticManager, SWGObject target, CommandQueueEnqueue request) {
			this.command = command;
			this.galacticManager = galacticManager;
			this.target = target;
			this.request = request;
		}
		
		@Override
		public int compareTo(EnqueuedCommand o) {
			return command.getDefaultPriority().compareTo(o.getCommand().getDefaultPriority());
		}
		
		public Command getCommand() {
			return command;
		}
		
		public GalacticManager getGalacticManager() {
			return galacticManager;
		}
		
		public SWGObject getTarget() {
			return target;
		}
		
		public CommandQueueEnqueue getRequest() {
			return request;
		}
		
	}
	
}
