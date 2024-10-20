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
package com.projectswg.holocore.services.gameplay.crafting.resource

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
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
		val ai = intent.target
		val player = intent.player

		val npcInfo = ServerData.npcs[ai.creatureId ?: return]
		grantResources(npcInfo?.hideResourceInfo ?: return, player, ai)
	}

	@IntentHandler
	private fun handleHarvestMeatIntent(intent: HarvestMeatIntent) {
		val ai = intent.target
		val player = intent.player

		val npcInfo = ServerData.npcs[ai.creatureId ?: return]
		grantResources(npcInfo?.meatResourceInfo ?: return, player, ai)
	}

	@IntentHandler
	private fun handleHarvestBoneIntent(intent: HarvestBoneIntent) {
		val ai = intent.target
		val player = intent.player

		val npcInfo = ServerData.npcs[ai.creatureId ?: return]
		grantResources(npcInfo?.boneResourceInfo ?: return, player, ai)
	}

	private fun grantResources(resourceInfo: NpcResourceInfo, player: Player, ai: AIObject) {
		if (ai.isHarvested) {
			SystemMessageIntent.broadcastPersonal(player, "@skl_use:nothing_to_harvest")
			return
		}

		val requestedCreatureResourceType = resourceInfo.type
		val spawnedResource = getSpawnedResource(player, requestedCreatureResourceType)

		if (spawnedResource == null) {
			StandardLog.onPlayerError(this, player, "unable to find a creature resource of type %s for %s", requestedCreatureResourceType, ai)
			return
		}

		val creatureQuality = creatureQualities.random()
		val amount = randomizeResourceAmount(resourceInfo, creatureQuality, player)
		val service = this
		val eventHandler = object : ResourceContainerEventHandler {
			override fun onUnknownError() {
				
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
		val experienceGained = player.creatureObject.level * 2
		ExperienceIntent(player.creatureObject, "scout", experienceGained).broadcast()
	}

	private fun randomizeResourceAmount(resourceInfo: NpcResourceInfo, creatureQuality: CreatureQuality, player: Player): Int {
		val baseNpcResourceAmount = ceil(resourceInfo.amount * creatureQuality.multiplier).toInt()
		val creatureHarvestingSkillModBonus = ceil(baseNpcResourceAmount * creatureHarvestingMultiplier(player)).toInt()
		return baseNpcResourceAmount + creatureHarvestingSkillModBonus
	}

	private fun creatureHarvestingMultiplier(player: Player) = player.creatureObject.getSkillModValue("creature_harvesting") / 100.0

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

}