/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.resources.gameplay.entertainment

/**
 * A special effect a performer can drop into a performance.
 */
enum class PerformEffect(private val datatableName: String, private val fileName: String, private val stfName: String, val levelled: Boolean) {
	DAZZLE("Dazzle", "dazzle", "dazzle", true),
	SPOT_LIGHT("SpotLight", "spot_light", "spot_light", true),
	COLOR_LIGHTS("ColorLights", "color_lights", "color_lights", true),
	DISTRACT("Distract", "distract", "distract", true),
	SMOKE_BOMB("SmokeBomb", "smoke_bomb", "smoke_bomb", true),
	FIRE_JETS("FireJets", "fire_jets", "fire_jets", true),
	VENTRILOQUISM("Ventriloquism", "ventriloquism", "ventriloquism", true),
	COLOR_SWIRL("ColorSwirl", "color_swirl", "color_swirl", false),
	CENTER_STAGE("CenterStage", "center_stage", "center_stage", false),
	DANCE_FLOOR("DanceFloor", "dance_floor", "dance_floor", false),
	LASER_SHOW("LaserShow", "laser_show", "laser_show", false),
	FIRE_JETS_2("FireJetsB", "fire_jets2", "fire_jets_2", false),
	FEATURED_SOLO("FeaturedSolo", "featured_solo", "featured_solo", false);

	fun datatableName(level: Int): String = if (levelled) "$datatableName$level" else datatableName

	fun effectFile(level: Int): String = if (levelled) "clienteffect/entertainer_${fileName}_level_$level.cef" else "clienteffect/entertainer_$fileName.cef"

	val systemMessage: String
		get() = "@performance:effect_perform_$stfName"
}
