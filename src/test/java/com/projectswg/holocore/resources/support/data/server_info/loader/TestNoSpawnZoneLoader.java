package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TestNoSpawnZoneLoader {
	
	@Parameterized.Parameter
	public Input input;
	
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Input> parameters() {
		Location mosEisleyLocation = Location.builder()
				.setTerrain(Terrain.TATOOINE)
				.setX(3500)
				.setZ(-4800)
				.build();
		
		// TODO location where result should be false
		
		Location wildernessLocation = Location.builder()
				.setTerrain(Terrain.DANTOOINE)
				.setX(-3500)
				.setZ(4800)
				.build();
		
		return Arrays.asList(
				new Input(mosEisleyLocation, true),	// Should not be able to build or spawn anything in Mos Eisley
				new Input(wildernessLocation, false)	// Should be able to build or spawn whatever in the middle of nowhere on Dantooine
		);
	}
	
	private NoSpawnZoneLoader loader;
	
	@Before
	public void setup() throws IOException {
		loader = new NoSpawnZoneLoader();
		loader.load();	// We just expect this to not throw an exception
	}
	
	@Test
	public void testIsInNoSpawnZone() {
		boolean actual = loader.isInNoSpawnZone(input.getLocation());
		boolean expected = input.isNoBuildZone();
		
		assertEquals(actual, expected);
	}
	
	private static class Input {
		private final Location location;
		private final boolean noBuildZone;
		
		public Input(Location location, boolean noBuildZone) {
			this.location = location;
			this.noBuildZone = noBuildZone;
		}
		
		public Location getLocation() {
			return location;
		}
		
		public boolean isNoBuildZone() {
			return noBuildZone;
		}
		
		@Override
		public String toString() {
			return location + ", noBuildZone=" + noBuildZone;
		}
	}
}
