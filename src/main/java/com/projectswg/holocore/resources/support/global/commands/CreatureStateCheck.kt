package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

class CreatureStateCheck(private val creatureState: CreatureState) : StateCheck {
	override fun isActive(creatureObject: CreatureObject): Boolean {
		return creatureObject.isStatesBitmask(creatureState)
	}
}