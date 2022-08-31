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

import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource.RawResourceBuilder;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
				StringBuilder crateTemplate = new StringBuilder(set.getText("crate_template"));
				crateTemplate.insert(crateTemplate.lastIndexOf("/")+1, "resource_container_");
				RawResource resource = new RawResourceBuilder(set.getInt("id"))
						.setParent(resources.get(set.getInt("parent")))
						.setName(set.getText("resource_name"))
						.setCrateTemplate(crateTemplate.toString())
						.setMinPools((int) set.getInt("min_pools"))
						.setMaxPools((int) set.getInt("max_pools"))
						.setMinTypes((int) set.getInt("min_types"))
						.setMaxTypes((int) set.getInt("max_types"))
						.setAttrColdResistance(set.getInt("attr_cold_resist") != 0)
						.setAttrConductivity(set.getInt("attr_conductivity") != 0)
						.setAttrDecayResistance(set.getInt("attr_decay_resist") != 0)
						.setAttrEntangleResistance(set.getInt("attr_entangle_resist") != 0)
						.setAttrFlavor(set.getInt("attr_flavor") != 0)
						.setAttrHeatResistance(set.getInt("attr_heat_resist") != 0)
						.setAttrMalleability(set.getInt("attr_malleability") != 0)
						.setAttrOverallQuality(set.getInt("attr_quality") != 0)
						.setAttrPotentialEnergy(set.getInt("attr_potential_energy") != 0)
						.setAttrShockResistance(set.getInt("attr_shock_resist") != 0)
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
