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

import com.projectswg.common.encoding.StringType
import com.projectswg.holocore.resources.support.data.collections.SWGMap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

fun ZonedInCharacter.waitForExperiencePoints(xpType: String) {
	@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // We want a Java boxed int for now
	val experienceMap = SWGMap<String, Integer>(8, 0, StringType.ASCII)
	while (true) { // Quits naturally when the packet timeout is exceeded
		val experienceDelta = player.waitForNextObjectDelta(player.playerObject.objectId, 8, 0, 50, TimeUnit.MILLISECONDS) ?: throw IllegalStateException("No XP delta received")
		experienceMap.decode(ByteBuffer.wrap(experienceDelta.deltaData).order(ByteOrder.LITTLE_ENDIAN), StringType.ASCII, Integer::class.java)
		if (experienceMap.containsKey(xpType))
			break
	}
}

fun ZonedInCharacter.getXP(xpType: String) = player.playerObject.getExperiencePoints(xpType)
