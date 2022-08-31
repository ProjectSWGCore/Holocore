package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class TerrainLevelLoader extends DataLoader {
	
	private final Map<Terrain, TerrainLevelInfo> terrainLevelInfoMap;
	
	public TerrainLevelLoader() {
		terrainLevelInfoMap = new HashMap<>();
	}
	
	/**
	 * Fetches level information about the given terrain.
	 * @param terrain to find level information for
	 * @return level information for the specified {@code terrain}
	 */
	public @Nullable TerrainLevelInfo getTerrainLevelInfo(@NotNull Terrain terrain) {
		return terrainLevelInfoMap.get(terrain);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/spawn/dynamic/terrain_levels.sdb"))) {
			while (set.next()) {
				String planet = set.getText("terrain");
				Terrain terrain = Terrain.getTerrainFromName(planet);
				assert terrain != null : "unable to find terrain by name " + planet;
				
				TerrainLevelLoader.TerrainLevelInfo terrainLevelInfo = new TerrainLevelLoader.TerrainLevelInfo(set);
				
				terrainLevelInfoMap.put(terrain, terrainLevelInfo);
			}
		}
	}
	
	public static class TerrainLevelInfo {
		private final long minLevel;
		private final long maxLevel;
		
		public TerrainLevelInfo(SdbLoader.SdbResultSet set) {
			this.minLevel = set.getInt("min_level");
			this.maxLevel = set.getInt("max_level");
		}
		
		public long getMinLevel() {
			return minLevel;
		}
		
		public long getMaxLevel() {
			return maxLevel;
		}
	}
}
