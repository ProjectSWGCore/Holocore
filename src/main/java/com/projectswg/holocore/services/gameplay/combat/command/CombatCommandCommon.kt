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

import com.projectswg.common.data.CRC
import com.projectswg.common.data.RGB
import com.projectswg.common.data.combat.AttackInfo
import com.projectswg.common.data.combat.AttackType
import com.projectswg.common.data.combat.CombatSpamType
import com.projectswg.common.data.combat.TrailLocation
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam
import com.projectswg.holocore.intents.gameplay.combat.BuffIntent
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus
import com.projectswg.holocore.resources.support.color.SWGColor.Blues.cyan
import com.projectswg.holocore.resources.support.color.SWGColor.Oranges.orange
import com.projectswg.holocore.resources.support.color.SWGColor.Whites.white
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.floor

object CombatCommandCommon {

	fun createCombatAction(source: CreatureObject, weapon: WeaponObject, trail: TrailLocation, command: CombatCommand): CombatAction {
		val combatAction = CombatAction(source.objectId)
		combatAction.actionCrc = CRC.getCrc(command.getRandomAnimation(weapon.type))
		combatAction.attackerId = source.objectId
		combatAction.posture = source.posture
		combatAction.weaponId = weapon.objectId
		combatAction.clientEffectId = 0.toByte()
		combatAction.commandCrc = command.crc
		combatAction.trail = trail
		return combatAction
	}

	fun createCombatSpam(receiver: CreatureObject, source: CreatureObject, target: TangibleObject, weapon: WeaponObject, info: AttackInfo, command: CombatCommand, combatSpamType: CombatSpamType): CombatSpam {
		val combatSpam = CombatSpam(receiver.objectId)
		combatSpam.info = info
		combatSpam.attacker = source.objectId
		combatSpam.weapon = weapon.objectId
		combatSpam.defender = target.objectId
		combatSpam.dataType = 0.toByte()
		combatSpam.attackName = StringId("cmd_n", command.name)
		combatSpam.spamType = combatSpamType

		return combatSpam
	}

	fun canPerform(source: CreatureObject, target: SWGObject?, c: CombatCommand): CombatStatus {
		if (source == target) {
			return CombatStatus.INVALID_TARGET
		}

		if (source.equippedWeapon == null) return CombatStatus.NO_WEAPON

		if (target == null || source == target) return CombatStatus.INVALID_TARGET

		if (target !is TangibleObject) return CombatStatus.INVALID_TARGET

		if (!source.isAttackable(target)) {
			return CombatStatus.INVALID_TARGET
		}

		if (!source.isLineOfSight(target)) return CombatStatus.TOO_FAR

		if (target is CreatureObject) {
			when (target.posture) {
				Posture.DEAD, Posture.INCAPACITATED -> return CombatStatus.INVALID_TARGET
				else                                -> {}
			}
		}

		return when (c.attackType) {
			AttackType.AREA, AttackType.CONE, AttackType.TARGET_AREA -> canPerformArea(source, c)
			AttackType.SINGLE_TARGET                                 -> canPerformSingle(source, target, c)
			else                                                     -> CombatStatus.UNKNOWN
		}
	}

	fun canPerformSingle(source: CreatureObject, target: SWGObject, c: CombatCommand): CombatStatus {
		if (target !is TangibleObject) return CombatStatus.NO_TARGET

		val weapon = source.equippedWeapon
		val dist = floor(source.worldLocation.distanceTo(target.getWorldLocation()))
		val commandRange = c.maxRange
		val range = if (commandRange > 0) commandRange else weapon.maxRange.toDouble()

		if (dist > range) return CombatStatus.TOO_FAR

		return CombatStatus.SUCCESS
	}

	fun canPerformArea(source: CreatureObject, c: CombatCommand): CombatStatus {
		return CombatStatus.SUCCESS
	}

	fun calculateBaseWeaponDamage(weapon: WeaponObject, command: CombatCommand): Int {
		val minDamage = weapon.minDamage
		val weaponDamage = ThreadLocalRandom.current().nextInt((weapon.maxDamage - minDamage) + 1) + minDamage

		return (weaponDamage * command.percentAddFromWeapon).toInt()
	}

	fun addBuff(caster: CreatureObject, receiver: CreatureObject, buffName: String) {
		if (buffName.isEmpty()) {
			return
		}

		BuffIntent(buffName, caster, receiver, false).broadcast()
	}

	fun handleStatus(source: CreatureObject, combatCommand: CombatCommand, status: CombatStatus) {
		when (status) {
			CombatStatus.NO_TARGET      -> showFlyText(source, "@combat_effects:target_invalid_fly", ShowFlyText.Scale.MEDIUM, white, ShowFlyText.Flag.PRIVATE)
			CombatStatus.TOO_FAR        -> showFlyText(source, "@combat_effects:range_too_far", ShowFlyText.Scale.MEDIUM, cyan, ShowFlyText.Flag.PRIVATE)
			CombatStatus.INVALID_TARGET -> showFlyText(source, "@combat_effects:target_invalid_fly", ShowFlyText.Scale.MEDIUM, cyan, ShowFlyText.Flag.PRIVATE)
			CombatStatus.TOO_TIRED      -> showFlyText(source, "@combat_effects:action_too_tired", ShowFlyText.Scale.MEDIUM, orange, ShowFlyText.Flag.PRIVATE)
			CombatStatus.SUCCESS        -> showTriggerEffect(source, combatCommand)
			else                        -> showFlyText(source, "@combat_effects:action_failed", ShowFlyText.Scale.MEDIUM, white, ShowFlyText.Flag.PRIVATE)
		}
	}

	private fun showTriggerEffect(source: CreatureObject, command: CombatCommand) {
		val triggerEffect = command.triggerEffect
		if (triggerEffect.isNotEmpty()) {
			val triggerEffectHardpoint = command.triggerEffectHardpoint
			source.sendObservers(PlayClientEffectObjectMessage(triggerEffect, triggerEffectHardpoint, source.objectId, ""))
		}
	}

	fun showFlyText(obj: TangibleObject, text: String, scale: ShowFlyText.Scale, c: RGB, vararg flags: ShowFlyText.Flag) {
		obj.sendSelf(ShowFlyText(obj.objectId, text, scale, c, *flags))
	}
}
