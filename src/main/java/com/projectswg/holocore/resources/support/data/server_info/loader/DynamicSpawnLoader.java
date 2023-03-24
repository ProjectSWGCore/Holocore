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
package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DynamicSpawnLoader extends DataLoader {
	
	private final Map<Terrain, Collection<DynamicSpawnInfo>> terrainSpawns;
	private final Map<String, DynamicSpawnInfo> dynamicIdToDynamicSpawn;
	
	DynamicSpawnLoader() {
		terrainSpawns = new HashMap<>();
		dynamicIdToDynamicSpawn = new HashMap<>();
	}
	
	/**
	 * Fetches dynamic spawn information for the given terrain.
	 * @param terrain to find spawn information for
	 * @return collection of spawn information. Never {@code null}, can be empty and is unmodifiable.
	 */
	public @NotNull Collection<DynamicSpawnInfo> getSpawnInfos(Terrain terrain) {
		if (!terrainSpawns.containsKey(terrain)) {
			return Collections.emptyList();
		}
		
		
		return Collections.unmodifiableCollection(terrainSpawns.get(terrain));
	}
	
	public @Nullable DynamicSpawnInfo getSpawnInfo(String dynamicId) {
		return dynamicIdToDynamicSpawn.get(dynamicId);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/spawn/dynamic/dynamic_spawns.sdb"))) {
			while (set.next()) {
				String planetsCellValue = set.getText("planets");
				String[] planets = planetsCellValue.split(";");
				DynamicSpawnLoader.DynamicSpawnInfo dynamicSpawnInfo = new DynamicSpawnLoader.DynamicSpawnInfo(set);
				
				for (String planet : planets) {
					Terrain terrain = Terrain.getTerrainFromName(planet);
					assert terrain != null : "unable to find terrain by name " + planet;
					
					Collection<DynamicSpawnInfo> dynamicSpawnInfos = terrainSpawns.computeIfAbsent(terrain, k -> new ArrayList<>());
					dynamicSpawnInfos.add(dynamicSpawnInfo);
				}
				
				dynamicIdToDynamicSpawn.put(dynamicSpawnInfo.dynamicId, dynamicSpawnInfo);
			}
		}
	}
	
	public static class DynamicSpawnInfo {
		private String dynamicId;
		private String[] lairIds;
		private String npcBoss;
		private String npcElite;
		private String npcNormal1;
		private String npcNormal2;
		private String npcNormal3;
		private String npcNormal4;
		private String npcNormal5;
		private String npcNormal6;
		private String npcNormal7;
		private String npcNormal8;
		private String npcNormal9;
		private final NpcStaticSpawnLoader.SpawnerFlag spawnerFlag;
		
		public DynamicSpawnInfo(SdbLoader.SdbResultSet set) {
			this.dynamicId = set.getText("dynamic_id");
			this.lairIds = set.getText("lair_id").split(";");
			this.npcBoss = set.getText("npc_boss");
			this.npcElite = set.getText("npc_elite");
			this.npcNormal1 = set.getText("npc_normal_1");
			this.npcNormal2 = set.getText("npc_normal_2");
			this.npcNormal3 = set.getText("npc_normal_3");
			this.npcNormal4 = set.getText("npc_normal_4");
			this.npcNormal4 = set.getText("npc_normal_5");
			this.npcNormal4 = set.getText("npc_normal_6");
			this.npcNormal4 = set.getText("npc_normal_7");
			this.npcNormal4 = set.getText("npc_normal_8");
			this.npcNormal4 = set.getText("npc_normal_9");
			this.spawnerFlag = readSpawnerFlag(dynamicId, set);
		}

		private NpcStaticSpawnLoader.SpawnerFlag readSpawnerFlag(String id, SdbLoader.SdbResultSet set) {
			String columnName = "attackable";

			try {
				return NpcStaticSpawnLoader.SpawnerFlag.valueOf(set.getText(columnName));
			} catch (IllegalArgumentException e) {
				Log.w("Unknown attackable flag for dynamic_id '%s': '%s'", id, set.getText(columnName));
				return NpcStaticSpawnLoader.SpawnerFlag.INVULNERABLE;
			}
		}
		
		public String getDynamicId() {
			return dynamicId;
		}
		
		public String[] getLairIds() {
			return lairIds;
		}
		
		public String getNpcBoss() {
			return npcBoss;
		}
		
		public String getNpcElite() {
			return npcElite;
		}
		
		public String getNpcNormal1() {
			return npcNormal1;
		}
		
		public String getNpcNormal2() {
			return npcNormal2;
		}
		
		public String getNpcNormal3() {
			return npcNormal3;
		}
		
		public String getNpcNormal4() {
			return npcNormal4;
		}

		public NpcStaticSpawnLoader.SpawnerFlag getSpawnerFlag() {
			return spawnerFlag;
		}
	}
}
