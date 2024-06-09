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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

class ObjectDataLoader internal constructor() : DataLoader() {
	private val attributes: MutableMap<String, Map<ObjectDataAttribute, Any?>> = HashMap()

	fun getAttributes(iff: String): Map<ObjectDataAttribute, Any?>? = attributes[iff]

	val objects: Collection<String>
		get() = Collections.unmodifiableCollection(attributes.keys)

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/objects/object_data.sdb")).use { set ->
			val mapping = ObjectDataAttribute.entries.toTypedArray()
			val columns = set.columns
			for (i in 1 until columns.size) {
				var j = 0
				for (attr in mapping) {
					if (attr.getName() == columns[i]) break
					j++
				}
				if (j >= mapping.size) Log.e("ObjectDataLoader could not find " + columns[0])
				val tmp = mapping[j]
				mapping[j] = mapping[i]
				mapping[i] = tmp
			}
			attributes.putAll(set.parallelStream { s: SdbResultSet -> parse(s, mapping) }.collect(Collectors.toMap ( { it.key }, { it.value })))
		}
	}

	companion object {
		private fun parse(set: SdbResultSet, mapping: Array<ObjectDataAttribute>): Map.Entry<String, Map<ObjectDataAttribute, Any?>> {
			val objectAttributes: MutableMap<ObjectDataAttribute, Any?> = EnumMap(ObjectDataAttribute::class.java)
			for (i in 1 until mapping.size) {
				val attr = mapping[i]
				objectAttributes[attr] = parse(attr, set, i)
			}
			return java.util.Map.entry<String, Map<ObjectDataAttribute, Any?>>(set.getText(0), objectAttributes)
		}

		private fun parse(attribute: ObjectDataAttribute, set: SdbResultSet, index: Int): Any? {
			when (attribute) {
				ObjectDataAttribute.ANIMATION_MAP_FILENAME, ObjectDataAttribute.APPEARANCE_FILENAME, ObjectDataAttribute.ARRANGEMENT_DESCRIPTOR_FILENAME, ObjectDataAttribute.CLIENT_DATA_FILE, ObjectDataAttribute.COCKPIT_FILENAME, ObjectDataAttribute.CRAFTED_SHARED_TEMPLATE, ObjectDataAttribute.INTERIOR_LAYOUT_FILENAME, ObjectDataAttribute.LOOK_AT_TEXT, ObjectDataAttribute.MOVEMENT_DATATABLE, ObjectDataAttribute.PORTAL_LAYOUT_FILENAME, ObjectDataAttribute.SLOT_DESCRIPTOR_FILENAME, ObjectDataAttribute.STRUCTURE_FOOTPRINT_FILENAME, ObjectDataAttribute.TERRAIN_MODIFICATION_FILENAME, ObjectDataAttribute.TINT_PALETTE, ObjectDataAttribute.WEAPON_EFFECT                                                                                                               -> return set.getText(index)
				ObjectDataAttribute.ATTACK_TYPE, ObjectDataAttribute.COLLISION_MATERIAL_BLOCK_FLAGS, ObjectDataAttribute.COLLISION_MATERIAL_FLAGS, ObjectDataAttribute.COLLISION_MATERIAL_PASS_FLAGS, ObjectDataAttribute.COLLISION_ACTION_BLOCK_FLAGS, ObjectDataAttribute.COLLISION_ACTION_FLAGS, ObjectDataAttribute.COLLISION_ACTION_PASS_FLAGS, ObjectDataAttribute.CONTAINER_TYPE, ObjectDataAttribute.CONTAINER_VOLUME_LIMIT, ObjectDataAttribute.GAME_OBJECT_TYPE, ObjectDataAttribute.GENDER, ObjectDataAttribute.NICHE, ObjectDataAttribute.RACE, ObjectDataAttribute.SPECIES, ObjectDataAttribute.SURFACE_TYPE, ObjectDataAttribute.WEAPON_EFFECT_INDEX                                                                                                                          -> return set.getInt(index)
				ObjectDataAttribute.ACCELERATION, ObjectDataAttribute.CAMERA_HEIGHT, ObjectDataAttribute.CLEAR_FLORA_RADIUS, ObjectDataAttribute.COLLISION_HEIGHT, ObjectDataAttribute.COLLISION_LENGTH, ObjectDataAttribute.COLLISION_OFFSET_X, ObjectDataAttribute.COLLISION_OFFSET_Z, ObjectDataAttribute.COLLISION_RADIUS, ObjectDataAttribute.LOCATION_RESERVATION_RADIUS, ObjectDataAttribute.NO_BUILD_RADIUS, ObjectDataAttribute.SCALE, ObjectDataAttribute.SCALE_THRESHOLD_BEFORE_EXTENT_TEST, ObjectDataAttribute.SLOPE_MOD_ANGLE, ObjectDataAttribute.SLOPE_MOD_PERCENT, ObjectDataAttribute.SPEED, ObjectDataAttribute.STEP_HEIGHT, ObjectDataAttribute.SWIM_HEIGHT, ObjectDataAttribute.TURN_RADIUS, ObjectDataAttribute.WARP_TOLERANCE, ObjectDataAttribute.WATER_MOD_PERCENT -> return set.getReal(index)
				ObjectDataAttribute.CLIENT_VISIBILITY_FLAG, ObjectDataAttribute.FORCE_NO_COLLISION, ObjectDataAttribute.HAS_WINGS, ObjectDataAttribute.ONLY_VISIBLE_IN_TOOLS, ObjectDataAttribute.PLAYER_CONTROLLED, ObjectDataAttribute.POSTURE_ALIGN_TO_TERRAIN, ObjectDataAttribute.SEND_TO_CLIENT, ObjectDataAttribute.SNAP_TO_TERRAIN, ObjectDataAttribute.TARGETABLE, ObjectDataAttribute.USE_STRUCTURE_FOOTPRINT_OUTLINE                                                                                                                                                                                                                                                                                                                                                             -> return set.getBoolean(index)
				ObjectDataAttribute.ATTRIBUTES, ObjectDataAttribute.CONST_STRING_CUSTOMIZATION_VARIABLES, ObjectDataAttribute.CUSTOMIZATION_VARIABLE_MAPPING, ObjectDataAttribute.PALETTE_COLOR_CUSTOMIZATION_VARIABLES, ObjectDataAttribute.RANGED_INT_CUSTOMIZATION_VARIABLES, ObjectDataAttribute.SLOTS                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  ->                // MAP
					return set.getText(index)

				ObjectDataAttribute.CERTIFICATIONS_REQUIRED, ObjectDataAttribute.SOCKET_DESTINATIONS                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        ->                // LIST
					return set.getText(index)

				ObjectDataAttribute.DETAILED_DESCRIPTION, ObjectDataAttribute.OBJECT_NAME                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   -> {
					val stf = set.getText(index)
					if (stf.isEmpty()) return StringId()
					return StringId(stf)
				}

				ObjectDataAttribute.HOLOCORE_BASELINE_TYPE                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  -> {
					val type = set.getText(index)
					return if (type.isEmpty()) null else BaselineType.valueOf(type)
				}

				else                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        -> throw RuntimeException("Unknown attribute: $attribute")
			}
		}
	}
}
