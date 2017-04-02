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
package services.map;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import intents.PlayerEventIntent;
import intents.network.GalacticPacketIntent;
import intents.object.ObjectCreatedIntent;
import network.packets.Packet;
import network.packets.swg.zone.object_controller.DataTransform;
import resources.Terrain;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;
import resources.server_info.RelationalServerFactory;

public class CityService extends Service {
	
	private static final String GET_ALL_CITIES = "SELECT * FROM cities";
	
	private final Map<Terrain, List<City>> cities;
	
	public CityService() {
		cities = new HashMap<>();
		loadCities();
		
		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
		registerForIntent(ObjectCreatedIntent.class, oci -> handleObjectCreatedIntent(oci));
	}
	
	private void loadCities() {
		cities.clear();
		try (RelationalDatabase db = RelationalServerFactory.getServerData("map/cities.db", "cities")) {
			try (ResultSet set = db.executeQuery(GET_ALL_CITIES)) {
				Terrain t = Terrain.getTerrainFromName(set.getString("terrain"));
				List<City> list = cities.get(t);
				if (list == null)
					cities.put(t, list = new ArrayList<>());
				list.add(new City(set.getString("city"), set.getInt("x"), set.getInt("z"), set.getInt("radius")));
			}
		} catch (SQLException e) {
			Log.e(e);
		}
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent i) {
		GalacticPacketIntent gpi = (GalacticPacketIntent) i;
		Packet p = gpi.getPacket();
		if (p instanceof DataTransform) {
			performLocationUpdate(gpi.getPlayer().getCreatureObject());
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent i) {
		Player player = ((PlayerEventIntent) i).getPlayer();
		CreatureObject creature = player.getCreatureObject();
		if (((PlayerEventIntent) i).getEvent() == PlayerEvent.PE_ZONE_IN_CLIENT) {
			performLocationUpdate(creature);
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject object = i.getObject();
		
		if(!(object instanceof TangibleObject) || object.getTerrain() == null) {
			return;
		}
		
		performLocationUpdate((TangibleObject) object);
	}
	
	private void performLocationUpdate(TangibleObject object) {
		List<City> list = cities.get(object.getTerrain());
		if (list == null)
			return; // No cities on that planet
		for (City city : list) {
			if (city.isWithinRange(object)) {
				object.setCurrentCity(city.getName());
				return;
			}
		}
		object.setCurrentCity("");
	}
	
	private static class City {
		
		private String name;
		private int x;
		private int z;
		private int radius;
		
		public City(String name, int x, int z, int radius) {
			this.name = name;
			this.x = x;
			this.z = z;
			this.radius = radius;
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isWithinRange(SWGObject obj) {
			return Math.sqrt(square((int) obj.getX()-x) + square((int) obj.getZ()-z)) <= radius;
		}
		
		private int square(int x) {
			return x * x;
		}
		
	}
	
}
