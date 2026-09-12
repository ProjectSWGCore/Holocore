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
package com.projectswg.holocore.services.gameplay.entertainment

import com.projectswg.common.network.packets.swg.zone.PlayClientEffectLocMessage
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.holocore.intents.gameplay.entertainment.PerformEffectIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.gameplay.entertainment.PerformEffect
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.PerformEffectLoader.PerformEffectInfo
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import com.projectswg.holocore.utilities.launchAfter
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class PerformanceEffectService : Service() {
	private val coroutineScope = HolocoreCoroutine.childScope()
	private val lockedOutPerformers = mutableSetOf<Long>()

	override fun stop(): Boolean {
		coroutineScope.cancelAndWait()
		return super.stop()
	}

	@IntentHandler
	private fun handlePerformEffectIntent(pei: PerformEffectIntent) {
		val player = pei.performer
		val performer = player.creatureObject
		val effect = pei.effect

		if (!performer.isPerforming) {
			SystemMessageIntent(player, "@performance:effect_not_performing").broadcast()
			return
		}

		if (isLockedOut(performer)) {
			SystemMessageIntent(player, "@performance:effect_wait_self").broadcast()
			return
		}

		val requestedLevel = pei.level ?: MAX_LEVEL    // The client asks for the highest level when none was given
		val level = highestAllowedLevel(performer, effect, requestedLevel)
		if (effect.levelled && pei.level != null && level < pei.level) {
			SystemMessageIntent(player, "@performance:effect_level_too_high").broadcast()
		}

		val datatableName = effect.datatableName(level)
		val info = ServerData.performEffects.getEffect(datatableName)
		if (info == null) {
			StandardLog.onPlayerError(this, performer, "tried to perform unknown effect %s", datatableName)
			return
		}

		val actionCost = actionCost(info)
		if (performer.action <= actionCost) {
			SystemMessageIntent(player, "@performance:effect_too_tired").broadcast()
			return
		}

		val target = if (info.targetType == TARGET_TYPE_TARGET) {
			val target = resolveTarget(performer)
			if (target == null) {
				SystemMessageIntent(player, "@performance:effect_need_target").broadcast()
				return
			}
			target
		} else null

		performer.modifyAction(-actionCost)
		lockOut(performer, (info.effectDuration * 1000).toLong())
		playEffect(performer, effect.effectFile(level), info, target)
		SystemMessageIntent(player, effect.systemMessage).broadcast()
	}

	private fun isLockedOut(performer: CreatureObject): Boolean {
		synchronized(lockedOutPerformers) {
			return lockedOutPerformers.contains(performer.objectId)
		}
	}

	private fun lockOut(performer: CreatureObject, durationMillis: Long) {
		synchronized(lockedOutPerformers) {
			lockedOutPerformers.add(performer.objectId)
		}

		coroutineScope.launchAfter(durationMillis) {
			synchronized(lockedOutPerformers) {
				lockedOutPerformers.remove(performer.objectId)
			}
		}
	}

	private fun playEffect(performer: CreatureObject, effectFile: String, info: PerformEffectInfo, target: CreatureObject?) {
		when (info.targetType) {
			TARGET_TYPE_LOCATION -> {
				// Plays where the performer stood, so moving does not carry the effect along
				val location = performer.location
				performer.sendObservers(PlayClientEffectLocMessage(effectFile, location.terrain, location.position, performer.parent?.objectId ?: 0, 0f, ""))
			}
			TARGET_TYPE_TARGET   -> target?.sendObservers(PlayClientEffectObjectMessage(effectFile, "", target.objectId, ""))
			else                 -> performer.sendObservers(PlayClientEffectObjectMessage(effectFile, "", performer.objectId, ""))
		}
	}

	/**
	 * The action pool is a flat 100 here, so the raw costs of the datatable are scaled down to a percentage of it, like combat commands are.
	 */
	private fun actionCost(info: PerformEffectInfo): Int = info.effectActionCost / ACTION_COST_DIVISOR

	private fun resolveTarget(performer: CreatureObject): CreatureObject? {
		val lookAtTargetId = performer.lookAtTargetId
		val target = if (lookAtTargetId != 0L) ObjectLookup.getObjectById(lookAtTargetId) else null

		return if (target is CreatureObject && target.isPlayer) target else null
	}

	/**
	 * Asking for a level above what Dance Knowledge or Music Knowledge allows drops to the highest one allowed.
	 */
	private fun highestAllowedLevel(performer: CreatureObject, effect: PerformEffect, requestedLevel: Int): Int {
		if (!effect.levelled) return 1

		val skillModValue = performer.getSkillModValue(skillModName(performer))

		for (level in requestedLevel downTo 1) {
			val info = ServerData.performEffects.getEffect(effect.datatableName(level)) ?: continue
			if (info.requiredSkillModValue <= skillModValue) return level
		}

		return 1    // Nothing sends effect_lack_skill_self, so the lowest level plays instead
	}

	private fun skillModName(performer: CreatureObject): String {
		// A performance ID of 0 is a dance, anything else is music
		return if (performer.performanceId == 0) "healing_dance_ability" else "healing_music_ability"
	}

	companion object {
		private const val TARGET_TYPE_LOCATION = 1
		private const val TARGET_TYPE_TARGET = 2
		private const val ACTION_COST_DIVISOR = 10
		private const val MAX_LEVEL = 3
	}
}
