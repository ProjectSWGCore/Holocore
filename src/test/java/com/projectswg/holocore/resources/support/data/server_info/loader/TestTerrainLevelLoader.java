package com.projectswg.holocore.resources.support.data.server_info.loader;

import org.junit.Test;

import java.io.IOException;

public class TestTerrainLevelLoader {
	
	@Test
	public void setup() throws IOException {
		var loader = new TerrainLevelLoader();
		loader.load();	// We just expect this to not throw an exception
	}
	
}
