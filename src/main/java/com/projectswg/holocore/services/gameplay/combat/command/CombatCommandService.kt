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
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.handleStatus
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.*

class CombatCommandService : Service() {
	
	private val hitTypeMap: MutableMap<HitType, CombatCommandHitType>
	
	init {
		this.hitTypeMap = EnumMap(HitType::class.java)
		hitTypeMap[HitType.ATTACK] = CombatCommandAttack.INSTANCE
		hitTypeMap[HitType.BUFF] = CombatCommandBuff.INSTANCE
//		hitTypeMap[HitType.DEBUFF] = null
		hitTypeMap[HitType.HEAL] = CombatCommandHeal.INSTANCE
		hitTypeMap[HitType.DELAY_ATTACK] = CombatCommandDelayAttack.INSTANCE
//		hitTypeMap[HitType.REVIVE] = null
		// TODO: Add in other hit types (DEBUFF/REVIVE)
	}
	
	override fun start(): Boolean {
		for (hitType in hitTypeMap.values)
			hitType.initialize()
		return true
	}
	
	override fun stop(): Boolean {
		for (hitType in hitTypeMap.values)
			hitType.terminate()
		return true
	}
	
	@IntentHandler
	private fun handleChatCommandIntent(eci: ExecuteCommandIntent) {
		if (!eci.command.isCombatCommand || eci.command !is CombatCommand)
			return
		val command = eci.command as CombatCommand
		
		val source = eci.source

		val equippedWeapon = source.equippedWeapon
		val specialAttackCost = equippedWeapon.specialAttackCost
		source.modifyHealth((-command.healthCost).toInt())
		source.modifyAction((-command.actionCost * specialAttackCost / 100).toInt())
		source.modifyMind((-command.mindCost * specialAttackCost / 100).toInt())
		
		val hitType = hitTypeMap[command.hitType]
		hitType?.handle(source, eci.target, command, eci.arguments) ?: handleStatus(source, CombatStatus.UNKNOWN)
	}

}
