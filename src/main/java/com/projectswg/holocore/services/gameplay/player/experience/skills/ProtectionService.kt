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
		val item = intent.getObject()
		val newContainer = intent.container ?: return
		val oldContainer = intent.oldContainer ?: return
		
		if (newContainer is CreatureObject) {
			// They equipped something
			handleTransfer(item, newContainer, true)    // newContainer is a character
		} else if (oldContainer is CreatureObject) {
			// They unequipped something
			handleTransfer(item, oldContainer, false)    // oldContainer is a character
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
	
	private fun handleTransfer(item: SWGObject, container: CreatureObject, equip: Boolean) {
		when (item.gameObjectType) {
			GameObjectType.GOT_CLOTHING_CLOAK -> handleTransferRobe(item, container, equip)
			GameObjectType.GOT_ARMOR_HEAD -> handleTransferArmor(item, "hat", container, equip)
			GameObjectType.GOT_ARMOR_BODY -> handleTransferArmor(item, "chest2", container, equip)
			GameObjectType.GOT_ARMOR_LEG -> handleTransferArmor(item, "pants1", container, equip)
			GameObjectType.GOT_ARMOR_ARM -> handleTransferArmor(item, "bicep_r", container, equip) // Covers both biceps and both bracers. They all have the same protection weight.
			else -> return
		}
	}
	
	private fun handleTransferRobe(robe: SWGObject, creature: CreatureObject, equip: Boolean) {
		// Deduct any existing armor protection if the robe is being equipped or add it back if the robe is unequipped
		for (slotName in armorSlotProtectionMap.keys) {
			val slottedObject = creature.getSlottedObject(slotName)
			
			if (slottedObject != null) {
				handleTransferArmor(slottedObject, slotName, creature, !equip)
			}
		}
		
		var robeProtection = robeProtection(robe)
		if (robeProtection <= 0)
			return // Robe doesn't offer protection. Do nothing.
		
		if (!equip)
			robeProtection = -robeProtection // They unequipped this item. Deduct the protection instead of adding it.
		
		adjustRobeProtectionType(creature, "kinetic", robeProtection)
		adjustRobeProtectionType(creature, "energy", robeProtection)
		adjustRobeProtectionType(creature, "heat", robeProtection)
		adjustRobeProtectionType(creature, "cold", robeProtection)
		adjustRobeProtectionType(creature, "acid", robeProtection)
		adjustRobeProtectionType(creature, "electricity", robeProtection)
	}
	
	private fun handleTransferArmor(armor: SWGObject, slotName: String, creature: CreatureObject, equip: Boolean) {
		var slotProtection = armorSlotProtectionMap[slotName] ?: return
		
		if (equip) {
			// They equipped this piece or armor. Check if they have a jedi robe with protection equipped. If they do, stop here.
			if (creature.slottedObjects.any { robeProtection(it) > 0 })
				return // Jedi robe equipped. Don't give them more protection from the piece of equipped armor.
		} else {
			// They unequipped this piece of armor. Deduct the protection instead of adding it.
			slotProtection = (-slotProtection)
		}
		
		adjustArmorProtectionType(creature, armor, "kinetic", "cat_armor_standard_protection.kinetic", slotProtection)
		adjustArmorProtectionType(creature, armor, "energy", "cat_armor_standard_protection.energy", slotProtection)
		adjustArmorProtectionType(creature, armor, "heat", "cat_armor_special_protection.special_protection_type_heat", slotProtection)
		adjustArmorProtectionType(creature, armor, "cold", "cat_armor_special_protection.special_protection_type_cold", slotProtection)
		adjustArmorProtectionType(creature, armor, "acid", "cat_armor_special_protection.special_protection_type_acid", slotProtection)
		adjustArmorProtectionType(creature, armor, "electricity", "cat_armor_special_protection.special_protection_type_electricity", slotProtection)
	}
	
	private fun getWeightedProtection(item: SWGObject, attribute: String, slotProtection: Int): Int {
		val protection = item.getAttribute(attribute)?.toDoubleOrNull() ?: return 0
		
		return (protection * slotProtection / 100.0).toInt()
	}
	
	private fun adjustArmorProtectionType(creature: CreatureObject, armor: SWGObject, skillModName: String, attribute: String, slotProtection: Int) {
		val protection = getWeightedProtection(armor, attribute, slotProtection)
		
		if (protection != 0)
			SkillModIntent(skillModName, 0, protection, creature).broadcast()
	}
	
	private fun adjustRobeProtectionType(creature: CreatureObject, skillModName: String, robeProtection: Int) {
		if (robeProtection != 0)
			SkillModIntent(skillModName, 0, robeProtection, creature).broadcast()
	}
	
	private fun robeProtection(item: SWGObject): Int {
		val protectionLevel = item.getAttribute("@obj_attr_n:protection_level") ?: return 0    // This robe does not offer protection
		
		val key = protectionLevel.replace("@obj_attr_n:", "")
		
		return robeProtectionMap.getOrDefault(key, 0)
	}
	
	companion object {
		private const val ARM_RPOTECTION = 7
	}
	
}
