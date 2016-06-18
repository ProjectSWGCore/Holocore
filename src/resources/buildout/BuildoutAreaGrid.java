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
package resources.buildout;

import java.util.ArrayList;
import java.util.List;

import resources.Terrain;

public class BuildoutAreaGrid {
	
	private static final double MIN = -1024*16;
	private static final double MAX = 1024*16;
	
	private final BuildoutAreaNode [][] grid;
	
	public BuildoutAreaGrid() {
		this(32);
	}
	
	public BuildoutAreaGrid(int width) {
		grid = new BuildoutAreaNode[width][width];
	}
	
	public void clear() {
		synchronized (grid) {
			for (int y = 0; y < grid.length; y++) {
				for (int x = 0; x < grid.length; x++) {
					grid[y][x] = null;
				}
			}
		}
	}
	
	public void addBuildoutArea(BuildoutArea area) {
		int zMax = getNodeIndex(area.getZ2());
		int xMax = getNodeIndex(area.getX2());
		BuildoutAreaNode node = null;
		synchronized (grid) {
			for (int z = getNodeIndex(area.getZ1()); z <= zMax && z < grid.length; z++) {
				for (int x = getNodeIndex(area.getX1()); x <= xMax && x < grid.length; x++) {
					node = grid[z][x];
					if (node == null) {
						node = new BuildoutAreaNode();
						grid[z][x] = node;
					}
					node.add(area);
				}
			}
		}
	}
	
	public BuildoutArea getBuildoutArea(Terrain t, double x, double z) {
		BuildoutAreaNode node;
		synchronized (grid) {
			node = grid[getNodeIndex(z)][getNodeIndex(x)];
			if (node == null)
				return null;
		}
		return node.getBestMatch(t, x, z);
	}
	
	private int getNodeIndex(double x) {
		return (int) ((x-MIN)/(MAX-MIN)*grid.length);
	}
	
	private static class BuildoutAreaNode {
		
		private List<BuildoutArea> areas;
		
		public BuildoutAreaNode() {
			areas = new ArrayList<>();
		}
		
		public void add(BuildoutArea area) {
			synchronized (areas) {
				areas.add(area);
				areas.sort((a, b) -> Double.compare(getArea(a), getArea(b)));
			}
		}
		
		public BuildoutArea getBestMatch(Terrain t, double x, double z) {
			synchronized (areas) {
				for (BuildoutArea area : areas) {
					if (!isWithin(area, t, x, z))
						continue;
					return area; // It's known to be the best area due to how it is sorted upon add
				}
			}
			return null;
		}
		
		private boolean isWithin(BuildoutArea area, Terrain t, double x, double z) {
			return area.getTerrain() == t && x >= area.getX1() && x <= area.getX2() && z >= area.getZ1() && z <= area.getZ2();
		}
		
		private double getArea(BuildoutArea area) {
			return (area.getX2() - area.getX1()) * (area.getZ2() - area.getZ1());
		}
		
	}
	
}
