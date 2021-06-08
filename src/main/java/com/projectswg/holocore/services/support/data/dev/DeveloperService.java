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
package com.projectswg.holocore.services.support.data.dev;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService;
import me.joshlarson.jlcommon.control.Service;

public class DeveloperService extends Service {
	
	public DeveloperService() {
		
	}
	
	@Override
	public boolean start() {
		if (PswgDatabase.INSTANCE.getConfig().getBoolean(this, "characterBuilder", false))
			setupCharacterBuilders();
		
		return super.start();
	}
	
	private void setupCharacterBuilders() {
		Location[] cbtLocations = {
			
			// Planet: Corellia
			new Location(4735, 26.5, -5676, Terrain.CORELLIA),
			new Location(5137, 16.9, 1518, Terrain.CORELLIA),
			new Location(213, 50.5, 4533, Terrain.CORELLIA),
			
			// Planet: Dantooine
			new Location(4078, 10.1, 5370, Terrain.DANTOOINE),						
			new Location(-6225, 48.8, 7381, Terrain.DANTOOINE),
			new Location(-564, 1, -3789, Terrain.DANTOOINE),
			
			// Planet: Dathomir
			new Location(-6079, 132, 971, Terrain.DATHOMIR),
			new Location(-3989, 124.7, -10, Terrain.DATHOMIR),
			new Location(-2457, 117.9, 1530, Terrain.DATHOMIR),
			new Location(-5786, 510, -6554, Terrain.DATHOMIR),
			
			// Planet: Endor
			new Location(-1714, 31.5, -8, Terrain.ENDOR),
			new Location(-4683, 13.3, 4326, Terrain.ENDOR),

			// Planet: Hoth
			new Location(-3717.9, 94.1, 5975.5, Terrain.ADVENTURE2),

			// Planet: Kashyyyk
			new Location(275, 48.1, 503, Terrain.KASHYYYK_HUNTING),
			new Location(146, 19.1, 162, Terrain.KASHYYYK_MAIN),
			new Location(-164, 16.5, -262, Terrain.KASHYYYK_DEAD_FOREST),
			new Location(534, 173.5, 82, Terrain.KASHYYYK_RRYATT_TRAIL),
			new Location(1422, 70.2, 722, Terrain.KASHYYYK_RRYATT_TRAIL),
			new Location(2526, 182.3, -278, Terrain.KASHYYYK_RRYATT_TRAIL),
			new Location(768, 140.9, -439, Terrain.KASHYYYK_RRYATT_TRAIL),
			new Location(2495, -24.1, -924, Terrain.KASHYYYK_RRYATT_TRAIL),
			new Location(561.8, 22.8, 1552.8, Terrain.KASHYYYK_NORTH_DUNGEONS),

			// Planet: Lok
			new Location(3331, 106, -4912, Terrain.LOK),
			new Location(3848, 62, -464, Terrain.LOK),
			new Location(-1914, 12, -3299, Terrain.LOK),
			new Location(-70, 41.1, 2768, Terrain.LOK),

			// Planet: Mustafar
			new Location(4908.3, 24.6, 6045.8, Terrain.MUSTAFAR),
			new Location(-2489, 230, 1621, Terrain.MUSTAFAR),
			new Location(2209.8, 74.8, 6410.2, Terrain.MUSTAFAR),
			new Location(2195.1, 74.8, 4990.4, Terrain.MUSTAFAR),
			new Location(2190.5, 74.8, 3564.8, Terrain.MUSTAFAR),

			// Planet: Naboo
			new Location(2535, 295.9, -3887, Terrain.NABOO),
			new Location(-6439, 41, -3265, Terrain.NABOO),
			
			// Planet: Rori
			new Location(-1211, 97.8, 4552, Terrain.RORI),
			new Location(5289, 80.0, 6142, Terrain.RORI),
			
			// Planet: Talus
			new Location(4958, 449.9, -5983, Terrain.TALUS),
			
			// Planet: Tatooine
			new Location(-3941, 60, 6318, Terrain.TATOOINE),
			new Location(7380, 122.8, 4298, Terrain.TATOOINE),
			new Location(3525, 4, -4807, Terrain.TATOOINE),
			new Location(3684, 7.8, 2357, Terrain.TATOOINE),
			new Location(57, 152.3, -79, Terrain.TATOOINE),
			new Location(-5458, 11, 2601, Terrain.TATOOINE),
			
			// Planet: Yavin 4
			new Location(-947, 86.4, -2131, Terrain.YAVIN4),
			new Location(4928, 103.4, 5587, Terrain.YAVIN4),
			new Location(5107, 81.7, 301, Terrain.YAVIN4),
			new Location(-5575, 88, 4902, Terrain.YAVIN4),
			new Location(-6485, 84, -446, Terrain.YAVIN4),

		};
		
		for (Location cbtLocation : cbtLocations) {
			spawnObject("object/tangible/terminal/shared_terminal_character_builder.iff", cbtLocation, TangibleObject.class);
		}

		// Space Stations:
		createCBT("du1_npe_station_1", 8, 50.2, 0.8, -36.5);
		createCBT("du1_nova_orion", 8, 79.1, 0.8, -57.5);

		// Dungeons:
		createCBT("du1_heroic_ek_1", 1, -11.8, 0.2, -119.2);
		createCBT("du1_heroic_isd_1", 36, -0.1, 173.8, 35.8);
		createCBT("du1_npe_dungeon_1", 1, 7.7, 9.5, 6.6);
		createCBT("kas_pob_myyydril_1", 1, -5.2, -1.3, -5.3);
		createCBT("kas_pob_avatar_1", 1, 103.2, 0.1, 21.7);

	}

	private void createCBT(String buildingName, int cellNumber, double x, double y, double z) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate("object/tangible/terminal/shared_terminal_character_builder.iff");
		BuildingObject building = ObjectStorageService.BuildingLookup.getBuildingByTag(buildingName);
		assert building != null : "building does not exist";
		CellObject cell = building.getCellByNumber(cellNumber);
		assert cell != null : "cell does not exist";
		obj.moveToContainer(cell, x, y, z);
		ObjectCreatedIntent.broadcast(obj);
	}
	
	private <T extends SWGObject> T spawnObject(String template, Location l, Class<T> c) {
		T obj = ObjectCreator.createObjectFromTemplate(template, c);
		obj.setLocation(l);
		ObjectCreatedIntent.broadcast(obj);
		return obj;
	}
	
}
