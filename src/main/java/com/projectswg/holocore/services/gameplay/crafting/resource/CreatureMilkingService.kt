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
package com.projectswg.holocore.services.gameplay.crafting.resource

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer.getRawResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer.getSpawnedResources
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class CreatureMilkingService(private val milkingDurationMs: LongRange) : Service() {

	constructor() : this(MILKING_DURATION_MS)

	private val milkingScope = HolocoreCoroutine.childScope()
	private val creaturesBeingMilked: MutableSet<AIObject> = ConcurrentHashMap.newKeySet()

	override fun stop(): Boolean {
		milkingScope.cancelAndWait()
		return super.stop()
	}

	@IntentHandler
	private fun handleMilkCreatureIntent(intent: MilkCreatureIntent) {
		val ai = intent.target
		val player = intent.player
		val creature = player.creatureObject

		val npcInfo = ServerData.npcs[ai.creatureId ?: return] ?: return
		val milkResourceInfo = npcInfo.milkResourceInfo
		if (milkResourceInfo.amount <= 0) return
		if (ai.ownerId > 0) return	// Tamed creatures can not be milked

		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) {
			SystemMessageIntent.broadcastPersonal(player, "@skl_use:skl_use")
			return
		}
		if (ai.isMilked) {
			SystemMessageIntent.broadcastPersonal(player, "@skl_use:milk_cant")
			return
		}
		if (!isAbleToContinueMilking(player, ai)) {
			SystemMessageIntent.broadcastPersonal(player, "@skl_use:milk_too_far")
			return
		}
		if (!creaturesBeingMilked.add(ai)) {
			SystemMessageIntent.broadcastPersonal(player, "@skl_use:being_milked")
			return
		}

		val spawnedResource = getSpawnedResource(player, milkResourceInfo.type)
		if (spawnedResource == null) {
			creaturesBeingMilked.remove(ai)
			StandardLog.onPlayerError(this, player, "unable to find a creature resource of type %s for %s", milkResourceInfo.type, ai)
			return
		}

		val amount = milkResourceInfo.amount
		milkingScope.launch {
			try {
				milk(player, ai, spawnedResource, amount)
			} finally {
				creaturesBeingMilked.remove(ai)
			}
		}
	}

	private suspend fun milk(player: Player, ai: AIObject, resource: GalacticResource, amount: Int) {
		val tickDelay = ThreadLocalRandom.current().nextLong(milkingDurationMs.first, milkingDurationMs.last + 1) / TICKS
		SystemMessageIntent.broadcastPersonal(player, "@skl_use:milk_begin")
		StandardLog.onPlayerTrace(this, player, "started milking %d %s from %s with a tick delay of %d ms", amount, resource.name, ai, tickDelay)

		for (tick in 1..TICKS) {
			delay(tickDelay)

			if (!isAbleToContinueMilking(player, ai)) {
				SystemMessageIntent.broadcastPersonal(player, "@skl_use:milk_too_far")
				StandardLog.onPlayerTrace(this, player, "aborted milking %s on tick %d of %d", ai, tick, TICKS)
				return
			}

			if (tick < TICKS) {
				SystemMessageIntent.broadcastPersonal(player, "@skl_use:milk_continue")
			}
		}

		val service = this
		val eventHandler = object : ResourceContainerEventHandler {
			override fun onUnknownError() {

			}

			override fun onInventoryFull() {
				SystemMessageIntent.broadcastPersonal(player, "@container_error_message:container03")
			}

			override fun onSuccess() {
				ai.isMilked = true
				StandardLog.onPlayerEvent(service, player, "milked %d %s from %s", amount, resource.name, ai)
				SystemMessageIntent.broadcastPersonal(player, "@skl_use:milk_success")
			}
		}
		ResourceContainerHelper.giveResourcesToPlayer(amount, resource, player, eventHandler)
	}

	private fun isAbleToContinueMilking(player: Player, ai: AIObject): Boolean {
		val creature = player.creatureObject

		if (ai.posture == Posture.DEAD || creature.posture == Posture.DEAD || creature.posture == Posture.INCAPACITATED) return false
		if (creature.isInCombat || ai.isInCombat) return false
		if (creature.terrain != ai.terrain) return false

		return creature.worldLocation.distanceTo(ai.worldLocation) <= MILKING_RANGE
	}

	private fun getSpawnedResource(player: Player, requestedCreatureResourceType: String): GalacticResource? {
		val spawnedResources = getSpawnedResources(player.creatureObject.terrain)
		for (spawnedResource in spawnedResources) {
			val rawResource = getRawResource(spawnedResource.rawResourceId) ?: continue

			if (rawResource.name.startsWith("${requestedCreatureResourceType}_")) {
				return spawnedResource
			}
		}

		return null
	}

	companion object {
		private const val MILKING_RANGE = 5.0
		private const val TICKS = 3
		private val MILKING_DURATION_MS = 20_000L..30_000L
	}
}
