/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
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

package com.projectswg.holocore.resources.gameplay.combat

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject

object EnemyProcessor {
	
	fun isAttackable(source: TangibleObject, target: TangibleObject): Boolean {
		if (target.hasOptionFlags(OptionFlag.INVULNERABLE))
			return false
		if (source !is CreatureObject || target !is CreatureObject)
			return isValidTangible(source) && isValidTangible(target) && isFactionAttackable(source, target)
		
		if (!isValidPosture(source) || !isValidPosture(target))
			return false // If neither of us have a valid posture, nobody can attack
		if (isAttackAllowedAnyways(source, target))
			return true
		
		// NPC-based checks
		if (source is AIObject) {
			if (target is AIObject)
				return isFactionAttackable(source, target)
			return ((source.hasOptionFlags(OptionFlag.AGGRESSIVE) || source.defenders.contains(target.objectId)) && isFactionAttackable(target, source)) || isFactionAttackable(source, target) // EvP only when aggressive or under attack
		} else if (target is AIObject) {
			return isFactionAttackable(source, target) // PvE allowed
		}
		return isFactionAttackable(source, target) // PvP
	}
	
	private fun isFactionAttackable(source: TangibleObject, target: TangibleObject): Boolean {
		val ourFaction = source.faction
		val otherFaction = target.faction
		
		if (ourFaction == null || ourFaction.name == "neutral" || otherFaction == null || otherFaction.name == "neutral")
			return source is CreatureObject && source.isPlayer && target is CreatureObject && !target.isPlayer // either neutral = PvE only
		
		// PvE / EvE - where one is a TangibleObject
		if (source !is CreatureObject || target !is CreatureObject)
			return ourFaction.isEnemy(otherFaction) || (source is CreatureObject && source.isPlayer && !ourFaction.isAlly(otherFaction))
		// PvP
		if (source.isPlayer && target.isPlayer)
			return ourFaction.isEnemy(otherFaction) && source.pvpStatus == PvpStatus.SPECIALFORCES && target.pvpStatus == PvpStatus.SPECIALFORCES
		// PvE
		if (source.isPlayer)
			return !ourFaction.isAlly(otherFaction)
		// EvE
		return ourFaction.isEnemy(otherFaction)
	}
	
	private fun isAttackAllowedAnyways(source: CreatureObject, target: CreatureObject): Boolean {
		// TODO bounty hunting
		// TODO pets, vehicles etc having same flagging as their owner
		// TODO guild wars
		if (source.isDuelingPlayer(target))
			return true  // If we're dueling, can always attack
		return false
	}
	
	private fun isValidTangible(obj: TangibleObject): Boolean {
		return obj !is CreatureObject || isValidPosture(obj)
	}
	
	private fun isValidPosture(obj: CreatureObject): Boolean {
		return when (obj.posture) {
			Posture.DEAD, Posture.INCAPACITATED -> false
			else -> true
		}
	}
	
}
