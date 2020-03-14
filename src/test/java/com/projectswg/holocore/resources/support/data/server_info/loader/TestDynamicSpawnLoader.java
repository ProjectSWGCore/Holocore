package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Terrain;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.*;

public class TestDynamicSpawnLoader {
	
	private DynamicSpawnLoader loader;
	
	@Before
	public void setup() throws IOException {
		loader = new DynamicSpawnLoader();
		loader.load();	// We just expect this to not throw an exception
	}
	
	@Test
	public void testGetSpawnInfosNullTerrain() {
		Collection<DynamicSpawnLoader.DynamicSpawnInfo> output = loader.getSpawnInfos(null);
		
		assertEquals("Unknown terrain should result in an empty list of dynamic spawns", 0, output.size());
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testGetSpawnInfosUnmodifiable() {
		Collection<DynamicSpawnLoader.DynamicSpawnInfo> output = loader.getSpawnInfos(Terrain.TATOOINE);	// Hopefully we have spawns on Tatooine
		
		output.add(null);	// Adding something to an unmodifiable collection should throw an exception
	}
}
