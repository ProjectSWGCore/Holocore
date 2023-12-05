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
package com.projectswg.holocore.services.support.global.commands;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.combat.AttackType;
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus;
import com.projectswg.common.data.combat.HitType;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.*;
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueDequeue.ErrorCode;
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.color.SWGColor;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ValidWeapon;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.commands.Locomotion;
import com.projectswg.holocore.resources.support.global.commands.State;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon;
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandHandler;
import com.projectswg.holocore.resources.support.random.Die;
import com.projectswg.holocore.resources.support.random.RandomDie;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CommandQueueService extends Service {
	
	private final ScheduledThreadPool executor;
	private final Map<CreatureObject, CreatureCombatQueue> combatQueueMap;
	private final CombatCommandHandler combatCommandHandler;
	private final long delayBetweenCheckingCommandQueue;
	
	public CommandQueueService() {
		this(100);
	}
	
	public CommandQueueService(long delayBetweenCheckingCommandQueue) {
		this(delayBetweenCheckingCommandQueue, new RandomDie(), new RandomDie());
	}
	
	public CommandQueueService(long delayBetweenCheckingCommandQueue, Die toHitDie, Die knockdownDie) {
		this.executor = new ScheduledThreadPool(4, "command-queue-%d");
		this.combatQueueMap = new ConcurrentHashMap<>();
		this.combatCommandHandler = new CombatCommandHandler(toHitDie, knockdownDie);
		this.delayBetweenCheckingCommandQueue = delayBetweenCheckingCommandQueue;
	}
	
	@Override
	public boolean initialize() {
		executor.start();
		executor.executeWithFixedRate(0, delayBetweenCheckingCommandQueue, this::executeQueuedCommands);
		return true;
	}
	
	@Override
	public boolean terminate() {
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	@Override
	public boolean start() {
		return super.start() && combatCommandHandler.start();
	}
	
	@Override
	public boolean stop() {
		return super.stop() && combatCommandHandler.stop();
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket p = gpi.getPacket();
		if (p instanceof CommandQueueEnqueue request) {
			Command command = DataLoader.Companion.commands().getCommand(request.getCommandCrc());
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
		getQueue(qci.getSource()).queueCommand(new EnqueuedCommand(qci.getSource(), qci.getCommand(), qci.getTarget(), qci.getArguments(), qci.getCounter()));
	}
	
	@IntentHandler
	private void handleExitCombatIntent(ExitCombatIntent eci) {
		if (eci.getSource() instanceof CreatureObject creatureSource) {
			combatQueueMap.remove(creatureSource);
		}
	}
	
	private void executeQueuedCommands() {
		combatQueueMap.values().forEach(CreatureCombatQueue::executeNextCommand);
	}
	
	private CreatureCombatQueue getQueue(CreatureObject creature) {
		return combatQueueMap.computeIfAbsent(creature, c -> new CreatureCombatQueue());
	}
	
	private class CreatureCombatQueue {
		
		private static final String GLOBAL_CD_NAME = "globalCD";
		private final Queue<EnqueuedCommand> commandQueue;
		private final Set<String> activeCooldownGroups;
		
		public CreatureCombatQueue() {
			this.commandQueue = new PriorityQueue<>();
			this.activeCooldownGroups = ConcurrentHashMap.newKeySet();
		}
		
		public synchronized void executeNextCommand() {
			EnqueuedCommand peek = commandQueue.peek();
			if (peek == null) {
				return;
			}
			Command rootCommand = peek.getCommand();
			
			if (isCommandOnCooldown(rootCommand))
				return;
			
			EnqueuedCommand command = commandQueue.poll();
			if (command != null)
				execute(command);
		}
		
		private boolean isCommandOnCooldown(Command rootCommand) {
			if (activeCooldownGroups.contains(rootCommand.getCooldownGroup())) {
				return true;
			} else if (activeCooldownGroups.contains(rootCommand.getCooldownGroup2())) {
				return true;
			} else {
				return activeCooldownGroups.contains(GLOBAL_CD_NAME);
			}
		}
		
		public synchronized void queueCommand(EnqueuedCommand command) {
			Command rootCommand = command.getCommand();
			
			if (rootCommand.getCooldownGroup().isBlank()) {
				execute(command);
			} else {
				StandardLog.onPlayerTrace(CommandQueueService.this, command.getSource(), "queued command %s", rootCommand.getName());
				
				if (commandQueue.size() > 0) {
					EnqueuedCommand previouslyEnqueuedCommand = commandQueue.remove();
					
					sendQueueRemove(previouslyEnqueuedCommand, new CheckCommandResult(ErrorCode.CANCELLED, 0));
				}
				
				commandQueue.offer(command);
			}
		}
		
		public synchronized void execute(EnqueuedCommand command) {
			StandardLog.onPlayerTrace(CommandQueueService.this, command.getSource(), "executed command %s", command.getCommand().getName());
			
			Command rootCommand = command.getCommand();
			
			double warmupTime = rootCommand.getWarmupTime();
			
			if (warmupTime > 0) {
				CommandTimer warmupTimer = new CommandTimer(command.getSource().getObjectId());
				warmupTimer.addFlag(CommandTimer.CommandTimerFlag.WARMUP);
				warmupTimer.setCommandNameCrc(rootCommand.getCrc());
				warmupTimer.setCooldownGroupCrc(0);
				warmupTimer.setWarmupTime((float) warmupTime);
				
				command.getSource().sendSelf(warmupTimer);
				
				executor.execute((long) (warmupTime * 1000), () -> executeCommandNow(command));
			} else {
				executeCommandNow(command);
			}
		}
		
		private void executeCommandNow(EnqueuedCommand command) {
			Command rootCommand = command.getCommand();
			CreatureObject source = command.getSource();
			CombatCommand combatCommand = DataLoader.Companion.combatCommands().getCombatCommand(rootCommand.getName(), source.getCommands());
			CheckCommandResult checkCommandResult = checkCommand(command, combatCommand);
			sendQueueRemove(command, checkCommandResult);
			ErrorCode error = checkCommandResult.getErrorCode();
			
			if (error != ErrorCode.SUCCESS) {
				sendCommandFailed(command);
				return;
			}

			if (combatCommand != null) {
				CombatStatus combatStatus = combatCommandHandler.executeCombatCommand(
						source,
						command.getTarget(),
						rootCommand,
						combatCommand,
						command.getArguments());

				CombatCommandCommon.handleStatus(source, combatCommand, combatStatus);
				if (combatStatus != CombatStatus.SUCCESS) {
					sendCommandFailed(command);
					return;
				}
			}
			
			float moddedWeaponAttackSpeedWithCap = source.getEquippedWeapon().getModdedWeaponAttackSpeedWithCap(source);
			boolean cd1 = rootCommand.getCooldownGroup().length() > 0;
			boolean cd2 = rootCommand.getCooldownGroup2().length() > 0;
			
			if (cd1) {
				startCooldownGroup(source, rootCommand, rootCommand.getCooldownGroup(), rootCommand.getCooldownTime(), command.getCounter(), moddedWeaponAttackSpeedWithCap);
				activeCooldownGroups.add(rootCommand.getCooldownGroup());
			}
			
			if (cd2) {
				startCooldownGroup(source, rootCommand, rootCommand.getCooldownGroup2(), rootCommand.getCooldownTime2(), command.getCounter(), moddedWeaponAttackSpeedWithCap);
				activeCooldownGroups.add(rootCommand.getCooldownGroup2());
			}
			
			if (cd1 || cd2) {
				activeCooldownGroups.add(GLOBAL_CD_NAME);
				executor.execute((long) (moddedWeaponAttackSpeedWithCap * 1000), () -> activeCooldownGroups.remove(GLOBAL_CD_NAME));
			}
			
			
			ExecuteCommandIntent.broadcast(source, command.getTarget(), command.getArguments(), command.getCommand());
		}
		
		private void sendQueueRemove(EnqueuedCommand command, CheckCommandResult checkCommandResult) {
			ErrorCode error = checkCommandResult.getErrorCode();
			int action = checkCommandResult.getAction();
			command.getSource().sendSelf(new CommandQueueDequeue(command.getSource().getObjectId(), command.getCounter(), (float) command.getCommand().getExecuteTime(), error, action));
		}
		
		private void sendCommandFailed(EnqueuedCommand enqueuedCommand) {
			int counter = enqueuedCommand.getCounter();
			CreatureObject source = enqueuedCommand.getSource();
			long objectId = source.getObjectId();
			Command command = enqueuedCommand.getCommand();
			
			CommandTimer commandTimer = new CommandTimer(objectId);
			commandTimer.addFlag(CommandTimer.CommandTimerFlag.FAILED);
			commandTimer.setCommandNameCrc(command.getCrc());
			commandTimer.setSequenceId(counter);
			source.sendSelf(commandTimer);
		}
		
		private void startCooldownGroup(CreatureObject creature, Command command, String group, double cooldownTime, int counter, float globalCooldownTime) {
			CommandTimer commandTimer = new CommandTimer(creature.getObjectId());
			commandTimer.setCooldownGroupCrc(CRC.getCrc(group));
			commandTimer.setGlobalCooldownTime(globalCooldownTime);
			commandTimer.setCooldownGroupTime((float) cooldownTime);
			commandTimer.setCommandNameCrc(command.getCrc());
			commandTimer.setSequenceId(counter);
			commandTimer.addFlag(CommandTimer.CommandTimerFlag.COOLDOWN);
			commandTimer.addFlag(CommandTimer.CommandTimerFlag.COOLDOWN2);
			commandTimer.addFlag(CommandTimer.CommandTimerFlag.EXECUTE);
			creature.sendSelf(commandTimer);
			
			executor.execute((long) ((cooldownTime + globalCooldownTime) * 1000), () -> activeCooldownGroups.remove(group));
		}
		
		private CheckCommandResult checkCommand(EnqueuedCommand command, CombatCommand combatCommand) {
			Command rootCommand = command.getCommand();
			CreatureObject source = command.getSource();
			Set<Locomotion> disallowedLocomotions = rootCommand.getDisallowedLocomotions();
			Set<Locomotion> sourceLocomotions = new HashSet<>();

			if (isCharacterAbilityRequirementPresent(rootCommand)) {
				if (isMissingCharacterAbility(rootCommand, source)) {
					return new CheckCommandResult(ErrorCode.ABILITY, 0);
				}
			}
			
			@NotNull Locomotion[] locomotions = Locomotion.values();
			
			for (Locomotion locomotion : locomotions) {
				boolean active = locomotion.isActive(source);
				
				if (active) {
					sourceLocomotions.add(locomotion);
				}
			}
			
			for (Locomotion disallowedLocomotion : disallowedLocomotions) {
				if (sourceLocomotions.contains(disallowedLocomotion)) {
					return new CheckCommandResult(ErrorCode.LOCOMOTION, disallowedLocomotion.getLocomotionTableId());
				}
			}
			
			Set<State> disallowedStates = rootCommand.getDisallowedStates();
			Set<State> sourceStates = new HashSet<>();
			
			@NotNull State[] states = State.values();
			
			for (State state : states) {
				boolean active = state.isActive(source);
				
				if (active) {
					sourceStates.add(state);
				}
			}
			
			for (State disallowedState : disallowedStates) {
				if (sourceStates.contains(disallowedState)) {
					return new CheckCommandResult(ErrorCode.STATE_PROHIBITED, disallowedState.getStateTableId());
				}
			}
			
			ValidWeapon validWeapon = rootCommand.getValidWeapon();
			WeaponObject equippedWeapon = source.getEquippedWeapon();
			WeaponType equippedWeaponType = equippedWeapon.getType();
			
			if (!validWeapon.isValid(equippedWeaponType)) {
				showInvalidWeaponFlyText(source);
				return new CheckCommandResult(ErrorCode.CANCELLED, 0);
			}

			if (combatCommand != null) {
				if (combatCommand.getHitType() == HitType.HEAL && combatCommand.getAttackType() == AttackType.SINGLE_TARGET) {
					SWGObject target;
					switch (rootCommand.getTargetType()) {
						case NONE:
							target = source;
							break;
						case REQUIRED:
							target  = command.getTarget();
							break;
						case OPTIONAL:
							if (command.getTarget() == null) {
								target = source;
							} else if (command.getTarget() instanceof CreatureObject) {
								target = source.isAttackable((CreatureObject) command.getTarget()) ? source : command.getTarget();
							} else {
								target = null;
							}
							break;
						default:
							target = null;
							break;
					}
					if (target instanceof CreatureObject && ((CreatureObject) target).getHealth() == ((CreatureObject) target).getMaxHealth())
						return new CheckCommandResult(ErrorCode.CANCELLED, 0);
				}
			}
			return new CheckCommandResult(ErrorCode.SUCCESS, 0);
		}

		private boolean isCharacterAbilityRequirementPresent(Command rootCommand) {
			return !rootCommand.getCharacterAbility().isBlank();
		}

		private boolean isMissingCharacterAbility(Command rootCommand, CreatureObject source) {
			Collection<String> characterAbilities = source.getCommands()
					.stream()
					.map(sourceCommand -> sourceCommand.toLowerCase(Locale.US))
					.collect(Collectors.toSet());
			String requiredCharacterAbility = rootCommand.getCharacterAbility().toLowerCase(Locale.US);
			
			if (requiredCharacterAbility.equalsIgnoreCase("admin")) {
				// To prevent admin commands only being usable when in God Mode
				Player owner = source.getOwner();

				if (owner != null) {
					return owner.getAccessLevel() == AccessLevel.PLAYER;
				}
			}
			
			return !characterAbilities.contains(requiredCharacterAbility);
		}

		private void showInvalidWeaponFlyText(CreatureObject source) {
			source.sendSelf(new ShowFlyText(source.getObjectId(), new StringId("cbt_spam", "invalid_weapon"), ShowFlyText.Scale.MEDIUM, SWGColor.Whites.INSTANCE.getWhite()));
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
