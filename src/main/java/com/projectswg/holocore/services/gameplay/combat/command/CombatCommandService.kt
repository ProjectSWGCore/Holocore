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

import com.projectswg.common.data.RGB
import com.projectswg.common.data.combat.CombatStatus
import com.projectswg.common.data.combat.HitType
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.SpecialLineLoader.SpecialLineInfo
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log

import java.util.EnumMap
import java.util.concurrent.ThreadLocalRandom

import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.handleStatus

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
		val specialLine = DataLoader.specialLines().getSpecialLine(command.specialLine)
		var actionCost = command.actionCost * command.attackRolls
		
		// TODO future: reduce actionCost with general ACR, weapon ACR and ability ACR
		
		if (specialLine != null && source.getSkillModValue(specialLine.freeshotModName) > ThreadLocalRandom.current().nextInt(0, 100)) {
			source.sendSelf(ShowFlyText(source.objectId, StringId("spam", "freeshot"), ShowFlyText.Scale.MEDIUM, RGB(255, 255, 255), ShowFlyText.Flag.IS_FREESHOT))
		} else {
			if (specialLine != null)
				actionCost = reduceActionCost(source, actionCost, specialLine.actionCostModName)
			if (actionCost > source.action)
				return
			
			source.modifyAction(-command.actionCost.toInt())
		}
		
		val hitType = hitTypeMap[command.hitType]
		hitType?.handle(source, eci.target, command, eci.arguments) ?: handleStatus(source, CombatStatus.UNKNOWN)
	}
	
	/**
	 * Calculates a new action cost based on the given action cost and a skill mod name.
	 * @param source to read the skillmod value from
	 * @param actionCost that has been calculated so far
	 * @param skillModName name of the skillmod to read from `source`
	 * @return new action cost that has been increased or reduced, depending on whether the skillmod value is
	 * positive or negative
	 */
	private fun reduceActionCost(source: CreatureObject, actionCost: Double, skillModName: String): Double {
		val actionCostModValue = source.getSkillModValue(skillModName)
		
		return actionCost + actionCost * actionCostModValue / 100
	}
	
}
