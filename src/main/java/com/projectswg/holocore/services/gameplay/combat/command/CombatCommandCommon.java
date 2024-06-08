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

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.RGB;
import com.projectswg.common.data.combat.AttackInfo;
import com.projectswg.common.data.combat.CombatSpamType;
import com.projectswg.common.data.combat.TrailLocation;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam;
import com.projectswg.holocore.intents.gameplay.combat.BuffIntent;
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus;
import com.projectswg.holocore.resources.support.color.SWGColor;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;

import java.util.concurrent.ThreadLocalRandom;

public class CombatCommandCommon {
	
	private CombatCommandCommon() {
		
	}
	
	static CombatAction createCombatAction(CreatureObject source, WeaponObject weapon, TrailLocation trail, CombatCommand command) {
		CombatAction combatAction = new CombatAction(source.getObjectId());
		combatAction.setActionCrc(CRC.getCrc(command.getRandomAnimation(weapon.getType())));
		combatAction.setAttackerId(source.getObjectId());
		combatAction.setPosture(source.getPosture());
		combatAction.setWeaponId(weapon.getObjectId());
		combatAction.setClientEffectId((byte) 0);
		combatAction.setCommandCrc(command.getCrc());
		combatAction.setTrail(trail);
		return combatAction;
	}
	
	static CombatSpam createCombatSpam(CreatureObject receiver, CreatureObject source, TangibleObject target, WeaponObject weapon, AttackInfo info, CombatCommand command, CombatSpamType combatSpamType) {
		CombatSpam combatSpam = new CombatSpam(receiver.getObjectId());
		combatSpam.setInfo(info);
		combatSpam.setAttacker(source.getObjectId());
		combatSpam.setWeapon(weapon.getObjectId());
		combatSpam.setDefender(target.getObjectId());
		combatSpam.setDataType((byte) 0);
		combatSpam.setAttackName(new StringId("cmd_n", command.getName()));
		combatSpam.setSpamType(combatSpamType);

		return combatSpam;
	}
	
	static CombatStatus canPerform(CreatureObject source, SWGObject target, CombatCommand c) {
		if (source.equals(target)) {
			return CombatStatus.INVALID_TARGET;
		}
		
		if (source.getEquippedWeapon() == null)
			return CombatStatus.NO_WEAPON;
		
		if (target == null || source.equals(target))
			return CombatStatus.INVALID_TARGET;
		
		if (!(target instanceof TangibleObject))
			return CombatStatus.INVALID_TARGET;
		
		if (!source.isAttackable((TangibleObject) target)) {
			return CombatStatus.INVALID_TARGET;
		}
		
		if (!source.isLineOfSight(target))
			return CombatStatus.TOO_FAR;
		
		if (target instanceof CreatureObject) {
			switch (((CreatureObject) target).getPosture()) {
				case DEAD:
				case INCAPACITATED:
					return CombatStatus.INVALID_TARGET;
				default:
					break;
			}
		}
		
		switch (c.getAttackType()) {
			case AREA:
			case CONE:
			case TARGET_AREA:
				return canPerformArea(source, c);
			case SINGLE_TARGET:
				return canPerformSingle(source, target, c);
			default:
				return CombatStatus.UNKNOWN;
		}
	}
	
	static CombatStatus canPerformSingle(CreatureObject source, SWGObject target, CombatCommand c) {
		if (!(target instanceof TangibleObject))
			return CombatStatus.NO_TARGET;
		
		WeaponObject weapon = source.getEquippedWeapon();
		double dist = Math.floor(source.getWorldLocation().distanceTo(target.getWorldLocation()));
		double commandRange = c.getMaxRange();
		double range = commandRange > 0 ? commandRange : weapon.getMaxRange();
		
		if (dist > range)
			return CombatStatus.TOO_FAR;
		
		return CombatStatus.SUCCESS;
	}
	
	static CombatStatus canPerformArea(CreatureObject source, CombatCommand c) {
		return CombatStatus.SUCCESS;
	}
	
	static int calculateBaseWeaponDamage(WeaponObject weapon, CombatCommand command) {
		int minDamage = weapon.getMinDamage();
		int weaponDamage = ThreadLocalRandom.current().nextInt((weapon.getMaxDamage() - minDamage) + 1) + minDamage;
		
		return (int) (weaponDamage * command.getPercentAddFromWeapon());
	}
	
	static void addBuff(CreatureObject caster, CreatureObject receiver, String buffName) {
		if (buffName.isEmpty()) {
			return;
		}
		
		new BuffIntent(buffName, caster, receiver, false).broadcast();
	}
	
	public static void handleStatus(CreatureObject source, CombatCommand combatCommand, CombatStatus status) {
		switch (status) {
			case NO_TARGET -> showFlyText(source, "@combat_effects:target_invalid_fly", Scale.MEDIUM, SWGColor.Whites.INSTANCE.getWhite(), ShowFlyText.Flag.PRIVATE);
			case TOO_FAR -> showFlyText(source, "@combat_effects:range_too_far", Scale.MEDIUM, SWGColor.Blues.INSTANCE.getCyan(), ShowFlyText.Flag.PRIVATE);
			case INVALID_TARGET -> showFlyText(source, "@combat_effects:target_invalid_fly", Scale.MEDIUM, SWGColor.Blues.INSTANCE.getCyan(), ShowFlyText.Flag.PRIVATE);
			case TOO_TIRED -> showFlyText(source, "@combat_effects:action_too_tired", Scale.MEDIUM, SWGColor.Oranges.INSTANCE.getOrange(), ShowFlyText.Flag.PRIVATE);
			case SUCCESS -> showTriggerEffect(source, combatCommand);
			default -> showFlyText(source, "@combat_effects:action_failed", Scale.MEDIUM, SWGColor.Whites.INSTANCE.getWhite(), ShowFlyText.Flag.PRIVATE);
		}
	}
	
	private static void showTriggerEffect(CreatureObject source, CombatCommand command) {
		String triggerEffect = command.getTriggerEffect();
		if (triggerEffect.length() > 0) {
			String triggerEffectHardpoint = command.getTriggerEffectHardpoint();
			source.sendObservers(new PlayClientEffectObjectMessage(triggerEffect, triggerEffectHardpoint, source.getObjectId(), ""));
		}
	}
	
	static void showFlyText(TangibleObject obj, String text, Scale scale, RGB c, ShowFlyText.Flag... flags) {
		obj.sendSelf(new ShowFlyText(obj.getObjectId(), text, scale, c, flags));
	}
	
}
