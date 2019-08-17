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
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class GalacticResource implements Persistable, MongoPersistable {
	
	private final GalacticResourceStats stats;
	private final List<GalacticResourceSpawn> spawns;
	private final Map<Terrain, List<GalacticResourceSpawn>> terrainSpawns;
	
	private long id;
	private String name;
	private long rawId;
	private RawResource rawResource;
	
	public GalacticResource() {
		this(0, "", 0);
	}
	
	public GalacticResource(long id, String name, long rawResourceId) {
		this.stats = new GalacticResourceStats();
		this.spawns = new CopyOnWriteArrayList<>();
		this.terrainSpawns = new ConcurrentHashMap<>();
		
		this.id = id;
		this.name = name;
		this.rawId = rawResourceId;
		this.rawResource = null;
	}
	
	public void generateRandomStats() {
		stats.generateRandomStats();
	}
	
	public long getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public long getRawResourceId() {
		return rawId;
	}
	
	public RawResource getRawResource() {
		return rawResource;
	}
	
	public GalacticResourceStats getStats() {
		return stats;
	}
	
	public List<GalacticResourceSpawn> getSpawns() {
		return Collections.unmodifiableList(spawns);
	}
	
	public List<GalacticResourceSpawn> getSpawns(Terrain terrain) {
		List<GalacticResourceSpawn> spawns = terrainSpawns.get(terrain);
		return spawns == null ? List.of() : Collections.unmodifiableList(spawns);
	}
	
	public void setRawResource(RawResource rawResource) {
		this.rawResource = rawResource;
	}
	
	public void addSpawn(@NotNull GalacticResourceSpawn spawn) {
		spawns.add(spawn);
		terrainSpawns.computeIfAbsent(spawn.getTerrain(), s -> new CopyOnWriteArrayList<>()).add(spawn);
	}
	
	public void removeSpawn(@NotNull GalacticResourceSpawn spawn) {
		spawns.remove(spawn);
		terrainSpawns.compute(spawn.getTerrain(), (t, spawns) -> {
			if (spawns == null)
				return null;
			spawns.remove(spawn);
			return spawns.isEmpty() ? null : spawns;
		});
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		id = stream.getLong();
		name = stream.getAscii();
		rawId = stream.getLong();
		stats.read(stream);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		stream.addLong(id);
		stream.addAscii(name);
		stream.addLong(rawId);
		stats.save(stream);
	}
	
	@Override
	public void readMongo(MongoData data) {
		spawns.clear();
		terrainSpawns.clear();
		
		id = data.getLong("id", id);
		name = data.getString("name", name);
		rawId = data.getLong("rawId", rawId);
		data.getDocument("stats", stats);
		spawns.addAll(data.getArray("spawns", (Supplier<GalacticResourceSpawn>) GalacticResourceSpawn::new));
		terrainSpawns.putAll(spawns.stream().collect(groupingBy(GalacticResourceSpawn::getTerrain)));
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putLong("id", id);
		data.putString("name", name);
		data.putLong("rawId", rawId);
		data.putDocument("stats", stats);
		data.putArray("spawns", spawns);
	}
	
	@Override
	public String toString() {
		return "GalacticResource[ID=" + id + "  NAME='" + name + "']";
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GalacticResource))
			return false;
		return ((GalacticResource) o).id == id && ((GalacticResource) o).name.equals(name);
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}
	
	public static GalacticResource create(NetBufferStream stream) {
		GalacticResource resource = new GalacticResource();
		resource.read(stream);
		return resource;
	}
	
	public void save(NetBufferStream stream, GalacticResource resource) {
		resource.save(stream);
	}
	
}
