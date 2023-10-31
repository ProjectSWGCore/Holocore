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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import java.io.File

class SchematicGroupLoader : DataLoader() {

	private val schematicGroupMap = mutableMapOf<String, MutableCollection<String>>()

	/**
	 * Returns a collection of all schematic names in the given group
	 * Example: getSchematicsInGroup("craftDroidDamageRepairA") returns a collection of ["object/draft_schematic/droid/droid_damage_repair_kit_a.iff"]
	 */
	fun getSchematicsInGroup(groupId: String): Collection<String> {
		return schematicGroupMap.getOrElse(groupId) { emptyList() }
	}

	override fun load() {
		val set = SdbLoader.load(File("serverdata/crafting/schematic_group.sdb"))

		set.use {
			while (set.next()) {
				val groupid = set.getText("groupid")
				val schematicname = set.getText("schematicname")
				
				if (groupid != "end") {
					ensureSchematicGroupExists(groupid)
					appendSchematicToGroup(groupid, schematicname)
				}
			}
		}
	}

	private fun appendSchematicToGroup(groupid: String, schematicname: String) {
		schematicGroupMap[groupid]?.add(schematicname)
	}

	private fun ensureSchematicGroupExists(groupid: String) {
		if (!schematicGroupMap.containsKey(groupid)) {
			schematicGroupMap[groupid] = mutableListOf()
		}
	}
}
