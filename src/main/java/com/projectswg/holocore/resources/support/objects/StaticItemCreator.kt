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

package com.projectswg.holocore.resources.support.objects

import com.projectswg.common.data.combat.DamageType
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.StaticItemLoader
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.tangible.ArmorCategory
import com.projectswg.holocore.resources.support.objects.swg.tangible.LightsaberPowerCrystalQuality
import com.projectswg.holocore.resources.support.objects.swg.tangible.Protection
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject

object StaticItemCreator {
	
	fun createItem(itemName: String): SWGObject? {
		val info = ServerData.staticItems.getItemByName(itemName) ?: return null
		val swgObject = ObjectCreator.createObjectFromTemplate(info.iffTemplate) as? TangibleObject ?: return null
		
		applyAttributes(swgObject, info)
		return swgObject
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.StaticItemInfo) {
		obj.maxHitPoints = info.hitPoints
		obj.volume = info.volume
		obj.objectName = info.stringName
		
		applyAttributes(obj, info.armorInfo)
		applyAttributes(obj, info.wearableInfo)
		applyAttributes(obj, info.weaponInfo)
		applyAttributes(obj, info.consumableInfo)
		applyAttributes(obj, info.costumeInfo)
		applyAttributes(obj, info.crystalInfo)
		applyAttributes(obj, info.grantInfo)
		applyAttributes(obj, info.genericInfo)
		applyAttributes(obj, info.itemInfo)
		applyAttributes(obj, info.objectInfo)
		applyAttributes(obj, info.schematicInfo)
		applyAttributes(obj, info.storytellerInfo)
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.ArmorItemInfo?) {
		if (info == null)
			return

		obj.requiredCombatLevel = info.requiredLevel

		var kineticMax = 0
		var energyMax = 0
		when (info.armorCategory) {
			ArmorCategory.assault -> {
				kineticMax = info.protection + 1000
				energyMax = info.protection - 1000
			}
			ArmorCategory.battle -> {
				kineticMax = info.protection
				energyMax = info.protection
			}
			ArmorCategory.reconnaissance -> {
				kineticMax = info.protection - 1000
				energyMax = info.protection + 1000
			}
		}
		obj.armorCategory = info.armorCategory

		val protection = Protection(
			kineticMax,
			energyMax,
			info.protection,
			info.protection,
			info.protection,
			info.protection
		)
		
		obj.protection = protection
		
		applySkillMods(obj, info.skillMods)
		applyColors(obj, info.color)
		applyItemValue(info.value, obj)
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.WearableItemInfo?) {
		if (info == null)
			return
		
		applySkillMods(obj, info.skillMods)
		obj.requiredCombatLevel = info.requiredLevel
		obj.requiredFaction = ServerData.factions.getFaction(info.requiredFaction)
		applyColors(obj, info.color)
		applyItemValue(info.value, obj)
	}

	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.WeaponItemInfo?) {
		if (info == null)
			return
		obj.requiredCombatLevel = info.requiredLevel

		val weapon = obj as WeaponObject
		weapon.type = info.weaponType
		weapon.attackSpeed = info.attackSpeed.toFloat()
		weapon.minRange = info.minRange.toFloat()
		weapon.maxRange = info.maxRange.toFloat()
		weapon.damageType = info.damageType
		weapon.elementalType = info.elementalType
		weapon.elementalValue = info.elementalDamage
		weapon.minDamage = info.minDamage
		weapon.maxDamage = info.maxDamage
		weapon.accuracy = info.accuracyBonus
		weapon.woundChance = (info.woundChance / 100f)
		weapon.procEffect = info.procEffect
		weapon.specialAttackCost = info.specialAttackCost
		weapon.requiredSkill = info.requiredSkill
		weapon.splashDamageRadius = info.splashDamageRadius
		weapon.splashDamagePercent = info.splashDamagePercent

		applySkillMods(obj, info.skillMods)
		applyItemValue(info.value, obj)
	}

	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.ConsumableItemInfo?) {
		if (info == null)
			return

		if (info.charges > 0) {
			obj.counter = info.charges
		}

		applyItemValue(info.value, obj)
	}
	
	@Suppress("UNUSED_PARAMETER")
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.CostumeItemInfo?) {
//		if (info == null)
//			return;
	}

	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.CrystalItemInfo?) {
		if (info == null)
			return

		applyColors(obj, info.color)

		val quality = info.quality

		val powerCrystal = info.maxDmg > 0
		if (powerCrystal) {
			obj.lightsaberPowerCrystalQuality = when (quality) {
				0 -> LightsaberPowerCrystalQuality.poor
				1 -> LightsaberPowerCrystalQuality.fair
				2 -> LightsaberPowerCrystalQuality.good
				3 -> LightsaberPowerCrystalQuality.quality
				4 -> LightsaberPowerCrystalQuality.select
				5 -> LightsaberPowerCrystalQuality.premium
				6 -> LightsaberPowerCrystalQuality.flawless
				else -> null
			}
			obj.lightsaberPowerCrystalMinDmg = info.minDmg
			obj.lightsaberPowerCrystalMaxDmg = info.maxDmg
		}

		if (info.elementalDamageType.isNotEmpty()) {
			val elementalType = DamageType.valueOf(info.elementalDamageType)
			obj.lightsaberColorCrystalElementalType = elementalType
			obj.lightsaberColorCrystalDamagePercent = info.elementalDamagePercent
		}
	}
	
	@Suppress("UNUSED_PARAMETER")
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.GrantItemInfo?) {
//		if (info == null)
//			return;
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.GenericItemInfo?) {
		if (info == null)
			return

		if (info.charges > 0) {
			obj.counter = info.charges
		}

		applyItemValue(info.value, obj)
		applyColors(obj, info.color)
		obj.requiredSkill = info.requiredSkill
	}
	
	private fun applyAttributes(obj: TangibleObject, info: StaticItemLoader.ObjectItemInfo?) {
		if (info == null)
			return

		if (info.charges != 0) {
			obj.counter = info.charges
		}

		applyItemValue(info.value, obj)
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
		for ((key, value) in skillMods) {
			obj.adjustSkillmod(key, value, 0)
		}
	}
	
	private fun applyColors(obj: TangibleObject, colors: IntArray) {
		for (i in 0..3) {
			if (colors[i] >= 0) {
				obj.putCustomization("/private/index_color_$i", colors[i])
			}
		}
	}

	private fun applyItemValue(value: Int, obj: TangibleObject) {
		if (value > 0) {
			obj.setServerAttribute(ServerAttribute.ITEM_VALUE, value)
		}
	}

}