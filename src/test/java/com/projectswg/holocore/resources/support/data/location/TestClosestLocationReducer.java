package com.projectswg.holocore.resources.support.data.location;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestClosestLocationReducer {
	
	private ClosestLocationReducer reducer;
	
	@Before
	public void setup() {
		Location base = Location.builder()
				.setTerrain(Terrain.TATOOINE)
				.setX(100)
				.setZ(-100)
				.build();
		reducer = new ClosestLocationReducer(base);
	}
	
	/**
	 * Verifies that the terrain check can return neither of the given locations if both are on different planets
	 */
	@Test
	public void testBothLocationsDifferentPlanet() {
		Location loc1 = Location.builder()
				.setTerrain(Terrain.DANTOOINE)
				.setX(100)
				.setZ(-100)
				.build();
		
		Location loc2 = Location.builder()
				.setTerrain(Terrain.DANTOOINE)
				.setX(900)
				.setZ(-900)
				.build();
		
		Location reduced = reducer.apply(loc1, loc2);
		
		assertNull("Null should be the result when reducing locations not located on the same planet as the base location", reduced);
	}
	
	/**
	 * Verifies that the distance check works when all locations are on the same planet
	 */
	@Test
	public void testBothLocationsSamePlanet() {
		Location closest = Location.builder()
				.setTerrain(Terrain.TATOOINE)
				.setX(110)
				.setZ(-110)
				.build();
		
		Location farthest = Location.builder()
				.setTerrain(Terrain.TATOOINE)
				.setX(900)
				.setZ(-900)
				.build();
		
		Location reduced = reducer.apply(closest, farthest);
		
		assertEquals("The location closest to the base location should be the reduced location", closest, reduced);
	}
	
	/**
	 * Verifies that the distance check only matters if on the same terrain
	 */
	@Test
	public void testOneLocationOnDifferentPlanet() {
		Location base = Location.builder()
				.setTerrain(Terrain.TATOOINE)
				.setX(100)
				.setZ(-100)
				.build();
		ClosestLocationReducer reducer = new ClosestLocationReducer(base);
		
		Location closestDifferentPlanet = Location.builder()
				.setTerrain(Terrain.DANTOOINE)
				.setX(110)
				.setZ(-110)
				.build();
		
		Location farthestSamePlanet = Location.builder()
				.setTerrain(Terrain.TATOOINE)
				.setX(900)
				.setZ(-900)
				.build();
		
		Location reduced = reducer.apply(closestDifferentPlanet, farthestSamePlanet);
		
		assertEquals("The location on the same planet should be picked even if it's furthest away from the X Y Z coordinates", farthestSamePlanet, reduced);
	}
	
}
