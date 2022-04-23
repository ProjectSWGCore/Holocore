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
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.gameplay.combat.RequestCreatureDeathIntent;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
						// TODO AoE based on Location instead of delay egg (free-targeting with heavy weapons)
					}
					break;
				case CONE:
					if (target != null) {
						doCombatCone(source, target, info, weapon, command);
					} else {
						// TODO CoE based on Location (free-targeting with flamethrowers)
					}
					break;
				default:
					break;
			}
		}
	}
	
	private void doCombatCone(CreatureObject source, Location targetWorldLocation, AttackInfo info, WeaponObject weapon, CombatCommand command) {
		double coneLength = command.getConeLength();
		double coneWidth = command.getConeWidth();
		
		Location sourceWorldLocation = source.getWorldLocation();
		
		double dirX = targetWorldLocation.getX() - sourceWorldLocation.getX();
		double dirZ = targetWorldLocation.getZ() - sourceWorldLocation.getZ();
		
		Set<SWGObject> objectsToCheck = source.getObjectsAware();
		
		Set<CreatureObject> targets = objectsToCheck.stream()
				.filter(CreatureObject.class::isInstance)
				.map(CreatureObject.class::cast)
				.filter(source::isAttackable)
				.filter(candidate -> canPerform(source, candidate, command) == CombatStatus.SUCCESS)
				.filter(candidate -> sourceWorldLocation.distanceTo(candidate.getLocation()) <= coneLength)
				.filter(candidate -> {
					Location candidateWorldLocation = candidate.getWorldLocation();
					
					return isInConeAngle(sourceWorldLocation, candidateWorldLocation, coneWidth, dirX, dirZ);
				})
				.collect(Collectors.toSet());
		
		doCombat(source, targets, weapon, info, command);
	}

	private void doCombatCone(CreatureObject source, SWGObject target, AttackInfo info, WeaponObject weapon, CombatCommand command) {
		doCombatCone(source, target.getWorldLocation(), info, weapon, command);
	}

	boolean isInConeAngle(Location attackerLocation, Location targetLocation, double coneWidth, double directionX, double directionZ) {
		double targetX = targetLocation.getX() - attackerLocation.getX();
		double targetZ = targetLocation.getZ() - attackerLocation.getZ();
		
		double targetAngle = Math.atan2(targetZ, targetX) - Math.atan2(directionZ, directionX);
		
		double degrees = targetAngle * 180 / Math.PI;
		
		return !(Math.abs(degrees) > coneWidth);
	}
	
	private static void doCombatSingle(CreatureObject source, SWGObject target, AttackInfo info, WeaponObject weapon, CombatCommand command) {
		Set<CreatureObject> targets = new HashSet<>();
		
		if (target instanceof CreatureObject) {
			CreatureObject creatureTarget = (CreatureObject) target;

			targets.add(creatureTarget);
		}
		
		doCombat(source, targets, weapon, info, command);
	}
	
	private static void doCombatArea(CreatureObject source, SWGObject origin, AttackInfo info, WeaponObject weapon, CombatCommand command, boolean includeOrigin) {
		double aoeRange = command.getConeLength();
		SWGObject originParent = origin.getParent();
		Collection<SWGObject> objectsToCheck = originParent == null ? origin.getObjectsAware() : originParent.getContainedObjects();
		
		Set<CreatureObject> targets = objectsToCheck.stream()
				.filter(CreatureObject.class::isInstance)
				.map(CreatureObject.class::cast)
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
		double weaponDamageBoost = 1.0;	// Damage increase of the weapon
		double addedDamageBoost = 1.0;	// Damage increase of the command
		
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
			int addedDamage = command.getAddedDamage();
			
			weaponDamage *= weaponDamageBoost;
			addedDamage *= addedDamageBoost;
			
			int rawDamage = weaponDamage + addedDamage;
			
			info.setRawDamage(rawDamage);
			info.setFinalDamage(rawDamage);
			info.setDamageType(damageType);
			
			// The armor of the target will mitigate some of the damage
			armorMitigate(info, damageType, target, command);
			
			if (info.isGlancing()) {
				handleGlancingBlow(info, target);
			}
			
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
	
	private static void handleGlancingBlow(AttackInfo info, SWGObject target) {
		int finalDamage = info.getFinalDamage();
		
		// Glancing blows cause only 40% damage to be applied
		finalDamage *= 0.4;
		
		info.setFinalDamage(finalDamage);
		
		// Show Glancing flytext above the object that rolled a glancing blow
		ShowFlyText glancingFlyText = new ShowFlyText(target.getObjectId(), new StringId("combat_effects", "glancing_blow"), ShowFlyText.Scale.MEDIUM, new RGB(0, 160, 0));
		
		target.sendSelf(glancingFlyText);
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

}
