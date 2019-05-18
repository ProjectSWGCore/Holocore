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
import com.projectswg.common.data.combat.CombatStatus;
import com.projectswg.common.data.combat.HitType;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SpecialLineLoader.SpecialLineInfo;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.handleStatus;

public class CombatCommandService extends Service {
	
	private final Map<HitType, CombatCommandHitType> hitTypeMap;
	
	public CombatCommandService() {
		this.hitTypeMap = new EnumMap<>(HitType.class);
		hitTypeMap.put(HitType.ATTACK, CombatCommandAttack.INSTANCE);
		hitTypeMap.put(HitType.BUFF, CombatCommandBuff.INSTANCE);
//		hitTypeMap.put(HitType.DEBUFF, null);
		hitTypeMap.put(HitType.HEAL, CombatCommandHeal.INSTANCE);
		hitTypeMap.put(HitType.DELAY_ATTACK, CombatCommandDelayAttack.INSTANCE);
//		hitTypeMap.put(HitType.REVIVE, null);
		// TODO: Add in other hit types (DEBUFF/REVIVE)
	}
	
	@Override
	public boolean start() {
		for (CombatCommandHitType hitType : hitTypeMap.values())
			hitType.initialize();
		return true;
	}
	
	@Override
	public boolean stop() {
		for (CombatCommandHitType hitType : hitTypeMap.values())
			hitType.terminate();
		return true;
	}
	
	@IntentHandler
	private void handleChatCommandIntent(ExecuteCommandIntent eci) {
		if (!eci.getCommand().isCombatCommand() || !(eci.getCommand() instanceof CombatCommand))
			return;
		CombatCommand command = (CombatCommand) eci.getCommand();
		
		CreatureObject source = eci.getSource();
		SpecialLineInfo specialLine = DataLoader.specialLines().getSpecialLine(command.getSpecialLine());
		double actionCost = command.getActionCost() * command.getAttackRolls();
		
		// TODO future: reduce actionCost with general ACR, weapon ACR and ability ACR
		
		if (actionCost <= 0) {
			return;
		}
		
		if (specialLine != null && source.getSkillModValue(specialLine.getFreeshotModName()) > ThreadLocalRandom.current().nextInt(0, 100)) {
			source.sendSelf(new ShowFlyText(source.getObjectId(), new StringId("spam", "freeshot"), ShowFlyText.Scale.MEDIUM, new RGB(255, 255, 255), ShowFlyText.Flag.IS_FREESHOT));
		} else {
			if (specialLine != null)
				actionCost = reduceActionCost(source, actionCost, specialLine.getActionCostModName());
			if (actionCost > source.getAction())
				return;
			
			source.modifyAction(-(int) command.getActionCost());
		}
		
		CombatCommandHitType hitType = hitTypeMap.get(command.getHitType());
		if (hitType != null)
			hitType.handle(source, eci.getTarget(), command, eci.getArguments());
		else
			handleStatus(source, CombatStatus.UNKNOWN);
	}
	
	/**
	 * Calculates a new action cost based on the given action cost and a skill mod name.
	 * @param source to read the skillmod value from
	 * @param actionCost that has been calculated so far
	 * @param skillModName name of the skillmod to read from {@code source}
	 * @return new action cost that has been increased or reduced, depending on whether the skillmod value is
	 * positive or negative
	 */
	private double reduceActionCost(CreatureObject source, double actionCost, String skillModName) {
		int actionCostModValue = source.getSkillModValue(skillModName);
		
		return actionCost + actionCost * actionCostModValue / 100;
	}
	
}
