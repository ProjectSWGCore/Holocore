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
package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.ChangeRoleIconChoice;
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent.IntentType;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.PlayerRoleLoader.RoleInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Collection;

public class ExperienceRoleService extends Service {
	
	public ExperienceRoleService() {
		
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		if (packet instanceof ChangeRoleIconChoice iconChoice) {
			changeRoleIcon(gpi.getPlayer().getCreatureObject(), iconChoice.getIconChoice());
		}
	}
	
	@IntentHandler
	private void handleGrantSkillIntent(GrantSkillIntent gsi) {
		if (gsi.getIntentType() != IntentType.GIVEN)
			return;
		
		Collection<RoleInfo> roles = DataLoader.Companion.playerRoles().getRolesBySkill(gsi.getSkillName());
		if (roles.isEmpty()) {
			return;
		}
		
		PlayerObject playerObject = gsi.getTarget().getPlayerObject();
		assert playerObject != null;
		
		RoleInfo role = roles.iterator().next();
		playerObject.setProfessionIcon(role.getIndex());
	}
	
	private void changeRoleIcon(CreatureObject creature, int chosenIcon) {
		Collection<RoleInfo> roles = DataLoader.Companion.playerRoles().getRolesByIndex(chosenIcon);
		if (roles.isEmpty()) {
			Log.w("%s tried to use undefined role icon %d", creature, chosenIcon);
			return;
		}
		PlayerObject playerObject = creature.getPlayerObject();
		assert playerObject != null;
		
		boolean creatureQualifiedForRoleIcon = roles.stream().anyMatch(role -> creature.hasSkill(role.getQualifyingSkill()));
		
		if (creatureQualifiedForRoleIcon) {
			playerObject.setProfessionIcon(chosenIcon);
		}
	}
	
}
