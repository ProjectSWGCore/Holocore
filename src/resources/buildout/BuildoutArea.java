package resources.buildout;

import resources.Location;
import resources.Terrain;

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
	
	public boolean isLoaded() {
		return loaded;
	}
	
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}
	
	public int hashCode() {
		return terrain.hashCode() ^ Double.hashCode(x1) ^ Double.hashCode(z1);
	}
	
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
	
	public void adjustLocation(Location l) {
		if (!isAdjustCoordinates())
			return;
		l.translatePosition(x1, 0, z1);
	}
	
	public void readjustLocation(Location l) {
		if (!isAdjustCoordinates())
			return;
		l.translatePosition(-x1, 0, -z1);
	}
	
	public String toString() {
		return String.format("%s  %s: %.1f, %.1f/%.1f, %.1f", name, terrain.getName(), x1, z1, x2, z2);
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
		
		public BuildoutAreaBuilder setAdjustCoordinates(boolean adjustCoordinates) {
			area.adjustCoordinates = adjustCoordinates;
			return this;
		}
		
		public BuildoutArea build() {
			return area;
		}
		
	}
	
}
