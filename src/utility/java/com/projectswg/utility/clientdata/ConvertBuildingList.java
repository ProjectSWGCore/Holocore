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
package com.projectswg.utility.clientdata;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.utilities.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

public class ConvertBuildingList implements Converter {
	
	@Override
	public void convert() {
		System.out.println("Converting building list...");
		Collection<SWGObject> objects = DataLoader.Companion.buildouts().getObjects().values();
		try (SdbGenerator gen = new SdbGenerator(new File("serverdata/building/buildings.sdb"))) {
			gen.writeColumnNames("building_id", "terrain_name", "object_id");
			for (SWGObject obj : objects) {
				if (obj instanceof BuildingObject) {
					
					String template = obj.getTemplate().substring(obj.getTemplate().lastIndexOf('/') + 1);
					Terrain terrain = obj.getTerrain();
					String name = terrain.name().charAt(0) + terrain.name().toLowerCase(Locale.US).substring(1);
					int cells = ((BuildingObject) obj).getCells().size();
					if (cells <= 0)
						continue;
					gen.writeLine("", terrain, obj.getObjectId(), template, name, cells, obj.getX(), obj.getY(), obj.getZ());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void convertFile(SdbGenerator sdb, File file) {
		
	}
	
}
