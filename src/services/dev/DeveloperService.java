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

import com.projectswg.common.control.Service;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

import intents.object.ObjectCreatedIntent;
import resources.PvpFlag;
import resources.config.ConfigFile;
import resources.objects.SWGObject;
import resources.objects.custom.DefaultAIObject;
import resources.objects.tangible.TangibleObject;
import resources.server_info.DataManager;
import services.objects.ObjectCreator;

public class DeveloperService extends Service {
	
	public DeveloperService() {
		
	}
	
	@Override
	public boolean start() {
		setupDeveloperArea();
		
		if (DataManager.getConfig(ConfigFile.FEATURES).getBoolean("CHARACTER-BUILDER", false))
			setupCharacterBuilders();
		
		return super.start();
	}
	
	private void setupDeveloperArea() {
		DefaultAIObject dummy = spawnObject("object/mobile/shared_target_dummy_blacksun.iff", new Location(3500, 5, -4800, Terrain.DEV_AREA), DefaultAIObject.class);
		dummy.setPvpFlags(PvpFlag.ATTACKABLE);
	}
	
	private void setupCharacterBuilders() {
		Location[] cbtLocations = {
			new Location(-3989, 124, -10, Terrain.DATHOMIR),
			new Location(-3989, 124, -10, Terrain.DATHOMIR),
			new Location(-5786, 510, -6554, Terrain.DATHOMIR),
			new Location(-4683, 13, 4326, Terrain.ENDOR),
			new Location(3331, 105, -4912, Terrain.LOK),
			new Location(-6439, 41, -3265, Terrain.NABOO),
			new Location(-3941, 60, 6318, Terrain.TATOOINE),
			new Location(7380, 123, 4298, Terrain.TATOOINE),
			new Location(3523, 4, -4802, Terrain.TATOOINE),
			new Location(58, 153, -78, Terrain.TATOOINE)
		};
		
		for (Location cbtLocation : cbtLocations) {
			spawnObject("object/tangible/terminal/shared_terminal_character_builder.iff", cbtLocation, TangibleObject.class);
		}
	}
	
	private <T extends SWGObject> T spawnObject(String template, Location l, Class<T> c) {
		T obj = ObjectCreator.createObjectFromTemplate(template, c);
		obj.setLocation(l);
		new ObjectCreatedIntent(obj).broadcast();
		return obj;
	}
	
}
