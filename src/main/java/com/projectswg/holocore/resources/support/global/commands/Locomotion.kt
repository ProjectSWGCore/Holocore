package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

enum class Locomotion(val locomotionTableId: Int, val commandSdbColumnName: String, val posture: Posture) {
	STANDING(0, "L:standing", Posture.UPRIGHT),
	KNEELING(4, "L:kneeling", Posture.CROUCHED),
	PRONE(7, "L:prone", Posture.PRONE),
	SITTING(14, "L:sitting", Posture.SITTING),
	SKILL_ANIMATING(15, "L:skillAnimating", Posture.SKILL_ANIMATING),
	DRIVING_VEHICLE(16, "L:drivingVehicle", Posture.DRIVING_VEHICLE),
	RIDING_CREATURE(17, "L:ridingCreature", Posture.RIDING_CREATURE),
	KNOCKED_DOWN(18, "L:knockedDown", Posture.KNOCKED_DOWN),
	INCAPACITATED(19, "L:incapacitated", Posture.INCAPACITATED),
	DEAD(20, "L:dead", Posture.DEAD);

	fun isActive(creatureObject: CreatureObject): Boolean {
		return creatureObject.posture == posture
	}
}
