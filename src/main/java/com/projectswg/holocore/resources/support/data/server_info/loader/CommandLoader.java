package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.combat.*;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.commands.DefaultPriority;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CommandLoader extends DataLoader {
	
	private final Map<String, Command> commandNameMap;
	private final Map<String, List<Command>> commandCallbackMap;
	private final Map<Integer, Command> commandCrcMap;
	
	CommandLoader() {
		this.commandNameMap = new HashMap<>();
		this.commandCallbackMap = new HashMap<>();
		this.commandCrcMap = new HashMap<>();
	}
	
	public boolean isCommand(String command) {
		return commandNameMap.containsKey(command);
	}
	
	public boolean isCommand(int crc) {
		return commandCrcMap.containsKey(crc);
	}
	
	public Command getCommand(String command) {
		return commandNameMap.get(command);
	}
	
	public List<Command> getCommandByCallback(String callback) {
		return commandCallbackMap.get(callback.toLowerCase(Locale.US));
	}
	
	public Command getCommand(int crc) {
		return commandCrcMap.get(crc);
	}
	
	@Override
	protected void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/command/commands.sdb"))) {
			while (set.next()) {
				Command command = Command.builder()
						.withName(set.getText("name").toLowerCase(Locale.US))
						.withCallback(set.getText("callback"))
						.withDefaultPriority(DefaultPriority.getDefaultPriority(set.getText("defaultPriority")))
						.withDefaultTime(set.getReal("defaultTime"))
						.withCharacterAbility(set.getText("characterAbility"))
						.withGodLevel((int) set.getInt("godLevel"))
						.withCooldownGroup(set.getText("cooldownGroup"))
						.withCooldownGroup2(set.getText("cooldownGroup2"))
						.withCooldownTime(set.getReal("cooldownTime"))
						.withCooldownTime2(set.getReal("cooldownTime2"))
						.withTargetType(TargetType.getTargetType(set.getText("targetType")))
						.withAddToCombatQueue(set.getBoolean("addToCombatQueue"))
						.build();
				if (commandNameMap.containsKey(command.getName())) {
					Log.w("Duplicate command name [ignoring]: %s", command.getName());
					continue;
				}
				if (commandCrcMap.containsKey(command.getCrc())) {
					Log.w("Duplicate command crc [ignoring]: %d [%s]", command.getCrc(), command.getName());
					continue;
				}
				commandNameMap.put(command.getName(), command);
				commandCallbackMap.computeIfAbsent(command.getCallback().toLowerCase(Locale.US), c -> new ArrayList<>()).add(command);
				commandCrcMap.put(command.getCrc(), command);
			}
		}
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
				Command base = getCommand(set.getText("actionName").toLowerCase(Locale.US));
				if (base == null) {
					Log.w("Invalid combat command actionName: %s", set.getText("actionName"));
					continue;
				}
				Command command = CombatCommand.builder(base)
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
						.withAnimations(WeaponType.LIGHT_RIFLE, getAnimationList(set.getText("anim_lightRifle")))
						.withAnimations(WeaponType.CARBINE, getAnimationList(set.getText("anim_carbine")))
						.withAnimations(WeaponType.RIFLE, getAnimationList(set.getText("anim_rifle")))
						.withAnimations(WeaponType.THROWN, getAnimationList(set.getText("anim_thrown")))
						.withAnimations(WeaponType.ONE_HANDED_SABER, getAnimationList(set.getText("anim_onehandlightsaber")))
						.withAnimations(WeaponType.TWO_HANDED_SABER, getAnimationList(set.getText("anim_twohandlightsaber")))
						.withAnimations(WeaponType.POLEARM_SABER, getAnimationList(set.getText("anim_polearmlightsaber")))
						.withAttackType(AttackType.valueOf(set.getText("attackType")))
						.withConeLength(set.getReal("coneLength"))
						.withAddedDamage((int) set.getInt("addedDamage"))
						.withPercentAddFromWeapon(set.getReal("percentAddFromWeapon"))
						.withHealthCost(set.getReal("healthCost"))
						.withActionCost(set.getReal("actionCost"))
						.withBuffNameTarget(set.getText("buffNameTarget"))
						.withBuffNameSelf(set.getText("buffNameSelf"))
						.withDamageType(DamageType.valueOf(set.getText("damageType")))
						.withIgnoreDistance(set.getBoolean("ignore_distance"))
						.withPvpOnly(set.getBoolean("pvp_only"))
						.withAttackRolls((int) set.getInt("attack_rolls"))
						.withMaxRange(set.getReal("maxRange"))
						.build();
				commandNameMap.put(command.getName(), command);
				commandCrcMap.put(command.getCrc(), command);
			}
		}
	}
	
	private static String[] getAnimationList(String cell) {
		if (cell.isEmpty())
			return new String[0];
		return cell.split(",");
	}
}
