/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.crafting.resource.galactic;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GalacticResourceSpawn implements Persistable, MongoPersistable {
	
	private static final int	MAP_SIZE						= 16384;
	private static final int	MUST_MAP_SIZE					= 8000;
	private static final int	KASH_MAP_SIZE					= 8192;
	private static final double	POSITION_GAUSSIAN_FACTOR		= MAP_SIZE / 2d * Math.sqrt(2);
	private static final double	POSITION_MUST_GAUSSIAN_FACTOR	= MUST_MAP_SIZE / 2d * Math.sqrt(2);
	private static final double	POSITION_KASH_GAUSSIAN_FACTOR	= KASH_MAP_SIZE / 2d * Math.sqrt(2);
	
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
	private Instant startTime;
	private Instant endTime;
	
	public GalacticResourceSpawn() {
		this.resourceId = 0;
		this.minConcentration = 0;
		this.maxConcentration = 0;
		this.terrain = null;
		this.x = 0;
		this.z = 0;
		this.radius = 0;
		this.startTime = Instant.EPOCH;
		this.endTime = Instant.EPOCH;
	}
	
	public GalacticResourceSpawn(long resourceId) {
		this();
		this.resourceId = resourceId;
	}
	
	public void setRandomValues(Terrain terrain) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		
		this.minConcentration = random.nextInt(50);
		this.maxConcentration = calculateRandomMaxConcentration(random, minConcentration);
		
		this.terrain = terrain;
		setPosition(random, terrain);
		this.radius = calculateRandomRadius(random);
		
		int minSpawnTime = getMinSpawnTime();
		int maxSpawnTime = getMaxSpawnTime();
		this.startTime = Instant.now();
		this.endTime = startTime.plus(random.nextInt(minSpawnTime, maxSpawnTime), ChronoUnit.DAYS);
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
	
	public Instant getStartTime() {
		return startTime;
	}
	
	public Instant getEndTime() {
		return endTime;
	}
	
	public int getConcentration(Terrain terrain, double x, double z) {
		double distance = getDistance(terrain, x, z);
		if (distance > radius)
			return 0;
		double factor = (1 - distance / radius);
		factor = factor * factor; // creates a more serious dropoff of concentration
		return (int) (factor * (maxConcentration - minConcentration) + minConcentration);
	}
	
	public boolean isExpired() {
		return Instant.now().isAfter(endTime);
	}
	
	private int getMinSpawnTime() {
		return PswgDatabase.config().getInt(this, "resourceMinSpawnTime", 7);
	}
	
	private int getMaxSpawnTime() {
		return PswgDatabase.config().getInt(this, "resourceMaxSpawnTime", 21);
	}
	
	private int getMinRadius() {
		return PswgDatabase.config().getInt(this, "resourceMinSpawnRadius", 200);
	}
	
	private int getMaxRadius() {
		return PswgDatabase.config().getInt(this, "resourceMaxSpawnRadius", 500);
	}
	
	private int calculateRandomMaxConcentration(Random random, int min) {
		double x;
		do {
			x = random.nextDouble();
			x = x * x * 100;
		} while (x <= min);
		return (int) x;
	}
	
	private void setPosition(Random random, Terrain terrain) {
		double angle = random.nextDouble() * 6.283185307;
		double distance = Math.max(-1, Math.min(1, random.nextGaussian() / 6 + 0.5));
		switch (terrain) {
			case MUSTAFAR:
				distance *= POSITION_MUST_GAUSSIAN_FACTOR;
				break;
			case KASHYYYK_MAIN:
				distance *= POSITION_KASH_GAUSSIAN_FACTOR;
				break;
			default:
				distance *= POSITION_GAUSSIAN_FACTOR;
				break;
		}
		this.x = capPosition((int) (Math.cos(angle) * distance));
		this.z = capPosition((int) (Math.sin(angle) * distance));
		if (terrain == Terrain.MUSTAFAR) {
			x += -2880;
			z += 2976;
		}
	}
	
	private int calculateRandomRadius(Random random) {
		double x = random.nextDouble();
		x = Math.sqrt(x);
		return (int) (x * (getMaxRadius() - getMinRadius()) + getMinRadius());
	}
	
	private int capPosition(int x) {
		return Math.max(-MAP_SIZE/2, Math.min(MAP_SIZE/2, x));
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
		this.startTime = Instant.ofEpochMilli(stream.getLong());
		this.endTime = Instant.ofEpochMilli(stream.getLong());
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
		stream.addLong(startTime.toEpochMilli());
		stream.addLong(endTime.toEpochMilli());
	}
	
	@Override
	public void readMongo(MongoData data) {
		{
			MongoData resource = data.getDocument("resource");
			resourceId = resource.getLong("id", resourceId);
			minConcentration = resource.getInteger("minConcentration", minConcentration);
			maxConcentration = resource.getInteger("maxConcentration", maxConcentration);
		}
		{
			MongoData location = data.getDocument("location");
			terrain = Terrain.valueOf(location.getString("terrain", Terrain.TATOOINE.name()));
			x = location.getInteger("x", x);
			z = location.getInteger("z", z);
			radius = location.getInteger("radius", radius);
		}
		{
			MongoData time = data.getDocument("time");
			startTime = time.getDate("start", Instant.EPOCH);
			endTime = time.getDate("end", Instant.EPOCH);
		}
	}
	
	@Override
	public void saveMongo(MongoData data) {
		{
			MongoData resource = data.getDocument("resource");
			resource.putLong("id", resourceId);
			resource.putInteger("minConcentration", minConcentration);
			resource.putInteger("maxConcentration", maxConcentration);
		}
		{
			MongoData location = data.getDocument("location");
			location.putString("terrain", terrain.name());
			location.putInteger("x", x);
			location.putInteger("z", z);
			location.putInteger("radius", radius);
		}
		{
			MongoData time = data.getDocument("time");
			time.putDate("start", startTime);
			time.putDate("end", endTime);
		}
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
		return "GalacticResourceSpawn[Resource ID=" + resourceId + "  Concentration=(" + minConcentration + ", " + maxConcentration + ")  Position=(" + x + ", " + z + ", " + terrain + ']';
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
