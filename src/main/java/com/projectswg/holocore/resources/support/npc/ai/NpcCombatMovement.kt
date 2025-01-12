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
package com.projectswg.holocore.resources.support.npc.ai

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Point3D
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class NpcCombatMovement(private val obj: AIObject, private val coroutineScope: CoroutineScope, private val npcRunSpeed: Double) {
	
	private var movingJob: Job? = null
	
	fun stopMovement() {
		movingJob?.cancel()
		movingJob = null
	}
	
	fun handleMovement(target: TangibleObject, weapon: WeaponObject, lineOfSight: Boolean) {
		if (obj.walkSpeed <= 0 || npcRunSpeed <= 0) return // Not really applicable in this case
		
		val targetDistance = target.worldLocation.distanceTo(obj.worldLocation)
		val attackRange = weapon.maxRange.toDouble()
		val reasonableDistance = (Random.nextDouble() - 0.5) * .4 * attackRange + attackRange / 3
		val comfortableDistance = when (weapon.type) {
			WeaponType.RIFLE, WeaponType.CARBINE, WeaponType.PISTOL, WeaponType.HEAVY -> 15.0
			else                                                                      -> 2.0
		}
		
		if (targetDistance < 2 || targetDistance > attackRange || !lineOfSight) {
			val intendedDistance = min(attackRange - 1, max(comfortableDistance, reasonableDistance))
			if (movingJob == null) {
				moveIntoRange(target, intendedDistance)
			}
		} else {
			movingJob?.cancel()
			movingJob = null
		}
	}
	
	private fun moveIntoRange(target: TangibleObject, intendedDistance: Double) {
		val targetLocation = target.location
		val targetHeading = target.location.yaw + ThreadLocalRandom.current().nextDouble(-75.0, 75.0)
		val moveX = targetLocation.x + sin(Math.toRadians(targetHeading)) * intendedDistance
		val moveZ = targetLocation.z + cos(Math.toRadians(targetHeading)) * intendedDistance
		val moveHeading = targetLocation.getHeadingTo(Point3D(moveX, targetLocation.y, moveZ)) + 180
		val moveLocation = Location.builder(targetLocation).setX(moveX).setZ(moveZ).setHeading(moveHeading).build()
		movingJob?.cancel()
		movingJob = coroutineScope.launch {
			val route = NavigationPoint.from(obj.parent, obj.location, target.effectiveParent, moveLocation, npcRunSpeed)
			for (point in route) {
				delay(1000L)
				point.move(obj)
			}
		}
	}
	
}
