/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.player.experience.skills;

import com.projectswg.holocore.intents.gameplay.player.experience.skills.SkillModIntent;
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Manages protection granted to the player when equipping a Jedi robe or a piece of equipment.
 */
public class ProtectionService extends Service {
	
	private final Map<String, Integer> armorSlotProtectionMap;	// Note that the values of this Map add up to 100.0
	private final Map<String, Integer> robeProtectionMap;
	private static final int ARM_RPOTECTION = 7;
	
	public ProtectionService() {
		armorSlotProtectionMap = Map.of(	// This map is unmodifiable
				"chest2", 37,
				"pants1", 21,
				"hat", 14,
				"bracer_upper_l", ARM_RPOTECTION,
				"bracer_upper_r", ARM_RPOTECTION,
				"bicep_l", ARM_RPOTECTION,
				"bicep_r", ARM_RPOTECTION
		);
		
		robeProtectionMap = Map.of(	// This map is unmodifiable
				"pseudo_1", 1400,	// Faint
				"pseudo_2", 3000,	// Weak
				"pseudo_3", 4000,	// Lucent
				"pseudo_4", 5000,	// Luminous
				"pseudo_5", 6500	// Radiant
		);
	}
	
	@Override
	public boolean initialize() {
		// Sanity check
		int armorProtectionPercentage = armorSlotProtectionMap
				.values()
				.stream()
				.mapToInt(Integer::intValue)
				.sum();
		
		return super.initialize() && Objects.equals(88, armorProtectionPercentage);
	}
	
	@IntentHandler
	private void handleContainerTransferIntent(ContainerTransferIntent intent) {
		SWGObject item = intent.getObject();
		SWGObject newContainer = intent.getContainer();
		SWGObject oldContainer = intent.getOldContainer();
		
		if (newContainer == null || oldContainer == null) {
			return;
		}
		
		if (newContainer instanceof CreatureObject) {
			// They equipped something
			handleTransfer(item, (CreatureObject) newContainer, true);	// newContainer is a character
		} else if (oldContainer instanceof CreatureObject) {
			// They unequipped something
			handleTransfer(item, (CreatureObject) oldContainer, false);	// oldContainer is a character
		}
	}
	
	private void handleTransfer(SWGObject item, CreatureObject container, boolean equip) {
		switch (item.getGameObjectType()) {
			case GOT_CLOTHING_CLOAK:
				handleTransferRobe(item, container, equip);
				break;
			case GOT_ARMOR_HEAD:
				handleTransferArmor(item, "hat", container, equip);
				break;
			case GOT_ARMOR_BODY:
				handleTransferArmor(item, "chest2", container, equip);
				break;
			case GOT_ARMOR_LEG:
				handleTransferArmor(item, "pants1", container, equip);
				break;
			case GOT_ARMOR_ARM:
				// Covers both biceps and both bracers. They all have the same protection weight.
				handleTransferArmor(item, "bicep_r", container, equip);
				break;
		}
	}
	
	private void handleTransferArmor(SWGObject armor, String slotName, CreatureObject creature, boolean equip) {
		Integer slotProtection = armorSlotProtectionMap.get(slotName);
		
		if (equip) {
			// They equipped this piece or armor. Check if they have a jedi robe with protection equipped. If they do, stop here.
			Collection<SWGObject> slottedObjects = creature.getSlottedObjects();
			
			for (SWGObject slottedObject : slottedObjects) {
				if (slottedObject.hasAttribute("@obj_attr_n:protection_level")) {
					// Jedi robe equipped. Don't give them more protection from the piece of equipped armor.
					return;
				}
			}
		} else {
			// They unequipped this piece of armor. Deduct the protection instead of adding it.
			slotProtection = -slotProtection;
		}
		
		adjustArmorProtectionType(creature, armor, "kinetic", "cat_armor_standard_protection.kinetic", slotProtection);
		adjustArmorProtectionType(creature, armor, "energy", "cat_armor_standard_protection.energy", slotProtection);
		adjustArmorProtectionType(creature, armor, "heat", "cat_armor_special_protection.special_protection_type_heat", slotProtection);
		adjustArmorProtectionType(creature, armor, "cold", "cat_armor_special_protection.special_protection_type_cold", slotProtection);
		adjustArmorProtectionType(creature, armor, "acid", "cat_armor_special_protection.special_protection_type_acid", slotProtection);
		adjustArmorProtectionType(creature, armor, "electricity", "cat_armor_special_protection.special_protection_type_electricity", slotProtection);
	}
	
	private int getWeightedProtection(SWGObject item, String attribute, Integer slotProtection) {
		if (!item.hasAttribute(attribute)) {
			return 0;
		}
		
		String attributeRaw = item.getAttribute(attribute);
		double protection = Double.parseDouble(attributeRaw);
		double weighted = protection * slotProtection / 100.0;
		
		return (int) weighted;
	}
	
	private void handleTransferRobe(SWGObject robe, CreatureObject creature, boolean equip) {
		// Deduct any existing armor protection if the robe is being equipped or add it back if the robe is unequipped
		for (String slotName : armorSlotProtectionMap.keySet()) {
			SWGObject slottedObject = creature.getSlottedObject(slotName);
			
			if (slottedObject != null) {
				handleTransferArmor(slottedObject, slotName, creature, !equip);
			}
		}
		
		int robeProtection = robeProtection(robe);
		
		if (robeProtection == 0) {
			// Robe doesn't offer protection. Do nothing.
			return;
		}
		
		if (!equip) {
			// They unequipped this item. Deduct the protection instead of adding it.
			robeProtection *= -1.0;
		}
		
		adjustRobeProtectionType(creature, "kinetic", robeProtection);
		adjustRobeProtectionType(creature, "energy", robeProtection);
		adjustRobeProtectionType(creature, "heat", robeProtection);
		adjustRobeProtectionType(creature, "cold", robeProtection);
		adjustRobeProtectionType(creature, "acid", robeProtection);
		adjustRobeProtectionType(creature, "electricity", robeProtection);
	}
	
	private void adjustArmorProtectionType(CreatureObject creature, SWGObject armor, String skillModName, String attribute, Integer slotProtection) {
		int protection = getWeightedProtection(armor, attribute, slotProtection);
		
		if (protection != 0)
			new SkillModIntent(skillModName, 0, protection, creature).broadcast();
	}
	
	private void adjustRobeProtectionType(CreatureObject creature, String skillModName, int robeProtection) {
		if (robeProtection != 0)
			new SkillModIntent(skillModName, 0, robeProtection, creature).broadcast();
	}
	
	private int robeProtection(SWGObject item) {
		String protectionLevel = item.getAttribute("@obj_attr_n:protection_level");
		
		if (protectionLevel == null) {
			return 0;	// This robe does not offer protection
		}
		
		String key = protectionLevel.replace("@obj_attr_n:", "");
		
		return robeProtectionMap.getOrDefault(key, 0);
	}
	
}
