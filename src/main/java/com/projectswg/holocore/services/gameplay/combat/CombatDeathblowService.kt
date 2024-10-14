/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.intents.gameplay.combat.*
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

class CombatDeathblowService : Service() {
	private val incapacitatedCreatures = ConcurrentHashMap<CreatureObject, Future<*>?>()
	private val executor = ScheduledThreadPool(1, 3, "combat-deathblow-service")

	override fun start(): Boolean {
		executor.start()
		return true
	}

	override fun stop(): Boolean {
		executor.stop()
		executor.awaitTermination(1000)
		return true
	}

	@IntentHandler
	private fun handleDeathblowIntent(di: DeathblowIntent) {
		val killer = di.killer
		val corpse = di.corpse

		if (!corpse.isPlayer) {
			return
		}

		if (!corpse.isAttackable(killer)) {
			return
		}

		if (corpse.posture != Posture.INCAPACITATED) {
			return
		}

		// If they're deathblown while incapacitated, their incapacitation expiration timer should cancel
		val incapacitationTimer = incapacitatedCreatures.remove(corpse)

		if (incapacitationTimer != null) {
			if (incapacitationTimer.cancel(true)) {    // Interrupt the incap timer and kill the creature immediately
				killCreature(killer, corpse)
			}
		} else {
			StandardLog.onPlayerError(this, corpse, "Incapacitation timer for player %s being deathblown unexpectedly didn't exist")
		}
	}

	@IntentHandler
	private fun handleIncapacitateCreatureIntent(ici: IncapacitateCreatureIntent) {
		incapacitatePlayer(ici.incapper, ici.incappee)
	}

	@IntentHandler
	private fun handleKillCreatureIntent(kci: KillCreatureIntent) {
		killCreature(kci.killer, kci.corpse)
	}

	@IntentHandler
	@Synchronized
	private fun handleRequestCreatureDeathIntent(rcdi: RequestCreatureDeathIntent) {
		val corpse = rcdi.corpse
		val killer = rcdi.killer
		if (corpse.posture == Posture.INCAPACITATED || corpse.posture == Posture.DEAD) return

		if (shouldDeathblow(killer, corpse)) {
			killCreature(rcdi.killer, corpse)
		} else {
			incapacitatePlayer(rcdi.killer, corpse)
		}

		corpse.health = 0
		corpse.setTurnScale(0.0)
		corpse.setMovementPercent(0.0)

		ExitCombatIntent(corpse).broadcast()
	}

	private fun shouldDeathblow(killer: CreatureObject, corpse: CreatureObject): Boolean {
		if (corpse !is AIObject && killer is AIObject) {	// If PvE
			return killer.spawner?.isDeathblow ?: false // Defaults to no-deathblow
		}
		return corpse is AIObject
	}

	private fun incapacitatePlayer(incapacitator: CreatureObject, incapacitated: CreatureObject) {
		incapacitated.counter = INCAP_TIMER.toSeconds().toInt()
		incapacitated.posture = Posture.INCAPACITATED

		StandardLog.onPlayerEvent(this, incapacitated, "was incapacitated by %s", incapacitator)

		// Once the incapacitation counter expires, revive them.
		incapacitatedCreatures[incapacitated] = executor.execute((INCAP_TIMER.toMillis())) { expireIncapacitation(incapacitated) }

		val incapacitatorOwner = incapacitator.owner
		if (incapacitatorOwner != null) { // This will be NPCs most of the time
			SystemMessageIntent(incapacitatorOwner, ProsePackage(StringId("base_player", "prose_target_incap"), "TT", incapacitated.objectName)).broadcast()
		}
		val incapacitatedOwner = incapacitated.owner
		if (incapacitatedOwner != null) { // Logged out player
			SystemMessageIntent(incapacitatedOwner, ProsePackage(StringId("base_player", "prose_victim_incap"), "TT", incapacitator.objectName)).broadcast()
		}
		CreatureIncapacitatedIntent(incapacitator, incapacitated).broadcast()

		val now = System.currentTimeMillis()
		incapacitated.lastIncapTime = now
	}

	private fun expireIncapacitation(incapacitatedPlayer: CreatureObject) {
		incapacitatedCreatures.remove(incapacitatedPlayer)
		reviveCreature(incapacitatedPlayer)
	}

	private fun reviveCreature(revivedCreature: CreatureObject) {
		if (revivedCreature.isPlayer) revivedCreature.counter = 0

		revivedCreature.posture = Posture.UPRIGHT

		// The creature is now able to turn around and move
		revivedCreature.setTurnScale(1.0)
		revivedCreature.setMovementPercent(1.0)

		// Give 'em a percentage of their health and schedule them for HAM regeneration.
		revivedCreature.health = (revivedCreature.baseHealth * 0.1).toInt() // Restores 10% health of their base health
		CreatureRevivedIntent(revivedCreature).broadcast()

		StandardLog.onPlayerEvent(this, revivedCreature, "was revived")
	}

	private fun killCreature(killer: CreatureObject, corpse: CreatureObject) {
		// We don't want to kill a creature that is already dead
		if (corpse.posture == Posture.DEAD) return

		corpse.posture = Posture.DEAD
		if (corpse.isPlayer) StandardLog.onPlayerEvent(this, corpse, "was killed by %s", killer)
		if (killer.isPlayer) StandardLog.onPlayerEvent(this, killer, "killed %s", corpse)
		CreatureKilledIntent(killer, corpse).broadcast()
	}

	companion object {
		private val INCAP_TIMER = Duration.ofSeconds(20)
	}
}
