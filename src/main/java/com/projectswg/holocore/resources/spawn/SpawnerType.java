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
package com.projectswg.holocore.resources.spawn;

public enum SpawnerType {

	AREA("object/tangible/ground_spawning/shared_area_spawner.iff"),
	MINEFIELD("object/tangible/ground_spawning/shared_minefield_spawner.iff"),
	PATROL("object/tangible/ground_spawning/shared_patrol_spawner.iff"),
	RANDOM("object/tangible/ground_spawning/shared_random_spawner.iff"),
	GCW_BANNER_INVASION("object/tangible/gcw/shared_flip_banner_invasion_spawner.iff"),
	GCW_BANNER_ONPOLE("object/tangible/gcw/shared_flip_banner_onpole_spawner.iff"),
	QUEST("object/tangible/spawning/shared_quest_spawner.iff"),
	EGG("object/tangible/spawning/shared_spawn_egg.iff"),
	GCW_CLONING("object/tangible/spawning/event/shared_gcw_cloning_sickness_droid_spawner.iff"),
	GCW_CITY("object/tangible/gcw/shared_gcw_city_spawner.iff"),
	MISSION_EASY("object/tangible/mission/shared_mission_informant_spawner_easy.iff"),
	MISSION_MEDIUM("object/tangible/mission/shared_mission_informant_spawner_medium.iff"),
	MISSION_HARD("object/tangible/mission/shared_mission_informant_spawner_hard.iff"),
	ROHAK("object/tangible/quest/township/shared_rohak_figurine_spawner.iff"),
	CITY_SIGN("object/tangible/spawning/shared_city_sign_spawner.iff"),
	FS_NPC("object/tangible/spawning/shared_fs_village_npc_spawner.iff"),
	REMOTE("object/tangible/spawning/shared_remote_theater_spawner.iff"),
	GCW_IMPERIAL("object/tangible/spawning/event/shared_gcw_imperial_guard_spawner.iff"),
	GCW_REBEL("object/tangible/spawning/event/shared_gcw_rebel_guard_spawner.iff"),
	NYM_WEED("object/tangible/spawning/event/shared_nym_themepark_weed_spawner.iff"),
	OUT_ALHA("object/tangible/spawning/event/shared_outbreak_alpha_survivor_spawner.iff"),
	OUT_BETA("object/tangible/spawning/event/shared_outbreak_beta_survivor_spawner.iff"),
	OUT_DELTA("object/tangible/spawning/event/shared_outbreak_delta_survivor_spawner.iff"),
	OUT_SCIENTIST("object/tangible/spawning/event/shared_outbreak_dungeon_scientist_spawner.iff"),
	OUT_GAMMA("object/tangible/spawning/event/shared_outbreak_gamma_survivor_spawner.iff"),
	OUT_HIDDEN("object/tangible/spawning/event/shared_outbreak_hidden_content_spawner.iff"),
	WOD_HERB("object/tangible/spawning/event/shared_wod_themepark_herb_spawner.iff"),
	UNCHECKED("object/mobile/shared_bossk.iff"),
	CHECKED("object/mobile/shared_boba_fett.iff");
	
	private static final SpawnerType [] VALUES = values();
	
	private String objectTemplate;
	
	SpawnerType(String objectTemplate) {
		this.objectTemplate = objectTemplate;
	}
	
	public String getObjectTemplate() {
		return objectTemplate;
	}
	
	public static SpawnerType getSpawnerTypeFromName(String name) {
		for (SpawnerType type : VALUES) {
			if (type.name().hashCode() == name.hashCode())
				return type;
		}
		return null;
	}
	
}
