package services.player;

import resources.Location;
import resources.Terrain;

public class TerrainZoneInsertion {
	
	public static final Location getInsertionForTerrain(Terrain terrain) {
		switch (terrain) {
			case TATOOINE:
				return generateRandomLocation(terrain, 3525, 4, -4807, 5);
			case NABOO:
				return generateRandomLocation(terrain, -5496, 6, 4369, 5);
			case CORELLIA:
				return generateRandomLocation(terrain, -145, 28, -4720, 5);
			case LOK:
				return generateRandomLocation(terrain, 420, 7, 5267, 5);
			case DATHOMIR:
				return generateRandomLocation(terrain, 5304, 78, -4135, 5);
			case TALUS:
				return generateRandomLocation(terrain, 336, 6, -2930, 5);
			case RORI:
				return generateRandomLocation(terrain, 5289, 80, 6131, 5);
			case DANTOOINE:
				return generateRandomLocation(terrain, -614, 3, 2527, 5);
			case KASHYYYK_HUNTING:
				return generateRandomLocation(terrain, 244, 41, 466, 5);
			default:
				return null;
		}
	}
	
	public static final Location getInsertionForArea(Terrain terrain, double x, double y, double z) {
		return generateRandomLocation(terrain, x, y, z, 5);
	}
	
	
	private static final Location generateRandomLocation(Terrain terrain, double x, double y, double z, double delta) {
		Location location = new Location();
		location.setTerrain(terrain);
		location.setX(x + (Math.random()-.5) * delta);
		location.setY(y);
		location.setZ(z + (Math.random()-.5) * delta);
		location.setOrientationX(0);
		location.setOrientationY(0);
		location.setOrientationZ(0);
		location.setOrientationW(1);
		return location;
	}
	
}
