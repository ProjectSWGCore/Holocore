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

import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.color.SWGColor;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Objects;

public class ExperiencePointService extends Service {
	
	private final double xpMultiplier;
	
	public ExperiencePointService() {
		xpMultiplier = PswgDatabase.INSTANCE.getConfig().getDouble(this, "xpMultiplier", 1);
	}
	
	@IntentHandler
	private void handleExperienceIntent(ExperienceIntent ei) {
		CreatureObject creatureObject = ei.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		
		if (playerObject != null) {
			String xpType = ei.getXpType();
			int experienceGained = ei.getExperienceGained();
			SWGObject flytextTarget = ei.getFlytextTarget();
			boolean xpMultiplied = ei.isXpMultiplied();
			
			if (xpMultiplied) {
				experienceGained *= xpMultiplier;
			}
			
			awardExperience(creatureObject, flytextTarget, playerObject, xpType, experienceGained);
		}
	}
	
	private void awardExperience(CreatureObject creatureObject, SWGObject flytextTarget, PlayerObject playerObject, String xpType, int xpGained) {
		int currentXp = playerObject.getExperiencePoints(xpType);
		int newXpTotal = currentXp + xpGained;
		
		playerObject.setExperiencePoints(xpType, newXpTotal);
		StandardLog.onPlayerTrace(this, creatureObject, "gained %d %s XP", xpGained, xpType);
		
		if (!Objects.equals("combat_general", xpType)) {
			showFlytext(creatureObject, flytextTarget, xpGained);
			showSystemMessage(creatureObject, xpType);
		}
	}
	
	private void showSystemMessage(CreatureObject creatureObject, String xpType) {
		// TODO display different messages with inspiration bonus and/or group bonus
		StringId xpTypeDisplayName = new StringId("exp_n", xpType);
		ProsePackage message = new ProsePackage(new StringId("base_player", "prose_grant_xp"), "TO", xpTypeDisplayName);
		SystemMessageIntent.Companion.broadcastPersonal(creatureObject.getOwner(), message, ChatSystemMessage.SystemChatType.CHAT_BOX);
	}
	
	private void showFlytext(CreatureObject creatureObject, SWGObject flytextTarget, int xpGained) {
		OutOfBandPackage message = new OutOfBandPackage(new ProsePackage(new StringId("base_player", "prose_flytext_xp"), "DI", xpGained));
		ShowFlyText packet = new ShowFlyText(flytextTarget.getObjectId(), message, Scale.MEDIUM, SWGColor.Violets.INSTANCE.getMagenta());
		creatureObject.sendSelf(packet);
	}
	
}
