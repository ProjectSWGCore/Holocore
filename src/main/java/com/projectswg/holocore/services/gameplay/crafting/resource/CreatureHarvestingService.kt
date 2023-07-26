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
package com.projectswg.holocore.services.gameplay.crafting.resource

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer.getRawResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer.getSpawnedResources
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcLoader.NpcResourceInfo
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import kotlin.math.ceil

class CreatureHarvestingService : Service() {

	private val creatureQualities = listOf(
		CreatureQuality(StringId("skl_use", "creature_quality_fat"), 1.0),
		CreatureQuality(StringId("skl_use", "creature_quality_medium"), 0.8),
		CreatureQuality(StringId("skl_use", "creature_quality_scrawny"), 0.5),
		CreatureQuality(StringId("skl_use", "creature_quality_skinny"), 0.3),
	)

	@IntentHandler
	private fun handleHarvestHideIntent(intent: HarvestHideIntent) {
		val (player, ai) = intent

		val npcInfo = ServerData.npcs.getNpc(ai.creatureId)
		grantResources(npcInfo.hideResourceInfo, player, ai)
	}

	@IntentHandler
	private fun handleHarvestMeatIntent(intent: HarvestMeatIntent) {
		val (player, ai) = intent

		val npcInfo = ServerData.npcs.getNpc(ai.creatureId)
		grantResources(npcInfo.meatResourceInfo, player, ai)
	}

	@IntentHandler
	private fun handleHarvestBoneIntent(intent: HarvestBoneIntent) {
		val (player, ai) = intent

		val npcInfo = ServerData.npcs.getNpc(ai.creatureId)
		grantResources(npcInfo.boneResourceInfo, player, ai)
	}

	private fun grantResources(resourceInfo: NpcResourceInfo, player: Player, ai: AIObject) {
		if (ai.isHarvested) {
			SystemMessageIntent.broadcastPersonal(player, "@skl_use:nothing_to_harvest")
			return
		}
		val requestedCreatureResourceType = resourceInfo.type

		val rawResourceType = RawResourceType.getByName(requestedCreatureResourceType)
		if (rawResourceType == null) {
			StandardLog.onPlayerError(this, player, "unknown resource type %s", requestedCreatureResourceType)
			return
		}

		val spawnedResource = getSpawnedResource(player, rawResourceType)

		if (spawnedResource == null) {
			StandardLog.onPlayerError(this, player, "unable to find a creature resource of type %s for %s", rawResourceType, ai)
			return
		}

		val creatureQuality = creatureQualities.random()
		val amount = randomizeResourceAmount(resourceInfo, creatureQuality, player)
		val service = this
		val eventHandler = object : ResourceContainerEventHandler {
			override fun onUnknownError() {
				// TODO
			}

			override fun onInventoryFull() {
				SystemMessageIntent.broadcastPersonal(player, "@container_error_message:container03")
			}

			override fun onSuccess() {
				StandardLog.onPlayerEvent(service, player, "received %d %s from %s", amount, requestedCreatureResourceType, ai)
				SystemMessageIntent.broadcastPersonal(player, ProsePackage(creatureQuality.stringId, "DI", amount, "TU", spawnedResource.name))
				ai.isHarvested = true
			}
		}
		ResourceContainerHelper.giveResourcesToPlayer(amount, spawnedResource, player, eventHandler)
	}

	private fun randomizeResourceAmount(resourceInfo: NpcResourceInfo, creatureQuality: CreatureQuality, player: Player): Int {
		val baseNpcResourceAmount = ceil(resourceInfo.amount * creatureQuality.multiplier).toInt()
		val creatureHarvestingSkillModBonus = ceil(baseNpcResourceAmount * creatureHarvestingMultiplier(player)).toInt()
		return baseNpcResourceAmount + creatureHarvestingSkillModBonus
	}

	private fun creatureHarvestingMultiplier(player: Player) = player.creatureObject.getSkillModValue("creature_harvesting") / 100.0

	private fun getSpawnedResource(player: Player, rawResourceType: RawResourceType): GalacticResource? {
		val spawnedResources = getSpawnedResources(player.creatureObject.terrain)
		for (spawnedResource in spawnedResources) {
			val rawResource = getRawResource(spawnedResource.rawResourceId) ?: continue

			if (rawResource.name.startsWith(rawResourceType.resourceName + "_")) {
				return spawnedResource
			}
		}

		return null
	}

}