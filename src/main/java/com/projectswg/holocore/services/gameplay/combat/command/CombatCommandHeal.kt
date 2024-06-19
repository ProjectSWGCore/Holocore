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

import com.projectswg.common.data.combat.*
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.global.commands.Command
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatAction

internal enum class CombatCommandHeal : CombatCommandHitType {
	INSTANCE;

	override fun handle(source: CreatureObject, target: SWGObject?, command: Command, combatCommand: CombatCommand, arguments: String): CombatStatus {
		var healAmount = combatCommand.addedDamage
		val healingEfficiency = source.getSkillModValue("healing_efficiency")
		var healedDamage = 0

		if (healingEfficiency > 0) {
			healAmount = (healAmount * ((100 + healingEfficiency) / 100.0)).toInt()
		}

		when (combatCommand.attackType) {
			AttackType.SINGLE_TARGET -> {
				when (command.targetType) {
					TargetType.NONE     -> {
						healedDamage += doHeal(source, source, healAmount, combatCommand)
					}

					TargetType.REQUIRED -> {
						if (target == null)
							return CombatStatus.NO_TARGET
						if (target !is CreatureObject)
							return CombatStatus.INVALID_TARGET

						val healTarget = if (source.isAttackable(target)) source else target
						healedDamage += doHeal(source, healTarget, healAmount, combatCommand)
					}

					TargetType.OPTIONAL -> {
						if (target != null) {
							if (target !is CreatureObject)
								return CombatStatus.INVALID_TARGET
							val healTarget = if (source.isAttackable(target)) source else target
							healedDamage += doHeal(source, healTarget, healAmount, combatCommand)
						} else {
							healedDamage += doHeal(source, source, healAmount, combatCommand)
						}
					}
				}
			}

			AttackType.AREA          -> {
				// Targets are never supplied for AoE heals
				val range = combatCommand.coneLength
				val sourceLocation = source.worldLocation

				for (nearbyObject in source.aware) {
					if (sourceLocation.isWithinDistance(nearbyObject.location, range)) {
						if (nearbyObject !is CreatureObject) {
							// We can't heal something that's not a creature
							continue
						}

						if (source.isAttackable(nearbyObject)) {
							// Don't heal (potential) enemies
							continue
						}

						if (nearbyObject.hasOptionFlags(OptionFlag.INVULNERABLE)) {
							// Don't heal creatures that can't take damage
							continue
						}

						// Heal nearby friendly
						healedDamage += doHeal(source, nearbyObject, healAmount, combatCommand)
					}
				}
			}

			else                     -> {}
		}
		grantMedicalXp(source, healedDamage)
		return CombatStatus.SUCCESS
	}

	private fun grantMedicalXp(source: CreatureObject, healedDamage: Int) {
		val medicalXp = Math.round(healedDamage.toFloat() * 0.25f)

		if (medicalXp > 0) {
			ExperienceIntent(source, "medical", medicalXp).broadcast()
		}
	}

	private fun doHeal(healer: CreatureObject, healed: CreatureObject, healAmount: Int, combatCommand: CombatCommand): Int {
		if (combatCommand.healAttrib != HealAttrib.HEALTH) {
			return 0
		}

		if (healed.health == healed.maxHealth) {
			return 0
		}

		val originalHealth = healed.health
		healed.modifyHealth(healAmount)
		val difference = healed.health - originalHealth

		val weapon = healer.equippedWeapon
		val combatAction: CombatAction = createCombatAction(healer, weapon, TrailLocation.RIGHT_HAND, combatCommand)
		combatAction.addDefender(Defender(healed.objectId, healed.posture, false, 0.toByte(), HitLocation.HIT_LOCATION_BODY, 0.toShort()))
		healed.sendObservers(combatAction)

		val targetEffect = combatCommand.targetEffect
		if (targetEffect.isNotEmpty()) {
			val targetEffectHardpoint = combatCommand.targetEffectHardpoint
			healed.sendObservers(PlayClientEffectObjectMessage(targetEffect, targetEffectHardpoint, healed.objectId, ""))
		}

		return difference
	}
}