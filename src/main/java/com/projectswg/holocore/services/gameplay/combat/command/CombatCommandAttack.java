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

import com.projectswg.common.data.combat.*;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam;
import com.projectswg.holocore.intents.gameplay.combat.ApplyCombatStateIntent;
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.gameplay.combat.KnockdownIntent;
import com.projectswg.holocore.intents.gameplay.combat.RequestCreatureDeathIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.color.SWGColor;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.global.commands.Locomotion;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.tangible.Protection;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponClass;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import com.projectswg.holocore.services.gameplay.combat.CombatState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.*;

enum CombatCommandAttack implements CombatCommandHitType {
	INSTANCE;
	
	private static final String jediArmorSkillMod = "jedi_armor";
	private static final String tkaArmorSkillMod = "tka_armor";
	
	@Override
	public CombatStatus handle(@NotNull CreatureObject source, @Nullable SWGObject target, @NotNull CombatCommand command, @NotNull String arguments) {
		return handle(source, target, null, command);
	}
	
	public CombatStatus handle(CreatureObject source, SWGObject target, SWGObject delayEgg, CombatCommand command) {
		CombatStatus combatStatus = canPerform(source, target, command);
		if (combatStatus != CombatStatus.SUCCESS) {
			return combatStatus;
		}
		
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
		
		return combatStatus;
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
		
		for (CreatureObject target : targets) {
			target.updateLastCombatTime();
			
			EnterCombatIntent.broadcast(source, target);
			EnterCombatIntent.broadcast(target, source);
			
			double toHit = calculateToHit(source, sourceWeapon, target);
			
			if (randomNumberBetween0And100() > toHit) {
				info.setSuccess(false);
				
				ShowFlyText missFlyText = new ShowFlyText(target.getObjectId(), new StringId("combat_effects", "miss"), ShowFlyText.Scale.MEDIUM, SWGColor.Whites.INSTANCE.getWhite());
				target.sendSelf(missFlyText);
				source.sendSelf(missFlyText);
				
				for (CreatureObject observerCreature : target.getObserverCreatures()) {
					observerCreature.sendSelf(createCombatSpam(observerCreature, source, target, sourceWeapon, info, command, CombatSpamType.MISS));
				}
				
				action.addDefender(new Defender(target.getObjectId(), target.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
				continue;	// This target negated the attack completely - move on to the next target
			}
			
			double knockdownChance = command.getKnockdownChance();
			if (knockdownChance > 0) {
				if (randomNumberBetween0And100() < knockdownChance) {
					KnockdownIntent.broadcast(target);
				} else {
					String yourAttackFailedToKnockDownYourOpponent = "@cbt_spam:knockdown_fail";
					SystemMessageIntent.broadcastPersonal(source.getOwner(), yourAttackFailedToKnockDownYourOpponent);
				}
			}
			
			addBuff(source, target, command.getBuffNameTarget());    // Add target buff
			
			DamageType damageType = getDamageType(command, sourceWeapon);	// Will be based on the equipped weapon or the combat command
			int weaponDamage = calculateBaseWeaponDamage(sourceWeapon, command);
			int addedDamage = command.getAddedDamage();
			
			weaponDamage += weaponDamageMod;
			
			int rawDamage = weaponDamage + addedDamage;
			
			info.setRawDamage(rawDamage);
			info.setFinalDamage(rawDamage);
			info.setDamageType(damageType);
			
			if (command.isBlinding()) {
				ApplyCombatStateIntent.broadcast(source, target, CombatState.BLINDED);
			}
			
			// The armor of the target will mitigate some damage
			if (isWearingPhysicalArmor(target)) {
				physicalArmorMitigate(info, damageType, target, command);
			} else if (hasInnateJediArmor(target)) {
				innateJediArmorMitigate(info, target);
			} else if (hasInnateTerasKasiArmor(target)) {
				innateTerasKasiArmorMitigate(info, target);
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
			
			int hate = (int) (finalDamage * command.getHateDamageModifier());
			hate += command.getHateAdd();
			target.handleHate(source, hate);
			
			boolean bothUsingMelee = target.getEquippedWeapon().getType().isMelee() && source.getEquippedWeapon().getType().isMelee();
			
			if (bothUsingMelee) {
				double percentOfDamageToReflectBackToAttacker = target.getSkillModValue("private_melee_dmg_shield") / 100d;
				
				if (percentOfDamageToReflectBackToAttacker > 0) {
					riposte(source, target, rawDamage, percentOfDamageToReflectBackToAttacker);
				}
			}
		}
		
		source.sendObservers(action);
	}
	
	private static void innateJediArmorMitigate(AttackInfo info, CreatureObject target) {
		innateArmorMitigate(info, target, jediArmorSkillMod);
	}
	
	private static void innateTerasKasiArmorMitigate(AttackInfo info, CreatureObject target) {
		innateArmorMitigate(info, target, tkaArmorSkillMod);
	}
	
	private static boolean hasInnateTerasKasiArmor(CreatureObject target) {
		return target.getSkillModValue(tkaArmorSkillMod) > 0;
	}
	
	
	private static boolean hasInnateJediArmor(CreatureObject target) {
		return target.getSkillModValue(jediArmorSkillMod) > 0;
	}
	
	private static boolean isWearingPhysicalArmor(CreatureObject target) {
		Collection<SWGObject> slottedObjects = target.getSlottedObjects();
		
		for (SWGObject slottedObject : slottedObjects) {
			if (slottedObject instanceof TangibleObject tangibleSlottedObject) {
				if (tangibleSlottedObject.getProtection() != null) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private static void riposte(CreatureObject source, CreatureObject target, int rawDamage, double percentOfDamageToReflectBackToAttacker) {
		int reflectedDamage = (int) (percentOfDamageToReflectBackToAttacker * rawDamage);
		
		ShowFlyText riposteFlytext = new ShowFlyText(target.getObjectId(), new StringId("cbt_spam", "dmg_shield_melee_fly"), ShowFlyText.Scale.MEDIUM, SWGColor.Reds.INSTANCE.getOrangered());
		target.sendSelf(riposteFlytext);
		source.sendSelf(riposteFlytext);
		
		OutOfBandPackage spamMessage = new OutOfBandPackage(new ProsePackage(new StringId("cbt_spam", "dmg_shield_melee_spam"), "TU", target.getObjectName(), "TT", source.getObjectName(), "DI", reflectedDamage));
		sendRiposteCombatSpam(target, spamMessage);
		sendRiposteCombatSpam(source, spamMessage);
		
		if (source.getHealth() < reflectedDamage) {
			// Took more damage than they had health left. Final damage becomes the amount of remaining health.
			RequestCreatureDeathIntent.broadcast(source, target);
		} else {
			source.modifyHealth(-reflectedDamage);
		}
	}
	
	private static void sendRiposteCombatSpam(CreatureObject receiver, OutOfBandPackage spamMessage) {
		CombatSpam riposteCombatSpamTo = new CombatSpam(receiver.getObjectId());
		riposteCombatSpamTo.setDataType((byte) 2);
		riposteCombatSpamTo.setSpamMessage(spamMessage);
		riposteCombatSpamTo.setSpamType(CombatSpamType.HIT);
		receiver.sendSelf(riposteCombatSpamTo);
	}
	
	private static double calculateToHit(CreatureObject source, WeaponObject sourceWeapon, CreatureObject target) {
		int toHit = 66;
		toHit += calculateAccMod(source, sourceWeapon);
		toHit -= calculateDefMod(source, target);
		toHit += calculateDefPosMod(sourceWeapon, target);
		toHit += calculateAtkPosMod(source);
		toHit -= calculateBlindModifier(source);	// Blind attackers have a hard time hitting anything
		toHit += calculateBlindModifier(target);	// Blind targets have a hard time avoiding anything
		
		return toHit;
	}
	
	private static int calculateBlindModifier(CreatureObject target) {
		if (target.isStatesBitmask(CreatureState.BLINDED)) {
			return 50;
		}
		
		// TODO Defenders modifiers for being stunned, or intimidated. Example of Defender Modifier would be +50 signifying the defender being easier to hit. Stunned and intimidate factors are unknown but it is estimated that they lower primary (melee and ranged) defenses by -50
		
		return 0;
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
	
	private static int calculateDefMod(CreatureObject source, CreatureObject target) {
		int defMod = 0;
		
		defMod += calculateDefModWhenWieldingWeaponType(target);
		defMod += calculateDefModAgainstWeaponClass(source, target);
		defMod += target.getSkillModValue("private_defense_bonus");
		
		return defMod;
	}
	
	private static int calculateDefModAgainstWeaponClass(CreatureObject source, CreatureObject target) {
		WeaponObject sourceWeapon = source.getEquippedWeapon();
		WeaponType sourceWeaponType = sourceWeapon.getType();
		WeaponClass sourceWeaponClass = sourceWeaponType.getWeaponClass();
		
		return target.getSkillModValue(sourceWeaponClass.getDefenseSkillMod());
	}
	
	private static int calculateDefModWhenWieldingWeaponType(CreatureObject target) {
		WeaponObject targetWeapon = target.getEquippedWeapon();
		WeaponType targetWeaponType = targetWeapon.getType();
		
		String defenseSkillMod = targetWeaponType.getDefenseSkillMod();
		
		if (defenseSkillMod != null) {
			return target.getSkillModValue(defenseSkillMod);
		}
		
		return 0;
	}
	
	private static int calculateWeaponDamageMod(CreatureObject source, WeaponObject weapon) {
		WeaponType type = weapon.getType();
		
		if (type == WeaponType.UNARMED) {
			return source.getSkillModValue("unarmed_damage");
		} else {
			return 0;
		}
	}
	
	private static void physicalArmorMitigate(AttackInfo info, DamageType damageType, CreatureObject target, CombatCommand command) {
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
	
	private static void innateArmorMitigate(AttackInfo info, CreatureObject target, String skillMod) {
		float damageReduction = target.getSkillModValue(skillMod) / 100f;
		int currentDamage = info.getFinalDamage();
		int armorAbsorbed = (int) (currentDamage * damageReduction);
		currentDamage -= armorAbsorbed;
		
		info.setFinalDamage(currentDamage);
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
		
		int privateArmorBreak = creature.getSkillModValue("private_armor_break");
		
		if (privateArmorBreak > 0) {
			double armorBreakPercent = privateArmorBreak / 10d;
			armor *= (1 - armorBreakPercent / 100d);
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
