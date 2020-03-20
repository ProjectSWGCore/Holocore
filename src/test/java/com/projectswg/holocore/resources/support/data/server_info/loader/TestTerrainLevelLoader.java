package com.projectswg.holocore.resources.support.data.server_info.loader;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class TestTerrainLevelLoader {
	
	private TerrainLevelLoader loader;
	
	@Before
	public void setup() throws IOException {
		loader = new TerrainLevelLoader();
		loader.load();	// We just expect this to not throw an exception
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetTerrainLevelInfoNull() {
		loader.getTerrainLevelInfo(null);
	}
	
}
