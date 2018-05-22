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
package com.projectswg.holocore.resources.server_info.loader.npc;

import com.projectswg.holocore.resources.server_info.SdbLoader;
import com.projectswg.holocore.resources.server_info.SdbLoader.SdbResultSet;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NpcStatLoader {
	
	private final Map<Integer, NpcStatInfo> npcStatMap;
	
	private NpcStatLoader() {
		this.npcStatMap = new HashMap<>();
	}
	
	public NpcStatInfo getNpcStats(int level) {
		return npcStatMap.get(level);
	}
	
	public int getNpcStatCount() {
		return npcStatMap.size();
	}
	
	private void loadFromFile() {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/npc/npc_stats.sdb"))) {
			while (set.next()) {
				NpcStatInfo stat = new NpcStatInfo(set);
				npcStatMap.put(stat.getLevel(), stat);
			}
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public static NpcStatLoader load() {
		NpcStatLoader loader = new NpcStatLoader();
		loader.loadFromFile();
		return loader;
	}
	
	public static class NpcStatInfo {
		
		private final int level;
		private final int healthRegen;
		private final int actionRegen;
		private final int mindRegen;
		private final DetailNpcStatInfo normalDetailStat;
		private final DetailNpcStatInfo eliteDetailStat;
		private final DetailNpcStatInfo bossDetailStat;
		
		public NpcStatInfo(SdbResultSet set) {
			this.level = (int) set.getInt("Level");
			this.healthRegen = (int) set.getInt("HealthRegen");
			this.actionRegen = (int) set.getInt("ActionRegen");
			this.mindRegen = (int) set.getInt("MindRegen");
			this.normalDetailStat = new DetailNpcStatInfo(set, null);
			this.eliteDetailStat = new DetailNpcStatInfo(set, "Elite");
			this.bossDetailStat = new DetailNpcStatInfo(set, "Boss");
		}
		
		public int getLevel() {
			return level;
		}
		
		public int getHealthRegen() {
			return healthRegen;
		}
		
		public int getActionRegen() {
			return actionRegen;
		}
		
		public int getMindRegen() {
			return mindRegen;
		}
		
		public DetailNpcStatInfo getNormalDetailStat() {
			return normalDetailStat;
		}
		
		public DetailNpcStatInfo getEliteDetailStat() {
			return eliteDetailStat;
		}
		
		public DetailNpcStatInfo getBossDetailStat() {
			return bossDetailStat;
		}
		
	}
	
	public static class DetailNpcStatInfo {
		
		private final int health;
		private final int action;
		private final int regen;
		private final int combatRegen;
		private final int damagePerSecond;
		private final int toHit;
		private final int def;
		private final int armor;
		private final int xp;
		
		private DetailNpcStatInfo(SdbResultSet set, String prefix) {
			health = get(set, prefix, "HP");
			action = get(set, prefix, "Action");
			regen = get(set, prefix, "Regen");
			combatRegen = get(set, prefix, "CombatRegen");
			damagePerSecond = get(set, prefix, "damagePerSecond");
			toHit = get(set, prefix, "ToHit");
			def = get(set, prefix, "Def");
			armor = get(set, prefix, "Armor");
			xp = get(set, prefix, "XP");
		}
		
		public int getHealth() {
			return health;
		}
		
		public int getAction() {
			return action;
		}
		
		public int getRegen() {
			return regen;
		}
		
		public int getCombatRegen() {
			return combatRegen;
		}
		
		public int getDamagePerSecond() {
			return damagePerSecond;
		}
		
		public int getToHit() {
			return toHit;
		}
		
		public int getDef() {
			return def;
		}
		
		public int getArmor() {
			return armor;
		}
		
		public int getXp() {
			return xp;
		}

		private static int get(SdbResultSet set, String prefix, String name) {
			if (prefix == null)
				return (int) set.getInt(name);
			return (int) set.getInt(prefix + "_" + name);
		}
		
	}
	
}
