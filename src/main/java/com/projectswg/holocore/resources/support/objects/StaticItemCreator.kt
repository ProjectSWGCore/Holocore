/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects

import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.StaticItemLoader
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import java.util.*
import kotlin.math.floor

object StaticItemCreator {
	
	fun createItem(itemName: String): SWGObject? {
		val info = DataLoader.staticItems().getItemByName(itemName) ?: return null
		val swgObject = ObjectCreator.createObjectFromTemplate(info.iffTemplate) as? TangibleObject ?: return null
		
		applyAttributes(swgObject, info)
		return swgObject
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.StaticItemInfo) {
		obj.addAttribute("condition", String.format("%d/%d", info.hitPoints, info.hitPoints))
		obj.addAttribute("volume", info.volume.toString())
		obj.objectName = info.stringName
		
		applyAttributes(obj, info.armorInfo)
		applyAttributes(obj, info.wearableInfo)
		applyAttributes(obj, info.weaponInfo)
		applyAttributes(obj, info.collectionInfo)
		applyAttributes(obj, info.costumeInfo)
		applyAttributes(obj, info.dnaInfo)
		applyAttributes(obj, info.grantInfo)
		applyAttributes(obj, info.genericInfo)
		applyAttributes(obj, info.objectInfo)
		applyAttributes(obj, info.schematicInfo)
		applyAttributes(obj, info.storytellerInfo)
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.ArmorItemInfo?) {
		if (info == null)
			return
		
		if (info.requiredProfession.isNotEmpty())
			obj.addAttribute("class_required", "@ui_roadmap:title_" + info.requiredProfession)
		obj.addAttribute("required_combat_level", info.requiredLevel.toString())
		
		val kineticMax: Int
		val energyMax: Int
		when (info.armorType) {
			StaticItemLoader.ArmorItemInfo.ArmorType.ASSAULT -> {
				kineticMax = info.protection + 1000
				energyMax = info.protection - 1000
				obj.addAttribute("armor_category", "@obj_attr_n:armor_assault")
			}
			StaticItemLoader.ArmorItemInfo.ArmorType.BATTLE -> {
				kineticMax = info.protection
				energyMax = info.protection
				obj.addAttribute("armor_category", "@obj_attr_n:armor_battle")
			}
			StaticItemLoader.ArmorItemInfo.ArmorType.RECON -> {
				kineticMax = info.protection - 1000
				energyMax = info.protection + 1000
				obj.addAttribute("armor_category", "@obj_attr_n:armor_reconnaissance")
			}
		}
		
		obj.addAttribute("cat_armor_standard_protection.kinetic", kineticMax.toString())
		obj.addAttribute("cat_armor_standard_protection.energy", energyMax.toString())
		obj.addAttribute("cat_armor_special_protection.special_protection_type_heat", info.protection.toString())
		obj.addAttribute("cat_armor_special_protection.special_protection_type_cold", info.protection.toString())
		obj.addAttribute("cat_armor_special_protection.special_protection_type_acid", info.protection.toString())
		obj.addAttribute("cat_armor_special_protection.special_protection_type_electricity", info.protection.toString())
		
		applySkillMods(obj, info.skillMods)
		applyColors(obj, info.color)
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.WearableItemInfo?) {
		if (info == null)
			return
		
		if (info.requiredProfession.isNotEmpty())
			obj.addAttribute("class_required", "@ui_roadmap:title_" + info.requiredProfession)
		obj.addAttribute("required_combat_level", info.requiredLevel.toString())
		
		if (info.requiredFaction.isNotEmpty())
			obj.addAttribute("faction_restriction", "@pvp_factions:" + info.requiredFaction)
		
		// Apply the mods!
		applySkillMods(obj, info.skillMods)
		applyColors(obj, info.color)
		
		// Add the race restrictions only if there are any
		val raceRestriction = buildRaceRestrictionString(info)
		if (raceRestriction.isNotEmpty())
			obj.addAttribute("species_restrictions.species_name", raceRestriction)
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.WeaponItemInfo?) {
		if (info == null)
			return
		
		if (info.requiredProfession.isNotEmpty())
			obj.addAttribute("class_required", "@ui_roadmap:title_" + info.requiredProfession)
		obj.addAttribute("required_combat_level", info.requiredLevel.toString())
		obj.addAttribute("cat_wpn_damage.wpn_damage_type", "@obj_attr_n:${info.damageType.name.toLowerCase(Locale.US)}")
		obj.addAttribute("cat_wpn_damage.wpn_category", "@obj_attr_n:wpn_category_" + info.weaponType.num)
		obj.addAttribute("cat_wpn_damage.wpn_attack_speed", (info.attackSpeed / 100).toString())
		obj.addAttribute("cat_wpn_damage.damage", "${info.minDamage}-${info.maxDamage}")
		if (info.elementalType != null) {    // Not all weapons have elemental damage.
			obj.addAttribute("cat_wpn_damage.wpn_elemental_type", "@obj_attr_n:${info.elementalType.name.toLowerCase(Locale.US)}")
			obj.addAttribute("cat_wpn_damage.wpn_elemental_value", info.elementalDamage.toString())
		}
		
		obj.addAttribute("cat_wpn_damage.weapon_dps", info.actualDps.toString())
		
		if (info.procEffect.isNotEmpty())
		// Not all weapons have a proc effect
			obj.addAttribute("proc_name", info.procEffect)
		
		// TODO set DPS
		
		obj.addAttribute("cat_wpn_other.wpn_range", String.format("%d-%dm", info.minRange, info.maxRange))
		// Ziggy: Special Action Cost would go under cat_wpn_other as well, but it's a pre-NGE artifact.
		
		val weapon = obj as WeaponObject
		weapon.type = info.weaponType
		weapon.attackSpeed = info.attackSpeed.toFloat()
		weapon.minRange = info.minRange.toFloat()
		weapon.maxRange = info.maxRange.toFloat()
		weapon.damageType = info.damageType
		weapon.elementalType = info.elementalType
		weapon.minDamage = info.minDamage
		weapon.maxDamage = info.maxDamage
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.CollectionItemInfo?) {
		if (info == null)
			return
		
		obj.addAttribute("collection_name", info.slotName)
		
		applyColors(obj, info.color)
	}
	
	@Suppress("UNUSED_PARAMETER")
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.CostumeItemInfo?) {
//		if (info == null)
//			return;
	}
	
	@Suppress("UNUSED_PARAMETER")
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.DnaItemInfo?) {
//		if (info == null)
//			return;
	}
	
	@Suppress("UNUSED_PARAMETER")
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.GrantItemInfo?) {
//		if (info == null)
//			return;
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.GenericItemInfo?) {
		if (info == null)
			return
		
		if (info.value != 0)
			obj.addAttribute("charges", info.value.toString())
		
		applyColors(obj, info.color)
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.ObjectItemInfo?) {
		if (info == null)
			return
		
		if (info.value != 0)
			obj.addAttribute("charges", info.value.toString())
		
		applyColors(obj, info.color)
	}
	
	@Suppress("UNUSED_PARAMETER")
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.SchematicItemInfo?) {
//		if (info == null)
//			return;
	}
	
	@Suppress("UNUSED_PARAMETER")
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.StorytellerItemInfo?) {
//		if (info == null)
//			return;
	}
	
	private fun applySkillMods(obj: TangibleObject, skillMods: Map<String, Int>) {
		for ((key, value) in skillMods)
			obj.addAttribute(key, value.toString())
	}
	
	private fun applyColors(obj: TangibleObject, colors: IntArray) {
		for (i in 0..3) {
			if (colors[i] >= 0) {
				obj.putCustomization("/private/index_color_$i", colors[i])
			}
		}
	}
	
	private fun buildRaceRestrictionString(info: StaticItemLoader.WearableItemInfo): String {
		var races = ""
		
		if (info.isRaceWookie)
			races += "Wookiee "
		if (info.isRaceIthorian)
			races += "Ithorian "
		if (info.isRaceRodian)
			races += "Rodian "
		if (info.isRaceTrandoshan)
			races += "Trandoshan "
		if (info.isRaceRest)
			races += "MonCal Human Zabrak Bothan Sullustan Twi'lek "
		
		return if (races.isEmpty()) "" else races.substring(0, races.length - 1)
	}
	
}
