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
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

/**
 * Manages protection granted to the player when equipping a Jedi robe or a piece of equipment.
 * Also adds additional protection granted by innate armor.
 */
class ProtectionService : Service() {
	
	private val armorSlotProtectionMap = mapOf( // Unmodifiable
				"chest2"         to 37,
				"pants1"         to 21,
				"hat"            to 14,
				"bracer_upper_l" to ARM_RPOTECTION,
				"bracer_upper_r" to ARM_RPOTECTION,
				"bicep_l"        to ARM_RPOTECTION,
				"bicep_r"        to ARM_RPOTECTION
		)
	private val robeProtectionMap = mapOf( // Unmodifiable
				"pseudo_1" to 1400, // Faint
				"pseudo_2" to 3000, // Weak
				"pseudo_3" to 4000, // Lucent
				"pseudo_4" to 5000, // Luminous
				"pseudo_5" to 6500  // Radiant
		)
	
	init {
		assert(armorSlotProtectionMap.values.sum() == 100)
	}
	
	@IntentHandler
	private fun handleContainerTransferIntent(intent: ContainerTransferIntent) {
		val item = intent.obj
		val newContainer = intent.container ?: return
		val oldContainer = intent.oldContainer ?: return
		
		if (newContainer is CreatureObject) {
			// They equipped something
			handleTransfer(item, newContainer, item.arrangement[intent.arrangement-4] ?: EMPTY_LIST, true)    // newContainer is a character
		} else if (oldContainer is CreatureObject) {
			// They unequipped something
			handleTransfer(item, oldContainer, item.arrangement[intent.oldArrangement-4] ?: EMPTY_LIST, false)    // oldContainer is a character
		}
	}
	
	@IntentHandler
	private fun handleSkillModIntent(intent: SkillModIntent) {
		// Modify protection values if an innate armor skillmod is being granted
		val skillModName = intent.skillModName
		
		if ("expertise_innate_protection_all" == skillModName) {
			SkillModIntent("kinetic", intent.adjustBase, intent.adjustModifier, *intent.affectedCreatures).broadcast()
			SkillModIntent("energy", intent.adjustBase, intent.adjustModifier, *intent.affectedCreatures).broadcast()
			SkillModIntent("heat", intent.adjustBase, intent.adjustModifier, *intent.affectedCreatures).broadcast()
			SkillModIntent("cold", intent.adjustBase, intent.adjustModifier, *intent.affectedCreatures).broadcast()
			SkillModIntent("acid", intent.adjustBase, intent.adjustModifier, *intent.affectedCreatures).broadcast()
			SkillModIntent("electricity", intent.adjustBase, intent.adjustModifier, *intent.affectedCreatures).broadcast()
		}
	}
	
	private fun handleTransfer(item: SWGObject, container: CreatureObject, arrangement: List<String>, equip: Boolean) {
		if (equip && item.gameObjectType == GameObjectType.GOT_CLOTHING_CLOAK) {
			// Deduct any existing armor protection if the robe is being equipped or add it back if the robe is unequipped
			for (slotName in armorSlotProtectionMap.keys) {
				val slottedObject = container.getSlottedObject(slotName)

				if (slottedObject != null && slottedObject != item) {
					handleTransferArmor(slottedObject, slotName, container, !equip)
				}
			}
		}
		for (slot in arrangement) {
			handleTransferArmor(item, slot, container, equip)
		}
	}
	
	private fun handleTransferArmor(armor: SWGObject, slotName: String, creature: CreatureObject, equip: Boolean) {
		var slotProtection = armorSlotProtectionMap[slotName] ?: return
		if (!equip)
			slotProtection = -slotProtection
		
//		if (equip) {
//			// They equipped this piece or armor. Check if they have a jedi robe with protection equipped. If they do, stop here.
//			if (creature.slottedObjects.any { robeProtection(it) > 0 })
//				return // Jedi robe equipped. Don't give them more protection from the piece of equipped armor.
//		} else {
//			// They unequipped this piece of armor. Deduct the protection instead of adding it.
//			slotProtection = (-slotProtection)
//		}
		
		adjustArmorProtectionType(creature, armor, "kinetic", "cat_armor_standard_protection.kinetic", slotProtection)
		adjustArmorProtectionType(creature, armor, "energy", "cat_armor_standard_protection.energy", slotProtection)
		adjustArmorProtectionType(creature, armor, "heat", "cat_armor_special_protection.special_protection_type_heat", slotProtection)
		adjustArmorProtectionType(creature, armor, "cold", "cat_armor_special_protection.special_protection_type_cold", slotProtection)
		adjustArmorProtectionType(creature, armor, "acid", "cat_armor_special_protection.special_protection_type_acid", slotProtection)
		adjustArmorProtectionType(creature, armor, "electricity", "cat_armor_special_protection.special_protection_type_electricity", slotProtection)
	}
	
	private fun adjustArmorProtectionType(creature: CreatureObject, armor: SWGObject, skillModName: String, attribute: String, slotProtection: Int) {
		val protection = getWeightedProtection(armor, attribute, slotProtection)
		
		if (protection != 0)
			SkillModIntent(skillModName, 0, protection, creature).broadcast()
	}
	
	private fun getWeightedProtection(item: SWGObject, attribute: String, slotProtection: Int): Int {
		val protection = item.getAttribute(attribute)?.toDoubleOrNull() ?: return 0
		
		return (protection * slotProtection / 100.0).toInt()
	}
	
	companion object {
		
		private const val ARM_RPOTECTION = 7
		
		private val EMPTY_LIST = listOf<String>()
		
	}
	
}
