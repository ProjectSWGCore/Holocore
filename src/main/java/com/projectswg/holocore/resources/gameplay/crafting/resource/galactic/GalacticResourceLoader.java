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

import com.projectswg.holocore.resources.support.data.server_info.CachedObjectDatabase;
import me.joshlarson.jlcommon.log.Log;

import java.util.ArrayList;
import java.util.List;

public class GalacticResourceLoader {
	
	public GalacticResourceLoader() {
		
	}
	
	public List<GalacticResource> loadResources() {
		CachedObjectDatabase<GalacticResource> resources = new CachedObjectDatabase<>("odb/resources.odb", GalacticResource::create, GalacticResource::save);
		if (!resources.load()) {
			Log.w("Unable to load resource ODB!");
			return new ArrayList<>();
		}
		List<GalacticResource> resourceList = new ArrayList<>(resources.size());
		resources.traverse(resourceList::add);
		resources.close();
		return resourceList;
	}
	
	public List<GalacticResourceSpawn> loadSpawns() {
		CachedObjectDatabase<GalacticResourceSpawn> spawns = new CachedObjectDatabase<>("odb/resource_spawns.odb", GalacticResourceSpawn::create, GalacticResourceSpawn::save);
		if (!spawns.load()) {
			Log.w("Unable to load resource spawn ODB!");
			return new ArrayList<>();
		}
		List<GalacticResourceSpawn> spawnList = new ArrayList<>(spawns.size());
		spawns.traverse(spawnList::add);
		spawns.close();
		return spawnList;
	}
	
	public void saveResources(List<GalacticResource> resourceList) {
		CachedObjectDatabase<GalacticResource> resources = new CachedObjectDatabase<>("odb/resources.odb", GalacticResource::create, GalacticResource::save);
		if (!resources.load()) {
			Log.w("Unable to load resource ODB to save");
			return;
		}
		resources.clearObjects();
		for (GalacticResource r : resourceList) {
			resources.add(r);
		}
		resources.save();
		resources.close();
	}
	
	public void saveSpawns(List<GalacticResourceSpawn> spawnList) {
		CachedObjectDatabase<GalacticResourceSpawn> spawns = new CachedObjectDatabase<>("odb/resource_spawns.odb", GalacticResourceSpawn::create, GalacticResourceSpawn::save);
		if (!spawns.load()) {
			Log.w("Unable to load resource spawn ODB to save");
			return;
		}
		spawns.clearObjects();
		for (GalacticResourceSpawn s : spawnList) {
			spawns.add(s);
		}
		spawns.save();
		spawns.close();
	}
	
}
