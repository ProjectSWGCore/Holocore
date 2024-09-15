package com.projectswg.holocore.services.gameplay.combat.buffs

import com.projectswg.holocore.resources.support.data.server_info.loader.BuffLoader
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

interface BuffCallback {
	fun execute(target: CreatureObject, buffData: BuffLoader.BuffInfo?, source: CreatureObject?)
}