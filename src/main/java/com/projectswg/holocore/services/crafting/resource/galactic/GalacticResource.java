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
package com.projectswg.holocore.services.crafting.resource.galactic;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.services.crafting.resource.raw.RawResource;

public class GalacticResource implements Persistable {
	
	private final GalacticResourceStats stats;
	
	private long id;
	private String name;
	private long rawId;
	private RawResource rawResource;
	
	public GalacticResource() {
		this(0, "", 0);
	}
	
	public GalacticResource(long id, String name, long rawResourceId) {
		this.id = id;
		this.name = name;
		this.rawId = rawResourceId;
		this.rawResource = null;
		this.stats = new GalacticResourceStats();
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
	
	public void setRawResource(RawResource rawResource) {
		this.rawResource = rawResource;
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
	public void read(NetBufferStream stream) {
		stream.getByte();
		id = stream.getLong();
		name = stream.getAscii();
		rawId = stream.getLong();
		stats.read(stream);
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
