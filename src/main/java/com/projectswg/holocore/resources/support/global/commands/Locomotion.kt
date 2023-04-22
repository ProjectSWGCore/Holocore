/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

enum class Locomotion(val locomotionTableId: Int, val commandSdbColumnName: String, private val locomotionCheck: LocomotionCheck) {
	STANDING(0, "L:standing", PostureLocomotionCheck(Posture.UPRIGHT)),
	RUNNING(3, "L:running", MovementLocomotionCheck()),
	KNEELING(4, "L:kneeling", PostureLocomotionCheck(Posture.CROUCHED)),
	PRONE(7, "L:prone", PostureLocomotionCheck(Posture.PRONE)),
	SITTING(14, "L:sitting", PostureLocomotionCheck(Posture.SITTING)),
	SKILL_ANIMATING(15, "L:skillAnimating", PostureLocomotionCheck(Posture.SKILL_ANIMATING)),
	DRIVING_VEHICLE(16, "L:drivingVehicle", DrivingVehicleLocomotionCheck()),
	RIDING_CREATURE(17, "L:ridingCreature", PostureLocomotionCheck(Posture.RIDING_CREATURE)),
	KNOCKED_DOWN(18, "L:knockedDown", PostureLocomotionCheck(Posture.KNOCKED_DOWN)),
	INCAPACITATED(19, "L:incapacitated", PostureLocomotionCheck(Posture.INCAPACITATED)),
	DEAD(20, "L:dead", PostureLocomotionCheck(Posture.DEAD));

	fun isActive(creatureObject: CreatureObject): Boolean {
		return locomotionCheck.isActive(creatureObject)
	}
}
