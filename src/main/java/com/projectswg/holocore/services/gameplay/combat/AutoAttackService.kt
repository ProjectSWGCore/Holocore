/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.intents.gameplay.combat.CombatCommandFailedIntent
import com.projectswg.holocore.intents.gameplay.combat.DefaultActionIntent
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import com.projectswg.holocore.utilities.launchWithFixedRate
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Repeats the default attack a player chose on their toolbar for as long as they are in combat.
 * Clearing the choice stops the attacks, as does logging out or running out of action or mind.
 *
 * The choice itself lives on the [CreatureObject], because the client only sends it when a toolbar
 * slot is ctrl-clicked or when it starts up.
 */
class AutoAttackService(private val delayBetweenAttackChecks: Long = 100) : Service() {

	/** Creatures currently auto-attacking, mapped to the time their next attack is due. */
	private val nextAttackTimes: MutableMap<CreatureObject, Long> = ConcurrentHashMap()
	private val coroutineScope = HolocoreCoroutine.childScope()

	override fun initialize(): Boolean {
		coroutineScope.launchWithFixedRate(delayBetweenAttackChecks) { attemptAttacks() }
		return true
	}

	override fun terminate(): Boolean {
		coroutineScope.cancelAndWait()
		return super.terminate()
	}

	@IntentHandler
	private fun handleDefaultActionIntent(dai: DefaultActionIntent) {
		val creature = dai.creature
		val command = dai.command

		if (command == null) {
			StandardLog.onPlayerTrace(this, creature, "cleared their default action")
			creature.defaultAttack = null
			stopAttacking(creature)
		} else {
			StandardLog.onPlayerTrace(this, creature, "set their default action to %s", command.name)
			creature.defaultAttack = command.name
			startAttacking(creature)
		}
	}

	@IntentHandler
	private fun handleCombatCommandFailedIntent(ccfi: CombatCommandFailedIntent) {
		if (ccfi.status == CombatStatus.TOO_TIRED) {
			StandardLog.onPlayerTrace(this, ccfi.source, "paused their default action")
			stopAttacking(ccfi.source)
		}
	}

	@IntentHandler
	private fun handleQueueCommandIntent(qci: QueueCommandIntent) {
		if (qci.counter != 0) {
			startAttacking(qci.source)    // a command the client sent itself means the player is attacking again
		}
	}

	@IntentHandler
	private fun handleExitCombatIntent(eci: ExitCombatIntent) {
		stopAttacking(eci.source as? CreatureObject)
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		when (pei.event) {
			PlayerEvent.PE_LOGGED_OUT, PlayerEvent.PE_DESTROYED -> stopAttacking(pei.player.creatureObject)
			else                                                -> {}
		}
	}

	private fun startAttacking(creature: CreatureObject?) {
		if (creature?.defaultAttack == null) {
			return
		}

		nextAttackTimes.putIfAbsent(creature, 0)
	}

	private fun stopAttacking(creature: CreatureObject?) {
		if (creature == null) {
			return
		}

		nextAttackTimes.remove(creature)
	}

	private fun attemptAttacks() {
		val now = System.currentTimeMillis()

		for ((creature, nextAttackTime) in nextAttackTimes) {
			if (!creature.isInCombat || now < nextAttackTime) {
				continue
			}

			val command = ServerData.commands.getCommand(creature.defaultAttack ?: continue) ?: continue
			val weapon = creature.equippedWeapon ?: continue
			val target = findTarget(creature) ?: continue

			nextAttackTimes[creature] = now + attackDelay(creature, weapon)
			QueueCommandIntent(creature, target, "", command, 0).broadcast()
		}
	}

	private fun attackDelay(creature: CreatureObject, weapon: WeaponObject): Long {
		return (weapon.getModdedWeaponAttackSpeedWithCap(creature) * 1000).toLong()
	}

	private fun findTarget(creature: CreatureObject): CreatureObject? {
		val lookAtTarget = ObjectLookup.getObjectById(creature.lookAtTargetId) as? CreatureObject ?: return null

		return if (isValidTarget(creature, lookAtTarget)) lookAtTarget else null
	}

	private fun isValidTarget(creature: CreatureObject, target: CreatureObject): Boolean {
		if (target.posture == Posture.INCAPACITATED || target.posture == Posture.DEAD) {
			return false
		}

		return target.isAttackable(creature)
	}
}
