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
package com.projectswg.holocore.services.gameplay.structures.housing;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CityService extends Service {
	
	private final Map<Terrain, List<City>> cities;
	
	public CityService() {
		cities = new EnumMap<>(Terrain.class);
	}
	
	@Override
	public boolean initialize() {
		loadCities();
		return true;
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket p = gpi.getPacket();
		if (p instanceof DataTransform) {
			performLocationUpdate(gpi.getPlayer().getCreatureObject());
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent i) {
		Player player = i.getPlayer();
		CreatureObject creature = player.getCreatureObject();
		if (i.getEvent() == PlayerEvent.PE_ZONE_IN_CLIENT) {
			performLocationUpdate(creature);
		}
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject object = i.getObject();
		
		if (!(object instanceof TangibleObject)) {
			return;
		}
		
		performLocationUpdate((TangibleObject) object);
	}
	
	private void loadCities() {
		cities.clear();
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/map/cities.sdb"))) {
			while (set.next()) {
				Terrain t = Terrain.getTerrainFromName(set.getText("terrain"));
				List<City> list = cities.computeIfAbsent(t, k -> new ArrayList<>());
				list.add(new City(set.getText("city"), (int) set.getInt("x"), (int) set.getInt("z"), (int) set.getInt("radius")));
			}
		} catch (IOException e) {
			Log.e(e);
		}
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
			return square((int) obj.getX() - x) + square((int) obj.getZ() - z) <= square(radius);
		}
		
		@Override
		public String toString() {
			return String.format("City[%s, (%d, %d), radius=%d]", name, x, z, radius);
		}
		
		private int square(int x) {
			return x * x;
		}
		
	}
	
}
