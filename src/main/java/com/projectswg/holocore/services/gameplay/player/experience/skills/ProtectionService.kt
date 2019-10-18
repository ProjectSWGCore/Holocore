/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.services.gameplay.player.experience.skills

import com.projectswg.common.data.objects.GameObjectType
import com.projectswg.holocore.intents.gameplay.player.experience.skills.SkillModIntent
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.player.Profession
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

/**
 * Manages protection granted to the player when equipping a Jedi robe or a piece of equipment.
 * Also adds additional protection granted by innate armor.
 */
class ProtectionService : Service() {
	
	private val attributeLookup = mapOf( // Unmodifiable
			"kinetic"     to "cat_armor_standard_protection.kinetic",
			"energy"      to "cat_armor_standard_protection.energy",
			"heat"        to "cat_armor_special_protection.special_protection_type_heat",
			"cold"        to "cat_armor_special_protection.special_protection_type_cold",
			"acid"        to "cat_armor_special_protection.special_protection_type_acid",
			"electricity" to "cat_armor_special_protection.special_protection_type_electricity"
	)
	private val armorSlotProtectionMap = mapOf( // Unmodifiable
				"chest2"         to 37,
				"pants1"         to 21,
				"hat"            to 14,
				"bracer_upper_l" to ARM_RPOTECTION,
				"bracer_upper_r" to ARM_RPOTECTION,
				"bicep_l"        to ARM_RPOTECTION,
				"bicep_r"        to ARM_RPOTECTION
		)
	
	init {
		assert(armorSlotProtectionMap.values.sum() == 100)
	}
	
	@IntentHandler
	private fun handleContainerTransferIntent(intent: ContainerTransferIntent) {
		val item = intent.obj
		val newContainer = intent.container
		val oldContainer = intent.oldContainer

		if (item.owner == null) {
			// Important that we don't grant protection to players while they are not online: https://bitbucket.org/projectswg/holocore/issues/192/protection-from-equipment-is-no-longer
			return
		}

		
		if (newContainer is CreatureObject) {
			// They equipped something
			handleTransfer(item, newContainer, item.arrangement.getOrNull(intent.arrangement-4) ?: EMPTY_LIST, true)    // newContainer is a character
		} else if (oldContainer is CreatureObject) {
			// They unequipped something
			handleTransfer(item, oldContainer, item.arrangement.getOrNull(intent.oldArrangement-4) ?: EMPTY_LIST, false)    // oldContainer is a character
		}
	}
	
	private fun handleTransfer(item: SWGObject, creature: CreatureObject, arrangement: List<String>, equip: Boolean) {
		val jedi = creature.playerObject?.profession == Profession.FORCE_SENSITIVE
		val robe = item.gameObjectType == GameObjectType.GOT_CLOTHING_CLOAK
		
		if (jedi && robe) {
			updateProtection(creature, equip) { attribute -> item.calculateProtection(attribute) }
		} else if (!jedi && !robe) {
			updateProtection(creature, equip) { attribute -> arrangement.sumBy { slot -> item.calculateProtection(attribute, armorSlotProtectionMap[slot] ?: 0) } }
		}
	}
	
	private inline fun updateProtection(creature: CreatureObject, equip: Boolean, calculateProtection: (attribute: String) -> Int) {
		for ((skillModName, attribute) in attributeLookup) {
			val protection = calculateProtection(attribute)
			SkillModIntent(skillModName, 0, if (equip) protection else -protection, creature).broadcast()
		}
	}
	
	private fun SWGObject.calculateProtection(attribute: String) = (getAttribute(attribute)?.toIntOrNull() ?: 0)
	private fun SWGObject.calculateProtection(attribute: String, slotProtection: Int) = (getAttribute(attribute)?.toIntOrNull() ?: 0) * slotProtection / 100
	
	companion object {
		
		private const val ARM_RPOTECTION = 7
		
		private val EMPTY_LIST = listOf<String>()
		
	}
	
}
