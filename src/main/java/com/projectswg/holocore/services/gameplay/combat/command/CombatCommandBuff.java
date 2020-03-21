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

import com.projectswg.common.data.combat.AttackInfo;
import com.projectswg.common.data.combat.HitLocation;
import com.projectswg.common.data.combat.TrailLocation;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;

import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.addBuff;
import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatAction;
import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatSpam;

enum CombatCommandBuff implements CombatCommandHitType {
	INSTANCE;
	
	public void handle(CreatureObject source, SWGObject targetPrecheck, CombatCommand combatCommand, String arguments) {
		// TODO group buffs
		addBuff(source, source, combatCommand.getBuffNameSelf());
		
		if (!(targetPrecheck instanceof CreatureObject))
			return;    // Only CreatureObjects have buffs
		CreatureObject target = (CreatureObject) targetPrecheck;
		
		String buffNameTarget = combatCommand.getBuffNameTarget();
		
		addBuff(source, target, buffNameTarget);
		
		WeaponObject weapon = source.getEquippedWeapon();
		CombatAction combatAction = createCombatAction(source, weapon, TrailLocation.RIGHT_HAND, combatCommand);
		combatAction.addDefender(new Defender(source.getObjectId(), source.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		
		if (!buffNameTarget.isEmpty()) {
			combatAction.addDefender(new Defender(target.getObjectId(), target.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		}
		
		source.sendObservers(combatAction, createCombatSpam(source, target, weapon, new AttackInfo(), combatCommand));
	}
	
}
