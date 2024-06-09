/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader.npc

import com.projectswg.common.data.encodables.tangible.PvpFaction
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.data.server_info.loader.*
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.factions
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader.Faction
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.IOException
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

class NpcLoader : DataLoader() {
	private val _npc: MutableMap<String, NpcInfo> = HashMap()
	val npcs: Collection<NpcInfo>
		get() = Collections.unmodifiableCollection(_npc.values)

	operator fun get(id: String): NpcInfo? {
		return _npc[id]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/npc/npc.msdb")).use { set ->
			_npc.putAll(set.parallelStream { NpcInfo(it) }.collect(Collectors.toMap({ obj: NpcInfo -> obj.id }, Function.identity())))
		}
	}

	class NpcInfo(set: SdbResultSet) {
		/*
			* means unimplemented
			npc_id						TEXT
			spawnerFlag					TEXT
			difficulty					TEXT
			combat_level				INTEGER
			npc_name					TEXT
			stf_name					TEXT
			niche						TEXT
			iff_template				TEXT
			planet					*	TEXT
			offers_mission			*	TEXT
			social_group				TEXT
			faction						TEXT
			spec_force					TEXT
			scale_min				*	REAL
			scale_max				*	REAL
			hue						*	INTEGER
			grant_skill				*	TEXT
			ignore_player			*	TEXT
			attack_speed				REAL
			movement_speed				REAL
			default_weapon				TEXT
			default_weapon_specials	*	TEXT
			thrown_weapon				TEXT
			thrown_weapon_specials	*	TEXT
			aggressive_radius			INTEGER
			assist_radius				INTEGER
			stalker					*	TEXT
			herd					*	TEXT
			death_blow					BOOLEAN
			skillmods				*	TEXT
			loot_table1_chance			INTEGER
			loot_table1					TEXT
			loot_table2_chance			INTEGER
			loot_table2					TEXT
			loot_table3_chance			INTEGER
			loot_table3					TEXT
			chronicle_loot_chance	*	INTEGER
			chronicle_loot_category	*	TEXT
		 */
		val id: String = set.getText("npc_id")
		val name: String = set.getText("npc_name").intern()
		val stfName: String = set.getText("stf_name")
		private val niche = set.getText("niche").intern()
		val faction: Faction
		val isSpecForce: Boolean = set.getBoolean("spec_force")
		val attackSpeed: Double = set.getReal("attack_speed")
		val movementSpeed: Double = set.getReal("movement_speed")
		val hue: Int = set.getInt("hue").toInt()
		val scaleMin: Double = set.getReal("scale_min")
		val scaleMax: Double = set.getReal("scale_max")
		val defaultWeaponSpecials: String = set.getText("default_weapon_specials")
		val thrownWeaponSpecials: String = set.getText("thrown_weapon_specials")
		val aggressiveRadius: Int = set.getInt("aggressive_radius").toInt()
		val assistRadius: Int = set.getInt("assist_radius").toInt()
		val isDeathblow: Boolean = set.getBoolean("death_blow")
		val lootTable1: String = set.getText("loot_table1")
		val lootTable2: String = set.getText("loot_table2")
		val lootTable3: String = set.getText("loot_table3")
		val lootTable1Chance: Int = set.getInt("loot_table1_chance").toInt()
		val lootTable2Chance: Int = set.getInt("loot_table2_chance").toInt()
		val lootTable3Chance: Int = set.getInt("loot_table3_chance").toInt()

		val iffs: List<String> = listOf(*set.getText("iff_template").split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).stream().map { s: String -> "object/mobile/$s" }.map { original: String? -> ClientFactory.formatToSharedFile(original) }.collect(Collectors.toUnmodifiableList())
		val defaultWeapon: List<String> = parseWeapons(set.getText("default_weapon"))
		val thrownWeapon: List<String> = parseWeapons(set.getText("thrown_weapon"))
		var humanoidInfo: HumanoidNpcInfo? = null
		var droidInfo: DroidNpcInfo? = null
		var creatureInfo: CreatureNpcInfo? = null
		val isResources: Boolean = set.getBoolean("resources")
		val milkResourceInfo = NpcResourceInfo(set, "milk")
		val meatResourceInfo = NpcResourceInfo(set, "meat")
		val hideResourceInfo = NpcResourceInfo(set, "hide")
		val boneResourceInfo = NpcResourceInfo(set, "bone")
		var socialGroup: String? = set.getText("social_group").let { if (it == "-") null else it }

		init {
			val factionString = set.getText("faction")
			var faction = factions.getFaction(factionString)
			if (faction == null) {
				if (factionString != "-")
					Log.w("Unknown faction: %s", factionString)
				faction = DEFAULT_FACTION
			}
			this.faction = faction

			require(!(scaleMax < scaleMin)) { "scaleMax must be greater than scaleMin" }

			when (niche) {
				"humanoid" -> {
					this.humanoidInfo = HumanoidNpcInfo(set)
					this.droidInfo = null
					this.creatureInfo = null
				}

				"droid"    -> {
					this.humanoidInfo = null
					this.droidInfo = DroidNpcInfo(set)
					this.creatureInfo = null
				}

				"creature" -> {
					this.humanoidInfo = null
					this.droidInfo = null
					this.creatureInfo = CreatureNpcInfo(set)
				}

				"vehicle"  -> {
					this.humanoidInfo = null
					this.droidInfo = null
					this.creatureInfo = null
				}

				else       -> throw IllegalArgumentException("Unknown NPC niche: $niche")
			}
		}

		companion object {
			private val DEFAULT_FACTION: Faction = factions.getFaction(PvpFaction.NEUTRAL.name.lowercase())!!

			private fun parseWeapons(str: String): List<String> {
				if (str.isEmpty() || str == "-") return listOf()
				return listOf(*str.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
			}
		}
	}

	class HumanoidNpcInfo(set: SdbResultSet)

	class DroidNpcInfo(set: SdbResultSet)

	class CreatureNpcInfo(set: SdbResultSet)

	class NpcResourceInfo(set: SdbResultSet, prefix: String) {
		val amount: Int = set.getInt(prefix + "_" + "amount").toInt()
		val type: String = set.getText(prefix + "_" + "type")
	}
}
