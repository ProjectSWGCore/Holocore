package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Terrain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class TestDynamicSpawnLoader {
	
	private DynamicSpawnLoader loader;
	
	@BeforeEach
	public void setup() throws IOException {
		loader = new DynamicSpawnLoader();
		loader.load();	// We just expect this to not throw an exception
	}
	
	@Test
	public void testGetSpawnInfosNullTerrain() {
		Collection<DynamicSpawnLoader.DynamicSpawnInfo> output = loader.getSpawnInfos(null);
		
		assertEquals(0, output.size(), "Unknown terrain should result in an empty list of dynamic spawns");
	}
	
	@Test
	public void testGetSpawnInfosUnmodifiable() {
		Collection<DynamicSpawnLoader.DynamicSpawnInfo> output = loader.getSpawnInfos(Terrain.TATOOINE);	// Hopefully we have spawns on Tatooine
		
		assertThrows(UnsupportedOperationException.class, () -> {
			output.add(null);
		});
	}
}
