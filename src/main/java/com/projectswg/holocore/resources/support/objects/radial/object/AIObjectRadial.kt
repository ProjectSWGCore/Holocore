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
package com.projectswg.holocore.resources.support.objects.radial.`object`

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.intents.gameplay.combat.LootRequestIntent
import com.projectswg.holocore.resources.gameplay.combat.loot.LootType
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcLoader
import com.projectswg.holocore.resources.support.global.commands.Locomotion
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.services.gameplay.crafting.resource.HarvestBoneIntent
import com.projectswg.holocore.services.gameplay.crafting.resource.HarvestHideIntent
import com.projectswg.holocore.services.gameplay.crafting.resource.HarvestMeatIntent
import com.projectswg.holocore.services.gameplay.crafting.resource.MilkCreatureIntent

class AIObjectRadial : RadialHandlerInterface {
	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		val ai = target as AIObject
		val creatureObject = player.creatureObject
		if (Locomotion.DEAD.isActive(creatureObject) || Locomotion.INCAPACITATED.isActive(creatureObject)) {
			return
		}
		if (ai.posture == Posture.DEAD) {
			appendDeadCreatureOptions(ai, creatureObject, options)
		} else {
			appendLivingCreatureOptions(ai, options)
		}
	}

	private fun appendDeadCreatureOptions(ai: AIObject, creatureObject: CreatureObject, options: MutableCollection<RadialOption>) {
		options.add(RadialOption.create(RadialItem.LOOT_ALL, RadialOption.create(RadialItem.LOOT)))

		if (isScout(creatureObject)) {
			appendScoutOptions(ai, options)
		}
	}

	private fun appendLivingCreatureOptions(ai: AIObject, options: MutableCollection<RadialOption>) {
		if (ai.isMilked || ai.ownerId > 0) {
			return
		}
		val npcInfo = ServerData.npcs[ai.creatureId ?: return] ?: return

		if (npcInfo.milkResourceInfo.amount > 0) {
			options.add(RadialOption.create(RadialItem.SERVER_MENU4, "@pet/pet_menu:milk_me"))
		}
	}

	private fun appendScoutOptions(ai: AIObject, options: MutableCollection<RadialOption>) {
		val npcInfo = ServerData.npcs[ai.creatureId ?: return] ?: return
		val harvestCreatureOptions = harvestCreatureOptions(npcInfo)
		
		if (harvestCreatureOptions.isNotEmpty()) {
			options.add(RadialOption.create(RadialItem.SERVER_HARVEST_CORPSE, "@sui:harvest_corpse", harvestCreatureOptions))
		}
	}

	private fun isScout(creatureObject: CreatureObject): Boolean {
		return creatureObject.hasSkill("outdoors_scout_novice")
	}

	private fun harvestCreatureOptions(npcInfo: NpcLoader.NpcInfo): Collection<RadialOption> {
		val resourceRadialOptions = mutableListOf<RadialOption>()
		val boneResourceInfo = npcInfo.boneResourceInfo
		val hideResourceInfo = npcInfo.hideResourceInfo
		val meatResourceInfo = npcInfo.meatResourceInfo

		if (boneResourceInfo.amount > 0) {
			resourceRadialOptions.add(RadialOption.create(RadialItem.SERVER_MENU1, "@sui:harvest_bone"))
		}
		if (hideResourceInfo.amount > 0) {
			resourceRadialOptions.add(RadialOption.create(RadialItem.SERVER_MENU2, "@sui:harvest_hide"))
		}
		if (meatResourceInfo.amount > 0) {
			resourceRadialOptions.add(RadialOption.create(RadialItem.SERVER_MENU3, "@sui:harvest_meat"))
		}

		return resourceRadialOptions
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		val ai = target as AIObject
		val creatureObject = player.creatureObject
		if (Locomotion.DEAD.isActive(creatureObject) || Locomotion.INCAPACITATED.isActive(creatureObject)) {
			return
		}
		if (ai.posture == Posture.DEAD) {
			handleDeadCreatureSelection(player, ai, selection)
		} else {
			handleLivingCreatureSelection(player, ai, selection)
		}
	}

	private fun handleDeadCreatureSelection(player: Player, ai: AIObject, selection: RadialItem) {
		when (selection) {
			RadialItem.LOOT         -> LootRequestIntent(player, ai, LootType.LOOT).broadcast()
			RadialItem.LOOT_ALL     -> LootRequestIntent(player, ai, LootType.LOOT_ALL).broadcast()
			RadialItem.SERVER_MENU1 -> HarvestBoneIntent(player, ai).broadcast()
			RadialItem.SERVER_MENU2 -> HarvestHideIntent(player, ai).broadcast()
			RadialItem.SERVER_MENU3 -> HarvestMeatIntent(player, ai).broadcast()
			else                    -> StandardLog.onPlayerError(this, player, "radial option without handling selected: %s", selection)
		}
	}

	private fun handleLivingCreatureSelection(player: Player, ai: AIObject, selection: RadialItem) {
		when (selection) {
			RadialItem.SERVER_MENU4 -> MilkCreatureIntent(player, ai).broadcast()
			else                    -> StandardLog.onPlayerError(this, player, "radial option without handling selected: %s", selection)
		}
	}
}
