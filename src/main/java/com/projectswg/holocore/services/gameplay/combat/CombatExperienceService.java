/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import com.projectswg.holocore.services.support.objects.ObjectStorageService;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CombatExperienceService extends Service {
	
	private final Map<Short, XpData> xpData;

	public CombatExperienceService() {
		xpData = new HashMap<>();
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
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getCorpse();
		
		if(corpseIsNpc(corpse)) {
			CreatureObject killer = i.getKiller();
			GroupObject group = getGroupThatKillerIsIn(killer);
			
			short killerLevel = getKillerLevel(killer, group);
			int experienceGained = calculateXpGain(killer, corpse, killerLevel);
			
			if (experienceGained <= 0) {
				return;
			}
			
			boolean killerIsGrouped = group != null;
			
			if (killerIsGrouped) {
				grantXpToGroup(corpse, group, experienceGained);
			} else {
				grantXpToKiller(killer, corpse, experienceGained);
			}
		}
	}
	
	private GroupObject getGroupThatKillerIsIn(CreatureObject killer) {
		long groupId = killer.getGroupId();
		if (groupId > 0) {
			return (GroupObject) ObjectStorageService.ObjectLookup.getObjectById(groupId);
		} else {
			return null;
		}
	}
	
	private void grantXpToGroup(CreatureObject corpse, GroupObject group, int experienceGained) {
		group.getGroupMemberObjects().stream()
				.filter(groupMember -> isMemberNearby(corpse, groupMember))
				.filter(groupMember -> didMemberParticipateInCombat(corpse, groupMember))
				.forEach(eligibleMember -> grantXpToKiller(eligibleMember, corpse, experienceGained));
	}
	
	private boolean didMemberParticipateInCombat(CreatureObject corpse, CreatureObject groupMember) {
		return corpse.getHateMap().containsKey(groupMember);
	}
	
	private short getKillerLevel(CreatureObject killer, GroupObject group) {
		if (group != null) {
			return group.getLevel();
		} else {
			return killer.getLevel();
		}
	}
	
	private boolean corpseIsNpc(CreatureObject corpse) {
		return !corpse.isPlayer();
	}
	
	private void grantXpToKiller(CreatureObject killer, CreatureObject corpse, int experienceGained) {
		boolean xpMultiply = experienceGained > 1;
		grantWeaponTypeXp(killer, corpse, experienceGained, xpMultiply);
		
		if (scoutKilledCreature(killer, corpse)) {
			grantTrappingXp(killer, experienceGained);
		}
		
		grantCombatXp(killer, corpse, experienceGained, xpMultiply);
	}
	
	private void grantTrappingXp(CreatureObject receiver, int experienceGained) {
		new ExperienceIntent(receiver, "trapping", (int) Math.ceil(experienceGained / 10f)).broadcast();
	}
	
	private boolean scoutKilledCreature(CreatureObject killer, CreatureObject corpse) {
		return isKillerScout(killer) && isCorpseCreature(corpse);
	}
	
	private boolean isCorpseCreature(CreatureObject corpse) {
		return corpse.getGameObjectType() == GameObjectType.GOT_CREATURE;
	}
	
	private boolean isKillerScout(CreatureObject killer) {
		return killer.hasSkill("outdoors_scout_novice");
	}
	
	private void grantCombatXp(CreatureObject receiver, CreatureObject corpse, int experienceGained, boolean xpMultiply) {
		new ExperienceIntent(receiver, corpse, "combat_general", (int) Math.ceil(experienceGained / 10f), xpMultiply).broadcast();
	}
	
	private void grantWeaponTypeXp(CreatureObject receiver, CreatureObject corpse, int experienceGained, boolean xpMultiply) {
		WeaponType weaponType = receiver.getEquippedWeapon().getType();
		String xpType = xpTypeForWeaponType(weaponType);
		
		if (xpType == null) {
			Log.w("%s did not receive %d xp because the used weapon %s had unrecognized type", receiver, experienceGained, weaponType);
		}
		
		new ExperienceIntent(receiver, corpse, xpType, experienceGained, xpMultiply).broadcast();
	}
	
	private String xpTypeForWeaponType(WeaponType weaponType) {
		return switch (weaponType) {
			case UNARMED -> "combat_meleespecialize_unarmed";
			case TWO_HANDED_MELEE -> "combat_meleespecialize_twohand";
			case ONE_HANDED_MELEE -> "combat_meleespecialize_onehand";
			case POLEARM_MELEE -> "combat_meleespecialize_polearm";
			case RIFLE -> "combat_rangedspecialize_rifle";
			case CARBINE -> "combat_rangedspecialize_carbine";
			case PISTOL -> "combat_rangedspecialize_pistol";
			case HEAVY -> "combat_rangedspecialize_heavy";
			case ONE_HANDED_SABER, POLEARM_SABER, TWO_HANDED_SABER -> "jedi_general";
			default -> null;
		};
	}
	
	private int calculateXpGain(CreatureObject killer, CreatureObject corpse, short killerLevel) {
		short corpseLevel = corpse.getLevel();
		
		if (killerLevel - corpseLevel >= 5) {
			return 1;
		} else {
			XpData xpForLevel = this.xpData.get(corpseLevel);

			if (xpForLevel == null) {
				Log.e("%s received no XP: No XP data was found for level %d!", killer, corpseLevel);
				return 0;
			}

			CreatureDifficulty creatureDifficulty = corpse.getDifficulty();

			return switch (creatureDifficulty) {
				case BOSS -> xpForLevel.bossXp();
				case ELITE -> xpForLevel.eliteXp();
				case NORMAL -> xpForLevel.xp();
			};
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

	private record XpData(int xp, int eliteXp, int bossXp) {

	}
	
}
