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
package com.projectswg.holocore.services.gameplay.combat;

import com.projectswg.common.data.info.RelationalDatabase;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CombatExperienceService extends Service {
	
	private final Map<Short, XpData> xpData;
	private final Map<Long, GroupObject> groupObjects;

	public CombatExperienceService() {
		xpData = new HashMap<>();
		groupObjects = new ConcurrentHashMap<>();
	}
	
	@Override
	public boolean initialize() {
		loadXpData();
		return true;
	}
	
	private void loadXpData() {
		long startTime = StandardLog.onStartLoad("combat XP rates");
		try (RelationalDatabase npcStats = RelationalServerFactory.getServerData("npc/npc_stats.db", "npc_stats")) {
			try (ResultSet set = npcStats.executeQuery("SELECT * FROM npc_stats")) {
				while (set.next()) {
					xpData.put(set.getShort("Level"), new XpData(set.getInt("XP"), set.getInt("Elite_XP"), set.getInt("Boss_XP")));
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
		StandardLog.onEndLoad(xpData.size(), "combat XP rates", startTime);
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject object = i.getObject();
		
		if (object.getBaselineType() == BaselineType.GRUP) {
			groupObjects.put(object.getObjectId(), (GroupObject) object);
		}
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent i) {
		SWGObject object = i.getObject();
		
		if(object instanceof GroupObject && groupObjects.remove(object.getObjectId()) == null) {
			Log.w("%s was expected to be in the GroupObject mapping but wasn't", object);
		}
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getCorpse();
		
		// You don't gain XP by PvP'ing
		if(corpse.isPlayer()) {
			return;
		}
		
		CreatureObject killer = i.getKiller();
		GroupObject group = groupObjects.get(killer.getGroupId());

		short killerLevel = group != null ? group.getLevel() : killer.getLevel();
		int experienceGained = calculateXpGain(killer, corpse, killerLevel);

		if (experienceGained <= 0) {
			return;
		}
		
		if (group == null) {
			grantXp(killer, corpse, experienceGained);
		} else {
			group.getGroupMemberObjects().stream()
					.filter(groupMember -> isMemberNearby(corpse, groupMember))	// Only nearby members gain XP
					.filter(groupMember -> corpse.getDamageMap().containsKey(groupMember))	// Only members who have dealt damage gain XP
					.forEach(eligibleMember -> grantXp(eligibleMember, corpse, experienceGained));
		}
	}

	private void grantXp(CreatureObject receiver, CreatureObject corpse, int experienceGained) {
		WeaponType weaponType =receiver.getEquippedWeapon().getType();
		String xpType = xpTypeForWeaponType(weaponType);

		if (xpType == null) {
			Log.w("%s did not receive %d xp because the used weapon %s had unrecognized type", receiver, experienceGained, weaponType);
			return;
		}

		// Scouts gain trapping XP by killing creatures
		if (receiver.hasSkill("outdoors_scout_novice") && corpse.getGameObjectType() == GameObjectType.GOT_CREATURE) {
			new ExperienceIntent(receiver, "trapping", (int) Math.ceil(experienceGained / 10f)).broadcast();
		}

		boolean xpMultiply = experienceGained > 1;

		new ExperienceIntent(receiver, xpType, experienceGained, xpMultiply).broadcast();
	}
	
	private String xpTypeForWeaponType(WeaponType weaponType) {
		switch (weaponType) {
			case UNARMED: return "combat_meleespecialize_unarmed";
			case TWO_HANDED_MELEE: return "combat_meleespecialize_twohand";
			case ONE_HANDED_MELEE: return "combat_meleespecialize_onehand";
			case POLEARM_MELEE: return "combat_meleespecialize_polearm";
			case RIFLE: return "combat_rangedspecialize_rifle";
			case CARBINE: return "combat_rangedspecialize_carbine";
			case PISTOL: return "combat_rangedspecialize_pistol";
			case HEAVY: return "combat_rangedspecialize_heavy";
			case ONE_HANDED_SABER: return "combat_meleespecialize_onehandlightsaber";
			case POLEARM_SABER: return "combat_meleespecialize_polearmlightsaber";
			case TWO_HANDED_SABER: return "combat_meleespecialize_twohandlightsaber";
			default: return null;
		}
	}
	
	private int calculateXpGain(CreatureObject killer, CreatureObject corpse, short killerLevel) {
		short corpseLevel = corpse.getLevel();
		
		if(killerLevel >= 90){
			return 0;
		} else if (killerLevel - corpseLevel >= 10) {
			return 1;
		} else {
			XpData xpForLevel = this.xpData.get(corpseLevel);

			if (xpForLevel == null) {
				Log.e("%s received no XP: No XP data was found for level %d!", killer, corpseLevel);
				return 0;
			}

			CreatureDifficulty creatureDifficulty = corpse.getDifficulty();

			switch (creatureDifficulty) {
				case BOSS:
					return xpForLevel.getBossXp();
				case ELITE:
					return xpForLevel.getEliteXp();
				case NORMAL:
					return xpForLevel.getXp();
				default:
					Log.e("%s received no XP: Unsupported creature difficulty %s of corpse %s", killer, creatureDifficulty, corpse);
					return 0;
			}
		}
	}
	
	/**
	 * @param corpse of the NPC that was killed
	 * @param groupMember of the group that killed the NPC
	 * @return true if {@code groupMember} is close enough to the corpse to gain XP
	 */
	private boolean isMemberNearby(CreatureObject corpse, CreatureObject groupMember) {
		return corpse.distanceTo(groupMember) <= 128;
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
