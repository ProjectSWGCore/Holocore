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

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

public class BuildoutArea implements Comparable<BuildoutArea> {
	
	private int id;
	private String name;
	private Terrain terrain;
	private String event;
	private double x1;
	private double z1;
	private double x2;
	private double z2;
	private boolean adjustCoordinates;
	private double translationX;
	private double translationZ;
	private boolean loaded;
	
	private BuildoutArea() {
		
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public Terrain getTerrain() {
		return terrain;
	}
	
	public String getEvent() {
		return event;
	}
	
	public double getX1() {
		return x1;
	}

	public double getZ1() {
		return z1;
	}

	public double getX2() {
		return x2;
	}

	public double getZ2() {
		return z2;
	}
	
	public boolean isAdjustCoordinates() {
		return adjustCoordinates;
	}
	
	public double getTranslationX() {
		return translationX;
	}
	
	public double getTranslationZ() {
		return translationZ;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}
	
	@Override
	public int hashCode() {
		return terrain.hashCode() ^ Double.hashCode(x1) ^ Double.hashCode(z1);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof BuildoutArea))
			return false;
		BuildoutArea area = (BuildoutArea) o;
		if (!terrain.equals(area.terrain))
			return false;
		if (Double.compare(x1, area.x1) != 0)
			return false;
		if (Double.compare(z1, area.z1) != 0)
			return false;
		return true;
	}
	
	@Override
	public int compareTo(BuildoutArea area) {
		int comp = terrain.getName().compareTo(area.terrain.getName());
		if (comp != 0)
			return comp;
		comp = Double.compare(x1, area.x1);
		if (comp != 0)
			return comp;
		comp = Double.compare(z1, area.z1);
		if (comp != 0)
			return comp;
		comp = Double.compare(x2, area.x2);
		if (comp != 0)
			return comp;
		comp = Double.compare(z2, area.z2);
		if (comp != 0)
			return comp;
		return 0;
	}
	
	public Location adjustLocation(Location l) {
		Location ret = new Location();
		adjustLocation(l, ret);
		return ret;
	}
	
	public void adjustLocation(Location l, Location adjusted) {
		adjusted.mergeWith(l);
		if (!isAdjustCoordinates())
			return;
		adjusted.translatePosition(-x1, 0, -z1);
	}
	
	public Location readjustLocation(Location l) {
		Location ret = new Location();
		readjustLocation(l, ret);
		return ret;
	}
	
	public void readjustLocation(Location l, Location adjusted) {
		adjusted.mergeWith(l);
		if (!isAdjustCoordinates())
			return;
		adjusted.translatePosition(x1, 0, z1);
	}
	
	@Override
	public String toString() {
		return String.format("%s/%s: (%.1f,%.1f)/(%.1f,%.1f) %b(%.1f,%.1f)", name, terrain.getName(), x1, z1, x2, z2, adjustCoordinates, translationX, translationZ);
	}
	
	public static class BuildoutAreaBuilder {
		
		private final BuildoutArea area = new BuildoutArea();
		
		public BuildoutAreaBuilder setId(int id) {
			area.id = id;
			return this;
		}
		
		public BuildoutAreaBuilder setName(String name){
			area.name = name;
			return this;
		}
		
		public BuildoutAreaBuilder setTerrain(Terrain terrain) {
			area.terrain = terrain;
			return this;
		}
		
		public BuildoutAreaBuilder setEvent(String event) {
			area.event = event;
			return this;
		}
		
		public BuildoutAreaBuilder setX1(double x1) {
			area.x1 = x1;
			return this;
		}
		
		public BuildoutAreaBuilder setZ1(double z1) {
			area.z1 = z1;
			return this;
		}
		
		public BuildoutAreaBuilder setX2(double x2) {
			area.x2 = x2;
			return this;
		}
		
		public BuildoutAreaBuilder setZ2(double z2) {
			area.z2 = z2;
			return this;
		}
		
		public BuildoutAreaBuilder setTranslationX(double x) {
			area.translationX = x;
			return this;
		}
		
		public BuildoutAreaBuilder setTranslationZ(double z) {
			area.translationZ = z;
			return this;
		}
		
		public BuildoutAreaBuilder setAdjustCoordinates(boolean adjustCoordinates) {
			area.adjustCoordinates = adjustCoordinates;
			return this;
		}
		
		public BuildoutArea build() {
			return area;
		}
		
	}
	
}
