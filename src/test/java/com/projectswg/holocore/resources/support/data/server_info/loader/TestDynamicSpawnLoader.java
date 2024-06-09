/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
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
		Collection<DynamicSpawnLoader.DynamicSpawnInfo> output = loader.getSpawnInfos(Terrain.TERRAIN_TEST);
		
		assertEquals(0, output.size(), "Unknown terrain should result in an empty list of dynamic spawns");
	}
	
	@Test
	public void testGetSpawnInfosUnmodifiable() {
		Collection<DynamicSpawnLoader.DynamicSpawnInfo> output = loader.getSpawnInfos(Terrain.TATOOINE);	// Hopefully we have spawns on Tatooine

		assertThrows(UnsupportedOperationException.class, () -> output.add(null));
	}
}
