/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package services.map;

import com.projectswg.common.encoding.CachedEncode;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;

public class MapLocation implements Encodable {
	
	private long id;
	private String name;
	private float x;
	private float y;
	private byte category;
	private byte subcategory;
	private boolean isActive;
	
	private final CachedEncode cache;
	
	public MapLocation() {
		this.cache = new CachedEncode(() -> encodeImpl());
	}
	
	public MapLocation(long id, String name, float x, float y, byte category, byte subcategory, boolean isActive) {
		this();
		this.id = id;
		this.name = name;
		this.x = x;
		this.y = y;
		this.category = category;
		this.subcategory = subcategory;
		this.isActive = isActive;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		cache.clearCached();
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		cache.clearCached();
		this.name = name;
	}
	
	public float getX() {
		return x;
	}
	
	public void setX(float x) {
		cache.clearCached();
		this.x = x;
	}
	
	public float getY() {
		return y;
	}
	
	public void setY(float y) {
		cache.clearCached();
		this.y = y;
	}
	
	public byte getCategory() {
		return category;
	}
	
	public void setCategory(byte category) {
		cache.clearCached();
		this.category = category;
	}
	
	public byte getSubcategory() {
		return subcategory;
	}
	
	public void setSubcategory(byte subcategory) {
		cache.clearCached();
		this.subcategory = subcategory;
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	public void setIsActive(boolean isActive) {
		cache.clearCached();
		this.isActive = isActive;
	}
	
	@Override
	public byte[] encode() {
		return cache.encode();
	}
	
	@Override
	public void decode(NetBuffer data) {
		id = data.getLong();
		name = data.getUnicode();
		x = data.getFloat();
		y = data.getFloat();
		category = data.getByte();
		subcategory = data.getByte();
		isActive = data.getBoolean();
	}
	
	private byte [] encodeImpl() {
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addLong(id);
		data.addUnicode(name);
		data.addFloat(x);
		data.addFloat(y);
		data.addByte(category);
		data.addByte(subcategory);
		data.addBoolean(isActive);
		return data.array();
	}
	
	@Override
	public int getLength() {
		return name.length() * 2 + 23;
	}
	
	@Override
	public String toString() {
		return name + " x: " + x + "y: " + y;
	}
}
