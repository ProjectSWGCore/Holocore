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
package com.projectswg.holocore.services.crafting.resource.raw;

import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.holocore.services.crafting.resource.galactic.RawResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RawResource {
	
	private final long id;
	private final StringId name;
	private final List<RawResource> children;
	
	private RawResource parent;
	private RawResourceType type;
	private String crateTemplate;
	private int minTypes;
	private int maxTypes;
	private int minPools;
	private int maxPools;
	private boolean recycled;
	
	public RawResource(long id) {
		this.id = id;
		this.parent = null;
		this.type = RawResourceType.RESOURCE;
		this.children = new ArrayList<>();
		this.crateTemplate = "";
		this.name = new StringId("resource/resource_names", "");
		this.minTypes = 0;
		this.maxTypes = 0;
		this.minPools = 0;
		this.maxPools = 0;
	}
	
	public long getId() {
		return id;
	}
	
	public StringId getName() {
		return name;
	}
	
	public RawResource getParent() {
		return parent;
	}
	
	public RawResourceType getResourceType() {
		return type;
	}
	
	public List<RawResource> getChildren() {
		return Collections.unmodifiableList(children);
	}
	
	public String getCrateTemplate() {
		return crateTemplate;
	}
	
	public int getMinTypes() {
		return minTypes;
	}
	
	public int getMaxTypes() {
		return maxTypes;
	}
	
	public int getMinPools() {
		return minPools;
	}
	
	public int getMaxPools() {
		return maxPools;
	}
	
	public boolean isRecycled() {
		return recycled;
	}
	
	public static class RawResourceBuilder {
		
		private final RawResource resource;
		
		public RawResourceBuilder(long id) {
			resource = new RawResource(id);
		}
		
		public RawResourceBuilder setName(String name) {
			resource.name.setKey(name);
			return this;
		}
		
		public RawResourceBuilder setParent(RawResource parent) {
			if (resource.parent != null)
				resource.parent.children.remove(resource);
			resource.parent = parent;
			if (parent != null)
				parent.children.add(resource);
			return this;
		}
		
		public RawResourceBuilder setCrateTemplate(String crateTemplate) {
			resource.crateTemplate = crateTemplate;
			return this;
		}
		
		public RawResourceBuilder setMinTypes(int minTypes) {
			resource.minTypes = minTypes;
			return this;
		}
		
		public RawResourceBuilder setMaxTypes(int maxTypes) {
			resource.maxTypes = maxTypes;
			return this;
		}
		
		public RawResourceBuilder setMinPools(int minPools) {
			resource.minPools = minPools;
			return this;
		}
		
		public RawResourceBuilder setMaxPools(int maxPools) {
			resource.maxPools = maxPools;
			return this;
		}
		
		public RawResourceBuilder setRecycled(boolean recycled) {
			resource.recycled = recycled;
			return this;
		}
		
		public RawResource build() {
			resource.type = RawResourceType.getRawResourceType(resource);
			return resource;
		}
		
	}
	
}
