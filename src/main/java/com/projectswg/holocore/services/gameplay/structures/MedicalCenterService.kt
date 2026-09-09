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
package com.projectswg.holocore.services.gameplay.structures

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.random.Random

class MedicalCenterService : Service() {

	private val coroutineScope = HolocoreCoroutine.childScope()
	private val healingJobs = ConcurrentHashMap<CreatureObject, Job>()

	override fun stop(): Boolean {
		coroutineScope.cancelAndWait()
		return super.stop()
	}

	@IntentHandler
	private fun handlePlayerTransformedIntent(intent: PlayerTransformedIntent) {
		if (intent.oldParent === intent.newParent) return
		val oldMedicalCenter = medicalCenter(intent.oldParent)
		val newMedicalCenter = medicalCenter(intent.newParent)
		if (oldMedicalCenter === newMedicalCenter) return

		val creature = intent.player
		if (oldMedicalCenter != null) stopHealing(creature)
		if (newMedicalCenter != null) startHealing(creature, newMedicalCenter)
	}

	@IntentHandler
	private fun handlePlayerEventIntent(intent: PlayerEventIntent) {
		val creature = intent.player.creatureObject ?: return
		when (intent.event) {
			PlayerEvent.PE_FIRST_ZONE -> medicalCenter(creature)?.let { startHealing(creature, it) }
			PlayerEvent.PE_LOGGED_OUT -> stopHealing(creature)
			else                      -> {}
		}
	}

	@IntentHandler
	private fun handleDestroyObjectIntent(intent: DestroyObjectIntent) {
		val creature = intent.obj as? CreatureObject ?: return
		stopHealing(creature)
	}

	private fun startHealing(creature: CreatureObject, medicalCenter: BuildingObject) {
		val job = coroutineScope.launch {
			while (isActive) {
				delay(Random.nextLong(PULSE_MIN_MS, PULSE_MAX_MS))
				pulse(creature)
			}
		}
		healingJobs.put(creature, job)?.cancel()
		StandardLog.onPlayerTrace(this, creature, "entered medical center %s", medicalCenter)
	}

	private fun stopHealing(creature: CreatureObject) {
		val job = healingJobs.remove(creature) ?: return
		job.cancel()
		StandardLog.onPlayerTrace(this, creature, "no longer healing in a medical center")
	}

	private fun pulse(creature: CreatureObject) {
		val medicalCenter = medicalCenter(creature)
		if (medicalCenter == null) {
			stopHealing(creature)
			return
		}
		if (creature.posture == Posture.DEAD || creature.posture == Posture.INCAPACITATED) return

		val wounds = creature.healthWounds
		if (wounds <= 0) {
			StandardLog.onPlayerTrace(this, creature, "medical center pulse in %s found no wounds to heal", medicalCenter)
			return
		}

		val healed = min(WOUND_HEAL, wounds)
		creature.healthWounds = wounds - healed
		StandardLog.onPlayerEvent(this, creature, "medical center pulse in %s healed %d health wounds, %d remaining", medicalCenter, healed, creature.healthWounds)
	}

	private fun medicalCenter(obj: SWGObject?): BuildingObject? {
		val building = obj?.superParent as? BuildingObject ?: return null
		if (building.template !in MEDICAL_CENTER_TEMPLATES) return null
		return building
	}

	private companion object {
		private const val WOUND_HEAL = 10
		private const val PULSE_MIN_MS = 200_000L
		private const val PULSE_MAX_MS = 400_000L

		private val MEDICAL_CENTER_TEMPLATES = setOf(
			"object/building/corellia/shared_hospital_corellia.iff",
			"object/building/corellia/shared_hospital_corellia_s02.iff",
			"object/building/general/shared_mun_all_hospital_s01.iff",
			"object/building/general/shared_mun_all_hospital_s02.iff",
			"object/building/naboo/shared_hospital_naboo.iff",
			"object/building/naboo/shared_hospital_naboo_s02.iff",
			"object/building/tatooine/shared_hospital_tatooine.iff",
			"object/building/tatooine/shared_hospital_tatooine_s02.iff",
		)
	}
}
