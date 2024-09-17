package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

interface CombatState {
	fun isApplied(victim: CreatureObject): Boolean
	fun apply(attacker: CreatureObject, victim: CreatureObject)
	fun loop(attacker: CreatureObject, victim: CreatureObject)
	fun clear(attacker: CreatureObject?, victim: CreatureObject)
}