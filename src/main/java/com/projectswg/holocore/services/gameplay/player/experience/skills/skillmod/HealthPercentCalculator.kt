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

import kotlin.math.abs
import kotlin.math.round

object HealthPercentCalculator {

	fun calculateNewMaxHealth(currentMaxHealth: Int, newHealthPercentMod: Int): Int {
		val multiplier = calculateMultiplier(newHealthPercentMod)
		val newMaxHealth = round(multiplier * currentMaxHealth).toInt()    // If healthPercent is 10, then mod is 1.1. A creature with 1000 health will then have 1100 health.
		
		return newMaxHealth
	}

	private fun calculateMultiplier(adjustModifier: Int): Double {
		return if (adjustModifier < 0) {
			removalMultiplier(adjustModifier)
		} else {
			additionMultiplier(adjustModifier)
		}
	}
	
	private fun removalMultiplier(percent: Int) = 1.0 / additionMultiplier(abs(percent))
	private fun additionMultiplier(percent: Int) = 1.0 + percent / 100.0
}
