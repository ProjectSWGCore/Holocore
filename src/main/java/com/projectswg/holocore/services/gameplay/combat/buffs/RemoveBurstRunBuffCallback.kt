package com.projectswg.holocore.services.gameplay.combat.buffs

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.MovementModifierIdentifier

class RemoveBurstRunBuffCallback : BuffCallback {
	override fun execute(creatureObject: CreatureObject) {
		creatureObject.removeMovementScale(MovementModifierIdentifier.BURST_RUN)
	}
}