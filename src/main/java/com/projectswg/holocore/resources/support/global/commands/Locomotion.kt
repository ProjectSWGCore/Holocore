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
	DRIVING_VEHICLE(16, "L:drivingVehicle", PostureLocomotionCheck(Posture.DRIVING_VEHICLE)),
	RIDING_CREATURE(17, "L:ridingCreature", PostureLocomotionCheck(Posture.RIDING_CREATURE)),
	KNOCKED_DOWN(18, "L:knockedDown", PostureLocomotionCheck(Posture.KNOCKED_DOWN)),
	INCAPACITATED(19, "L:incapacitated", PostureLocomotionCheck(Posture.INCAPACITATED)),
	DEAD(20, "L:dead", PostureLocomotionCheck(Posture.DEAD));

	fun isActive(creatureObject: CreatureObject): Boolean {
		return locomotionCheck.isActive(creatureObject)
	}
}
