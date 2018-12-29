package com.projectswg.holocore.services.support.global.commands;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.combat.AttackType;
import com.projectswg.common.data.combat.HitType;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueDequeue;
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueDequeue.ErrorCode;
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue;
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandTimer;
import com.projectswg.common.network.packets.swg.zone.object_controller.IntendedTarget;
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
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
		executor.executeWithFixedRate(1000, 1000, this::executeQueuedCommands);
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
		} else if (p instanceof IntendedTarget) {
			if (((IntendedTarget) p).getTargetId() == 0)
				combatQueueMap.remove(gpi.getPlayer().getCreatureObject());
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		if (pei.getEvent() == PlayerEvent.PE_LOGGED_OUT) {
			// No reason to keep their combat queue in the map if they log out
			// This also prevents queued commands from executing after the player logs out
			if (creature != null)
				combatQueueMap.remove(creature);
		}
	}
	
	@IntentHandler
	private void handleQueueCommandIntent(QueueCommandIntent qci) {
		getQueue(qci.getSource()).startCommand(new EnqueuedCommand(qci.getSource(), qci.getCommand(), qci.getTarget(), qci.getArguments(), qci.getCounter()));
	}
	
	@IntentHandler
	private void handleExitCombatIntent(ExitCombatIntent eci) {
		combatQueueMap.remove(eci.getSource());
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
			this.activeCooldownGroups = ConcurrentHashMap.newKeySet();
		}
		
		public synchronized void executeNextCommand() {
			EnqueuedCommand command = commandQueue.poll();
			if (command != null)
				execute(command);
		}
		
		public synchronized void startCommand(EnqueuedCommand command) {
			if (isValidCooldownGroup(command.getCommand().getCooldownGroup()) && command.getCommand().isAddToCombatQueue()) {
				StandardLog.onPlayerTrace(CommandQueueService.this, command.getSource(), "queued command %s", command.getCommand().getName());
				if (!commandQueue.contains(command))
					commandQueue.offer(command);
			} else {
				execute(command);
			}
		}
		
		public synchronized void execute(EnqueuedCommand command) {
			StandardLog.onPlayerTrace(CommandQueueService.this, command.getSource(), "executed command %s", command.getCommand().getName());
			
			Command rootCommand = command.getCommand();
			ErrorCode error = checkCommand(command);
			if (error == ErrorCode.SUCCESS) {
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
				} else {
					sendQueueRemove(command, error);
				}
				ExecuteCommandIntent.broadcast(command.getSource(), command.getTarget(), command.getArguments(), command.getCommand());
			} else {
				sendQueueRemove(command, error);
			}
		}
		
		private void sendQueueRemove(EnqueuedCommand command, ErrorCode error) {
			command.getSource().sendSelf(new CommandQueueDequeue(command.getSource().getObjectId(), command.getCounter(), (float) command.getCommand().getExecuteTime(), error, 0));
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
		
		private ErrorCode checkCommand(EnqueuedCommand command) {
			Command rootCommand = command.getCommand();
			if (rootCommand instanceof CombatCommand) {
				CombatCommand combatCommand = (CombatCommand) rootCommand;
				if (combatCommand.getHitType() == HitType.HEAL && combatCommand.getAttackType() == AttackType.SINGLE_TARGET) {
					SWGObject target;
					switch (combatCommand.getTargetType()) {
						case NONE:
							target = command.getSource();
							break;
						case REQUIRED:
							target  = command.getTarget();
							break;
						case OPTIONAL:
						case ALL:
							if (command.getTarget() == null) {
								target = command.getSource();
							} else if (command.getTarget() instanceof CreatureObject) {
								target = command.getSource().isEnemyOf((CreatureObject) command.getTarget()) ? command.getSource() : command.getTarget();
							} else {
								target = null;
							}
							break;
						default:
							target = null;
							break;
					}
					if (target instanceof CreatureObject && ((CreatureObject) target).getHealth() == ((CreatureObject) target).getMaxHealth())
						return ErrorCode.CANCELLED;
				}
			}
			return ErrorCode.SUCCESS;
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
		
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			EnqueuedCommand that = (EnqueuedCommand) o;
			return Objects.equals(command, that.command);
		}
		
		@Override
		public int hashCode() {
			return command.hashCode();
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
