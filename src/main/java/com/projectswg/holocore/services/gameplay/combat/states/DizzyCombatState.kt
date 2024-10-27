/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.gameplay.combat.states

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

class DizzyCombatState : CombatState {
	
	private val loopEffectLabel = "dizzy"
	
	override fun isApplied(victim: CreatureObject): Boolean {
		return victim.isStatesBitmask(CreatureState.DIZZY)
	}
	
	override fun apply(attacker: CreatureObject, victim: CreatureObject) {
		victim.setStatesBitmask(CreatureState.DIZZY)
		
		val dizzyEffect = PlayClientEffectObjectMessage("clienteffect/combat_special_defender_dizzy.cef", "", victim.objectId, "")
		victim.sendObservers(dizzyEffect)
		
		val dizzyFlytext = ShowFlyText(victim.objectId, StringId("combat_effects", "go_dizzy"), ShowFlyText.Scale.MEDIUM, SWGColor.Greens.lawngreen)
		victim.sendSelf(dizzyFlytext)
		attacker.sendSelf(dizzyFlytext)
		
		val victimOwner = victim.owner
		
		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@cbt_spam:go_dizzy_single")
		}
	}
	
	override fun loop(attacker: CreatureObject, victim: CreatureObject) {
		val stateDizzyEffect = PlayClientEffectObjectMessage("appearance/pt_state_dizzy.prt", "rbrow2", victim.objectId, loopEffectLabel)
		victim.sendObservers(stateDizzyEffect)
	}
	
	override fun clear(attacker: CreatureObject, victim: CreatureObject) {
		victim.clearStatesBitmask(CreatureState.DIZZY)
		
		val victimOwner = victim.owner
		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@cbt_spam:no_dizzy_single")
		}
		
		val noDizzyFlytext = ShowFlyText(victim.objectId, StringId("combat_effects", "no_dizzy"), ShowFlyText.Scale.MEDIUM, SWGColor.Reds.orangered)
		victim.sendSelf(noDizzyFlytext)
		attacker.sendSelf(noDizzyFlytext)
		
		val stopClientEffectObjectByLabelMessage = StopClientEffectObjectByLabelMessage(victim.objectId, loopEffectLabel, false)
		victim.sendObservers(stopClientEffectObjectByLabelMessage)
	}
	
}