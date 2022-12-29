package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

enum class State(val stateTableId: Int, val commandSdbColumnName: String, private val stateCheck: StateCheck) {
	COMBAT(1, "S:combat", CreatureStateCheck(CreatureState.COMBAT)),
	STUNNED(12, "S:stunned", CreatureStateCheck(CreatureState.STUNNED)),
	BLEEDING(24, "S:bleeding", CreatureStateCheck(CreatureState.BLEEDING));

	fun isActive(creatureObject: CreatureObject): Boolean {
		return stateCheck.isActive(creatureObject)
	}
}