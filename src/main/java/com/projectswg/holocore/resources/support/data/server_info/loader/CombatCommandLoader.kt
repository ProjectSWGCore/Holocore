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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.combat.*
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import java.io.File
import java.io.IOException
import java.util.stream.Collectors

class CombatCommandLoader internal constructor() : DataLoader() {
	private val commandNameMap: MutableMap<String, CombatCommand> = HashMap()

	fun getCombatCommand(command: String, ownedCommands: Collection<String>): CombatCommand? {
		val ownedCommandsLowerCased = ownedCommands.stream().map { ownedCommand: String -> ownedCommand.lowercase() }.collect(Collectors.toSet())
		val basicVersion = command.lowercase()

		val advancedVersion = basicVersion + "_2"
		if (isVersionUsable(ownedCommandsLowerCased, advancedVersion)) {
			return commandNameMap[advancedVersion]
		}

		val improvedVersion = basicVersion + "_1"
		if (isVersionUsable(ownedCommandsLowerCased, improvedVersion)) {
			return commandNameMap[improvedVersion]
		}

		return commandNameMap[basicVersion]
	}

	private fun isVersionUsable(ownedCommandsLowerCased: Set<String>, version: String): Boolean {
		return ownedCommandsLowerCased.contains(version) && commandNameMap.containsKey(version)
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/command/combat_commands.sdb")).use { set ->
			while (set.next()) {				/*
				 * actionName             profession             Working                comment                commandType         validTarget                hitType  healAttrib  setCombatTarget
				 * triggerEffect          triggerEffectHardpoint effectOnTarget         delayAttackEggTemplate delayAttackParticle initialDelayAttackInterval
				 * delayAttackInterval    delayAttackLoops       delayAttackEggPosition validEggTarget         doClientAnim        forcesCharacterIntoCombat
				 * animDefault            anim_unarmed           anim_onehandmelee      anim_twohandmelee      anim_polearm        anim_pistol
				 * anim_lightRifle        anim_carbine           anim_rifle             anim_heavyweapon       anim_thrown         anim_onehandlightsaber
				 * anim_twohandlightsaber anim_polearmlightsaber attackType             coneLength             coneWidth           minRange
				 * maxRange               addedDamage            flatActionDamage       percentAddFromWeapon   bypassArmor         hateDamageModifier
				 * maxHate                hateAdd                hateAddTime            hateReduce             healthCost          actionCost
				 * vigorCost              mindCost               convertDamageToHealth  dotType                dotIntensity        dotDuration
				 * buffNameTarget         buffStrengthTarget     buffDurationTarget     buffNameSelf           buffStrengthSelf    buffDurationSelf
				 * canBePunishing         increaseCritical       increaseStrikethrough  reduceGlancing         reduceParry         reduceBlock
				 * reduceDodge            overloadWeapon         minDamage              maxDamage              maxOverloadRange    weaponType
				 * weaponCategory         damageType             elementalType          elementalValue         attackSpeed         damageRadius
				 * specialLine            cancelsAutoAttack      performance_spam       hit_spam               ignore_distance     pvp_only
				 * attack_rolls
				 */
				val command = CombatCommand.builder()
					.withName(set.getText("actionName").lowercase())
					.withDelayAttackEggTemplate(set.getText("delayAttackEggTemplate"))
					.withDelayAttackParticle(set.getText("delayAttackParticle"))
					.withInitialDelayAttackInterval(set.getReal("initialDelayAttackInterval"))
					.withDelayAttackInterval(set.getReal("delayAttackInterval"))
					.withDelayAttackLoops(set.getInt("delayAttackLoops").toInt())
					.withEggPosition(DelayAttackEggPosition.valueOf(set.getText("delayAttackEggPosition")))
					.withValidTarget(ValidTarget.valueOf(set.getText("validTarget")))
					.withHitType(HitType.valueOf(set.getText("hitType")))
					.withHealAttrib(HealAttrib.valueOf(set.getText("healAttrib")))
					.withForceCombat(set.getBoolean("forcesCharacterIntoCombat"))
					.withDefaultAnimation(getAnimationList(set.getText("animDefault")))
					.withAnimations(WeaponType.UNARMED, getAnimationList(set.getText("anim_unarmed")))
					.withAnimations(WeaponType.ONE_HANDED_MELEE, getAnimationList(set.getText("anim_onehandmelee")))
					.withAnimations(WeaponType.TWO_HANDED_MELEE, getAnimationList(set.getText("anim_twohandmelee")))
					.withAnimations(WeaponType.POLEARM_MELEE, getAnimationList(set.getText("anim_polearm")))
					.withAnimations(WeaponType.PISTOL, getAnimationList(set.getText("anim_pistol")))
					.withAnimations(WeaponType.CARBINE, getAnimationList(set.getText("anim_carbine")))
					.withAnimations(WeaponType.RIFLE, getAnimationList(set.getText("anim_rifle")))
					.withAnimations(WeaponType.THROWN, getAnimationList(set.getText("anim_thrown")))
					.withAnimations(WeaponType.ONE_HANDED_SABER, getAnimationList(set.getText("anim_onehandlightsaber")))
					.withAnimations(WeaponType.TWO_HANDED_SABER, getAnimationList(set.getText("anim_twohandlightsaber")))
					.withAnimations(WeaponType.POLEARM_SABER, getAnimationList(set.getText("anim_polearmlightsaber")))
					.withAttackType(AttackType.valueOf(set.getText("attackType")))
					.withConeLength(set.getReal("coneLength"))
					.withConeWidth(set.getReal("coneWidth"))
					.withAddedDamage(set.getInt("addedDamage").toInt())
					.withPercentAddFromWeapon(set.getReal("percentAddFromWeapon"))
					.withBypassArmor(set.getReal("bypassArmor"))
					.withHateDamageModifier(set.getReal("hateDamageModifier"))
					.withHateAdd(set.getInt("hateAdd").toInt())
					.withHealthCost(set.getReal("healthCost"))
					.withActionCost(set.getReal("actionCost"))
					.withMindCost(set.getReal("mindCost"))
					.withForceCost(set.getReal("forceCost"))
					.withForceCostModifier(set.getReal("fcModifier"))
					.withKnockdownChance(set.getReal("knockdownChance"))
					.withBlinding(set.getBoolean("blinding"))
					.withBleeding(set.getBoolean("bleeding"))
					.withDizzying(set.getBoolean("dizzy"))
					.withStunning(set.getBoolean("stun"))
					.withTriggerEffect(set.getText("triggerEffect"))
					.withTriggerEffectHardpoint(set.getText("triggerEffectHardpoint"))
					.withTargetEffect(set.getText("targetEffect"))
					.withTargetEffectHardpoint(set.getText("targetEffectHardpoint"))
					.withBuffNameTarget(set.getText("buffNameTarget"))
					.withBuffNameSelf(set.getText("buffNameSelf"))
					.withDamageType(DamageType.valueOf(set.getText("damageType")))
					.withElementalType(DamageType.valueOf(set.getText("elementalType")))
					.withIgnoreDistance(set.getBoolean("ignore_distance"))
					.withPvpOnly(set.getBoolean("pvp_only"))
					.withMaxRange(set.getReal("maxRange"))
					.withHitLocation(HitLocation.valueOf(set.getText("hitLocation")))
					.build()
				commandNameMap[command.name] = command
			}
		}
	}

	companion object {
		private fun getAnimationList(cell: String): Array<String> {
			if (cell.isEmpty()) return Array(0) { "" }
			return cell.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		}
	}
}
