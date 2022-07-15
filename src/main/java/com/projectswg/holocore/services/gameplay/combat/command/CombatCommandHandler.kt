/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */

package com.projectswg.holocore.services.gameplay.combat.command

import com.projectswg.common.data.combat.CombatStatus
import com.projectswg.common.data.combat.HitType
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import java.util.*

class CombatCommandHandler {
	
	private val hitTypeMap: MutableMap<HitType, CombatCommandHitType>
	
	init {
		this.hitTypeMap = EnumMap(HitType::class.java)
		hitTypeMap[HitType.ATTACK] = CombatCommandAttack.INSTANCE
		hitTypeMap[HitType.BUFF] = CombatCommandBuff.INSTANCE
		hitTypeMap[HitType.DEBUFF] = CombatCommandDebuff
		hitTypeMap[HitType.HEAL] = CombatCommandHeal.INSTANCE
		hitTypeMap[HitType.DELAY_ATTACK] = CombatCommandDelayAttack.INSTANCE
//		hitTypeMap[HitType.REVIVE] = null
	}
	
	fun start(): Boolean {
		for (hitType in hitTypeMap.values)
			hitType.initialize()
		return true
	}
	
	fun stop(): Boolean {
		for (hitType in hitTypeMap.values)
			hitType.terminate()
		return true
	}
	
	fun executeCombatCommand(source: CreatureObject, target: SWGObject?, command: CombatCommand, arguments: String): CombatStatus {
		val equippedWeapon = source.equippedWeapon
		val specialAttackCost = equippedWeapon.specialAttackCost
		source.modifyHealth((-command.healthCost).toInt())
		source.modifyAction((-command.actionCost * specialAttackCost / 100).toInt())
		source.modifyMind((-command.mindCost * specialAttackCost / 100).toInt())
		
		val hitType = hitTypeMap[command.hitType]

		if (command.triggerEffect.isNotEmpty()) {
			val triggerEffect = PlayClientEffectObjectMessage(command.triggerEffect, command.triggerEffectHardpoint, source.objectId, "")
			source.sendSelf(triggerEffect)
		}

		return hitType?.handle(source, target, command, arguments) ?: CombatStatus.UNKNOWN
	}

}
