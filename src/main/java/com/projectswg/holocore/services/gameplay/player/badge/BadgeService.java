package com.projectswg.holocore.services.gameplay.player.badge;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.network.packets.swg.zone.BadgesResponseMessage;
import com.projectswg.holocore.intents.gameplay.player.badge.GrantBadgeIntent;
import com.projectswg.holocore.intents.gameplay.player.badge.RequestBadgesIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.common.data.Badges;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.HashMap;
import java.util.Map;

public class BadgeService extends Service {
	
	//TODO
	//research categories
	//music
	//fix to appropriate message ex: kill_merek_activation_01

	private DatatableData collectionTable;
	private Map<String, BadgeData> badgeMap;
	
	public BadgeService() {
		collectionTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/badge/badge_map.iff");
		badgeMap = new HashMap<>();
	}
	
	
	@Override
	public boolean initialize() {
		for (int row = 0; row < collectionTable.getRowCount(); row++) {
			int badgeIndex = (int) collectionTable.getCell(row, 0);	// Index
			String badgeName = (String) collectionTable.getCell(row, 1);	// Badge name
			int category = (int) collectionTable.getCell(row, 3);	// Category
			String type = (String) collectionTable.getCell(row, 5);	// Type
			
			boolean explorationBadge = category == 2 && !type.equals("accumulation");
			
			badgeMap.put(badgeName, new BadgeData(badgeIndex, explorationBadge));
		}
		
		return super.initialize();
	}
	
	private void handleBadgeGrant(GrantBadgeIntent i) {
		CreatureObject target = i.getCreature();
		String badgeName = i.getCollectionBadgeName();
		
		if (badgeMap.containsKey(badgeName)) {
			
			Badges badges = target.getPlayerObject().getBadges();
			
			if (!badges.hasBadge(badgeMap.get(badgeName).getBadgeIndex())) {
				// They don't already have this badge
				giveBadge(target.getPlayerObject(), badgeName, true);
			}
		} else {
			Log.e( "%s could not receive badge %s because it does not exist", target, badgeName);
		}
	}
	
	@IntentHandler
	private void handleBadgesRequest(RequestBadgesIntent i) {
		SWGObject target = i.getTarget();
		Player requester = i.getRequester();
		
		if (target != null) {
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
		} else {
            Log.e("%s attempted to request badges of a null target", requester);
		}
	}
	
	private void giveBadge(PlayerObject target, String badgeName, boolean playMusic) {
		Badges badges = target.getBadges();
		
		BadgeData badgeData = badgeMap.get(badgeName);
		
		if (badgeData != null) {
			boolean explorationBadge = badgeData.isExplorationBadge();
			badges.set(badgeData.getBadgeIndex(), explorationBadge, true);

			SystemMessageIntent.broadcastPersonal(target.getOwner(), new ProsePackage(new StringId("badge_n", "prose_grant"), "TO", "@badge_n:" + badgeName));
			
			if (playMusic) {
				// TODO play music...
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
	
	private class BadgeData {
		
		private final int badgeIndex;
		private final boolean explorationBadge;
		
		public BadgeData(int badgeIndex, boolean explorationBadge) {
			this.badgeIndex = badgeIndex;
			this.explorationBadge = explorationBadge;
		}
		
		public int getBadgeIndex() {
			return badgeIndex;
		}
		
		public boolean isExplorationBadge() {
			return explorationBadge;
		}
	}
	
}
