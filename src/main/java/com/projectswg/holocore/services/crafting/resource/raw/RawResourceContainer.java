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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectswg.common.debug.Log;

import com.projectswg.holocore.resources.server_info.SdbLoader;
import com.projectswg.holocore.resources.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.server_info.StandardLog;
import com.projectswg.holocore.services.crafting.resource.raw.RawResource.RawResourceBuilder;

public class RawResourceContainer {
	
	private final Map<Long, RawResource> resources;
	
	public RawResourceContainer() {
		this.resources = new HashMap<>();
	}
	
	public List<RawResource> getResources() {
		return new ArrayList<>(resources.values());
	}
	
	public RawResource getResource(long id) {
		return resources.get(id);
	}
	
	public void loadResources() {
		resources.clear();
		long startTime = StandardLog.onStartLoad("raw resources");
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/resources/resources.sdb"))) {
			while (set.next()) {
				RawResource resource = new RawResourceBuilder(set.getInt("id"))
						.setParent(resources.get(set.getInt("parent")))
						.setName(set.getText("resource_name"))
						.setCrateTemplate(set.getText("crate_template"))
						.setMinPools((int) set.getInt("min_pools"))
						.setMaxPools((int) set.getInt("max_pools"))
						.setMinTypes((int) set.getInt("min_types"))
						.setMaxTypes((int) set.getInt("max_types"))
						.setRecycled(set.getInt("recycled") != 0)
						.build();
				resources.put(resource.getId(), resource);
			}
		} catch (IOException e) {
			Log.e(e);
		}
		StandardLog.onEndLoad(resources.size(), "raw resources", startTime);
	}
	
}
