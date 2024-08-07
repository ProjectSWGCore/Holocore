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
package com.projectswg.holocore.services.gameplay.player.badge;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.BadgesResponseMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.holocore.intents.gameplay.player.badge.GrantBadgeIntent;
import com.projectswg.holocore.intents.gameplay.player.badge.RequestBadgesIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.BadgeLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.common.data.Badges;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

public class BadgeService extends Service {
	
	//TODO
	//research categories
	//music
	//fix to appropriate message ex: kill_merek_activation_01
	
	@IntentHandler
	private void handleBadgeGrant(GrantBadgeIntent i) {
		CreatureObject target = i.getCreature();
		String badgeKey = i.getBadgeKey();
		BadgeLoader.BadgeInfo badgeFromKey = DataLoader.Companion.badges().getBadgeFromKey(badgeKey);
		
		if (badgeFromKey != null) {
			Badges badges = target.getPlayerObject().getBadges();
			int badgeIndex = badgeFromKey.getIndex();
			boolean receiverDoesNotHaveThisBadge = !badges.hasBadge(badgeIndex);
			
			if (receiverDoesNotHaveThisBadge) {
				giveBadge(target.getPlayerObject(), badgeKey, true);
			}
		} else {
			Log.e( "%s could not receive badge %s because it does not exist", target, badgeKey);
		}
	}
	
	@IntentHandler
	private void handleBadgesRequest(RequestBadgesIntent i) {
		SWGObject target = i.getTarget();
		Player requester = i.getRequester();

		if (target instanceof CreatureObject) {
			PlayerObject playerObject = ((CreatureObject) target).getPlayerObject();

			if (playerObject != null) {
				requester.sendPacket(new BadgesResponseMessage(target.getObjectId(), playerObject.getBadges()));
			} else {
				Log.e("%s attempted to request badges of NPC %s", requester, target);
			}
		} else {
			Log.e("%s attempted to request badges of a non-creature %s", requester, target);
		}
	}
	
	private void giveBadge(PlayerObject target, String badgeName, boolean playMusic) {
		BadgeLoader.BadgeInfo badgeFromKey = DataLoader.Companion.badges().getBadgeFromKey(badgeName);
		
		if (badgeFromKey != null) {
			boolean explorationBadge = badgeFromKey.getCategory() == 2 && !"accumulation".equals(badgeFromKey.getType());
			Badges badges = target.getBadges();
			badges.set(badgeFromKey.getIndex(), explorationBadge, true);

			SystemMessageIntent.Companion.broadcastPersonal(target.getOwner(), new ProsePackage(new StringId("badge_n", "prose_grant"), "TO", "@badge_n:" + badgeName));
			
			if (playMusic) {
				playMusicForBadgeReceiver(target, badgeFromKey);
			}
			
			// Check exploration badge accumulation badges
			if (explorationBadge) {
				switch (badges.getExplorationBadgeCount()) {
					case 10:
						giveBadge(target, "bdg_exp_10_badges", false);
						break;
					case 20:
						giveBadge(target, "bdg_exp_20_badges", false);
						break;
					case 30:
						giveBadge(target, "bdg_exp_30_badges", false);
						break;
					case 40:
						giveBadge(target, "bdg_exp_40_badges", false);
						break;
					case 45:
						giveBadge(target, "bdg_exp_45_badges", false);
						break;
				}
			}
			
			// Check accumulation badges
			switch (badges.getBadgeCount()) {
				case 5:
					giveBadge(target, "count_5", false);
					break;
				case 10:
					giveBadge(target, "count_10", false);
					break;
				case 25:
					giveBadge(target, "count_25", false);
					break;
				case 50:
					giveBadge(target, "count_50", false);
					break;
				case 75:
					giveBadge(target, "count_75", false);
					break;
				case 100:
					giveBadge(target, "count_100", false);
					break;
				case 125:
					giveBadge(target, "count_125", false);
					break;
			}
		} else {
            Log.e("%s could not receive badge %s because it does not exist", target, badgeName);
		}
		
	}
	
	private void playMusicForBadgeReceiver(PlayerObject target, BadgeLoader.BadgeInfo badgeFromKey) {
		String music = badgeFromKey.getMusic();
		target.getOwner().sendPacket(new PlayMusicMessage(0, music, 1, false));
	}
	
}
