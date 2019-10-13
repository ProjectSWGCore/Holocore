/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.common.data.RGB;
import com.projectswg.common.data.combat.*;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.object_controller.Animation;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.gameplay.combat.RequestCreatureDeathIntent;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.*;

enum CombatCommandAttack implements CombatCommandHitType {
	INSTANCE;
	
	@Override
	public void handle(CreatureObject source, SWGObject target, CombatCommand command, String arguments) {
		handle(source, target, null, command);
	}
	
	public void handle(CreatureObject source, SWGObject target, SWGObject delayEgg, CombatCommand command) {
		if (!handleStatus(source, canPerform(source, target, command)))
			return;
		
		WeaponObject weapon = source.getEquippedWeapon();
		
		for (int i = 0; i < command.getAttackRolls(); i++) {
			AttackInfo info = new AttackInfo();
			
			switch (command.getAttackType()) {
				case SINGLE_TARGET:
					doCombatSingle(source, target, info, weapon, command);
					break;
				case AREA:
					doCombatArea(source, source, info, weapon, command, false);
					break;
				case TARGET_AREA:
					if (target != null) {
						// Same as AREA, but the target is the destination for the AoE and  can take damage
						doCombatArea(source, delayEgg != null ? delayEgg : target, info, weapon, command, true);
					} else {
						// TODO AoE based on Location instead of delay egg
					}
					break;
				default:
					break;
			}
		}
	}
	
	private static void doCombatSingle(CreatureObject source, SWGObject target, AttackInfo info, WeaponObject weapon, CombatCommand command) {
		// TODO single target only defence rolls against target
		if (target instanceof CreatureObject && isAttackDodged(source, (CreatureObject) target)) {
			info.setDodge(true);
			info.setSuccess(false);	// This means that the attack did no damage to the target at all
			long targetObjectId = target.getObjectId();
			
			// Play dodge animation on the creature that's dodging for everyone that can see the creature
			target.sendObservers(new Animation(targetObjectId, "dodge"));
			
			// Send flytext to the relevant receivers
			ShowFlyText showFlyText = new ShowFlyText(targetObjectId, new StringId("combat_effects", "dodge"), ShowFlyText.Scale.SMALLEST, new RGB(255, 255, 255));
			target.sendSelf(showFlyText);
			source.sendSelf(showFlyText);
		}
		
		// TODO single target only offence rolls for source
		
		// TODO below logic should be in CommandService when target checks are implemented in there
		Set<CreatureObject> targets = new HashSet<>();
		
		if (target instanceof CreatureObject)
			targets.add((CreatureObject) target);
		
		doCombat(source, targets, weapon, info, command);
	}
	
	private static void doCombatArea(CreatureObject source, SWGObject origin, AttackInfo info, WeaponObject weapon, CombatCommand command, boolean includeOrigin) {
		double aoeRange = command.getConeLength();
		SWGObject originParent = origin.getParent();
		Collection<SWGObject> objectsToCheck = originParent == null ? origin.getObjectsAware() : originParent.getContainedObjects();
		
		// TODO block
		// TODO evasion if no block
		
		// TODO line of sight checks between the explosive and each target
		Set<CreatureObject> targets = objectsToCheck.stream()
				.filter(CreatureObject.class::isInstance)
				.map(CreatureObject.class::cast)
				.filter(target -> !target.equals(source))	// Make sure the attacker can't damage themselves
				.filter(source::isAttackable)
				.filter(target -> canPerform(source, target, command) == CombatStatus.SUCCESS)
				.filter(creature -> origin.getLocation().distanceTo(creature.getLocation()) <= aoeRange)
				.collect(Collectors.toSet());
		
		// This way, mines or grenades won't try to harm themselves
		if (includeOrigin && origin instanceof CreatureObject)
			targets.add((CreatureObject) origin);
		
		doCombat(source, targets, weapon, info, command);
	}
	
	private static void doCombat(CreatureObject source, Set<CreatureObject> targets, WeaponObject weapon, AttackInfo info, CombatCommand command) {
		source.updateLastCombatTime();
		
		CombatAction action = createCombatAction(source, weapon, TrailLocation.WEAPON, command);
		boolean devastating = isAttackDevastating(source, command);
		
		if (devastating) {
			// Show Devastation flytext above the source for every target
			ShowFlyText devastationFlyText = new ShowFlyText(source.getObjectId(), new StringId("combat_effects", "devastation"), ShowFlyText.Scale.MEDIUM, new RGB(255, 255, 255));
			
			source.sendSelf(devastationFlyText);
			
			for (CreatureObject target : targets) {
				target.sendSelf(devastationFlyText);
			}
		}
		
		for (CreatureObject target : targets) {
			target.updateLastCombatTime();
			
			EnterCombatIntent.broadcast(source, target);
			EnterCombatIntent.broadcast(target, source);
			
			if (!info.isSuccess()) {    // Single target negate, like dodge or parry!
				target.sendObservers(createCombatSpam(source, target, weapon, info, command));
				action.addDefender(new Defender(target.getObjectId(), target.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
				continue;	// This target negated the attack completely - move on to the next target
			}
			
			addBuff(source, target, command.getBuffNameTarget());    // Add target buff
			
			DamageType damageType = getDamageType(command, weapon);	// Will be based on the equipped weapon or the combat command
			int weaponDamage = calculateWeaponDamage(source, weapon, command);
			
			if (devastating) {
				weaponDamage *= 1.5;	// Devastation increases raw weapon damage by 50%;
			}
			
			int rawDamage = weaponDamage + command.getAddedDamage();
			
			info.setRawDamage(rawDamage);
			info.setFinalDamage(rawDamage);
			info.setDamageType(damageType);
			
			// The armor of the target will mitigate some of the damage
			armorMitigate(info, damageType, target, command);
			
			// TODO block roll for defender
			// TODO Critical hit roll for attacker
			
			// End rolls
			int targetHealth = target.getHealth();
			
			final int finalDamage;
			if (targetHealth <= info.getFinalDamage()) {
				finalDamage = targetHealth;	// Target took more damage than they had health left. Final damage becomes the amount of remaining health.
				RequestCreatureDeathIntent.broadcast(source, target);
			} else {
				finalDamage = info.getFinalDamage();
				target.modifyHealth(-finalDamage);
			}
			
			info.setFinalDamage(finalDamage);
			
			target.sendObservers(createCombatSpam(source, target, weapon, info, command));
			action.addDefender(new Defender(target.getObjectId(), target.getPosture(), true, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) finalDamage));
			
			target.handleDamage(source, finalDamage);
		}
		
		source.sendObservers(action);
	}
	
	private static void armorMitigate(AttackInfo info, DamageType damageType, CreatureObject target, CombatCommand command) {
		// Armor mitigation
		int armor = getArmor(damageType, target);
		float armorReduction = getArmorReduction(target, damageType, command);
		int currentDamage = info.getFinalDamage();
		int armorAbsorbed = (int) (currentDamage * armorReduction);
		currentDamage -= armorAbsorbed;
		
		info.setArmor(armor);	// Assumed to be the amount of armor points the defender has against the primary damage type
		info.setBlockedDamage(armorAbsorbed);	// Describes how many points of damage the armor absorbed
		
		info.setFinalDamage(currentDamage);
	}
	
	private static int getArmor(DamageType damageType, CreatureObject creature) {
		switch (damageType) {
			case KINETIC:				return creature.getSkillModValue("kinetic");
			case ENERGY:				return creature.getSkillModValue("energy");
			case ELEMENTAL_HEAT:		return creature.getSkillModValue("heat");
			case ELEMENTAL_COLD:		return creature.getSkillModValue("cold");
			case ELEMENTAL_ACID:		return creature.getSkillModValue("acid");
			case ELEMENTAL_ELECTRICAL:	return creature.getSkillModValue("electricity");
			default:					return 0;
		}
	}
	
	/**
	 *
	 * @param command to get the damage type of
	 * @param weapon fallback in case the combat command does not provide its own {@code DamageType}
	 * @return {@code DamageType} of either the {@code command} or the {@code weapon}.
	 */
	private static DamageType getDamageType(CombatCommand command, WeaponObject weapon) {
		return command.getPercentAddFromWeapon() > 0 ? weapon.getDamageType() : command.getElementalType();
	}
	
	/**
	 *
	 * @param target to read armor values from
	 * @param damageType to get an armor value for
	 * @param command that has been executed by an enemy of {@code target}
	 * @return a number between 0.0 and 1.0
	 */
	private static float getArmorReduction(CreatureObject target, DamageType damageType, CombatCommand command) {
		int baseArmor = getArmor(damageType, target);

		double commandBypassArmor = command.getBypassArmor();
		
		if(commandBypassArmor > 0) {
			// This command bypasses armor
			baseArmor *= 1.0 - commandBypassArmor;
		}
		
		float mitigation = (float) (90 * (1 - Math.exp(-0.000125 * baseArmor))) + baseArmor / 9000f;
		
		return mitigation / 100;
		
	}
	
	private static boolean isAttackDodged(CreatureObject source, CreatureObject target) {
		double dodgeChance = (target.getSkillModValue("display_only_dodge") - source.getSkillModValue("display_only_opp_dodge_reduction")) / 100;
		double roll = ThreadLocalRandom.current().nextDouble(100);	// Generate number between 0 and 100
		
		return roll <= dodgeChance;	// If dodge chance is 25%, then the roll should be between 0 and 25 (both inclusive)
	}
	
	private static boolean isAttackDevastating(CreatureObject source, CombatCommand command) {
		if (command.getPercentAddFromWeapon() == 0) {
			// If this ability doesn't use weapon damage it does not qualify for a devastation roll
			return false;
		}
		
		WeaponObject weapon = source.getEquippedWeapon();
		WeaponType type = weapon.getType();
		
		if (!WeaponType.HEAVY_WEAPON.equals(type) && !WeaponType.DIRECTIONAL_TARGET_WEAPON.equals(type)) {
			// If the weapon type isn't a heavy weapon and isn't a flamethrower, then the weapon cannot roll a devastation
			return false;
		}
		
		double devastationChance = source.getSkillModValue("expertise_devastation_bonus") / 10;
		double roll = ThreadLocalRandom.current().nextDouble(100);	// Generate number between 0 and 100
		
		return roll <= devastationChance;	// If devastation chance is 20%, then the roll should be between 0 and 20 (both inclusive)
	}
	
}
