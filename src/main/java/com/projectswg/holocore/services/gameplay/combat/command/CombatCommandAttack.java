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
import com.projectswg.holocore.resources.support.global.commands.Locomotion;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.Protection;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
		
		for (int i = 0; i < command.getAttackRolls(); i++) {
			AttackInfo info = new AttackInfo();
			
			switch (command.getAttackType()) {
				case SINGLE_TARGET:
					doCombatSingle(source, target, info, command);
					break;
				case AREA:
					doCombatArea(source, source, info, command, false);
					break;
				case TARGET_AREA:
					if (target != null) {
						// Same as AREA, but the target is the destination for the AoE and  can take damage
						doCombatArea(source, delayEgg != null ? delayEgg : target, info, command, true);
					} else {
						// TODO AoE based on Location instead of delay egg (free-targeting with heavy weapons)
					}
					break;
				case CONE:
					if (target != null) {
						doCombatCone(source, target, info, command);
					} else {
						// TODO CoE based on Location (free-targeting with flamethrowers)
					}
					break;
				default:
					break;
			}
		}
	}
	
	private void doCombatCone(CreatureObject source, Location targetWorldLocation, AttackInfo info, CombatCommand command) {
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
		
		doCombat(source, targets, info, command);
	}

	private void doCombatCone(CreatureObject source, SWGObject target, AttackInfo info, CombatCommand command) {
		doCombatCone(source, target.getWorldLocation(), info, command);
	}

	boolean isInConeAngle(Location attackerLocation, Location targetLocation, double coneWidth, double directionX, double directionZ) {
		double targetX = targetLocation.getX() - attackerLocation.getX();
		double targetZ = targetLocation.getZ() - attackerLocation.getZ();
		
		double targetAngle = Math.atan2(targetZ, targetX) - Math.atan2(directionZ, directionX);
		
		double degrees = targetAngle * 180 / Math.PI;
		
		return !(Math.abs(degrees) > coneWidth);
	}
	
	private static void doCombatSingle(CreatureObject source, SWGObject target, AttackInfo info, CombatCommand command) {
		Set<CreatureObject> targets = new HashSet<>();
		
		if (target instanceof CreatureObject creatureTarget) {
			targets.add(creatureTarget);
		}
		
		doCombat(source, targets, info, command);
	}
	
	private static void doCombatArea(CreatureObject source, SWGObject origin, AttackInfo info, CombatCommand command, boolean includeOrigin) {
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
		
		doCombat(source, targets, info, command);
	}
	
	private static void doCombat(CreatureObject source, Set<CreatureObject> targets, AttackInfo info, CombatCommand command) {
		source.updateLastCombatTime();
		WeaponObject sourceWeapon = source.getEquippedWeapon();
		
		CombatAction action = createCombatAction(source, sourceWeapon, TrailLocation.WEAPON, command);
		double weaponDamageMod = calculateWeaponDamageMod(source, sourceWeapon);
		double addedDamageBoost = 1.0;	// Damage increase of the command
		
		for (CreatureObject target : targets) {
			target.updateLastCombatTime();
			
			EnterCombatIntent.broadcast(source, target);
			EnterCombatIntent.broadcast(target, source);
			
			double toHit = calculateToHit(source, sourceWeapon, target);
			int diceRoll = randomNumberBetween0And100();
			
			if (diceRoll > toHit) {
				info.setSuccess(false);
				
				ShowFlyText missFlyText = new ShowFlyText(target.getObjectId(), new StringId("combat_effects", "miss"), ShowFlyText.Scale.MEDIUM, new RGB(255, 255, 255));
				target.sendSelf(missFlyText);
				source.sendSelf(missFlyText);
				
				for (CreatureObject observerCreature : target.getObserverCreatures()) {
					observerCreature.sendSelf(createCombatSpam(observerCreature, source, target, sourceWeapon, info, command, CombatSpamType.MISS));
				}
				
				action.addDefender(new Defender(target.getObjectId(), target.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
				continue;	// This target negated the attack completely - move on to the next target
			}
			
			addBuff(source, target, command.getBuffNameTarget());    // Add target buff
			
			DamageType damageType = getDamageType(command, sourceWeapon);	// Will be based on the equipped weapon or the combat command
			int weaponDamage = calculateBaseWeaponDamage(sourceWeapon, command);
			int addedDamage = command.getAddedDamage();
			
			weaponDamage += weaponDamageMod;
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
			
			for (CreatureObject observerCreature : target.getObserverCreatures()) {
				observerCreature.sendSelf(createCombatSpam(observerCreature, source, target, sourceWeapon, info, command, CombatSpamType.HIT));
			}
			
			action.addDefender(new Defender(target.getObjectId(), target.getPosture(), true, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) finalDamage));
			
			target.handleDamage(source, finalDamage);
		}
		
		source.sendObservers(action);
	}
	
	private static double calculateToHit(CreatureObject source, WeaponObject sourceWeapon, CreatureObject target) {
		int accMod = calculateAccMod(source, sourceWeapon);
		int defMod = calculateDefMod(target);
		int defPosMod = calculateDefPosMod(sourceWeapon, target);
		int aimShot = 0;	// TODO The sum of your General Ranged Aiming, and weapon-specific Aiming mods. Only applies if you use Aim prior to your attack.
		int covMod = 0;	// TODO Defense Modifier for the take cover ability
		int atkPosMod = calculateAtkPosMod(source);
		int atkStateMod = 0; // TODO Attackers modifiers for being blind or intimidated. Example of Attacker modifier would be -50 signifying that the attacker suffers a penalty to accuracy. Intimidate and Blind state penalties are unknown factors but it is estimated to be -50 penalty to the hit chance.
		int defStateMod = 0;	// TODO Defenders modifiers for being stunned, or intimidated . Example of Defender Modifier would be +50 signifying the defender being easier to hit. Stunned and intimidate factors are unknown but it is estimated that they lower primary (melee and ranged) defenses by -50
		
		return 66 + ( accMod - defMod - defPosMod + aimShot - covMod ) / (2d + ( atkPosMod + atkStateMod + defStateMod ) );
	}
	
	private static int calculateAtkPosMod(CreatureObject source) {
		if (Locomotion.RUNNING.isActive(source)) {
			return -50;
		}
		
		if (Locomotion.STANDING.isActive(source)) {
			return 0;
		}
		
		if (Locomotion.KNEELING.isActive(source)) {
			return 16;
		}
		
		if (Locomotion.PRONE.isActive(source)) {
			return 50;
		}
		
		return 0;
	}
	
	private static int calculateDefPosMod(WeaponObject sourceWeapon, CreatureObject target) {
		if (Locomotion.RUNNING.isActive(target)) {
			return -25;
		}
		
		if (Locomotion.STANDING.isActive(target)) {
			return 0;
		}
		
		if (Locomotion.KNEELING.isActive(target)) {
			WeaponType sourceWeaponType = sourceWeapon.getType();
			
			if (sourceWeaponType.isRanged()) {
				return -16;
			}
			
			if (sourceWeaponType.isMelee()) {
				return 16;
			}
		}
		
		if (Locomotion.PRONE.isActive(target)) {
			WeaponType sourceWeaponType = sourceWeapon.getType();
			
			if (sourceWeaponType.isRanged()) {
				return -25;
			}
			
			if (sourceWeaponType.isMelee()) {
				return 25;
			}
		}
		
		return 0;
	}
	
	private static int randomNumberBetween0And100() {
		return ThreadLocalRandom.current().nextInt(0, 101);
	}
	
	private static int calculateAccMod(CreatureObject source, WeaponObject sourceWeapon) {
		WeaponType sourceWeaponType = sourceWeapon.getType();
		Collection<String> accuracySkillMods = sourceWeaponType.getAccuracySkillMods();
		int accMod = sourceWeapon.getAccuracy();
		
		for (String accuracySkillMod : accuracySkillMods) {
			accMod += source.getSkillModValue(accuracySkillMod);
		}
		
		return accMod;
	}
	
	private static int calculateDefMod(CreatureObject target) {
		int defMod = 0;
		WeaponObject targetWeapon = target.getEquippedWeapon();
		WeaponType targetWeaponType = targetWeapon.getType();
		Collection<String> defenseSkillMods = targetWeaponType.getDefenseSkillMods();
		for (String defenseSkillMod : defenseSkillMods) {
			defMod += target.getSkillModValue(defenseSkillMod);
		}
		
		defMod += target.getSkillModValue("private_defense_bonus");
		
		return defMod;
	}
	
	private static int calculateWeaponDamageMod(CreatureObject source, WeaponObject weapon) {
		WeaponType type = weapon.getType();
		
		if (type == WeaponType.UNARMED) {
			return source.getSkillModValue("unarmed_damage");
		} else {
			return 0;
		}
	}
	
	private static void armorMitigate(AttackInfo info, DamageType damageType, CreatureObject target, CombatCommand command) {
		// Armor mitigation
		int armor = getArmor(damageType, target);
		float armorReduction = getArmorReduction(armor, command);
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
		int armProtection = 7;
		Map<String, Integer> protectionMap = Map.of(
				"chest2", 35,
				"pants1", 20,
				"hat", 14,
				"bracer_upper_l", armProtection,
				"bracer_upper_r", armProtection,
				"bicep_l", armProtection,
				"bicep_r", armProtection,
				"utility_belt", 3
		);
		
		double armor = 0;
		
		for (Map.Entry<String, Integer> entry : protectionMap.entrySet()) {
			String slot = entry.getKey();
			TangibleObject slottedObject = (TangibleObject) creature.getSlottedObject(slot);
			
			if (slottedObject != null) {
				Protection protection = slottedObject.getProtection();
				
				if (protection != null) {
					int protectionFromArmorPiece = switch (damageType) {
						case KINETIC -> protection.getKinetic();
						case ENERGY -> protection.getEnergy();
						case ELEMENTAL_HEAT -> protection.getHeat();
						case ELEMENTAL_COLD -> protection.getCold();
						case ELEMENTAL_ACID -> protection.getAcid();
						case ELEMENTAL_ELECTRICAL -> protection.getElectricity();
						default -> 0;
					};
					
					Integer value = entry.getValue();
					
					armor += protectionFromArmorPiece * (value / 100d);
				}
			}
		}
		
		return (int) armor;
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
	
	private static float getArmorReduction(int baseArmor, CombatCommand command) {
		double commandBypassArmor = command.getBypassArmor();
		
		if(commandBypassArmor > 0) {
			// This command bypasses armor
			baseArmor *= 1.0 - commandBypassArmor;
		}
		
		float mitigation = (float) (90 * (1 - Math.exp(-0.000125 * baseArmor))) + baseArmor / 9000f;
		
		return mitigation / 100;
		
	}

}
