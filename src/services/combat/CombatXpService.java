/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.combat;

import intents.combat.CreatureKilledIntent;
import intents.experience.ExperienceIntent;
import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureDifficulty;
import resources.objects.creature.CreatureObject;
import resources.objects.group.GroupObject;
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;
import resources.server_info.RelationalServerFactory;

/**
 *
 * @author mads
 */
public class CombatXpService extends Service {
	
	private final Map<Short, XpData> xpData;
	private final Map<Long, GroupObject> groupObjects;

	public CombatXpService() {
		xpData = new HashMap<>();
		groupObjects = new HashMap<>();
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case ObjectCreatedIntent.TYPE: handleObjectCreatedIntent((ObjectCreatedIntent) i); break;
			case DestroyObjectIntent.TYPE: handleDestroyObjectIntent((DestroyObjectIntent) i); break;
			case CreatureKilledIntent.TYPE: handleCreatureKilledIntent((CreatureKilledIntent) i); break;
		}
	}

	@Override
	public boolean initialize() {
		loadXpData();
		
		return super.initialize();
	}

	@Override
	public boolean start() {
		// The objects we care about are only created/destroyed at this point anyways.
		registerForIntent(ObjectCreatedIntent.TYPE);
		registerForIntent(DestroyObjectIntent.TYPE);
		registerForIntent(CreatureKilledIntent.TYPE);
		
		return super.start();
	}
	
	private void loadXpData() {
		long start = System.nanoTime();
		
		Log.i(this, "Loading combat XP rates...");
		try (RelationalDatabase npcStats = RelationalServerFactory.getServerData("creatures/npc_stats.db", "npc_stats")) {
			try (ResultSet set = npcStats.executeQuery("SELECT * FROM npc_stats")) {
				while (set.next()) {
					xpData.put(set.getShort("Level"), new XpData(set.getInt("XP"), set.getInt("Elite_XP"), set.getInt("Boss_XP")));
				}
			} catch (SQLException e) {
				Log.e(this, e);
			}
		}
		
		double time = (System.nanoTime()-start)/1E6;
		Log.i(this, "Finished loading %d combat XP rates. Time: %fms", xpData.size(), time);
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject object = i.getObject();
		
		if(object instanceof GroupObject) {
			synchronized (groupObjects) {
				groupObjects.put(object.getObjectId(), (GroupObject) object);
			}
		}
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent i) {
		SWGObject object = i.getObject();
		
		synchronized (groupObjects) {
			if(object instanceof GroupObject && groupObjects.remove(object.getObjectId()) == null) {
				Log.w(this, "%s was expected to be in the GroupObject mapping but wasn't", object);
			}
		}
	}
	
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getCorpse();
		
		// You don't gain XP by PvP'ing
		if(corpse.isPlayer()) {
			return;
		}
		
		CreatureObject killer = i.getKiller();
		GroupObject group = groupObjects.get(killer.getGroupId());
		
		// Ungrouped entertainer
		if (group == null && isEntertainer(killer)) {
			return;
		}
		
		short killerLevel = group != null ? group.getLevel() : killer.getLevel();
		int experienceGained = calculateXpGain(killer, corpse, killerLevel);
		
		if (experienceGained <= 0) {
			Log.w(this, "%s received no XP: XP for creature difficulty %s at level %d was %d", killer, corpse.getDifficulty(), killerLevel, experienceGained);
			return;
		}
		
		if (group == null) {
			new ExperienceIntent(killer, "combat", experienceGained).broadcast();
		} else {
			group.getGroupMemberObjects().stream()
					.filter(groupMember -> !isEntertainer(groupMember) && isMemberNearby(corpse, groupMember))
					.forEach(eligibleMember -> new ExperienceIntent(eligibleMember, "combat", experienceGained).broadcast());
		}
	}
	
	private boolean isEntertainer(CreatureObject creature) {
		return creature.hasSkill("class_entertainer_phase1_novice");
	}
	
	private int calculateXpGain(CreatureObject killer, CreatureObject corpse, short killerLevel) {
		int experienceGained;
		short corpseLevel = corpse.getLevel();
		// If the difference between killer and corpse is 10 or above, they only gain 1 xp
		if (killerLevel - corpseLevel >= 10) {
			experienceGained = 1;
		} else {
			XpData xpForLevel = this.xpData.get(corpseLevel);

			if (xpForLevel == null) {
				Log.e(this, "%s received no XP: No XP data was found for level %d!", killer, corpseLevel);
				return 0;
			}

			CreatureDifficulty creatureDifficulty = corpse.getDifficulty();

			switch (creatureDifficulty) {
				case BOSS:
					experienceGained = xpForLevel.getBossXp();
					break;
				case ELITE:
					experienceGained = xpForLevel.getEliteXp();
					break;
				case NORMAL:
					experienceGained = xpForLevel.getXp();
					break;
				default:
					Log.e(this, "%s received no XP: Unsupported creature difficulty %s of corpse %s", killer, creatureDifficulty, corpse);
					return 0;
			}
		}
		
		return experienceGained;
	}
	
	/**
	 * @return true if {@code groupMember} is an observer of {@code corpse}
	 */
	private boolean isMemberNearby(CreatureObject corpse, CreatureObject groupMember) {
		return corpse.getObservers().contains(groupMember);
	}
	
	private static class XpData {
		private final int xp;
		private final int eliteXp;
		private final int bossXp;

		public XpData(int xp, int eliteXp, int bossXp) {
			this.xp = xp;
			this.eliteXp = eliteXp;
			this.bossXp = bossXp;
		}

		public int getXp() {
			return xp;
		}

		public int getEliteXp() {
			return eliteXp;
		}

		public int getBossXp() {
			return bossXp;
		}
		
	}
	
}
