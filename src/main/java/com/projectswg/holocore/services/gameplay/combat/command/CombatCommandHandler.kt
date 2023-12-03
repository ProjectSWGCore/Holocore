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

import com.projectswg.holocore.resources.gameplay.combat.CombatStatus
import com.projectswg.common.data.combat.HitType
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.global.commands.Command
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import java.util.*

class CombatCommandHandler(toHitDie: Die, knockdownDie: Die) {
	
	private val hitTypeMap: MutableMap<HitType, CombatCommandHitType>
	
	init {
		this.hitTypeMap = EnumMap(HitType::class.java)
		val combatCommandAttack = CombatCommandAttack(toHitDie, knockdownDie)
		hitTypeMap[HitType.ATTACK] = combatCommandAttack
		hitTypeMap[HitType.BUFF] = CombatCommandBuff.INSTANCE
		hitTypeMap[HitType.DEBUFF] = CombatCommandDebuff
		hitTypeMap[HitType.HEAL] = CombatCommandHeal.INSTANCE
		hitTypeMap[HitType.DELAY_ATTACK] = CombatCommandDelayAttack(combatCommandAttack)
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
	
	fun executeCombatCommand(source: CreatureObject, target: SWGObject?, command: Command, combatCommand: CombatCommand, arguments: String): CombatStatus {
		val equippedWeapon = source.equippedWeapon
		val specialAttackCost = equippedWeapon.specialAttackCost
		val healthCost = combatCommand.healthCost.toInt()
		val actionCost = (combatCommand.actionCost * specialAttackCost / 100).toInt()
		val mindCost = (combatCommand.mindCost * specialAttackCost / 100).toInt()
		val forceCost = (combatCommand.forceCost) + (equippedWeapon.forcePowerCost * combatCommand.forceCostModifier)
		val playerObject = source.playerObject

		if (healthCost > source.health || actionCost > source.action || mindCost > source.mind || (playerObject != null && forceCost > playerObject.forcePower)) {
			return CombatStatus.TOO_TIRED
		}
		
		val hitType = hitTypeMap[combatCommand.hitType]
		val combatStatus = hitType?.handle(source, target, command, combatCommand, arguments) ?: CombatStatus.UNKNOWN

		if (combatStatus == CombatStatus.SUCCESS) {
			source.modifyHealth(-healthCost)
			source.modifyAction(-actionCost)
			source.modifyMind(-mindCost)
			
			if (playerObject != null) {
				playerObject.forcePower = (playerObject.forcePower - forceCost).toInt()
			}
		}

		return combatStatus
	}

}
