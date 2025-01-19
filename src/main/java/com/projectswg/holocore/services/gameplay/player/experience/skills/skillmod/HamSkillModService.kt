/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod

import com.projectswg.holocore.intents.gameplay.player.experience.SkillModIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

/**
 * This service is responsible for modifying the health, action, and mind (HAM) of creatures based on skill mods.
 * An example of a skill mod is a health mod that increases the health of a creature by a percentage (like healthPercent).
 */
class HamSkillModService : Service() {
	@IntentHandler
	private fun handleSkillModIntent(intent: SkillModIntent) {
		val skillModName = intent.skillModName
		val adjustModifier = intent.adjustModifier

		when (skillModName) {
			"healthPercent" -> handleHealthPercent(intent, adjustModifier)
		}
	}

	private fun handleHealthPercent(intent: SkillModIntent, adjustModifier: Int) {
		for (creature in intent.affectedCreatures) {
			val originalMaxHealth = creature.maxHealth
			val newMaxHealth = HealthPercentCalculator.calculateNewMaxHealth(originalMaxHealth, adjustModifier)
			creature.maxHealth = newMaxHealth

			StandardLog.onPlayerTrace(this, creature, "max health increased by $adjustModifier%% $originalMaxHealth -> $newMaxHealth")	// %% is used to escape the % character
		}
	}
}