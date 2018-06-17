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
package com.projectswg.holocore.resources.gameplay.crafting.resource.raw;

import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType;
import me.joshlarson.jlcommon.utilities.Arguments;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class RawResource {
	
	private final long id;
	private final StringId name;
	private final RawResource parent;
	private final RawResourceType type;
	private final String crateTemplate;
	private final int minTypes;
	private final int maxTypes;
	private final int minPools;
	private final int maxPools;
	private final boolean recycled;
	private final List<RawResource> children;
	
	private RawResource(RawResourceBuilder builder) {
		this.id = builder.id;
		this.children = new CopyOnWriteArrayList<>();
		
		this.parent = builder.parent;
		this.crateTemplate = Objects.requireNonNull(builder.crateTemplate, "crateTemplate");
		this.name = Objects.requireNonNull(builder.name, "name");
		this.minTypes = builder.minTypes;
		this.maxTypes = builder.maxTypes;
		this.minPools = builder.minPools;
		this.maxPools = builder.maxPools;
		this.recycled = builder.recycled;
		
		Arguments.validate(minTypes != -1, "minTypes must be initialized");
		Arguments.validate(maxTypes != -1, "maxTypes must be initialized");
		Arguments.validate(minPools != -1, "minPools must be initialized");
		Arguments.validate(maxPools != -1, "maxPools must be initialized");
		this.type = RawResourceType.getRawResourceType(this);
		if (parent != null)
			parent.children.add(this);
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
	
	public static RawResourceBuilder builder(long id) {
		return new RawResourceBuilder(id);
	}
	
	public static class RawResourceBuilder {
		
		private final long id;
		
		private StringId name = null;
		private RawResource parent = null;
		private String crateTemplate = null;
		private int minTypes = -1;
		private int maxTypes = -1;
		private int minPools = -1;
		private int maxPools = -1;
		private boolean recycled = false;
		
		public RawResourceBuilder(long id) {
			this.id = id;
		}
		
		public RawResourceBuilder setName(String name) {
			this.name = new StringId("resource/resource_names", name);
			return this;
		}
		
		public RawResourceBuilder setParent(RawResource parent) {
			this.parent = parent;
			return this;
		}
		
		public RawResourceBuilder setCrateTemplate(String crateTemplate) {
			this.crateTemplate = crateTemplate;
			return this;
		}
		
		public RawResourceBuilder setMinTypes(int minTypes) {
			this.minTypes = minTypes;
			return this;
		}
		
		public RawResourceBuilder setMaxTypes(int maxTypes) {
			this.maxTypes = maxTypes;
			return this;
		}
		
		public RawResourceBuilder setMinPools(int minPools) {
			this.minPools = minPools;
			return this;
		}
		
		public RawResourceBuilder setMaxPools(int maxPools) {
			this.maxPools = maxPools;
			return this;
		}
		
		public RawResourceBuilder setRecycled(boolean recycled) {
			this.recycled = recycled;
			return this;
		}
		
		public RawResource build() {
			return new RawResource(this);
		}
		
	}
	
}
