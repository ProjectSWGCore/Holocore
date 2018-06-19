package com.projectswg.holocore.services.support.global.commands;

import com.projectswg.common.data.CRC;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.*;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandQueueService extends Service {
	
	private final ScheduledThreadPool executor;
	private final Map<CreatureObject, CreatureCombatQueue> combatQueueMap;
	
	public CommandQueueService() {
		this.executor = new ScheduledThreadPool(4, "command-queue-%d");
		this.combatQueueMap = new ConcurrentHashMap<>();
	}
	
	@Override
	public boolean initialize() {
		executor.start();
		executor.executeWithFixedDelay(1000, 1000, this::executeQueuedCommands);
		return true;
	}
	
	@Override
	public boolean terminate() {
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket p = gpi.getPacket();
		if (p instanceof CommandQueueEnqueue) {
			CommandQueueEnqueue request = (CommandQueueEnqueue) p;
			Command command = DataLoader.commands().getCommand(request.getCommandCrc());
			if (command == null) {
				if (request.getCommandCrc() != 0)
					Log.e("Invalid command crc: %x [%s]", request.getCommandCrc(), CRC.getString(request.getCommandCrc()));
				return;
			}
			long targetId = request.getTargetId();
			SWGObject target = targetId != 0 ? ObjectLookup.getObjectById(targetId) : null;
			QueueCommandIntent.broadcast(gpi.getPlayer().getCreatureObject(), target, request.getArguments(), command, request.getCounter());
		} else if (p instanceof LookAtTarget) {
			if (((LookAtTarget) p).getTargetId() == 0)
				combatQueueMap.remove(gpi.getPlayer().getCreatureObject());
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		switch (pei.getEvent()) {
			case PE_LOGGED_OUT:
				// No reason to keep their combat queue in the map if they log out
				// This also prevents queued commands from executing after the player logs out
				if (creature != null)
					combatQueueMap.remove(creature);
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleQueueCommandIntent(QueueCommandIntent qci) {
		getQueue(qci.getSource()).startCommand(new EnqueuedCommand(qci.getSource(), qci.getCommand(), qci.getTarget(), qci.getArguments(), qci.getCounter()));
	}
	
	private void executeQueuedCommands() {
		combatQueueMap.values().forEach(CreatureCombatQueue::executeNextCommand);
	}
	
	private CreatureCombatQueue getQueue(CreatureObject creature) {
		return combatQueueMap.computeIfAbsent(creature, c -> new CreatureCombatQueue());
	}
	
	private class CreatureCombatQueue {
		
		private final Queue<EnqueuedCommand> commandQueue;
		private final Set<String> activeCooldownGroups;
		
		public CreatureCombatQueue() {
			this.commandQueue = new PriorityQueue<>();
			this.activeCooldownGroups = new HashSet<>();
		}
		
		public synchronized void executeNextCommand() {
			EnqueuedCommand command = commandQueue.poll();
			if (command != null)
				execute(command);
		}
		
		public synchronized void startCommand(EnqueuedCommand command) {
			if (isValidCooldownGroup(command.getCommand().getCooldownGroup()) && command.getCommand().isAddToCombatQueue())
				commandQueue.offer(command);
			else
				execute(command);
		}
		
		public synchronized void execute(EnqueuedCommand command) {
			Player player = command.getSource().getOwner();
			if (player != null)
				player.sendPacket(new CommandQueueDequeue(command.getSource().getObjectId(), command.getCounter(), (float) command.getCommand().getExecuteTime(), 0, 0));
			
			Command rootCommand = command.getCommand();
			if (isValidCooldownGroup(rootCommand.getCooldownGroup())) {
				if (!activeCooldownGroups.add(rootCommand.getCooldownGroup()))
					return;
				if (isValidCooldownGroup(rootCommand.getCooldownGroup2())) {
					if (!activeCooldownGroups.add(rootCommand.getCooldownGroup2())) {
						activeCooldownGroups.remove(rootCommand.getCooldownGroup());
						return;
					}
					startCooldownGroup(command.getSource(), rootCommand, rootCommand.getCooldownGroup2(), rootCommand.getCooldownTime2(), command.getCounter());
				}
				startCooldownGroup(command.getSource(), rootCommand, rootCommand.getCooldownGroup(), rootCommand.getCooldownTime(), command.getCounter());
			}
			
			ExecuteCommandIntent.broadcast(command.getSource(), command.getTarget(), command.getArguments(), command.getCommand());
		}
		
		private boolean isValidCooldownGroup(String group) {
			return !group.isEmpty() && !group.equals("defaultCooldownGroup");
		}
		
		private void startCooldownGroup(CreatureObject creature, Command command, String group, double cooldownTime, int counter) {
			CommandTimer commandTimer = new CommandTimer(creature.getObjectId());
			commandTimer.setCooldownGroupCrc(CRC.getCrc(group));
			commandTimer.setCooldownMax((float) cooldownTime);
			commandTimer.setCommandNameCrc(command.getCrc());
			commandTimer.setSequenceId(counter);
			creature.sendSelf(commandTimer);
			
			executor.execute((long) (cooldownTime * 1000), () -> activeCooldownGroups.remove(group));
		}
		
	}
	
	private static class EnqueuedCommand implements Comparable<EnqueuedCommand> {
		
		private final CreatureObject source;
		private final SWGObject target;
		private final String arguments;
		private final Command command;
		private final int counter;
		
		public EnqueuedCommand(@NotNull CreatureObject source, @NotNull Command command, @Nullable SWGObject target, @NotNull String arguments, int counter) {
			this.source = source;
			this.command = command;
			this.target = target;
			this.arguments = arguments;
			this.counter = counter;
		}
		
		@Override
		public int compareTo(@NotNull EnqueuedCommand o) {
			return command.getDefaultPriority().compareTo(o.getCommand().getDefaultPriority());
		}
		
		@NotNull
		public CreatureObject getSource() {
			return source;
		}
		
		@Nullable
		public SWGObject getTarget() {
			return target;
		}
		
		@NotNull
		public String getArguments() {
			return arguments;
		}
		
		@NotNull
		public Command getCommand() {
			return command;
		}
		
		public int getCounter() {
			return counter;
		}
		
	}
	
}
