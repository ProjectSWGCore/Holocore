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

import com.projectswg.common.data.combat.CombatStatus;
import com.projectswg.common.data.combat.HitLocation;
import com.projectswg.common.data.combat.TrailLocation;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.addBuff;
import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatAction;

enum CombatCommandBuff implements CombatCommandHitType {
	INSTANCE;
	
	@Override
	public CombatStatus handle(@NotNull CreatureObject source, @Nullable SWGObject targetPrecheck, @NotNull CombatCommand combatCommand, @NotNull String arguments) {
		// TODO group buffs
		addBuff(source, source, combatCommand.getBuffNameSelf());
		
		CreatureObject target;
		
		if (targetPrecheck instanceof CreatureObject) {
			target = (CreatureObject) targetPrecheck;
		} else {
			target = source;
		}
		
		boolean applyToSelf = isApplyToSelf(source, target);
		CreatureObject effectiveTarget = applyToSelf ? source : target;
		
		String buffNameTarget = combatCommand.getBuffNameTarget();
		addBuff(source, effectiveTarget, buffNameTarget);
		
		WeaponObject weapon = source.getEquippedWeapon();
		CombatAction combatAction = createCombatAction(source, weapon, TrailLocation.RIGHT_HAND, combatCommand);
		combatAction.addDefender(new Defender(source.getObjectId(), source.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		
		if (!buffNameTarget.isEmpty()) {
			combatAction.addDefender(new Defender(target.getObjectId(), effectiveTarget.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		}
		
		return CombatStatus.SUCCESS;
	}
	
	private boolean isApplyToSelf(CreatureObject source, CreatureObject target) {
		FactionLoader.Faction sourceFaction = source.getFaction();
		FactionLoader.Faction targetFaction = target.getFaction();
		
		if (sourceFaction == null || targetFaction == null) {
			return true;
		}
		
		if (target.isAttackable(source)) {
			// You can't buff someone you can attack
			return true;
		} else if (sourceFaction.isEnemy(targetFaction)) {
			PvpStatus sourcePvpStatus = source.getPvpStatus();
			PvpStatus targetPvpStatus = target.getPvpStatus();
			
			if (sourcePvpStatus == PvpStatus.COMBATANT && targetPvpStatus == PvpStatus.ONLEAVE) {
				return false;
			}
			
			return sourcePvpStatus != PvpStatus.ONLEAVE || targetPvpStatus != PvpStatus.ONLEAVE;
		}
		
		if (source.isPlayer() && !target.isPlayer()) {
			if (target.hasOptionFlags(OptionFlag.INVULNERABLE)) {
				// You can't buff invulnerable NPCs
				return true;
			}
			// A player is attempting to buff a NPC
			long sourceGroupId = source.getGroupId();
			long npcGroupId = target.getGroupId();
			boolean bothGrouped = sourceGroupId != 0 &&  npcGroupId != 0;
			
			// Buff ourselves instead if player and NPC are ungrouped or are in different groups
			return !bothGrouped || sourceGroupId != npcGroupId;
		}
		
		return false;
	}
	
}
