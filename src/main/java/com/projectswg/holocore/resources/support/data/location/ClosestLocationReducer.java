package com.projectswg.holocore.resources.support.data.location;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

import java.util.function.BinaryOperator;

/**
 * Reducer that determines the Location that is closest to a given base Location.
 */
public class ClosestLocationReducer implements BinaryOperator<Location> {
	
	private final Location baseLocation;
	
	public ClosestLocationReducer(Location baseLocation) {
		this.baseLocation = baseLocation;
	}
	
	@Override
	public Location apply(Location location1, Location location2) {
		Terrain terrainBase = baseLocation.getTerrain();
		Terrain terrain1 = location1.getTerrain();
		Terrain terrain2 = location2.getTerrain();
		boolean terrain1Match = terrainBase == terrain1;
		boolean terrain2Match = terrainBase == terrain2;
		
		if (!terrain1Match && !terrain2Match) {
			// Given locations are both located on different planets
			return null;
		}
		
		if (terrain1Match && !terrain2Match) {
			// Location 1 is a best fit since it has the same terrain as the base location while location 2 doesn't
			return location1;
		} else if (!terrain1Match) {
			// Location 2 is a best fit since it has the same terrain as the base location while location 1 doesn't
			return location2;
		}
		
		// location1 and location2 are on same terrain as baseLocation. Let's perform a distance check.
		double location1Distance = baseLocation.flatDistanceTo(location1);
		double location2Distance = baseLocation.flatDistanceTo(location2);
		
		if (location1Distance > location2Distance) {
			return location2;    // Location 2 is closest to the player - return location2
		} else {
			return location1;    // Location 1 is closest to the player or both locations are equally close - return location1
		}
	}
}
