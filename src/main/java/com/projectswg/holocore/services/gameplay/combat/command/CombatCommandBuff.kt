/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.combat.command

import com.projectswg.common.data.combat.HitLocation
import com.projectswg.common.data.combat.TrailLocation
import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.global.commands.Command
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.addBuff
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatAction

internal enum class CombatCommandBuff : CombatCommandHitType {
	INSTANCE;

	override fun handle(source: CreatureObject, targetPrecheck: SWGObject?, command: Command, combatCommand: CombatCommand, arguments: String): CombatStatus {
		// TODO group buffs
		addBuff(source, source, combatCommand.buffNameSelf)

		val target = if (targetPrecheck is CreatureObject) {
			targetPrecheck
		} else {
			source
		}

		val applyToSelf = isApplyToSelf(source, target)
		val effectiveTarget = if (applyToSelf) source else target

		val buffNameTarget = combatCommand.buffNameTarget
		addBuff(source, effectiveTarget, buffNameTarget)

		val weapon = source.equippedWeapon
		val combatAction: CombatAction = createCombatAction(source, weapon, TrailLocation.RIGHT_HAND, combatCommand)
		combatAction.addDefender(Defender(source.objectId, source.posture, false, 0.toByte(), HitLocation.HIT_LOCATION_BODY, 0.toShort()))

		if (buffNameTarget.isNotEmpty()) {
			combatAction.addDefender(Defender(target.objectId, effectiveTarget.posture, false, 0.toByte(), HitLocation.HIT_LOCATION_BODY, 0.toShort()))
		}

		return CombatStatus.SUCCESS
	}

	private fun isApplyToSelf(source: CreatureObject, target: CreatureObject): Boolean {
		val sourceFaction = source.faction
		val targetFaction = target.faction

		if (sourceFaction == null || targetFaction == null) {
			return true
		}

		if (target.isAttackable(source)) {
			// You can't buff someone you can attack
			return true
		} else if (sourceFaction.isEnemy(targetFaction)) {
			val sourcePvpStatus = source.pvpStatus
			val targetPvpStatus = target.pvpStatus

			if (sourcePvpStatus == PvpStatus.COMBATANT && targetPvpStatus == PvpStatus.ONLEAVE) {
				return false
			}

			return sourcePvpStatus != PvpStatus.ONLEAVE || targetPvpStatus != PvpStatus.ONLEAVE
		}

		if (source.isPlayer && !target.isPlayer) {
			if (target.hasOptionFlags(OptionFlag.INVULNERABLE)) {
				// You can't buff invulnerable NPCs
				return true
			}
			// A player is attempting to buff a NPC
			val sourceGroupId = source.groupId
			val npcGroupId = target.groupId
			val bothGrouped = sourceGroupId != 0L && npcGroupId != 0L


			// Buff ourselves instead if player and NPC are ungrouped or are in different groups
			return !bothGrouped || sourceGroupId != npcGroupId
		}

		return false
	}
}
