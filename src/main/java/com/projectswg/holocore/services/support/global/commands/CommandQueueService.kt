/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.services.support.global.commands

import com.projectswg.common.data.CRC
import com.projectswg.common.data.combat.AttackType
import com.projectswg.common.data.combat.HitType
import com.projectswg.common.data.combat.TargetType
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.object_controller.*
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus
import com.projectswg.holocore.resources.support.color.SWGColor.Whites.white
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.combatCommands
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.commands
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.global.commands.Command
import com.projectswg.holocore.resources.support.global.commands.Locomotion
import com.projectswg.holocore.resources.support.global.commands.State
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.random.Die
import com.projectswg.holocore.resources.support.random.RandomDie
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.handleStatus
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandHandler
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import com.projectswg.holocore.utilities.launchAfter
import com.projectswg.holocore.utilities.launchWithFixedRate
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.stream.Collectors

class CommandQueueService @JvmOverloads constructor(private val delayBetweenCheckingCommandQueue: Long = 100, toHitDie: Die = RandomDie(), knockdownDie: Die = RandomDie(), woundDie: Die = RandomDie(), private val skipWarmup: Boolean = false) : Service() {
	private val combatQueueMap: MutableMap<CreatureObject, CreatureCombatQueue> = ConcurrentHashMap()
	private val combatCommandHandler: CombatCommandHandler = CombatCommandHandler(toHitDie, knockdownDie, woundDie)
	private val coroutineScope = HolocoreCoroutine.childScope()

	override fun initialize(): Boolean {
		coroutineScope.launchWithFixedRate(delayBetweenCheckingCommandQueue) { executeQueuedCommands() }
		return true
	}

	override fun terminate(): Boolean {
		coroutineScope.cancelAndWait()
		return super.terminate()
	}

	override fun start(): Boolean {
		return super.start() && combatCommandHandler.start()
	}

	override fun stop(): Boolean {
		return super.stop() && combatCommandHandler.stop()
	}

	@IntentHandler
	private fun handleInboundPacketIntent(gpi: InboundPacketIntent) {
		val p = gpi.packet
		if (p is CommandQueueEnqueue) {
			val command = commands().getCommand(p.commandCrc)
			if (command == null) {
				if (p.commandCrc != 0) Log.e("Invalid command crc: %x [%s]", p.commandCrc, CRC.getString(p.commandCrc))
				return
			}
			val targetId: Long = p.targetId
			val target = if (targetId != 0L) ObjectLookup.getObjectById(targetId) else null
			QueueCommandIntent(gpi.player.creatureObject, target, p.arguments, command, p.counter).broadcast()
		} else if (p is IntendedTarget) {
			if (p.targetId == 0L) combatQueueMap.remove(gpi.player.creatureObject)
		}
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		val creature = pei.player.creatureObject
		if (pei.event == PlayerEvent.PE_LOGGED_OUT) {
			// No reason to keep their combat queue in the map if they log out
			// This also prevents queued commands from executing after the player logs out
			if (creature != null) combatQueueMap.remove(creature)
		}
	}

	@IntentHandler
	private fun handleQueueCommandIntent(qci: QueueCommandIntent) {
		getQueue(qci.source).queueCommand(EnqueuedCommand(qci.source, qci.command, qci.target, qci.arguments, qci.counter))
	}

	@IntentHandler
	private fun handleExitCombatIntent(eci: ExitCombatIntent) {
		if (eci.source is CreatureObject) {
			combatQueueMap.remove(eci.source)
		}
	}

	private fun executeQueuedCommands() {
		combatQueueMap.values.forEach(Consumer { obj: CreatureCombatQueue -> obj.executeNextCommand() })
	}

	private fun getQueue(creature: CreatureObject): CreatureCombatQueue {
		return combatQueueMap.computeIfAbsent(creature) { _: CreatureObject -> CreatureCombatQueue() }
	}

	private inner class CreatureCombatQueue {
		private val commandQueue: Queue<EnqueuedCommand> = PriorityQueue()
		private val activeCooldownGroups: MutableSet<String> = ConcurrentHashMap.newKeySet()

		@Synchronized
		fun executeNextCommand() {
			val peek = commandQueue.peek() ?: return
			val rootCommand: Command = peek.command

			if (isCommandOnCooldown(rootCommand)) return

			val command = commandQueue.poll()
			if (command != null) execute(command)
		}

		private fun isCommandOnCooldown(rootCommand: Command): Boolean {
			return if (activeCooldownGroups.contains(rootCommand.cooldownGroup)) {
				true
			} else if (activeCooldownGroups.contains(rootCommand.cooldownGroup2)) {
				true
			} else {
				activeCooldownGroups.contains(Companion.GLOBAL_CD_NAME)
			}
		}

		@Synchronized
		fun queueCommand(command: EnqueuedCommand) {
			val rootCommand: Command = command.command

			if (rootCommand.cooldownGroup.isBlank()) {
				execute(command)
			} else {
				StandardLog.onPlayerTrace(this@CommandQueueService, command.source, "queued command %s", rootCommand.name)

				if (commandQueue.size > 0) {
					val previouslyEnqueuedCommand = commandQueue.remove()

					sendQueueRemove(previouslyEnqueuedCommand, CheckCommandResult(CommandQueueDequeue.ErrorCode.CANCELLED, 0))
				}

				commandQueue.offer(command)
			}
		}

		@Synchronized
		fun execute(command: EnqueuedCommand) {
			StandardLog.onPlayerTrace(this@CommandQueueService, command.source, "executed command %s", command.command.name)

			val rootCommand: Command = command.command

			val warmupTime = rootCommand.warmupTime

			if (warmupTime > 0 && !skipWarmup) {
				val warmupTimer = CommandTimer(command.source.objectId)
				warmupTimer.addFlag(CommandTimer.CommandTimerFlag.WARMUP)
				warmupTimer.commandNameCrc = rootCommand.crc
				warmupTimer.cooldownGroupCrc = 0
				warmupTimer.warmupTime = warmupTime.toFloat()

				command.source.sendSelf(warmupTimer)

				coroutineScope.launchAfter((warmupTime * 1000).toLong()) {
					executeCommandNow(command)
				}
			} else {
				executeCommandNow(command)
			}
		}

		private fun executeCommandNow(command: EnqueuedCommand) {
			val rootCommand: Command = command.command
			val source: CreatureObject = command.source
			val combatCommand = combatCommands().getCombatCommand(rootCommand.name, source.commands)
			val checkCommandResult = checkCommand(command, combatCommand)
			sendQueueRemove(command, checkCommandResult)
			val error = checkCommandResult.errorCode

			if (error != CommandQueueDequeue.ErrorCode.SUCCESS) {
				sendCommandFailed(command)
				return
			}

			if (combatCommand != null) {
				val combatStatus = combatCommandHandler.executeCombatCommand(
					source, command.target, rootCommand, combatCommand, command.arguments
				)

				handleStatus(source, combatCommand, combatStatus)
				if (combatStatus != CombatStatus.SUCCESS) {
					sendCommandFailed(command)
					return
				}
			}

			val moddedWeaponAttackSpeedWithCap = source.equippedWeapon.getModdedWeaponAttackSpeedWithCap(source)
			val cd1 = rootCommand.cooldownGroup.isNotEmpty()
			val cd2 = rootCommand.cooldownGroup2.isNotEmpty()

			if (cd1) {
				startCooldownGroup(source, rootCommand, rootCommand.cooldownGroup, rootCommand.cooldownTime, command.counter, moddedWeaponAttackSpeedWithCap)
				activeCooldownGroups.add(rootCommand.cooldownGroup)
			}

			if (cd2) {
				startCooldownGroup(source, rootCommand, rootCommand.cooldownGroup2, rootCommand.cooldownTime2, command.counter, moddedWeaponAttackSpeedWithCap)
				activeCooldownGroups.add(rootCommand.cooldownGroup2)
			}

			if (cd1 || cd2) {
				activeCooldownGroups.add(Companion.GLOBAL_CD_NAME)
				coroutineScope.launchAfter((moddedWeaponAttackSpeedWithCap * 1000).toLong()) {
					activeCooldownGroups.remove(Companion.GLOBAL_CD_NAME)
				}
			}

			ExecuteCommandIntent(source, command.target, command.arguments, command.command).broadcast()
		}

		private fun sendQueueRemove(command: EnqueuedCommand, checkCommandResult: CheckCommandResult) {
			val error = checkCommandResult.errorCode
			val action = checkCommandResult.action
			command.source.sendSelf(CommandQueueDequeue(command.source.objectId, command.counter, command.command.executeTime.toFloat(), error, action))
		}

		private fun sendCommandFailed(enqueuedCommand: EnqueuedCommand) {
			val counter: Int = enqueuedCommand.counter
			val source: CreatureObject = enqueuedCommand.source
			val objectId = source.objectId
			val command: Command = enqueuedCommand.command

			val commandTimer = CommandTimer(objectId)
			commandTimer.addFlag(CommandTimer.CommandTimerFlag.FAILED)
			commandTimer.commandNameCrc = command.crc
			commandTimer.sequenceId = counter
			source.sendSelf(commandTimer)
		}

		private fun startCooldownGroup(creature: CreatureObject, command: Command, group: String, cooldownTime: Double, counter: Int, globalCooldownTime: Float) {
			val commandTimer = CommandTimer(creature.objectId)
			commandTimer.cooldownGroupCrc = CRC.getCrc(group)
			commandTimer.globalCooldownTime = globalCooldownTime
			commandTimer.setCooldownGroupTime(cooldownTime.toFloat())
			commandTimer.commandNameCrc = command.crc
			commandTimer.sequenceId = counter
			commandTimer.addFlag(CommandTimer.CommandTimerFlag.COOLDOWN)
			commandTimer.addFlag(CommandTimer.CommandTimerFlag.COOLDOWN2)
			commandTimer.addFlag(CommandTimer.CommandTimerFlag.EXECUTE)
			creature.sendSelf(commandTimer)

			coroutineScope.launchAfter(((cooldownTime + globalCooldownTime) * 1000).toLong()) {
				activeCooldownGroups.remove(group)
			}
		}

		private fun checkCommand(command: EnqueuedCommand, combatCommand: CombatCommand?): CheckCommandResult {
			val rootCommand: Command = command.command
			val source: CreatureObject = command.source
			val disallowedLocomotions = rootCommand.getDisallowedLocomotions()
			val sourceLocomotions: MutableSet<Locomotion> = HashSet()

			if (isCharacterAbilityRequirementPresent(rootCommand)) {
				if (isMissingCharacterAbility(rootCommand, source)) {
					return CheckCommandResult(CommandQueueDequeue.ErrorCode.ABILITY, 0)
				}
			}

			val locomotions = Locomotion.entries.toTypedArray()

			for (locomotion in locomotions) {
				val active = locomotion.isActive(source)

				if (active) {
					sourceLocomotions.add(locomotion)
				}
			}

			for (disallowedLocomotion in disallowedLocomotions) {
				if (sourceLocomotions.contains(disallowedLocomotion)) {
					return CheckCommandResult(CommandQueueDequeue.ErrorCode.LOCOMOTION, disallowedLocomotion.locomotionTableId)
				}
			}

			val disallowedStates = rootCommand.getDisallowedStates()
			val sourceStates: MutableSet<State> = HashSet()

			val states = State.entries.toTypedArray()

			for (state in states) {
				val active = state.isActive(source)

				if (active) {
					sourceStates.add(state)
				}
			}

			for (disallowedState in disallowedStates) {
				if (sourceStates.contains(disallowedState)) {
					return CheckCommandResult(CommandQueueDequeue.ErrorCode.STATE_PROHIBITED, disallowedState.stateTableId)
				}
			}

			val validWeapon = rootCommand.validWeapon
			val equippedWeapon = source.equippedWeapon
			val equippedWeaponType = equippedWeapon.type

			if (!validWeapon.isValid(equippedWeaponType)) {
				showInvalidWeaponFlyText(source)
				return CheckCommandResult(CommandQueueDequeue.ErrorCode.CANCELLED, 0)
			}

			if (combatCommand != null) {
				if (combatCommand.hitType == HitType.HEAL && combatCommand.attackType == AttackType.SINGLE_TARGET) {
					val target = when (rootCommand.targetType) {
						TargetType.NONE     -> source
						TargetType.REQUIRED -> command.target
						TargetType.OPTIONAL -> {
							if (command.target == null) {
								source
							} else if (command.target is CreatureObject) {
								if (source.isAttackable(command.target)) source else command.target
							} else {
								null
							}
						}
						else                -> null
					}
					if (target is CreatureObject && target.health == target.maxHealth)
						return CheckCommandResult(CommandQueueDequeue.ErrorCode.CANCELLED, 0)
				}
			}
			return CheckCommandResult(CommandQueueDequeue.ErrorCode.SUCCESS, 0)
		}

		private fun isCharacterAbilityRequirementPresent(rootCommand: Command): Boolean {
			return rootCommand.characterAbility.isNotBlank()
		}

		private fun isMissingCharacterAbility(rootCommand: Command, source: CreatureObject): Boolean {
			val characterAbilities: Collection<String> = source.commands.stream().map { sourceCommand: String -> sourceCommand.lowercase() }.collect(Collectors.toSet())
			val requiredCharacterAbility = rootCommand.characterAbility.lowercase()

			if (requiredCharacterAbility.equals("admin", ignoreCase = true)) {
				// To prevent admin commands only being usable when in God Mode
				val owner = source.owner

				if (owner != null) {
					return owner.accessLevel == AccessLevel.PLAYER
				}
			}

			return !characterAbilities.contains(requiredCharacterAbility)
		}

		private fun showInvalidWeaponFlyText(source: CreatureObject) {
			source.sendSelf(ShowFlyText(source.objectId, StringId("cbt_spam", "invalid_weapon"), ShowFlyText.Scale.MEDIUM, white))
		}
	}

	private class EnqueuedCommand(val source: CreatureObject, val command: Command, val target: SWGObject?, val arguments: String, val counter: Int) : Comparable<EnqueuedCommand> {
		override fun compareTo(other: EnqueuedCommand): Int {
			return command.defaultPriority.compareTo(other.command.defaultPriority)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other == null || javaClass != other.javaClass) return false
			val that = other as EnqueuedCommand
			return command == that.command
		}

		override fun hashCode(): Int {
			return command.hashCode()
		}
	}

	companion object {
		private const val GLOBAL_CD_NAME = "globalCD"
	}
}
