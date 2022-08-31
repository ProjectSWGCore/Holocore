package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class NoSpawnZoneLoader extends DataLoader {
	
	private final Map<Terrain, Collection<NoSpawnZoneInfo>> noSpawnZoneMap;
	
	public NoSpawnZoneLoader() {
		noSpawnZoneMap = new HashMap<>();
	}
	
	/**
	 * Determines whether a given location is located within a zone where spawning buildings and dynamic NPCs is disallowed.
	 * @param location to check
	 * @return {@code true} if the location is located inside a no spawn zone and {@code false} otherwise
	 */
	public boolean isInNoSpawnZone(@NotNull Location location) {
		Terrain terrain = location.getTerrain();
		
		Collection<NoSpawnZoneInfo> noSpawnZoneInfos = getNoSpawnZoneInfos(terrain);
		
		for (NoSpawnZoneInfo noSpawnZoneInfo : noSpawnZoneInfos) {
			long x = noSpawnZoneInfo.getX();
			long z = noSpawnZoneInfo.getZ();
			long radius = noSpawnZoneInfo.getRadius();
			
			Location noSpawnLocation = Location.builder()
					.setTerrain(terrain)
					.setX(x)
					.setZ(z)
					.build();
			
			boolean noSpawnZone = noSpawnLocation.isWithinFlatDistance(location, radius);
			
			if (noSpawnZone) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Finds no spawn zone information for the given terrain.
	 * Dynamic NPCs and player structures must not be spawned in these zones!
	 * @param terrain to find no spawn zone information for
	 * @return never {@code null}
	 */
	public @NotNull Collection<NoSpawnZoneInfo> getNoSpawnZoneInfos(Terrain terrain) {
		return noSpawnZoneMap.getOrDefault(terrain, Collections.emptyList());
	}
	
	@Override
	public void load() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/spawn/zones_nobuild_nospawn.sdb"))) {
			while (set.next()) {
				String planet = set.getText("terrain");
				Terrain terrain = Terrain.getTerrainFromName(planet);
				assert terrain != null : "unable to find terrain by name " + planet;
				
				Collection<NoSpawnZoneInfo> noSpawnInfos = noSpawnZoneMap.computeIfAbsent(terrain, k -> new ArrayList<>());
				NoSpawnZoneInfo info = new NoSpawnZoneInfo(set);
				noSpawnInfos.add(info);
			}
		}
	}
	
	public static class NoSpawnZoneInfo {
		private final long x;
		private final long z;
		private final long radius;
		
		public NoSpawnZoneInfo(SdbLoader.SdbResultSet set) {
			this.x = set.getInt("x");
			this.z = set.getInt("z");
			this.radius = set.getInt("radius");
		}
		
		public long getX() {
			return x;
		}
		
		public long getZ() {
			return z;
		}
		
		public long getRadius() {
			return radius;
		}
	}
}
