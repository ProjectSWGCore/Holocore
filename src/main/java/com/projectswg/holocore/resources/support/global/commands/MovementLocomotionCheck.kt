package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class MovementLocomotionCheck : LocomotionCheck {
	override fun isActive(creatureObject: CreatureObject): Boolean {
		return false	// TODO we need a way to check if the creature's moving around. We could probably get away with treating any movement locomotions the same.
	}
}