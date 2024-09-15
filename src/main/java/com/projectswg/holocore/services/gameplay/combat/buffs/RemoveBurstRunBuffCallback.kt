package com.projectswg.holocore.services.gameplay.combat.buffs

import com.projectswg.holocore.resources.support.data.server_info.loader.BuffLoader
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.MovementModifierIdentifier

class RemoveBurstRunBuffCallback : BuffCallback {
	override fun execute(target: CreatureObject, buffData: BuffLoader.BuffInfo?, source: CreatureObject?) {
		target.removeMovementScale(MovementModifierIdentifier.BURST_RUN)
	}
}