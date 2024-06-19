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

package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.common.data.combat.AttackInfo;
import com.projectswg.common.data.combat.CombatSpamType;
import com.projectswg.common.data.combat.DamageType;
import com.projectswg.common.data.combat.TrailLocation;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam;
import com.projectswg.holocore.intents.gameplay.combat.*;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent;
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus;
import com.projectswg.holocore.resources.support.color.SWGColor;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.commands.Locomotion;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.tangible.Protection;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponClass;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import com.projectswg.holocore.resources.support.random.Die;
import com.projectswg.holocore.services.gameplay.combat.BleedingCombatState;
import com.projectswg.holocore.services.gameplay.combat.BlindedCombatState;
import com.projectswg.holocore.services.gameplay.combat.StunnedCombatState;
import kotlin.ranges.IntRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.*;

class CombatCommandAttack implements CombatCommandHitType {

	private static final String jediArmorSkillMod = "jedi_armor";
	private static final String tkaArmorSkillMod = "tka_armor";
	private final Die toHitDie;
	private final Die knockdownDie;
	private final Die woundDie;

	public CombatCommandAttack(Die toHitDie, Die knockdownDie, Die woundDie) {
		this.toHitDie = toHitDie;
		this.knockdownDie = knockdownDie;
		this.woundDie = woundDie;
	}

	@Override
	public CombatStatus handle(@NotNull CreatureObject source, @Nullable SWGObject target, @NotNull Command command, @NotNull CombatCommand combatCommand, @NotNull String arguments) {
		return handle(source, target, null, combatCommand);
	}

	public CombatStatus handle(CreatureObject source, SWGObject target, SWGObject delayEgg, CombatCommand combatCommand) {
		CombatStatus combatStatus = canPerform(source, target, combatCommand);
		if (combatStatus != CombatStatus.SUCCESS) {
			return combatStatus;
		}

		AttackInfo info = new AttackInfo();

		switch (combatCommand.getAttackType()) {
			case SINGLE_TARGET -> doCombatSingle(source, target, info, combatCommand);
			case AREA -> doCombatArea(source, source, info, combatCommand, false);
			case TARGET_AREA -> doCombatTargetArea(source, target, delayEgg, combatCommand, info);
			case CONE -> doCombatCone(source, target, info, combatCommand);
		}

		return combatStatus;
	}

	private void doCombatTargetArea(CreatureObject source, SWGObject target, SWGObject delayEgg, CombatCommand combatCommand, AttackInfo info) {
		doCombatArea(source, delayEgg != null ? delayEgg : target, info, combatCommand, true);
	}

	private void doCombatCone(CreatureObject source, Location targetWorldLocation, AttackInfo info, CombatCommand command) {
		double coneLength = command.getConeLength();
		double coneWidth = command.getConeWidth();

		Location sourceWorldLocation = source.getWorldLocation();

		double dirX = targetWorldLocation.getX() - sourceWorldLocation.getX();
		double dirZ = targetWorldLocation.getZ() - sourceWorldLocation.getZ();

		Set<SWGObject> objectsToCheck = source.getObjectsAware();

		Set<TangibleObject> targets = objectsToCheck.stream()
				.filter(TangibleObject.class::isInstance)
				.map(TangibleObject.class::cast)
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

	private void doCombatSingle(CreatureObject source, SWGObject target, AttackInfo info, CombatCommand combatCommand) {
		Set<TangibleObject> targets = new HashSet<>();

		if (target instanceof TangibleObject tangibleObject) {
			targets.add(tangibleObject);
		}

		doCombat(source, targets, info, combatCommand);
	}

	private void doCombatArea(CreatureObject source, SWGObject origin, AttackInfo info, CombatCommand combatCommand, boolean includeOrigin) {
		double aoeRange = combatCommand.getConeLength();
		SWGObject originParent = origin.getParent();
		Collection<SWGObject> objectsToCheck = originParent == null ? origin.getObjectsAware() : originParent.getContainedObjects();

		Set<TangibleObject> targets = objectsToCheck.stream()
				.filter(TangibleObject.class::isInstance)
				.map(TangibleObject.class::cast)
				.filter(source::isAttackable)
				.filter(target -> canPerform(source, target, combatCommand) == CombatStatus.SUCCESS)
				.filter(creature -> origin.getLocation().distanceTo(creature.getLocation()) <= aoeRange)
				.collect(Collectors.toSet());

		// This way, mines or grenades won't try to harm themselves
		if (includeOrigin && origin instanceof CreatureObject)
			targets.add((CreatureObject) origin);

		doCombat(source, targets, info, combatCommand);
	}

	private void doCombat(CreatureObject source, Set<TangibleObject> targets, AttackInfo info, CombatCommand combatCommand) {
		source.updateLastCombatTime();
		WeaponObject sourceWeapon = source.getEquippedWeapon();

		CombatAction action = createCombatAction(source, sourceWeapon, TrailLocation.WEAPON, combatCommand);
		double weaponDamageMod = calculateWeaponDamageMod(source, sourceWeapon);

		DamageType damageType = getDamageType(combatCommand, sourceWeapon);
		for (TangibleObject tangibleTarget : targets) {
			tangibleTarget.updateLastCombatTime();
			new EnterCombatIntent(source, tangibleTarget).broadcast();
			new EnterCombatIntent(tangibleTarget, source).broadcast();

			if (tangibleTarget instanceof CreatureObject creatureTarget) {
				doCombatCreature(source, info, combatCommand, sourceWeapon, action, weaponDamageMod, damageType, creatureTarget);
			} else {
				doCombatTangible(source, info, combatCommand, sourceWeapon, action, weaponDamageMod, damageType, tangibleTarget);
			}
		}

		source.sendObservers(action);
	}

	private static void doCombatTangible(CreatureObject source, AttackInfo info, CombatCommand combatCommand, WeaponObject sourceWeapon, CombatAction action, double weaponDamageMod, DamageType damageType, TangibleObject tangibleTarget) {
		int rawDamage = calculateBaseDamage(combatCommand, sourceWeapon, weaponDamageMod);
		int nextConditionDamage = tangibleTarget.getConditionDamage() + rawDamage;
		info.setSuccess(true);
		info.setDamageType(damageType);
		info.setRawDamage(rawDamage);
		info.setFinalDamage(rawDamage);

		if (nextConditionDamage > tangibleTarget.getMaxHitPoints()) {
			new DestroyObjectIntent(tangibleTarget).broadcast();
			new ExitCombatIntent(tangibleTarget).broadcast();
		} else {
			tangibleTarget.setConditionDamage(nextConditionDamage);
		}

		for (CreatureObject observerCreature : tangibleTarget.getObserverCreatures()) {
			observerCreature.sendSelf(createCombatSpam(observerCreature, source, tangibleTarget, sourceWeapon, info, combatCommand, CombatSpamType.HIT));
		}

		action.addDefender(new Defender(tangibleTarget.getObjectId(), Posture.UPRIGHT, true, (byte) 0, combatCommand.getHitLocation(), (short) info.getFinalDamage()));
	}

	private void doCombatCreature(CreatureObject source, AttackInfo info, CombatCommand combatCommand, WeaponObject sourceWeapon, CombatAction action, double weaponDamageMod, DamageType damageType, CreatureObject creatureTarget) {
		double toHit = calculateToHit(source, sourceWeapon, creatureTarget);

		if (toHitDie.roll(new IntRange(0, 100)) > toHit) {
			info.setSuccess(false);

			ShowFlyText missFlyText = new ShowFlyText(creatureTarget.getObjectId(), new StringId("combat_effects", "miss"), ShowFlyText.Scale.MEDIUM, SWGColor.Whites.INSTANCE.getWhite());
			creatureTarget.sendSelf(missFlyText);
			source.sendSelf(missFlyText);

			for (CreatureObject observerCreature : creatureTarget.getObserverCreatures()) {
				observerCreature.sendSelf(createCombatSpam(observerCreature, source, creatureTarget, sourceWeapon, info, combatCommand, CombatSpamType.MISS));
			}

			action.addDefender(new Defender(creatureTarget.getObjectId(), creatureTarget.getPosture(), false, (byte) 0, combatCommand.getHitLocation(), (short) 0));
			return;
		}

		double knockdownChance = combatCommand.getKnockdownChance();
		if (knockdownChance > 0) {
			if (knockdownDie.roll(new IntRange(0, 100)) < knockdownChance) {
				new KnockdownIntent(creatureTarget).broadcast();
			} else {
				String yourAttackFailedToKnockDownYourOpponent = "@cbt_spam:knockdown_fail";
				SystemMessageIntent.Companion.broadcastPersonal(source.getOwner(), yourAttackFailedToKnockDownYourOpponent);
			}
		}

		addBuff(source, creatureTarget, combatCommand.getBuffNameTarget());    // Add target buff

		int rawDamage = calculateBaseDamage(combatCommand, sourceWeapon, weaponDamageMod);

		if (creatureTarget.getPosture() == Posture.KNOCKED_DOWN) {
			rawDamage *= 1.5;
		}

		info.setDamageType(damageType);
		info.setRawDamage(rawDamage);
		info.setFinalDamage(rawDamage);

		if (combatCommand.isBlinding()) {
			new ApplyCombatStateIntent(source, creatureTarget, new BlindedCombatState()).broadcast();
		}

		if (combatCommand.isBleeding()) {
			new ApplyCombatStateIntent(source, creatureTarget, new BleedingCombatState()).broadcast();
		}

		if (combatCommand.isStunning()) {
			new ApplyCombatStateIntent(source, creatureTarget, new StunnedCombatState()).broadcast();
		}

		// The armor of the target will mitigate some damage
		if (isWearingPhysicalArmor(creatureTarget)) {
			physicalArmorMitigate(info, damageType, creatureTarget, combatCommand);
		} else if (hasInnateJediArmor(creatureTarget)) {
			innateJediArmorMitigate(info, creatureTarget);
		} else if (hasInnateTerasKasiArmor(creatureTarget)) {
			innateTerasKasiArmorMitigate(info, creatureTarget);
		}

		// End rolls
		int targetHealth = creatureTarget.getHealth() - creatureTarget.getHealthWounds();

		final int finalDamage;
		if (targetHealth <= info.getFinalDamage()) {
			finalDamage = targetHealth;    // Target took more damage than they had health left. Final damage becomes the amount of remaining health.
			new RequestCreatureDeathIntent(source, creatureTarget).broadcast();
		} else {
			finalDamage = info.getFinalDamage();
			creatureTarget.modifyHealth(-finalDamage);
		}

		info.setFinalDamage(finalDamage);

		float sourceWeaponWoundChance = sourceWeapon.getWoundChance();
		if (sourceWeaponWoundChance > 0) {
			if (woundDie.roll(new IntRange(0, 100)) < sourceWeaponWoundChance) {
				int wounds = 1;
				applyHealthWounds(creatureTarget, wounds);
			}
		}

		for (CreatureObject observerCreature : creatureTarget.getObserverCreatures()) {
			observerCreature.sendSelf(createCombatSpam(observerCreature, source, creatureTarget, sourceWeapon, info, combatCommand, CombatSpamType.HIT));
		}

		action.addDefender(new Defender(creatureTarget.getObjectId(), creatureTarget.getPosture(), true, (byte) 0, combatCommand.getHitLocation(), (short) finalDamage));

		int hate = (int) (finalDamage * combatCommand.getHateDamageModifier());
		hate += combatCommand.getHateAdd();
		creatureTarget.handleHate(source, hate);

		boolean bothUsingMelee = creatureTarget.getEquippedWeapon().getType().isMelee() && source.getEquippedWeapon().getType().isMelee();

		if (bothUsingMelee) {
			double percentOfDamageToReflectBackToAttacker = creatureTarget.getSkillModValue("private_melee_dmg_shield") / 100d;

			if (percentOfDamageToReflectBackToAttacker > 0) {
				riposte(source, creatureTarget, rawDamage, percentOfDamageToReflectBackToAttacker);
			}
		}
	}

	private static void applyHealthWounds(CreatureObject creatureTarget, int healthWounds) {
		int existingHealthWounds = creatureTarget.getHealthWounds();
		creatureTarget.setHealthWounds(existingHealthWounds + healthWounds);
	}

	private static int calculateBaseDamage(CombatCommand combatCommand, WeaponObject sourceWeapon, double weaponDamageMod) {
		int weaponDamage = calculateBaseWeaponDamage(sourceWeapon, combatCommand);
		int addedDamage = combatCommand.getAddedDamage();

		weaponDamage += weaponDamageMod;

		return weaponDamage + addedDamage;
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
			new RequestCreatureDeathIntent(source, target).broadcast();
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
		toHit += (calculateAccMod(source, sourceWeapon) - calculateDefMod(source, target)) / 10;
		toHit += calculateDefPosMod(sourceWeapon, target);
		toHit += calculateAtkPosMod(source);
		toHit -= calculateBlindModifier(source);    // Blind attackers have a hard time hitting anything
		toHit += calculateBlindModifier(target);    // Blind targets have a hard time avoiding anything

		return Math.max(32, toHit);
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

		info.setArmor(armor);    // Assumed to be the amount of armor points the defender has against the primary damage type
		info.setBlockedDamage(armorAbsorbed);    // Describes how many points of damage the armor absorbed

		info.setFinalDamage(currentDamage);
	}

	private static void innateArmorMitigate(AttackInfo info, CreatureObject target, String skillMod) {
		float damageReduction = target.getSkillModValue(skillMod) / 100f;
		damageReduction -= getArmorBreakPercent(target);
		int currentDamage = info.getFinalDamage();
		int armorAbsorbed = (int) (currentDamage * damageReduction);
		currentDamage -= armorAbsorbed;

		info.setFinalDamage(currentDamage);
	}

	private static int getArmor(DamageType damageType, CreatureObject creature) {
		int armProtection = 7;
		Map<String, Integer> protectionMap = Map.of("chest2", 35, "pants1", 20, "hat", 14, "bracer_upper_l", armProtection, "bracer_upper_r", armProtection, "bicep_l", armProtection, "bicep_r", armProtection, "utility_belt", 3);

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

		double armorBreakPercent = getArmorBreakPercent(creature);

		if (armorBreakPercent > 0) {
			armor *= (1 - armorBreakPercent / 100d);
		}

		return (int) armor;
	}

	private static double getArmorBreakPercent(CreatureObject creature) {
		int privateArmorBreak = creature.getSkillModValue("private_armor_break");
		return privateArmorBreak / 10d;
	}

	/**
	 * @param command to get the damage type of
	 * @param weapon  fallback in case the combat command does not provide its own {@code DamageType}
	 * @return {@code DamageType} of either the {@code command} or the {@code weapon}.
	 */
	private static DamageType getDamageType(CombatCommand command, WeaponObject weapon) {
		return command.getPercentAddFromWeapon() > 0 ? weapon.getDamageType() : command.getElementalType();
	}

	private static float getArmorReduction(int baseArmor, CombatCommand command) {
		double commandBypassArmor = command.getBypassArmor();

		if (commandBypassArmor > 0) {
			// This command bypasses armor
			baseArmor *= 1.0 - commandBypassArmor;
		}

		float mitigation = (float) (90 * (1 - Math.exp(-0.000125 * baseArmor))) + baseArmor / 9000f;

		return mitigation / 100;

	}

}
