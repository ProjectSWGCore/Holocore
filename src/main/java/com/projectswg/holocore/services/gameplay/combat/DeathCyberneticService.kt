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
package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.intents.gameplay.combat.CloneActivatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.random.Die
import com.projectswg.holocore.resources.support.random.RandomDie
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class DeathCyberneticService(private val deathCyberneticDie: Die = RandomDie()) : Service() {

	@IntentHandler
	private fun handleCloneActivatedIntent(intent: CloneActivatedIntent) {
		val creature = intent.creature
		val diedOnTerrain = intent.diedOnTerrain

		if (isSpace(diedOnTerrain)) return
		if (isEasyPlanet(diedOnTerrain)) return
		if (isADeathCyberneticAlreadyInstalled(creature)) return
		if (deathCyberneticDie.roll(1..100) < 5) installDeathCybernetic(creature)
	}

	private fun installDeathCybernetic(creature: CreatureObject) {
		val iffTemplate = selectDeathCyberneticLimb()

		val cybernetic = ObjectCreator.createObjectFromTemplate(iffTemplate)
		cybernetic.moveToContainer(creature)

		StandardLog.onPlayerEvent(this, creature, "has been given a cybernetic limb (%s) upon death", iffTemplate)
	}

	private fun selectDeathCyberneticLimb(): String {
		return if (deathCyberneticDie.roll(1..100) < 25) {
			"object/tangible/wearables/cybernetic/s01/cybernetic_s01_legs.iff"
		} else {
			if (deathCyberneticDie.roll(1..100) < 50) {
				"object/tangible/wearables/cybernetic/s01/shared_cybernetic_s01_arm_r.iff"
			} else {
				"object/tangible/wearables/cybernetic/s01/shared_cybernetic_s01_arm_l.iff"
			}
		}
	}

	private fun isADeathCyberneticAlreadyInstalled(creature: CreatureObject): Boolean {
		val equippedItems = creature.slottedObjects

		return equippedItems.map { it.template }.any { it.startsWith("object/tangible/wearables/cybernetic/s01") }
	}

	private fun isSpace(diedOnTerrain: Terrain): Boolean {
		return diedOnTerrain.name.lowercase().startsWith("space_")
	}

	private fun isEasyPlanet(terrain: Terrain): Boolean {
		val easyPlanets = setOf(Terrain.RORI, Terrain.NABOO, Terrain.TATOOINE, Terrain.CORELLIA, Terrain.TALUS)

		return terrain in easyPlanets
	}
}