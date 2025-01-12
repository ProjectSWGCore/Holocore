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
package com.projectswg.holocore.headless

import java.util.concurrent.TimeUnit

fun ZonedInCharacter.waitForExperiencePoints(xpType: String) {
	val originalXP = getXP(xpType)
	player.waitForNextObjectDelta(player.playerObject.objectId, 8, 0, 1, TimeUnit.SECONDS) ?: throw IllegalStateException("No XP delta received")
	val newXP = getXP(xpType)
	
	if (newXP == originalXP) {
		// We may have received a delta for a different XP type. Try again.
		// The exception above will be thrown if we never get the delta we're waiting for, preventing an infinite loop.
		waitForExperiencePoints(xpType) 
	}
}

private fun ZonedInCharacter.getXP(xpType: String) = player.playerObject.getExperiencePoints(xpType)