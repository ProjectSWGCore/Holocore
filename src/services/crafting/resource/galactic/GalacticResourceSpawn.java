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
package services.crafting.resource.galactic;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.common.utilities.TimeUtilities;

import resources.config.ConfigFile;
import resources.server_info.DataManager;

public class GalacticResourceSpawn implements Persistable {
	
	private static final int MAP_SIZE = 16384;
	
	// Resource-based
	private long resourceId;
	private int minConcentration;
	private int maxConcentration;
	// Location-based
	private Terrain terrain;
	private int x;
	private int z;
	private int radius;
	// Time-based
	private long startTime;
	private long endTime;
	
	public GalacticResourceSpawn() {
		
	}
	
	public GalacticResourceSpawn(long resourceId) {
		this.resourceId = resourceId;
	}
	
	public void setRandomValues(Terrain terrain) {
		Random random = new Random();
		
		this.minConcentration = random.nextInt(100);
		this.maxConcentration = random.nextInt(100 - minConcentration) + minConcentration;
		
		this.terrain = terrain;
		this.x = random.nextInt(MAP_SIZE) - MAP_SIZE/2;
		this.z = random.nextInt(MAP_SIZE) - MAP_SIZE/2;
		this.radius = (int) (random.nextDouble() * (getMaxRadius() - getMinRadius()) + getMinRadius());
		
		int minSpawnTime = getMinSpawnTime();
		int maxSpawnTime = getMaxSpawnTime();
		this.startTime = TimeUtilities.getTime();
		this.endTime = startTime + TimeUnit.DAYS.toMillis(random.nextInt(maxSpawnTime - minSpawnTime) + minSpawnTime);
	}
	
	public long getResourceId() {
		return resourceId;
	}
	
	public int getMinConcentration() {
		return minConcentration;
	}
	
	public int getMaxConcentration() {
		return maxConcentration;
	}
	
	public Terrain getTerrain() {
		return terrain;
	}
	
	public double getX() {
		return x;
	}
	
	public double getZ() {
		return z;
	}
	
	public int getRadius() {
		return radius;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public long getEndTime() {
		return endTime;
	}
	
	public int getConcentration(Terrain terrain, double x, double z) {
		double distance = getDistance(terrain, x, z);
		if (distance > radius)
			return 0;
		return (int) ((1 - distance / radius) * (maxConcentration - minConcentration) + minConcentration);
	}
	
	public boolean isExpired() {
		return TimeUtilities.getTime() > endTime;
	}
	
	private int getMinSpawnTime() {
		return DataManager.getConfig(ConfigFile.FEATURES).getInt("RESOURCES-MIN-SPAWN-TIME", 7);
	}
	
	private int getMaxSpawnTime() {
		return DataManager.getConfig(ConfigFile.FEATURES).getInt("RESOURCES-MAX-SPAWN-TIME", 21);
	}
	
	private int getMinRadius() {
		return DataManager.getConfig(ConfigFile.FEATURES).getInt("RESOURCES-MIN-SPAWN-RADIUS", 200);
	}
	
	private int getMaxRadius() {
		return DataManager.getConfig(ConfigFile.FEATURES).getInt("RESOURCES-MAX-SPAWN-RADIUS", 500);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		// Resource
		stream.addLong(resourceId);
		stream.addInt(minConcentration);
		stream.addInt(maxConcentration);
		// Location
		stream.addAscii(terrain.name());
		stream.addInt(x);
		stream.addInt(z);
		stream.addInt(radius);
		// Time
		stream.addLong(startTime);
		stream.addLong(endTime);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		// Resource
		this.resourceId = stream.getLong();
		this.minConcentration = stream.getInt();
		this.maxConcentration = stream.getInt();
		// Location
		this.terrain = Terrain.valueOf(stream.getAscii());
		this.x = stream.getInt();
		this.z = stream.getInt();
		this.radius = stream.getInt();
		// Time
		this.startTime = stream.getLong();
		this.endTime = stream.getLong();
	}

	private double getDistance(Terrain terrain, double x, double z) {
		if (this.terrain != terrain)
			return Double.MAX_VALUE;
		return Math.sqrt(square(this.x - x) + square(this.z - z));
	}
	
	private double square(double x) {
		return x * x;
	}
	
	@Override
	public String toString() {
		return "GalacticResourceSpawn[Resource ID=" + resourceId + "  Concentration=(" + minConcentration + ", " + maxConcentration + ")  Position=(" + x + ", " + z + ", " + terrain + "]";
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GalacticResourceSpawn))
			return false;
		GalacticResourceSpawn s = (GalacticResourceSpawn) o;
		if (s.resourceId != resourceId)
			return false;
		if (s.minConcentration != minConcentration || s.maxConcentration != maxConcentration)
			return false;
		if (s.terrain != terrain || s.x != x || s.z != z || s.radius != radius)
			return false;
		return s.startTime == startTime && s.endTime == endTime;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(resourceId) * 7 + x * 23 + z * 89;
	}
	
	public static GalacticResourceSpawn create(NetBufferStream stream) {
		GalacticResourceSpawn spawn = new GalacticResourceSpawn();
		spawn.read(stream);
		return spawn;
	}
	
	public static void save(NetBufferStream stream, GalacticResourceSpawn spawn) {
		spawn.save(stream);
	}
	
}
