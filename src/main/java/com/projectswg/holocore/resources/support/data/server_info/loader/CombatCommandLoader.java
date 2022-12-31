package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.combat.*;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CombatCommandLoader extends DataLoader {
	
	private final Map<String, CombatCommand> commandNameMap;
	
	CombatCommandLoader() {
		this.commandNameMap = new HashMap<>();
	}
	
	@Nullable
	public CombatCommand getCombatCommand(String command, Collection<String> ownedCommands) {
		Set<String> ownedCommandsLowerCased = ownedCommands.stream()
				.map(ownedCommand -> ownedCommand.toLowerCase(Locale.US))
				.collect(Collectors.toSet());
		String basicVersion = command.toLowerCase(Locale.US);

		String advancedVersion = basicVersion + "_2";
		if (isVersionUsable(ownedCommandsLowerCased, advancedVersion)) {
			return commandNameMap.get(advancedVersion);
		}

		String improvedVersion = basicVersion + "_1";
		if (isVersionUsable(ownedCommandsLowerCased, improvedVersion)) {
			return commandNameMap.get(improvedVersion);
		}
		
		return commandNameMap.get(basicVersion);
	}

	private boolean isVersionUsable(Set<String> ownedCommandsLowerCased, String version) {
		return ownedCommandsLowerCased.contains(version) && commandNameMap.containsKey(version);
	}

	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/command/combat_commands.sdb"))) {
			while (set.next()) {
				/*
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
				CombatCommand command = CombatCommand.builder()
						.withName(set.getText("actionName").toLowerCase(Locale.US))
						.withDelayAttackEggTemplate(set.getText("delayAttackEggTemplate"))
						.withDelayAttackParticle(set.getText("delayAttackParticle"))
						.withInitialDelayAttackInterval(set.getReal("initialDelayAttackInterval"))
						.withDelayAttackInterval(set.getReal("delayAttackInterval"))
						.withDelayAttackLoops((int) set.getInt("delayAttackLoops"))
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
						.withAddedDamage((int) set.getInt("addedDamage"))
						.withPercentAddFromWeapon(set.getReal("percentAddFromWeapon"))
						.withBypassArmor(set.getReal("bypassArmor"))
						.withHateDamageModifier(set.getReal("hateDamageModifier"))
						.withHateAdd((int) set.getInt("hateAdd"))
						.withHealthCost(set.getReal("healthCost"))
						.withActionCost(set.getReal("actionCost"))
						.withMindCost(set.getReal("mindCost"))
						.withForceCost(set.getReal("forceCost"))
						.withForceCostModifier(set.getReal("fcModifier"))
						.withKnockdownChance(set.getReal("knockdownChance"))
						.withBlinding(set.getBoolean("blinding"))
						.withBleeding(set.getBoolean("bleeding"))
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
						.withAttackRolls((int) set.getInt("attack_rolls"))
						.withMaxRange(set.getReal("maxRange"))
						.build();
				commandNameMap.put(command.getName(), command);
			}
		}
	}
	
	private static String[] getAnimationList(String cell) {
		if (cell.isEmpty())
			return new String[0];
		return cell.split(",");
	}
}
