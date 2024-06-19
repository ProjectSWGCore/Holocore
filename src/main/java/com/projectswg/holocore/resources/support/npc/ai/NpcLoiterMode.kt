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
package com.projectswg.holocore.resources.support.npc.ai

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.terrains
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * AI object that loiters the area
 */
class NpcLoiterMode(obj: AIObject, private val radius: Double) : NpcMode(obj) {
	private var mainLocation: Location? = null

	override fun onModeStart() {
		val currentLocation = ai.location
		if (mainLocation == null) mainLocation = currentLocation
		if (mainLocation!!.distanceTo(currentLocation) >= 1) runTo(mainLocation)
	}

	override fun act() {
		val random: Random = ThreadLocalRandom.current()
		if (isRooted) {
			queueNextLoop((10000 + random.nextInt(5)).toLong())
			return
		}
		val currentLocation = mainLocation

		if (random.nextDouble() > 0.25) { // Only a 25% movement chance
			queueNextLoop((10000 + random.nextInt(5)).toLong())
			return
		}
		val dist = sqrt(radius)
		var theta: Double
		val l = Location.builder(currentLocation)
		do {
			theta = random.nextDouble() * Math.PI * 2
			l.setX(currentLocation!!.x + cos(theta) * dist)
			l.setZ(currentLocation.z + sin(theta) * dist)
			l.setY(terrains().getHeight(l))
		} while (!l.isWithinFlatDistance(mainLocation, radius))
		l.setHeading(l.yaw - Math.toDegrees(theta))

		moveTo(ai.parent, l.build())
		queueNextLoop((30 + random.nextInt(3)).toLong())
	}
}
