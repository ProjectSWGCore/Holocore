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
import com.projectswg.common.data.encodables.oob.OutOfBandPackage
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam
import com.projectswg.holocore.intents.gameplay.combat.*
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus
import com.projectswg.holocore.resources.support.color.SWGColor.Reds.orangered
import com.projectswg.holocore.resources.support.color.SWGColor.Whites.white
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.global.commands.Command
import com.projectswg.holocore.resources.support.global.commands.Locomotion
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import com.projectswg.holocore.resources.support.random.Die
import com.projectswg.holocore.services.gameplay.combat.BleedingCombatState
import com.projectswg.holocore.services.gameplay.combat.BlindedCombatState
import com.projectswg.holocore.services.gameplay.combat.StunnedCombatState
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.addBuff
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.calculateBaseWeaponDamage
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.canPerform
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatAction
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatSpam
import java.util.stream.Collectors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.max

internal class CombatCommandAttack(private val toHitDie: Die, private val knockdownDie: Die, private val woundDie: Die) : CombatCommandHitType {
	override fun handle(source: CreatureObject, target: SWGObject?, command: Command, combatCommand: CombatCommand, arguments: String): CombatStatus {
		return handle(source, target, null, combatCommand)
	}

	fun handle(source: CreatureObject, target: SWGObject?, delayEgg: SWGObject?, combatCommand: CombatCommand): CombatStatus {
		val combatStatus: CombatStatus = canPerform(source, target, combatCommand)
		if (combatStatus != CombatStatus.SUCCESS) {
			return combatStatus
		}

		val info = AttackInfo()

		when (combatCommand.attackType) {
			AttackType.SINGLE_TARGET -> doCombatSingle(source, target, info, combatCommand)
			AttackType.AREA          -> doCombatArea(source, source, info, combatCommand, false)
			AttackType.TARGET_AREA   -> doCombatTargetArea(source, target, delayEgg, combatCommand, info)
			AttackType.CONE          -> doCombatCone(source, target, info, combatCommand)
			else                     -> {}
		}
		return combatStatus
	}

	private fun doCombatTargetArea(source: CreatureObject, target: SWGObject?, delayEgg: SWGObject?, combatCommand: CombatCommand, info: AttackInfo) {
		doCombatArea(source, (delayEgg ?: target)!!, info, combatCommand, true)
	}

	private fun doCombatCone(source: CreatureObject, targetWorldLocation: Location, info: AttackInfo, command: CombatCommand) {
		val coneLength = command.coneLength
		val coneWidth = command.coneWidth

		val sourceWorldLocation = source.worldLocation

		val dirX = targetWorldLocation.x - sourceWorldLocation.x
		val dirZ = targetWorldLocation.z - sourceWorldLocation.z

		val objectsToCheck = source.objectsAware

		val targets = objectsToCheck.stream().filter { obj: SWGObject? -> TangibleObject::class.java.isInstance(obj) }.map { obj: SWGObject? -> TangibleObject::class.java.cast(obj) }.filter { otherObject: TangibleObject? -> source.isAttackable(otherObject) }.filter { candidate: TangibleObject? -> canPerform(source, candidate, command) === CombatStatus.SUCCESS }.filter { candidate: TangibleObject -> sourceWorldLocation.distanceTo(candidate.location) <= coneLength }.filter { candidate: TangibleObject ->
				val candidateWorldLocation = candidate.worldLocation
				isInConeAngle(sourceWorldLocation, candidateWorldLocation, coneWidth, dirX, dirZ)
			}.collect(Collectors.toSet())

		doCombat(source, targets, info, command)
	}

	private fun doCombatCone(source: CreatureObject, target: SWGObject?, info: AttackInfo, command: CombatCommand) {
		doCombatCone(source, target!!.worldLocation, info, command)
	}

	fun isInConeAngle(attackerLocation: Location, targetLocation: Location, coneWidth: Double, directionX: Double, directionZ: Double): Boolean {
		val targetX = targetLocation.x - attackerLocation.x
		val targetZ = targetLocation.z - attackerLocation.z
		val targetAngle = atan2(targetZ, targetX) - atan2(directionZ, directionX)
		val degrees = targetAngle * 180 / Math.PI

		return !(abs(degrees) > coneWidth)
	}

	private fun doCombatSingle(source: CreatureObject, target: SWGObject?, info: AttackInfo, combatCommand: CombatCommand) {
		val targets: MutableSet<TangibleObject> = HashSet()

		if (target is TangibleObject) {
			targets.add(target)
		}

		doCombat(source, targets, info, combatCommand)
	}

	private fun doCombatArea(source: CreatureObject, origin: SWGObject, info: AttackInfo, combatCommand: CombatCommand, includeOrigin: Boolean) {
		val aoeRange = combatCommand.coneLength
		val originParent = origin.parent
		val objectsToCheck = if (originParent == null) origin.objectsAware else originParent.containedObjects

		val targets = objectsToCheck.stream().filter { obj: SWGObject? -> TangibleObject::class.java.isInstance(obj) }.map { obj: SWGObject? -> TangibleObject::class.java.cast(obj) }.filter { otherObject: TangibleObject? -> source.isAttackable(otherObject) }.filter { target: TangibleObject? -> canPerform(source, target, combatCommand) === CombatStatus.SUCCESS }.filter { creature: TangibleObject -> origin.location.distanceTo(creature.location) <= aoeRange }.collect(Collectors.toSet())

		// This way, mines or grenades won't try to harm themselves
		if (includeOrigin && origin is CreatureObject) targets.add(origin)

		doCombat(source, targets, info, combatCommand)
	}

	private fun doCombat(source: CreatureObject, targets: Set<TangibleObject>, info: AttackInfo, combatCommand: CombatCommand) {
		source.updateLastCombatTime()
		val sourceWeapon = source.equippedWeapon

		val action: CombatAction = createCombatAction(source, sourceWeapon, TrailLocation.WEAPON, combatCommand)
		val weaponDamageMod = calculateWeaponDamageMod(source, sourceWeapon).toDouble()

		val damageType = getDamageType(combatCommand, sourceWeapon)
		for (tangibleTarget in targets) {
			tangibleTarget.updateLastCombatTime()
			EnterCombatIntent(source, tangibleTarget).broadcast()
			EnterCombatIntent(tangibleTarget, source).broadcast()

			if (tangibleTarget is CreatureObject) {
				doCombatCreature(source, info, combatCommand, sourceWeapon, action, weaponDamageMod, damageType, tangibleTarget)
			} else {
				doCombatTangible(source, info, combatCommand, sourceWeapon, action, weaponDamageMod, damageType, tangibleTarget)
			}
		}

		source.sendObservers(action)
	}

	private fun doCombatCreature(source: CreatureObject, info: AttackInfo, combatCommand: CombatCommand, sourceWeapon: WeaponObject, action: CombatAction, weaponDamageMod: Double, damageType: DamageType, creatureTarget: CreatureObject) {
		val toHit = calculateToHit(source, sourceWeapon, creatureTarget)

		if (toHitDie.roll(IntRange(0, 100)) > toHit) {
			info.isSuccess = false

			val missFlyText = ShowFlyText(creatureTarget.objectId, StringId("combat_effects", "miss"), ShowFlyText.Scale.MEDIUM, white)
			creatureTarget.sendSelf(missFlyText)
			source.sendSelf(missFlyText)

			for (observerCreature in creatureTarget.observerCreatures) {
				observerCreature.sendSelf(createCombatSpam(observerCreature, source, creatureTarget, sourceWeapon, info, combatCommand, CombatSpamType.MISS))
			}

			action.addDefender(Defender(creatureTarget.objectId, creatureTarget.posture, false, 0.toByte(), combatCommand.hitLocation, 0.toShort()))
			return
		}

		val knockdownChance = combatCommand.knockdownChance
		if (knockdownChance > 0) {
			if (knockdownDie.roll(IntRange(0, 100)) < knockdownChance) {
				KnockdownIntent(creatureTarget).broadcast()
			} else {
				val yourAttackFailedToKnockDownYourOpponent = "@cbt_spam:knockdown_fail"
				broadcastPersonal(source.owner!!, yourAttackFailedToKnockDownYourOpponent)
			}
		}

		addBuff(source, creatureTarget, combatCommand.buffNameTarget) // Add target buff

		var rawDamage = calculateBaseDamage(combatCommand, sourceWeapon, weaponDamageMod)

		if (creatureTarget.posture == Posture.KNOCKED_DOWN) {
			rawDamage = (rawDamage * 1.5).toInt()
		}

		info.damageType = damageType
		info.rawDamage = rawDamage
		info.finalDamage = rawDamage

		if (combatCommand.isBlinding) {
			ApplyCombatStateIntent(source, creatureTarget, BlindedCombatState()).broadcast()
		}

		if (combatCommand.isBleeding) {
			ApplyCombatStateIntent(source, creatureTarget, BleedingCombatState()).broadcast()
		}

		if (combatCommand.isStunning) {
			ApplyCombatStateIntent(source, creatureTarget, StunnedCombatState()).broadcast()
		}

		// The armor of the target will mitigate some damage
		if (isWearingPhysicalArmor(creatureTarget)) {
			physicalArmorMitigate(info, damageType, creatureTarget, combatCommand)
		} else if (hasInnateJediArmor(creatureTarget)) {
			innateJediArmorMitigate(info, creatureTarget)
		} else if (hasInnateTerasKasiArmor(creatureTarget)) {
			innateTerasKasiArmorMitigate(info, creatureTarget)
		}

		// End rolls
		val targetHealth = creatureTarget.health - creatureTarget.healthWounds

		val finalDamage: Int
		if (targetHealth <= info.finalDamage) {
			finalDamage = targetHealth // Target took more damage than they had health left. Final damage becomes the amount of remaining health.
			RequestCreatureDeathIntent(source, creatureTarget).broadcast()
		} else {
			finalDamage = info.finalDamage
			creatureTarget.modifyHealth(-finalDamage)
		}

		info.finalDamage = finalDamage

		val sourceWeaponWoundChance = sourceWeapon.woundChance
		if (sourceWeaponWoundChance > 0) {
			if (woundDie.roll(IntRange(0, 100)) < sourceWeaponWoundChance) {
				val wounds = 1
				applyHealthWounds(creatureTarget, wounds)
			}
		}

		for (observerCreature in creatureTarget.observerCreatures) {
			observerCreature.sendSelf(createCombatSpam(observerCreature, source, creatureTarget, sourceWeapon, info, combatCommand, CombatSpamType.HIT))
		}

		action.addDefender(Defender(creatureTarget.objectId, creatureTarget.posture, true, 0.toByte(), combatCommand.hitLocation, finalDamage.toShort()))

		var hate = (finalDamage * combatCommand.hateDamageModifier).toInt()
		hate += combatCommand.hateAdd
		creatureTarget.handleHate(source, hate)

		val bothUsingMelee = creatureTarget.equippedWeapon.type.isMelee && source.equippedWeapon.type.isMelee

		if (bothUsingMelee) {
			val percentOfDamageToReflectBackToAttacker = creatureTarget.getSkillModValue("private_melee_dmg_shield") / 100.0

			if (percentOfDamageToReflectBackToAttacker > 0) {
				riposte(source, creatureTarget, rawDamage, percentOfDamageToReflectBackToAttacker)
			}
		}
	}

	companion object {
		private const val JEDI_ARMOR_SKILL_MOD = "jedi_armor"
		private const val TKA_ARMOR_SKILL_MOD = "tka_armor"

		private fun doCombatTangible(source: CreatureObject, info: AttackInfo, combatCommand: CombatCommand, sourceWeapon: WeaponObject, action: CombatAction, weaponDamageMod: Double, damageType: DamageType, tangibleTarget: TangibleObject) {
			val rawDamage = calculateBaseDamage(combatCommand, sourceWeapon, weaponDamageMod)
			val nextConditionDamage = tangibleTarget.conditionDamage + rawDamage
			info.isSuccess = true
			info.damageType = damageType
			info.rawDamage = rawDamage
			info.finalDamage = rawDamage

			if (nextConditionDamage > tangibleTarget.maxHitPoints) {
				DestroyObjectIntent(tangibleTarget).broadcast()
				ExitCombatIntent(tangibleTarget).broadcast()
			} else {
				tangibleTarget.conditionDamage = nextConditionDamage
			}

			for (observerCreature in tangibleTarget.observerCreatures) {
				observerCreature.sendSelf(createCombatSpam(observerCreature, source, tangibleTarget, sourceWeapon, info, combatCommand, CombatSpamType.HIT))
			}

			action.addDefender(Defender(tangibleTarget.objectId, Posture.UPRIGHT, true, 0.toByte(), combatCommand.hitLocation, info.finalDamage.toShort()))
		}

		private fun applyHealthWounds(creatureTarget: CreatureObject, healthWounds: Int) {
			val existingHealthWounds = creatureTarget.healthWounds
			creatureTarget.healthWounds = existingHealthWounds + healthWounds
		}

		private fun calculateBaseDamage(combatCommand: CombatCommand, sourceWeapon: WeaponObject, weaponDamageMod: Double): Int {
			var weaponDamage: Int = calculateBaseWeaponDamage(sourceWeapon, combatCommand)
			val addedDamage = combatCommand.addedDamage

			weaponDamage = (weaponDamage + weaponDamageMod).toInt()

			return weaponDamage + addedDamage
		}

		private fun innateJediArmorMitigate(info: AttackInfo, target: CreatureObject) {
			innateArmorMitigate(info, target, JEDI_ARMOR_SKILL_MOD)
		}

		private fun innateTerasKasiArmorMitigate(info: AttackInfo, target: CreatureObject) {
			innateArmorMitigate(info, target, TKA_ARMOR_SKILL_MOD)
		}

		private fun hasInnateTerasKasiArmor(target: CreatureObject): Boolean {
			return target.getSkillModValue(TKA_ARMOR_SKILL_MOD) > 0
		}

		private fun hasInnateJediArmor(target: CreatureObject): Boolean {
			return target.getSkillModValue(JEDI_ARMOR_SKILL_MOD) > 0
		}

		private fun isWearingPhysicalArmor(target: CreatureObject): Boolean {
			val slottedObjects = target.slottedObjects

			for (slottedObject in slottedObjects) {
				if (slottedObject is TangibleObject) {
					if (slottedObject.protection != null) {
						return true
					}
				}
			}

			return false
		}

		private fun riposte(source: CreatureObject, target: CreatureObject, rawDamage: Int, percentOfDamageToReflectBackToAttacker: Double) {
			val reflectedDamage = (percentOfDamageToReflectBackToAttacker * rawDamage).toInt()

			val riposteFlytext = ShowFlyText(target.objectId, StringId("cbt_spam", "dmg_shield_melee_fly"), ShowFlyText.Scale.MEDIUM, orangered)
			target.sendSelf(riposteFlytext)
			source.sendSelf(riposteFlytext)

			val spamMessage = OutOfBandPackage(ProsePackage(StringId("cbt_spam", "dmg_shield_melee_spam"), "TU", target.objectName, "TT", source.objectName, "DI", reflectedDamage))
			sendRiposteCombatSpam(target, spamMessage)
			sendRiposteCombatSpam(source, spamMessage)

			if (source.health < reflectedDamage) {
				// Took more damage than they had health left. Final damage becomes the amount of remaining health.
				RequestCreatureDeathIntent(source, target).broadcast()
			} else {
				source.modifyHealth(-reflectedDamage)
			}
		}

		private fun sendRiposteCombatSpam(receiver: CreatureObject, spamMessage: OutOfBandPackage) {
			val riposteCombatSpamTo = CombatSpam(receiver.objectId)
			riposteCombatSpamTo.dataType = 2.toByte()
			riposteCombatSpamTo.spamMessage = spamMessage
			riposteCombatSpamTo.spamType = CombatSpamType.HIT
			receiver.sendSelf(riposteCombatSpamTo)
		}

		private fun calculateToHit(source: CreatureObject, sourceWeapon: WeaponObject, target: CreatureObject): Double {
			var toHit = 66
			toHit += (calculateAccMod(source, sourceWeapon) - calculateDefMod(source, target)) / 10
			toHit += calculateDefPosMod(sourceWeapon, target)
			toHit += calculateAtkPosMod(source)
			toHit -= calculateBlindModifier(source) // Blind attackers have a hard time hitting anything
			toHit += calculateBlindModifier(target) // Blind targets have a hard time avoiding anything

			return max(32.0, toHit.toDouble())
		}

		private fun calculateBlindModifier(target: CreatureObject): Int {
			if (target.isStatesBitmask(CreatureState.BLINDED)) {
				return 50
			}

			// TODO Defenders modifiers for being stunned, or intimidated. Example of Defender Modifier would be +50 signifying the defender being easier to hit. Stunned and intimidate factors are unknown but it is estimated that they lower primary (melee and ranged) defenses by -50
			return 0
		}

		private fun calculateAtkPosMod(source: CreatureObject): Int {
			if (Locomotion.RUNNING.isActive(source)) {
				return -50
			}

			if (Locomotion.STANDING.isActive(source)) {
				return 0
			}

			if (Locomotion.KNEELING.isActive(source)) {
				return 16
			}

			if (Locomotion.PRONE.isActive(source)) {
				return 50
			}

			return 0
		}

		private fun calculateDefPosMod(sourceWeapon: WeaponObject, target: CreatureObject): Int {
			if (Locomotion.RUNNING.isActive(target)) {
				return -25
			}

			if (Locomotion.STANDING.isActive(target)) {
				return 0
			}

			if (Locomotion.KNEELING.isActive(target)) {
				val sourceWeaponType = sourceWeapon.type

				if (sourceWeaponType.isRanged) {
					return -16
				}

				if (sourceWeaponType.isMelee) {
					return 16
				}
			}

			if (Locomotion.PRONE.isActive(target)) {
				val sourceWeaponType = sourceWeapon.type

				if (sourceWeaponType.isRanged) {
					return -25
				}

				if (sourceWeaponType.isMelee) {
					return 25
				}
			}

			return 0
		}

		private fun calculateAccMod(source: CreatureObject, sourceWeapon: WeaponObject): Int {
			val sourceWeaponType = sourceWeapon.type
			val accuracySkillMods = sourceWeaponType.accuracySkillMods
			var accMod = sourceWeapon.accuracy

			for (accuracySkillMod in accuracySkillMods) {
				accMod += source.getSkillModValue(accuracySkillMod)
			}

			accMod += source.getSkillModValue("private_accuracy_bonus")

			return accMod
		}

		private fun calculateDefMod(source: CreatureObject, target: CreatureObject): Int {
			var defMod = 0

			defMod += calculateDefModWhenWieldingWeaponType(target)
			defMod += calculateDefModAgainstWeaponClass(source, target)
			defMod += target.getSkillModValue("private_defense_bonus")

			return defMod
		}

		private fun calculateDefModAgainstWeaponClass(source: CreatureObject, target: CreatureObject): Int {
			val sourceWeapon = source.equippedWeapon
			val sourceWeaponType = sourceWeapon.type
			val sourceWeaponClass = sourceWeaponType.weaponClass

			return target.getSkillModValue(sourceWeaponClass.defenseSkillMod)
		}

		private fun calculateDefModWhenWieldingWeaponType(target: CreatureObject): Int {
			val targetWeapon = target.equippedWeapon
			val targetWeaponType = targetWeapon.type

			val defenseSkillMod = targetWeaponType.defenseSkillMod

			if (defenseSkillMod != null) {
				return target.getSkillModValue(defenseSkillMod)
			}

			return 0
		}

		private fun calculateWeaponDamageMod(source: CreatureObject, weapon: WeaponObject): Int {
			val type = weapon.type

			return if (type == WeaponType.UNARMED) {
				source.getSkillModValue("unarmed_damage")
			} else {
				0
			}
		}

		private fun physicalArmorMitigate(info: AttackInfo, damageType: DamageType, target: CreatureObject, command: CombatCommand) {
			// Armor mitigation
			val armor = getArmor(damageType, target)
			val armorReduction = getArmorReduction(armor, command)
			var currentDamage = info.finalDamage
			val armorAbsorbed = (currentDamage * armorReduction).toInt()
			currentDamage -= armorAbsorbed

			info.armor = armor.toLong() // Assumed to be the amount of armor points the defender has against the primary damage type
			info.blockedDamage = armorAbsorbed // Describes how many points of damage the armor absorbed

			info.finalDamage = currentDamage
		}

		private fun innateArmorMitigate(info: AttackInfo, target: CreatureObject, skillMod: String) {
			var damageReduction = target.getSkillModValue(skillMod) / 100f
			damageReduction -= getArmorBreakPercent(target).toFloat()
			var currentDamage = info.finalDamage
			val armorAbsorbed = (currentDamage * damageReduction).toInt()
			currentDamage -= armorAbsorbed

			info.finalDamage = currentDamage
		}

		private fun getArmor(damageType: DamageType, creature: CreatureObject): Int {
			val armProtection = 7
			val protectionMap = mapOf(
				Pair("chest2", 35),
				Pair("pants1", 20),
				Pair("hat", 14),
				Pair("bracer_upper_l", armProtection),
				Pair("bracer_upper_r", armProtection),
				Pair("bicep_l", armProtection),
				Pair("bicep_r", armProtection),
				Pair("utility_belt", 3),
			)

			var armor = 0.0

			for ((slot, value) in protectionMap) {
				val slottedObject = creature.getSlottedObject(slot) as TangibleObject? ?: continue
				val protection = slottedObject.protection

				if (protection != null) {
					val protectionFromArmorPiece = when (damageType) {
						DamageType.KINETIC              -> protection.kinetic
						DamageType.ENERGY               -> protection.energy
						DamageType.ELEMENTAL_HEAT       -> protection.heat
						DamageType.ELEMENTAL_COLD       -> protection.cold
						DamageType.ELEMENTAL_ACID       -> protection.acid
						DamageType.ELEMENTAL_ELECTRICAL -> protection.electricity
						else                            -> 0
					}

					armor += protectionFromArmorPiece * (value / 100.0)
				}
			}

			val armorBreakPercent = getArmorBreakPercent(creature)

			if (armorBreakPercent > 0) {
				armor *= (1 - armorBreakPercent / 100.0)
			}

			return armor.toInt()
		}

		private fun getArmorBreakPercent(creature: CreatureObject): Double {
			val privateArmorBreak = creature.getSkillModValue("private_armor_break")
			return privateArmorBreak / 10.0
		}

		/**
		 * @param command to get the damage type of
		 * @param weapon  fallback in case the combat command does not provide its own `DamageType`
		 * @return `DamageType` of either the `command` or the `weapon`.
		 */
		private fun getDamageType(command: CombatCommand, weapon: WeaponObject): DamageType {
			return if (command.percentAddFromWeapon > 0) weapon.damageType else command.elementalType
		}

		private fun getArmorReduction(baseArmor: Int, command: CombatCommand): Float {
			var baseArmor = baseArmor
			val commandBypassArmor = command.bypassArmor

			if (commandBypassArmor > 0) {
				// This command bypasses armor
				baseArmor = (baseArmor * (1.0 - commandBypassArmor)).toInt()
			}

			val mitigation = (90 * (1 - exp(-0.000125 * baseArmor))).toFloat() + baseArmor / 9000f

			return mitigation / 100
		}
	}
}
