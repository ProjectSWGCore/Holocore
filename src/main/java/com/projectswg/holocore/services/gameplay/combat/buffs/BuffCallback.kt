package com.projectswg.holocore.services.gameplay.combat.buffs

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

interface BuffCallback {
	fun execute(creatureObject: CreatureObject)
}