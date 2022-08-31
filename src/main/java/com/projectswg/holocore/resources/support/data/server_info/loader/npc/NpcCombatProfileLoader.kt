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

package com.projectswg.holocore.resources.support.data.server_info.loader.npc

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty

import java.io.File
import java.io.IOException
import java.util.*

import java.util.stream.Collectors.groupingBy

class NpcCombatProfileLoader : DataLoader() {
	
	private val profiles: MutableMap<String, List<CombatProfile>> = HashMap()
	
	fun getAbilities(id: String?, difficulty: CreatureDifficulty, level: Int): Set<String> {
		val subprofiles = Objects.requireNonNull<List<CombatProfile>>(profiles[id], "unknown profile id")
		val abilities = HashSet<String>()
		for (profile in subprofiles) {
			if (profile.difficulty == difficulty && level >= profile.minLevel && level <= profile.maxLevel)
				abilities.addAll(profile.abilities)
		}
		return abilities
	}
	
	@Throws(IOException::class)
	public override fun load() {
		SdbLoader.load(File("serverdata/spawn/static.msdb")).use { set -> profiles.putAll(set.stream { CombatProfile(it) }.collect(groupingBy { it.id })) }
	}
	
	private class CombatProfile(set: SdbResultSet) {
		
		val id  = set.getText("profile_id")!!
		val difficulty = CreatureDifficulty.valueOf(set.getText("difficulty"))
		val minLevel = set.getInt("min_cl").toInt()
		val maxLevel = set.getInt("max_cl").toInt()
		val abilities = setOf(*set.getText("action").split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
		
	}
	
}
