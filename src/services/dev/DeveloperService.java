/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.dev;

import intents.object.ObjectCreatedIntent;
import resources.Location;
import resources.PvpFlag;
import resources.Terrain;
import resources.config.ConfigFile;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.custom.DefaultAIObject;
import resources.objects.tangible.TangibleObject;
import services.objects.ObjectCreator;

public class DeveloperService extends Service {
	
	public DeveloperService() {
		
	}
	
	@Override
	public boolean start() {
		setupDeveloperArea();
		
		if (getConfig(ConfigFile.FEATURES).getBoolean("CHARACTER-BUILDER", false))
			setupCharacterBuilders();
		
		return super.start();
	}
	
	private void setupDeveloperArea() {
		DefaultAIObject dummy = spawnObject("object/mobile/shared_target_dummy_blacksun.iff", new Location(3500, 5, -4800, Terrain.DEV_AREA), DefaultAIObject.class);
		dummy.setPvpFlags(PvpFlag.ATTACKABLE);
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
			
			// Planet: Kashyyyk
			new Location(275, 48.1, 503, Terrain.KASHYYYK_HUNTING),
			new Location(146, 19.1, 162, Terrain.KASHYYYK_MAIN),
			new Location(-164, 16.5, -262, Terrain.KASHYYYK_DEAD_FOREST),
			
			// Planet: Lok
			new Location(3331, 106, -4912, Terrain.LOK),
			new Location(3848, 62, -464, Terrain.LOK),
			new Location(-1914, 12, -3299, Terrain.LOK),
			new Location(-70, 41.1, 2768, Terrain.LOK),
			
			// Planet: Naboo
			new Location(2535, 295.9, -3887, Terrain.NABOO),
			new Location(-6439, 41, -3265, Terrain.NABOO),
			
			// Planet: Rori
			new Location(-1211, 97.8, 4552, Terrain.RORI),
			
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
		
		for (int i = 0; i < cbtLocations.length; i++) {
			spawnObject("object/tangible/terminal/shared_terminal_character_builder.iff", cbtLocations[i] , TangibleObject.class);
		}
	}
	
	private <T extends SWGObject> T spawnObject(String template, Location l, Class<T> c) {
		T obj = ObjectCreator.createObjectFromTemplate(template, c);
		obj.setLocation(l);
		new ObjectCreatedIntent(obj).broadcast();
		return obj;
	}
	
}
