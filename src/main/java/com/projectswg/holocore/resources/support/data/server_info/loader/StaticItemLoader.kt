/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.combat.DamageType
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbIntegerColumnArraySet
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import java.io.File
import java.io.IOException
import java.util.*

@Suppress("unused")
class StaticItemLoader internal constructor() : DataLoader() {
	
	private val itemByName: MutableMap<String, StaticItemInfo>
	
	val items: Collection<StaticItemInfo>
		get() = Collections.unmodifiableCollection(itemByName.values)
	
	init {
		this.itemByName = HashMap()
	}
	
	fun getItemByName(itemName: String): StaticItemInfo? {
		return itemByName[itemName]
	}
	
	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/items/master_item.msdb")).use { set ->
			val colorArray = set.getIntegerArrayParser("index_color_(\\d+)")
			while (set.next()) {
				val slot = StaticItemInfo(set, colorArray)
				itemByName[slot.itemName] = slot
			}
		}
	}
	
	class StaticItemInfo(set: SdbResultSet, colorArray: SdbIntegerColumnArraySet) {
		
		val itemName: String = set.getText("item_name")
		val iffTemplate: String = set.getText("iff_template")
		val stringName: String = set.getText("string_name")
		val volume: Int = set.getInt("volume").toInt()
		val hitPoints: Int = set.getInt("hit_points").toInt()
		
		val armorInfo: ArmorItemInfo?
		val collectionInfo: CollectionItemInfo?
		val consumableInfo: ConsumableItemInfo?
		val costumeInfo: CostumeItemInfo?
		val dnaInfo: DnaItemInfo?
		val grantInfo: GrantItemInfo?
		val genericInfo: GenericItemInfo?
		val objectInfo: ObjectItemInfo?
		val schematicInfo: SchematicItemInfo?
		val storytellerInfo: StorytellerItemInfo?
		val weaponInfo: WeaponItemInfo?
		val wearableInfo: WearableItemInfo?
		
		init {
			val type = set.getText("type")
			
			this.armorInfo = if ("armor" == type) ArmorItemInfo(set, colorArray) else null
			this.collectionInfo = if ("collection" == type) CollectionItemInfo(set, colorArray) else null
			this.consumableInfo = if ("consumable" == type) ConsumableItemInfo(set) else null
			this.costumeInfo = if ("costume" == type) CostumeItemInfo(set) else null
			this.dnaInfo = if ("dna" == type) DnaItemInfo(set) else null
			this.grantInfo = if ("grant" == type) GrantItemInfo(set) else null
			this.genericInfo = if ("generic" == type) GenericItemInfo(set, colorArray) else null
			this.objectInfo = if ("object" == type) ObjectItemInfo(set, colorArray) else null
			this.schematicInfo = if ("schematic" == type) SchematicItemInfo(set) else null
			this.storytellerInfo = if ("storyteller" == type) StorytellerItemInfo(set) else null
			this.weaponInfo = if ("weapon" == type) WeaponItemInfo(set) else null
			this.wearableInfo = if ("wearable" == type) WearableItemInfo(set, colorArray) else null
		}
	}
	
	class ArmorItemInfo(set: SdbResultSet, colorArray: SdbIntegerColumnArraySet) {
		
		val armorLevel: String = set.getText("armor_level")
		val armorType: ArmorType
		val protection: Int				= set.getInt("protection").toInt()
		val requiredFaction: String		= set.getText("required_faction")
		val requiredLevel: Int			= set.getInt("required_level").toInt()
		val requiredProfession: String	= set.getText("required_profession")
		val isRaceWookie: Boolean		= set.getInt("race_wookiee") != 0L
		val isRaceIthorian: Boolean		= set.getInt("race_ithorian") != 0L
		val isRaceRodian: Boolean		= set.getInt("race_rodian") != 0L
		val isRaceTrandoshan: Boolean	= set.getInt("race_trandoshan") != 0L
		val isRaceRest: Boolean			= set.getInt("race_rest") != 0L
		val isNoTrade: Boolean			= set.getInt("no_trade") != 0L
		val isBioLink: Boolean			= set.getInt("bio_link") != 0L
		val wornItemBuff: Int			= set.getInt("worn_item_buff").toInt()
		val isDeconstruct: Boolean		= set.getInt("deconstruct") != 0L
		val isSockets: Boolean			= set.getInt("sockets") != 0L
		val skillMods: Map<String, Int>	= Collections.unmodifiableMap(parseSkillMods(set.getText("skill_mods")))
		val color: IntArray				= Arrays.copyOfRange(colorArray.getArray(set), 1, 5)
			get() = field.clone()
		val value: Int = set.getInt("value").toInt()
		
		init {
			when (set.getText("armor_category")) {
				"assault" -> this.armorType = ArmorType.ASSAULT
				"battle" -> this.armorType = ArmorType.BATTLE
				"recon" -> this.armorType = ArmorType.RECON
				else -> throw IllegalArgumentException("Unsupported armor category: " + set.getText("armor_category"))
			}
		}
		
		enum class ArmorType {
			ASSAULT,
			BATTLE,
			RECON
		}
	}
	
	class CollectionItemInfo(set: SdbResultSet, colorArray: SdbIntegerColumnArraySet) {
		
		val slotName: String = set.getText("collection_slot_name")
		val color: IntArray = Arrays.copyOfRange(colorArray.getArray(set), 1, 5)
			get() = field.clone()
		
	}
	
	class ConsumableItemInfo(set: SdbResultSet) {
		
		val lifespan: Int				= set.getInt("lifespan").toInt()
		val buffName: String			= set.getText("buff_name")
		val hideBuffIdentity: Boolean	= set.getInt("hide_buff_identity") != 0L
		val cooldownGroup: String		= set.getText("cool_down_group")
		val reuseTime: Int				= set.getInt("reuse_time").toInt()
		val healingPower: Int			= set.getInt("healing_power").toInt()
		val clientEffect: String		= set.getText("client_effect")
		val clientAnimation: String		= set.getText("client_animation")
		val requiredLevel: Int			= set.getInt("required_level").toInt()
		val requiredProfession: String	= set.getText("required_profession")
		val noTrade: Boolean			= set.getInt("no_trade") != 0L
		val bioLink: Boolean			= set.getInt("bio_link") != 0L
		val charges: Int				= set.getInt("charges").toInt()
		
	}
	
	class CostumeItemInfo(set: SdbResultSet) {
		
		val buffName: String = set.getText("buff_name")
		
	}
	
	class DnaItemInfo(set: SdbResultSet)
	
	class GrantItemInfo(set: SdbResultSet) {
		
		val grantGcwFaction: String		= set.getText("grant_GCW_faction")
		val grantGcwValue: Int			= set.getInt("grant_GCW_value").toInt()
		val grantVehicle: String		= set.getText("grant_vehicle")
		val grantMount: String			= set.getText("grant_mount")
		val grantGreeter: String		= set.getText("grant_greeter")
		val grantHolopet: String		= set.getText("grant_holopet")
		val grantFamiliar: String		= set.getText("grant_familiar")
		val isNoTrade: Boolean			= set.getInt("no_trade") != 0L
		val skillMods: Map<String, Int>	= Collections.unmodifiableMap(parseSkillMods(set.getText("skill_mods")))
		val isUnique: Boolean			= set.getInt("isUnique") != 0L
		
	}
	
	class GenericItemInfo(set: SdbResultSet, colorArray: SdbIntegerColumnArraySet) {
		
		val color: IntArray	= Arrays.copyOfRange(colorArray.getArray(set), 1, 5)
			get() = field.clone()
		val value: Int				= set.getInt("value").toInt()
		val isUnique: Boolean		= set.getInt("isUnique") != 0L
		
	}
	
	class ObjectItemInfo(set: SdbResultSet, colorArray: SdbIntegerColumnArraySet) {
		
		val color: IntArray = Arrays.copyOfRange(colorArray.getArray(set), 1, 5)
			get() = field.clone()
		val value: Int = set.getInt("value").toInt()
		val isUnique: Boolean = set.getInt("isUnique") != 0L
		
	}
	
	class SchematicItemInfo(set: SdbResultSet) {
		
		val schematicId: String = set.getText("schematic_id")
		val schematicType: Int = set.getInt("schematic_type").toInt()
		val schematicUse: Int = set.getInt("schematic_use").toInt()
		val schematicSkillNeeded: String = set.getText("schematic_skill_needed")
		
	}
	
	@Suppress("UNUSED_PARAMETER")
	class StorytellerItemInfo(set: SdbResultSet)
	
	class WeaponItemInfo(set: SdbResultSet) {
		
		val minDamage: Int = set.getInt("min_damage").toInt()
		val maxDamage: Int = set.getInt("max_damage").toInt()
		val weaponCategory: String = set.getText("weapon_category")
		val weaponType: WeaponType
		val damageType: DamageType = requireNotNull(getDamageType(set.getText("damage_type"))) { "damage_type must be defined" }
		val elementalType: DamageType? = getDamageType(set.getText("elemental_type"))
		val elementalDamage: Int = set.getInt("elemental_damage").toInt()
		val attackSpeed: Double = set.getReal("attack_speed")
		val specialAttackCost: Int = set.getInt("special_attack_cost").toInt()
		val minRange: Int = set.getInt("min_range_distance").toInt()
		val maxRange: Int = set.getInt("max_range_distance").toInt()
		val procEffect: String = set.getText("proc_effect")
		val targetDps: Int = set.getInt("target_dps").toInt()
		val actualDps: Int = set.getInt("actual_dps").toInt()
		val requiredFaction: String = set.getText("required_faction")
		val requiredLevel: Int = set.getInt("required_level").toInt()
		val requiredProfession: String = set.getText("required_profession")
		val isRaceWookie: Boolean = set.getInt("race_wookiee") != 0L
		val isRaceIthorian: Boolean = set.getInt("race_ithorian") != 0L
		val isRaceRodian: Boolean = set.getInt("race_rodian") != 0L
		val isRaceTrandoshan: Boolean = set.getInt("race_trandoshan") != 0L
		val isRaceRest: Boolean = set.getInt("race_rest") != 0L
		val isNoTrade: Boolean = set.getInt("no_trade") != 0L
		val isBioLink: Boolean = set.getInt("bio_link") != 0L
		val isDeconstruct: Boolean = set.getInt("deconstruct") != 0L
		val isSockets: Boolean = set.getInt("sockets") != 0L
		val skillMods: Map<String, Int> = Collections.unmodifiableMap(parseSkillMods(set.getText("skill_mods")))
		val value: Int = set.getInt("value").toInt()
		
		init {
			when (set.getText("weapon_type")) {
				"RIFLE" -> weaponType = WeaponType.RIFLE
				"CARBINE" -> weaponType = WeaponType.CARBINE
				"PISTOL" -> weaponType = WeaponType.PISTOL
				"HEAVY" -> weaponType = WeaponType.HEAVY
				"ONE_HANDED_MELEE" -> weaponType = WeaponType.ONE_HANDED_MELEE
				"TWO_HANDED_MELEE" -> weaponType = WeaponType.TWO_HANDED_MELEE
				"UNARMED" -> weaponType = WeaponType.UNARMED
				"POLEARM_MELEE" -> weaponType = WeaponType.POLEARM_MELEE
				"THROWN" -> weaponType = WeaponType.THROWN
				"ONE_HANDED_SABER" -> weaponType = WeaponType.ONE_HANDED_SABER
				"TWO_HANDED_SABER" -> weaponType = WeaponType.TWO_HANDED_SABER
				"POLEARM_SABER" -> weaponType = WeaponType.POLEARM_SABER
				"GROUND_TARGETTING" -> weaponType = WeaponType.HEAVY_WEAPON
				"DIRECTIONAL_TARGET_WEAPON" -> weaponType = WeaponType.DIRECTIONAL_TARGET_WEAPON
				"LIGHT_RIFLE" -> weaponType = WeaponType.LIGHT_RIFLE
				else -> throw IllegalArgumentException("weapon_type is unrecognized: " + set.getText("weapon_type"))
			}// pre-NGE artifact for pre-NGE heavy weapons
		}
		
		private fun getDamageType(str: String): DamageType? {
			return when (str) {
				"", "none"		-> null
				"kinetic"		-> DamageType.KINETIC
				"energy"		-> DamageType.ENERGY
				"heat"			-> DamageType.ELEMENTAL_HEAT
				"cold"			-> DamageType.ELEMENTAL_COLD
				"acid"			-> DamageType.ELEMENTAL_ACID
				"electricity"	-> DamageType.ELEMENTAL_ELECTRICAL
				else			-> throw IllegalArgumentException("unknown damage type: $str")
			}
		}
	}
	
	class WearableItemInfo(set: SdbResultSet, colorArray: SdbIntegerColumnArraySet) {
		
		val requiredFaction: String		= set.getText("required_faction")
		val requiredLevel: Int			= set.getInt("required_level").toInt()
		val requiredProfession: String	= set.getText("required_profession")
		val isRaceWookie: Boolean		= set.getInt("race_wookiee") != 0L
		val isRaceIthorian: Boolean		= set.getInt("race_ithorian") != 0L
		val isRaceRodian: Boolean		= set.getInt("race_rodian") != 0L
		val isRaceTrandoshan: Boolean	= set.getInt("race_trandoshan") != 0L
		val isRaceRest: Boolean			= set.getInt("race_rest") != 0L
		val isNoTrade: Boolean			= set.getInt("no_trade") != 0L
		val isBioLink: Boolean			= set.getInt("bio_link") != 0L
		val wornItemBuff: Int			= set.getInt("worn_item_buff").toInt()
		val isDeconstruct: Boolean		= set.getInt("deconstruct") != 0L
		val isSockets: Boolean			= set.getInt("sockets") != 0L
		val skillMods: Map<String, Int>	= Collections.unmodifiableMap(parseSkillMods(set.getText("skill_mods")))
		val color: IntArray				= Arrays.copyOfRange(colorArray.getArray(set), 1, 5)
			get() = field.clone()
		val value: Int					= set.getInt("value").toInt()
		
	}
	
	companion object {
		
		private fun parseSkillMods(modsString: String): Map<String, Int> {
			val mods = HashMap<String, Int>()    // skillmods/statmods
			
			if (modsString.isNotEmpty()) {
				val modStrings = modsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()    // The mods strings are comma-separated
				
				for (modString in modStrings) {
					val splitValues = modString.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()    // Name and value are separated by "="
					val modName = splitValues[0]
					
					// Common statmods end with "_modified"
					// If not, it's a skillmod
					val category = if (modName.endsWith("_modified")) "cat_stat_mod_bonus" else "cat_skill_mod_bonus"
					
					mods["$category.@stat_n:$modName"] = Integer.parseInt(splitValues[1])
				}
			}
			return mods
		}
	}
	
}
