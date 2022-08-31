package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

interface StateCheck {
	fun isActive(creatureObject: CreatureObject): Boolean
}
