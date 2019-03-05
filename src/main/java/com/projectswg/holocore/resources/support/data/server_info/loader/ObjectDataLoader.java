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
 * Holocore is free software:
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
 * along with Holocore.  If not, see <http:
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public final class ObjectDataLoader extends DataLoader {
	
	private final Map<String, Map<ObjectDataAttribute, Object>> attributes;
	
	ObjectDataLoader() {
		this.attributes = new HashMap<>();
	}
	
	public Map<ObjectDataAttribute, Object> getAttributes(String iff) {
		return attributes.get(iff);
	}
	
	public Collection<String> getObjects() {
		return Collections.unmodifiableCollection(attributes.keySet());
	}
	
	public static void main(String [] args) {
		// 721.813107	123.546451
		// 415.357121	55.732692
		long [] lastExecutions = new long[5];
		for (int i = 0; i < 30; i++) {
			ObjectDataLoader loader = new ObjectDataLoader();
			long start = System.nanoTime();
			try {
				loader.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
			long end = System.nanoTime();
			lastExecutions[i % lastExecutions.length] = end - start;
			System.out.printf("%d,%.6f%n", i, (end-start)/1E6);
		}
		System.out.printf("AVG,%.6f%n", LongStream.of(lastExecutions).average().orElseThrow() / 1E6);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/objects/object_data.sdb"))) {
			ObjectDataAttribute [] mapping = ObjectDataAttribute.values();
			List<String> columns = set.getColumns();
			for (int i = 1; i < columns.size(); i++) {
				int j = 0;
				for (ObjectDataAttribute attr : mapping) {
					if (attr.getName().equals(columns.get(i)))
						break;
					j++;
				}
				if (j >= mapping.length)
					Log.e("ObjectDataLoader could not find " + columns.get(0));
				ObjectDataAttribute tmp = mapping[j];
				mapping[j] = mapping[i];
				mapping[i] = tmp;
			}
			attributes.putAll(set.stream(s -> {
				Map<ObjectDataAttribute, Object> objectAttributes = new EnumMap<>(ObjectDataAttribute.class);
				for (int i = 1; i < mapping.length; i++) {
					ObjectDataAttribute attr = mapping[i];
					objectAttributes.put(attr, parse(attr, s, i));
				}
				return Map.entry(s.getText(0), objectAttributes);
			}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
//			while (set.next()) {
//				Map<ObjectDataAttribute, Object> objectAttributes = new EnumMap<>(ObjectDataAttribute.class);
//				for (int i = 1; i < mapping.length; i++) {
//					ObjectDataAttribute attr = mapping[i];
//					objectAttributes.put(attr, parse(attr, set, i));
//				}
//				attributes.put(set.getText(0), objectAttributes);
//			}
		}
	}
	
	private static Object parse(ObjectDataAttribute attribute, SdbResultSet set, int index) {
		switch (attribute) {
			case ANIMATION_MAP_FILENAME:
			case APPEARANCE_FILENAME:
			case ARRANGEMENT_DESCRIPTOR_FILENAME:
			case CLIENT_DATA_FILE:
			case COCKPIT_FILENAME:
			case CRAFTED_SHARED_TEMPLATE:
			case INTERIOR_LAYOUT_FILENAME:
			case LOOK_AT_TEXT:
			case MOVEMENT_DATATABLE:
			case PORTAL_LAYOUT_FILENAME:
			case SLOT_DESCRIPTOR_FILENAME:
			case STRUCTURE_FOOTPRINT_FILENAME:
			case TERRAIN_MODIFICATION_FILENAME:
			case TINT_PALETTE:
			case WEAPON_EFFECT:
				return set.getText(index);
			case ATTACK_TYPE:
			case COLLISION_MATERIAL_BLOCK_FLAGS:
			case COLLISION_MATERIAL_FLAGS:
			case COLLISION_MATERIAL_PASS_FLAGS:
			case COLLISION_ACTION_BLOCK_FLAGS:
			case COLLISION_ACTION_FLAGS:
			case COLLISION_ACTION_PASS_FLAGS:
			case CONTAINER_TYPE:
			case CONTAINER_VOLUME_LIMIT:
			case GAME_OBJECT_TYPE:
			case GENDER:
			case NICHE:
			case RACE:
			case SPECIES:
			case SURFACE_TYPE:
			case WEAPON_EFFECT_INDEX:
				return set.getInt(index);
			case ACCELERATION:
			case CAMERA_HEIGHT:
			case CLEAR_FLORA_RADIUS:
			case COLLISION_HEIGHT:
			case COLLISION_LENGTH:
			case COLLISION_OFFSET_X:
			case COLLISION_OFFSET_Z:
			case COLLISION_RADIUS:
			case LOCATION_RESERVATION_RADIUS:
			case NO_BUILD_RADIUS:
			case SCALE:
			case SCALE_THRESHOLD_BEFORE_EXTENT_TEST:
			case SLOPE_MOD_ANGLE:
			case SLOPE_MOD_PERCENT:
			case SPEED:
			case STEP_HEIGHT:
			case SWIM_HEIGHT:
			case TURN_RADIUS:
			case WARP_TOLERANCE:
			case WATER_MOD_PERCENT:
				return set.getReal(index);
			case CLIENT_VISIBILITY_FLAG:
			case FORCE_NO_COLLISION:
			case HAS_WINGS:
			case ONLY_VISIBLE_IN_TOOLS:
			case PLAYER_CONTROLLED:
			case POSTURE_ALIGN_TO_TERRAIN:
			case SEND_TO_CLIENT:
			case SNAP_TO_TERRAIN:
			case TARGETABLE:
			case USE_STRUCTURE_FOOTPRINT_OUTLINE:
				return set.getBoolean(index);
			case ATTRIBUTES:
			case CONST_STRING_CUSTOMIZATION_VARIABLES:
			case CUSTOMIZATION_VARIABLE_MAPPING:
			case PALETTE_COLOR_CUSTOMIZATION_VARIABLES:
			case RANGED_INT_CUSTOMIZATION_VARIABLES:
			case SLOTS:
				// MAP
				return set.getText(index);
			case CERTIFICATIONS_REQUIRED:
			case SOCKET_DESTINATIONS:
				// LIST
				return set.getText(index);
			case DETAILED_DESCRIPTION:
			case OBJECT_NAME: {
				String stf = set.getText(index);
				if (stf.isEmpty())
					return new StringId();
				return new StringId(stf);
			}
			case HOLOCORE_BASELINE_TYPE: {
				String type = set.getText(index);
				return type == null || type.isEmpty() ? null : BaselineType.valueOf(type);
			}
			default:
				throw new RuntimeException("Unknown attribute: " + attribute);
		}
	}
	
}
