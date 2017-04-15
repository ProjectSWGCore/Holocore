/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.objects.awareness;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

import resources.objects.SWGObject;
import resources.objects.cell.CellObject;

@RunWith(JUnit4.class)
public class TestTerrainMapChunk {
	
	/*
	 * Test Cases:
	 *   A: Within A and B's load range in same chunk
	 *   B: Within A's load range but not B's in same chunk
	 *   C: Within A and B's load range in different chunks
	 *   D: Within A's load range but not B's in different chunks
	 *   E: Not within A's load range but within B's in same chunk
	 *   F: Not within A's load range and not within B's in same chunk
	 *   G: Not within A's load range but within B's in different chunks
	 *   H: Not within A's load range and not within B's in different chunks
	 */
	
	@Test
	public void testContains() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = new GenericCreatureObject(1);
		GenericCreatureObject objB = new GenericCreatureObject(2);
		objA.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		objB.setLocation(new Location(100, 0, 0, Terrain.TATOOINE)); // 100m away
		chunk.addObject(objA);
		chunk.addObject(objB);
		Assert.assertTrue("CONTAINS-OBJA", chunk.containsObject(objA));
		Assert.assertTrue("CONTAINS-OBJB", chunk.containsObject(objB));
	}
	
	@Test
	public void testRemove() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = new GenericCreatureObject(1);
		GenericCreatureObject objB = new GenericCreatureObject(2);
		objA.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		objB.setLocation(new Location(100, 0, 0, Terrain.TATOOINE)); // 100m away
		chunk.addObject(objA);
		Assert.assertTrue("CONTAINS-OBJA", chunk.containsObject(objA));
		chunk.addObject(objB);
		Assert.assertTrue("CONTAINS-OBJB", chunk.containsObject(objB));
		chunk.removeObject(objA);
		Assert.assertFalse("CONTAINS-OBJA", chunk.containsObject(objA));
		chunk.removeObject(objB);
		Assert.assertFalse("CONTAINS-OBJB", chunk.containsObject(objB));
	}
	
	@Test
	public void testCaseA() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = createCreature(chunk, 1, 0, 0, 0, 100);
		GenericCreatureObject objB = createCreature(chunk, 2, 50, 0, 0, 50);
		testContains(chunk, objA, objB);
	}
	
	@Test
	public void testCaseB() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = createCreature(chunk, 1, 0, 0, 0, 100);
		GenericCreatureObject objB = createCreature(chunk, 2, 50, 0, 0, 25);
		testContains(chunk, objA, objB);
	}
	
	@Test
	public void testCaseC() {
		TerrainMapChunk chunkA = new TerrainMapChunk(0, 0, 0, 0);
		TerrainMapChunk chunkB = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = createCreature(chunkA, 1, 0, 0, 0, 100);
		GenericCreatureObject objB = createCreature(chunkB, 2, 50, 0, 0, 50);
		testContains(chunkB, objA, objB);
	}
	
	@Test
	public void testCaseD() {
		TerrainMapChunk chunkA = new TerrainMapChunk(0, 0, 0, 0);
		TerrainMapChunk chunkB = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = createCreature(chunkA, 1, 0, 0, 0, 100);
		GenericCreatureObject objB = createCreature(chunkB, 2, 50, 0, 0, 25);
		testContains(chunkB, objA, objB);
	}
	
	@Test
	public void testCaseE() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = createCreature(chunk, 1, 0, 0, 0, 25);
		GenericCreatureObject objB = createCreature(chunk, 2, 50, 0, 0, 50);
		testContains(chunk, objA, objB);
	}
	
	@Test
	public void testCaseF() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = createCreature(chunk, 1, 0, 0, 0, 25);
		createCreature(chunk, 2, 50, 0, 0, 25);
		testContains(chunk, objA);
	}
	
	@Test
	public void testCaseG() {
		TerrainMapChunk chunkA = new TerrainMapChunk(0, 0, 0, 0);
		TerrainMapChunk chunkB = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = createCreature(chunkA, 1, 0, 0, 0, 25);
		GenericCreatureObject objB = createCreature(chunkB, 2, 50, 0, 0, 50);
		testContains(chunkB, objA, objB);
	}
	
	@Test
	public void testCaseH() {
		TerrainMapChunk chunkA = new TerrainMapChunk(0, 0, 0, 0);
		TerrainMapChunk chunkB = new TerrainMapChunk(0, 0, 0, 0);
		GenericCreatureObject objA = createCreature(chunkA, 1, 0, 0, 0, 25);
		createCreature(chunkB, 2, 50, 0, 0, 25);
		testContains(chunkB, objA);
	}
	
	@Test
	public void testCaseCellA() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		CellObject cell = new CellObject(3);
		cell.setPrefLoadRange(0);
		chunk.addObject(cell);
		cell.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		GenericCreatureObject objA = createCreature(null, 1, 0, 0, 0, 100);
		GenericCreatureObject objB = createCreature(chunk, 2, 50, 0, 0, 50);
		cell.addObject(objA);
		Assert.assertEquals("CELL-LOAD-RANGE", 100, cell.getLoadRange(), 1E-7);
		testContains(chunk, cell, objB);
	}
	
	@Test
	public void testCaseCellB() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		CellObject cell = new CellObject(3);
		cell.setPrefLoadRange(0);
		chunk.addObject(cell);
		cell.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		GenericCreatureObject objA = createCreature(null, 1, 0, 0, 0, 100);
		GenericCreatureObject objB = createCreature(chunk, 2, 50, 0, 0, 25);
		cell.addObject(objA);
		Assert.assertEquals("CELL-LOAD-RANGE", 100, cell.getLoadRange(), 1E-7);
		testContains(chunk, cell, objB);
	}
	
	@Test
	public void testCaseCellC() {
		TerrainMapChunk chunkA = new TerrainMapChunk(0, 0, 0, 0);
		TerrainMapChunk chunkB = new TerrainMapChunk(0, 0, 0, 0);
		CellObject cell = new CellObject(3);
		cell.setPrefLoadRange(0);
		chunkA.addObject(cell);
		cell.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		GenericCreatureObject objA = createCreature(null, 1, 0, 0, 0, 100);
		GenericCreatureObject objB = createCreature(chunkB, 2, 50, 0, 0, 50);
		cell.addObject(objA);
		Assert.assertEquals("CELL-LOAD-RANGE", 100, cell.getLoadRange(), 1E-7);
		testContains(chunkB, cell, objB);
	}
	
	@Test
	public void testCaseCellD() {
		TerrainMapChunk chunkA = new TerrainMapChunk(0, 0, 0, 0);
		TerrainMapChunk chunkB = new TerrainMapChunk(0, 0, 0, 0);
		CellObject cell = new CellObject(3);
		cell.setPrefLoadRange(0);
		chunkA.addObject(cell);
		cell.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		GenericCreatureObject objA = createCreature(null, 1, 0, 0, 0, 100);
		GenericCreatureObject objB = createCreature(chunkB, 2, 50, 0, 0, 25);
		cell.addObject(objA);
		Assert.assertEquals("CELL-LOAD-RANGE", 100, cell.getLoadRange(), 1E-7);
		testContains(chunkB, cell, objB);
	}
	
	@Test
	public void testCaseCellE() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		CellObject cell = new CellObject(3);
		cell.setPrefLoadRange(0);
		cell.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		GenericCreatureObject objA = createCreature(null, 1, 0, 0, 0, 25);
		GenericCreatureObject objB = createCreature(chunk, 2, 50, 0, 0, 50);
		cell.addObject(objA);
		Assert.assertEquals("CELL-LOAD-RANGE", 25, cell.getLoadRange(), 1E-7);
		testContains(chunk, cell, objB);
	}
	
	@Test
	public void testCaseCellF() {
		TerrainMapChunk chunk = new TerrainMapChunk(0, 0, 0, 0);
		CellObject cell = new CellObject(3);
		cell.setPrefLoadRange(0);
		cell.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		GenericCreatureObject objA = createCreature(null, 1, 0, 0, 0, 25);
		createCreature(chunk, 2, 50, 0, 0, 25);
		cell.addObject(objA);
		Assert.assertEquals("CELL-LOAD-RANGE", 25, cell.getLoadRange(), 1E-7);
		testContains(chunk, cell);
	}
	
	@Test
	public void testCaseCellG() {
		TerrainMapChunk chunkA = new TerrainMapChunk(0, 0, 0, 0);
		TerrainMapChunk chunkB = new TerrainMapChunk(0, 0, 0, 0);
		CellObject cell = new CellObject(3);
		cell.setPrefLoadRange(0);
		chunkA.addObject(cell);
		cell.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		GenericCreatureObject objA = createCreature(null, 1, 0, 0, 0, 25);
		GenericCreatureObject objB = createCreature(chunkB, 2, 50, 0, 0, 50);
		cell.addObject(objA);
		Assert.assertEquals("CELL-LOAD-RANGE", 25, cell.getLoadRange(), 1E-7);
		testContains(chunkB, cell, objB);
	}
	
	@Test
	public void testCaseCellH() {
		TerrainMapChunk chunkA = new TerrainMapChunk(0, 0, 0, 0);
		TerrainMapChunk chunkB = new TerrainMapChunk(0, 0, 0, 0);
		CellObject cell = new CellObject(3);
		cell.setPrefLoadRange(0);
		chunkA.addObject(cell);
		cell.setLocation(new Location(0, 0, 0, Terrain.TATOOINE));
		GenericCreatureObject objA = createCreature(null, 1, 0, 0, 0, 25);
		createCreature(chunkB, 2, 50, 0, 0, 25);
		cell.addObject(objA);
		Assert.assertEquals("CELL-LOAD-RANGE", 25, cell.getLoadRange(), 1E-7);
		testContains(chunkB, cell);
	}
	
	private GenericCreatureObject createCreature(TerrainMapChunk chunk, long id, double x, double y, double z, double loadRange) {
		GenericCreatureObject creature = new GenericCreatureObject(id);
		creature.setLocation(new Location(x, y, z, Terrain.TATOOINE));
		creature.setPrefLoadRange(loadRange);
		if (chunk != null)
			chunk.addObject(creature);
		return creature;
	}
	
	private void testContains(TerrainMapChunk chunk, SWGObject src, SWGObject ... expected) {
		List<SWGObject> aware = chunk.getWithinAwareness(src);
		Assert.assertEquals("TEST-SIZE", expected.length, aware.size());
		for (SWGObject e : expected) {
			Assert.assertTrue("TEST-CONTAIN-"+e, aware.contains(e));
		}
	}
	
}
