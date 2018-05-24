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
package com.projectswg.holocore.resources.server_info.loader.buildouts;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.buildout.BuildoutArea;
import com.projectswg.holocore.resources.buildout.BuildoutArea.BuildoutAreaBuilder;
import com.projectswg.holocore.resources.server_info.SdbLoader;
import com.projectswg.holocore.resources.server_info.SdbLoader.SdbResultSet;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AreaLoader {
	
	private final Map<String, BuildoutArea> areasByName;
	private final Map<Integer, BuildoutArea> areasById;
	private final List<BuildoutArea> areaList;
	private final Collection<String> events;
	
	private AreaLoader(Collection<String> events) {
		this.areasByName = new HashMap<>();
		this.areasById = new HashMap<>();
		this.areaList = new ArrayList<>();
		this.events = events;
	}
	
	public BuildoutArea getArea(String areaName) {
		return areasByName.get(areaName);
	}
	
	public BuildoutArea getArea(int areaId) {
		return areasById.get(areaId);
	}
	
	public List<BuildoutArea> getAreaList() {
		return areaList;
	}
	
	public Map<String, BuildoutArea> getAreasByName() {
		return areasByName;
	}
	
	public Map<Integer, BuildoutArea> getAreasById() {
		return areasById;
	}
	
	private void loadFromFile() {
		BuildoutArea area;
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/areas.sdb"))) {
			while (set.next()) {
				area = new BuildoutAreaBuilder()
						.setId((int) set.getInt(0))
						.setTerrain(Terrain.getTerrainFromName(set.getText(1)))
						.setName(set.getText(2))
						.setEvent(set.getText(3))
						.setX1(set.getReal(4))
						.setZ1(set.getReal(5))
						.setX2(set.getReal(6))
						.setZ2(set.getReal(7))
						.setAdjustCoordinates(set.getInt(8) != 0)
						.setTranslationX(set.getReal(9))
						.setTranslationX(set.getReal(10))
						.build();
				BuildoutArea replaced = areasByName.get(area.getName());
				if ((replaced == null && area.getEvent().isEmpty()) || (!area.getEvent().isEmpty() && events.contains(area.getEvent()))) {
					areasByName.put(area.getName(), area);
					areasById.put(area.getId(), area);
					areaList.add(area);
				}
			}
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public static AreaLoader load() {
		return load(Collections.emptySet());
	}
	
	public static AreaLoader load(String ... events) {
		return load(Arrays.asList(events));
	}
	
	public static AreaLoader load(Collection<String> events) {
		AreaLoader loader = new AreaLoader(events);
		loader.loadFromFile();
		return loader;
	}
	
}
