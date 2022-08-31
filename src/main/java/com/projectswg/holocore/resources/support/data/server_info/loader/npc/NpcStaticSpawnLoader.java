/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader.npc;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.npc.spawn.SpawnInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class NpcStaticSpawnLoader extends DataLoader {
	
	private final List<StaticSpawnInfo> spawns;
	
	public NpcStaticSpawnLoader() {
		this.spawns = new ArrayList<>();
	}
	
	public List<StaticSpawnInfo> getSpawns() {
		return spawns;
	}
	
	public void iterate(Consumer<StaticSpawnInfo> spawn) {
		spawns.forEach(spawn);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/spawn/static.msdb"))) {
			spawns.addAll(set.parallelStream(StaticSpawnInfo::new).collect(Collectors.toList()));
		}
	}
	
	public enum SpawnerFlag {
		AGGRESSIVE,
		ATTACKABLE,
		INVULNERABLE
	}
	
	public static class StaticSpawnInfo implements SpawnInfo {
		
		private final String id;
		private final Terrain terrain;
		private final double x;
		private final double y;
		private final double z;
		private final int heading;
		private final int cellId;
		private final SpawnerFlag spawnerFlag;
		private final CreatureDifficulty difficulty;
		private final int minLevel;
		private final int maxLevel;
		private final String spawnerType;
		private final String npcId;
		private final String buildingId;
		private final String mood;
		private final AIBehavior behavior;
		private final String patrolId;
		private final PatrolFormation patrolFormation;
		private final int loiterRadius;
		private final int minSpawnTime;
		private final int maxSpawnTime;
		private final int amount;
		private final String conversationId;
		
		private StaticSpawnInfo(SdbResultSet set) {
			this.id = set.getText("spawn_id");
			this.terrain = Terrain.valueOf(set.getText("terrain"));
			this.x = set.getReal("x");
			this.y = set.getReal("y");
			this.z = set.getReal("z");
			this.heading = (int) set.getInt("heading");
			this.cellId = (int) set.getInt("cell_id");
			this.spawnerType = set.getText("spawner_type").intern();
			this.npcId = set.getText("npc_id").intern();
			{
				SpawnerFlag flag;
				try {
					flag = SpawnerFlag.valueOf(set.getText("attackable"));
				} catch (IllegalArgumentException e) {
					Log.w("Unknown attackable flag for spawn_id '%s': '%s'", id, set.getText("attackable"));
					flag = SpawnerFlag.INVULNERABLE;
				}
				this.spawnerFlag = flag;
				// INVULERNABLE
				// INVULNERABLE
			}
			this.minLevel = (int) set.getInt("min_cl");
			this.maxLevel = (int) set.getInt("max_cl");
			this.buildingId = set.getText("building_id").intern();
			this.mood = parseMood(set.getText("mood")).intern();
			this.behavior = AIBehavior.valueOf(set.getText("behaviour"));
			this.patrolId = set.getText("patrol_id");
			this.patrolFormation = parsePatrolFormation(set.getText("patrol_formation"));
			this.loiterRadius = (int) set.getInt("loiter_radius");
			int spawnTime = (int) set.getInt("respawn");
			this.minSpawnTime = (int) (spawnTime * 0.9);
			this.maxSpawnTime = (int) (spawnTime * 1.1);
			this.amount = (int) set.getInt("amount");
			
			switch (set.getText("difficulty")) {
				default:
					Log.w("Unknown difficulty: %s", set.getText("difficulty"));
				case "N":
					this.difficulty = CreatureDifficulty.NORMAL;
					break;
				case "E":
					this.difficulty = CreatureDifficulty.ELITE;
					break;
				case "B":
					this.difficulty = CreatureDifficulty.BOSS;
					break;
			}
			
			this.conversationId = set.getText("conversation_id");
		}
		
		@Override
		public String getId() {
			return id;
		}

		@Override
		public Terrain getTerrain() {
			return terrain;
		}

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

		@Override
		public double getZ() {
			return z;
		}

		@Override
		public int getHeading() {
			return heading;
		}

		@Override
		public int getCellId() {
			return cellId;
		}

		@Override
		public String getSpawnerType() {
			return spawnerType;
		}

		@Override
		public String getNpcId() {
			return npcId;
		}

		@Override
		public SpawnerFlag getSpawnerFlag() {
			return spawnerFlag;
		}

		@Override
		public CreatureDifficulty getDifficulty() {
			return difficulty;
		}

		@Override
		public int getMinLevel() {
			return minLevel;
		}

		@Override
		public int getMaxLevel() {
			return maxLevel;
		}

		@Override
		public String getBuildingId() {
			return buildingId;
		}

		@Override
		public String getMood() {
			return mood;
		}

		@Override
		public AIBehavior getBehavior() {
			return behavior;
		}

		@Override
		public String getPatrolId() {
			return patrolId;
		}

		@Override
		public PatrolFormation getPatrolFormation() {
			return patrolFormation;
		}

		@Override
		public int getLoiterRadius() {
			return loiterRadius;
		}

		@Override
		public int getMinSpawnTime() {
			return minSpawnTime;
		}

		@Override
		public int getMaxSpawnTime() {
			return maxSpawnTime;
		}

		@Override
		public int getAmount() {
			return amount;
		}
		
		@Override
		public String getConversationId() {
			return conversationId;
		}
		
		private static PatrolFormation parsePatrolFormation(String str) {
			switch (str.toUpperCase(Locale.US)) {
				case "COLUMN":
					return PatrolFormation.COLUMN;
				case "WEDGE":
					return PatrolFormation.WEDGE;
				case "LINE":
					return PatrolFormation.LINE;
				case "BOX":
					return PatrolFormation.BOX;
				case "":
				default:
					return PatrolFormation.NONE;
			}
		}
		
		private static String parseMood(String mood) {
			return mood.equals("idle") ? "neutral" : mood;
		}
		
	}
	
	public enum PatrolFormation {
		NONE,
		COLUMN,
		WEDGE,
		LINE,
		BOX
	}
	
}
