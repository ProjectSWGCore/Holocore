package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class PostureLocomotionCheck(val posture: Posture) : LocomotionCheck {
	override fun isActive(creatureObject: CreatureObject): Boolean {
		return creatureObject.posture == posture
	}
}