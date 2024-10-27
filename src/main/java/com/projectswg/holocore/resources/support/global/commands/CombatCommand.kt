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
package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.common.data.CRC
import com.projectswg.common.data.combat.*
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import java.util.*

class CombatCommand private constructor(builder: CombatCommandBuilder) {

	val validTarget: ValidTarget = builder.validTarget
	val isForceCombat: Boolean = builder.forceCombat
	private val animations: Map<WeaponType, Array<String>> = builder.animations
	private val defaultAnimation: Array<String> = requireNotNull(builder.defaultAnimation)
	val attackType: AttackType = builder.attackType
	val healthCost: Double = builder.healthCost
	val actionCost: Double = builder.actionCost
	val mindCost: Double = builder.mindCost
	val forceCost: Double = builder.forceCost
	val forceCostModifier: Double = builder.forceCostModifier
	val knockdownChance: Double = builder.knockdownChance
	val isBlinding: Boolean = builder.blinding
	val isBleeding: Boolean = builder.bleeding
	val isDizzying: Boolean = builder.dizzying
	val isStunning: Boolean = builder.stunning
	val triggerEffect: String = builder.triggerEffect
	val triggerEffectHardpoint: String = builder.triggerEffectHardpoint
	val targetEffect: String = builder.targetEffect
	val targetEffectHardpoint: String = builder.targetEffectHardpoint
	val damageType: DamageType = builder.damageType
	val elementalType: DamageType = builder.elementalType
	val isIgnoreDistance: Boolean = builder.ignoreDistance
	val isPvpOnly: Boolean = builder.pvpOnly
	val percentAddFromWeapon: Double = builder.percentAddFromWeapon
	val bypassArmor: Double = builder.bypassArmor
	val hateDamageModifier: Double = builder.hateDamageModifier
	val hateAdd: Int = builder.hateAdd
	val addedDamage: Int = builder.addedDamage
	val buffNameTarget: String = builder.buffNameTarget
	val buffNameSelf: String = builder.buffNameSelf
	val hitType: HitType = builder.hitType
	val delayAttackEggTemplate: String = builder.delayAttackEggTemplate
	val delayAttackParticle: String = builder.delayAttackParticle
	val initialDelayAttackInterval: Double = builder.initialDelayAttackInterval
	val delayAttackInterval: Double = builder.delayAttackInterval
	val delayAttackLoops: Int = builder.delayAttackLoops
	val eggPosition: DelayAttackEggPosition = builder.eggPosition
	val coneLength: Double = builder.coneLength
	val coneWidth: Double = builder.coneWidth
	val healAttrib: HealAttrib = builder.healAttrib
	val name: String = requireNotNull(builder.name)
	val maxRange: Double = builder.maxRange
	val crc: Int = CRC.getCrc(builder.name)
	val hitLocation: HitLocation = builder.hitLocation

	fun getAnimations(type: WeaponType): Array<String> {
		return animations.getOrDefault(type, Array(0) { "" })
	}

	fun getRandomAnimation(type: WeaponType): String {
		var animations = animations[type]
		if (animations.isNullOrEmpty()) animations = defaultAnimation
		if (animations.isEmpty()) return ""
		return animations[(Math.random() * animations.size).toInt()]
	}

	class CombatCommandBuilder {
		val animations: MutableMap<WeaponType, Array<String>> = EnumMap(WeaponType::class.java)

		var validTarget: ValidTarget = ValidTarget.STANDARD
		var forceCombat: Boolean = false
		var defaultAnimation: Array<String> = Array(0) { "" }
		var attackType: AttackType = AttackType.SINGLE_TARGET
		var healthCost: Double = 0.0
		var actionCost: Double = 0.0
		var mindCost: Double = 0.0
		var forceCost: Double = 0.0
		var forceCostModifier: Double = 0.0
		var knockdownChance: Double = 0.0
		var blinding: Boolean = false
		var bleeding: Boolean = false
		var dizzying: Boolean = false
		var stunning: Boolean = false
		var triggerEffect: String = ""
		var triggerEffectHardpoint: String = ""
		var targetEffect: String = ""
		var targetEffectHardpoint: String = ""
		var damageType: DamageType = DamageType.KINETIC
		var elementalType: DamageType = DamageType.KINETIC
		var ignoreDistance: Boolean = false
		var pvpOnly: Boolean = false
		var percentAddFromWeapon: Double = 0.0
		var bypassArmor: Double = 0.0
		var hateDamageModifier: Double = 0.0
		var hateAdd: Int = 0
		var addedDamage: Int = 0
		var buffNameTarget: String = ""
		var buffNameSelf: String = ""
		var hitType: HitType = HitType.ATTACK
		var delayAttackEggTemplate: String = ""
		var delayAttackParticle: String = ""
		var initialDelayAttackInterval: Double = 0.0
		var delayAttackInterval: Double = 0.0
		var delayAttackLoops: Int = 0
		var eggPosition: DelayAttackEggPosition = DelayAttackEggPosition.TARGET
		var coneLength: Double = 0.0
		var coneWidth: Double = 0.0
		var healAttrib: HealAttrib = HealAttrib.HEALTH
		var name: String = ""
		var maxRange: Double = 0.0
		var hitLocation: HitLocation = HitLocation.HIT_LOCATION_BODY

		fun withValidTarget(validTarget: ValidTarget): CombatCommandBuilder {
			this.validTarget = validTarget
			return this
		}

		fun withForceCombat(forceCombat: Boolean): CombatCommandBuilder {
			this.forceCombat = forceCombat
			return this
		}

		fun withAnimations(type: WeaponType, animations: Array<String>): CombatCommandBuilder {
			this.animations[type] = animations
			return this
		}

		fun withDefaultAnimation(defaultAnimation: Array<String>): CombatCommandBuilder {
			this.defaultAnimation = defaultAnimation
			return this
		}

		fun withAttackType(attackType: AttackType): CombatCommandBuilder {
			this.attackType = attackType
			return this
		}

		fun withHealthCost(healthCost: Double): CombatCommandBuilder {
			this.healthCost = healthCost
			return this
		}

		fun withActionCost(actionCost: Double): CombatCommandBuilder {
			this.actionCost = actionCost
			return this
		}

		fun withMindCost(mindCost: Double): CombatCommandBuilder {
			this.mindCost = mindCost
			return this
		}

		fun withForceCost(forceCost: Double): CombatCommandBuilder {
			this.forceCost = forceCost
			return this
		}

		fun withForceCostModifier(forceCostModifier: Double): CombatCommandBuilder {
			this.forceCostModifier = forceCostModifier
			return this
		}

		fun withKnockdownChance(knockdownChance: Double): CombatCommandBuilder {
			this.knockdownChance = knockdownChance
			return this
		}

		fun withBlinding(blinding: Boolean): CombatCommandBuilder {
			this.blinding = blinding
			return this
		}

		fun withBleeding(bleeding: Boolean): CombatCommandBuilder {
			this.bleeding = bleeding
			return this
		}

		fun withDizzying(dizzying: Boolean): CombatCommandBuilder {
			this.dizzying = dizzying
			return this
		}

		fun withStunning(stunning: Boolean): CombatCommandBuilder {
			this.stunning = stunning
			return this
		}

		fun withTriggerEffect(triggerEffect: String): CombatCommandBuilder {
			this.triggerEffect = triggerEffect
			return this
		}

		fun withTriggerEffectHardpoint(triggerEffectHardpoint: String): CombatCommandBuilder {
			this.triggerEffectHardpoint = triggerEffectHardpoint
			return this
		}

		fun withTargetEffect(targetEffect: String): CombatCommandBuilder {
			this.targetEffect = targetEffect
			return this
		}

		fun withTargetEffectHardpoint(targetEffectHardpoint: String): CombatCommandBuilder {
			this.targetEffectHardpoint = targetEffectHardpoint
			return this
		}

		fun withDamageType(damageType: DamageType): CombatCommandBuilder {
			this.damageType = damageType
			return this
		}

		fun withElementalType(elementalType: DamageType): CombatCommandBuilder {
			this.elementalType = elementalType
			return this
		}

		fun withIgnoreDistance(ignoreDistance: Boolean): CombatCommandBuilder {
			this.ignoreDistance = ignoreDistance
			return this
		}

		fun withPvpOnly(pvpOnly: Boolean): CombatCommandBuilder {
			this.pvpOnly = pvpOnly
			return this
		}

		fun withPercentAddFromWeapon(percentAddFromWeapon: Double): CombatCommandBuilder {
			this.percentAddFromWeapon = percentAddFromWeapon
			return this
		}

		fun withBypassArmor(bypassArmor: Double): CombatCommandBuilder {
			this.bypassArmor = bypassArmor
			return this
		}

		fun withHateDamageModifier(hateDamageModifier: Double): CombatCommandBuilder {
			this.hateDamageModifier = hateDamageModifier
			return this
		}

		fun withHateAdd(hateAdd: Int): CombatCommandBuilder {
			this.hateAdd = hateAdd
			return this
		}

		fun withAddedDamage(addedDamage: Int): CombatCommandBuilder {
			this.addedDamage = addedDamage
			return this
		}

		fun withBuffNameTarget(buffNameTarget: String): CombatCommandBuilder {
			this.buffNameTarget = buffNameTarget
			return this
		}

		fun withBuffNameSelf(buffNameSelf: String): CombatCommandBuilder {
			this.buffNameSelf = buffNameSelf
			return this
		}

		fun withHitType(hitType: HitType): CombatCommandBuilder {
			this.hitType = hitType
			return this
		}

		fun withDelayAttackEggTemplate(delayAttackEggTemplate: String): CombatCommandBuilder {
			this.delayAttackEggTemplate = delayAttackEggTemplate
			return this
		}

		fun withDelayAttackParticle(delayAttackParticle: String): CombatCommandBuilder {
			this.delayAttackParticle = delayAttackParticle
			return this
		}

		fun withInitialDelayAttackInterval(initialDelayAttackInterval: Double): CombatCommandBuilder {
			this.initialDelayAttackInterval = initialDelayAttackInterval
			return this
		}

		fun withDelayAttackInterval(delayAttackInterval: Double): CombatCommandBuilder {
			this.delayAttackInterval = delayAttackInterval
			return this
		}

		fun withDelayAttackLoops(delayAttackLoops: Int): CombatCommandBuilder {
			this.delayAttackLoops = delayAttackLoops
			return this
		}

		fun withEggPosition(eggPosition: DelayAttackEggPosition): CombatCommandBuilder {
			this.eggPosition = eggPosition
			return this
		}

		fun withConeLength(coneLength: Double): CombatCommandBuilder {
			this.coneLength = coneLength
			return this
		}

		fun withConeWidth(coneWidth: Double): CombatCommandBuilder {
			this.coneWidth = coneWidth
			return this
		}

		fun withHealAttrib(healAttrib: HealAttrib): CombatCommandBuilder {
			this.healAttrib = healAttrib
			return this
		}

		fun withName(name: String): CombatCommandBuilder {
			this.name = name
			return this
		}

		fun withMaxRange(maxRange: Double): CombatCommandBuilder {
			this.maxRange = maxRange
			return this
		}

		fun withHitLocation(hitLocation: HitLocation): CombatCommandBuilder {
			this.hitLocation = hitLocation
			return this
		}

		fun build(): CombatCommand {
			return CombatCommand(this)
		}
	}

	companion object {
		fun builder(): CombatCommandBuilder {
			return CombatCommandBuilder()
		}
	}
}
