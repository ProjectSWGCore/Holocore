package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

enum class State(val stateTableId: Int, val commandSdbColumnName: String, private val stateCheck: StateCheck) {
	COMBAT(1, "S:combat", CreatureStateCheck(CreatureState.COMBAT));

	fun isActive(creatureObject: CreatureObject): Boolean {
		return stateCheck.isActive(creatureObject)
	}
}