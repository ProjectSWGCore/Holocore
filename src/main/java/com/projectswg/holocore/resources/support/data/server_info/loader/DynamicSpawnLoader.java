package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DynamicSpawnLoader extends DataLoader {
	
	private final Map<Terrain, Collection<DynamicSpawnInfo>> terrainSpawns;
	
	DynamicSpawnLoader() {
		terrainSpawns = new HashMap<>();
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
	
	@Override
	public void load() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/nge/spawn/dynamic/dynamic_spawns.sdb"))) {
			while (set.next()) {
				String planet = set.getText("planet");
				Terrain terrain = Terrain.getTerrainFromName(planet);
				assert terrain != null : "unable to find terrain by name " + planet;
				
				Collection<DynamicSpawnInfo> dynamicSpawnInfos = terrainSpawns.computeIfAbsent(terrain, k -> new ArrayList<>());
				DynamicSpawnLoader.DynamicSpawnInfo dynamicSpawninfo = new DynamicSpawnLoader.DynamicSpawnInfo(set);
				dynamicSpawnInfos.add(dynamicSpawninfo);
			}
		}
	}
	
	public static class DynamicSpawnInfo {
		private String lairTemplate;
		private String npcBoss;
		private String npcElite;
		private String npcNormal1;
		private String npcNormal2;
		private String npcNormal3;
		private String npcNormal4;
		
		public DynamicSpawnInfo(SdbLoader.SdbResultSet set) {
			this.lairTemplate = set.getText("lair_type");
			this.npcBoss = set.getText("npc_boss");
			this.npcElite = set.getText("npc_elite");
			this.npcNormal1 = set.getText("npc_normal_1");
			this.npcNormal2 = set.getText("npc_normal_2");
			this.npcNormal3 = set.getText("npc_normal_3");
			this.npcNormal4 = set.getText("npc_normal_4");
		}
		
		public String getLairTemplate() {
			return lairTemplate;
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
	}
}
