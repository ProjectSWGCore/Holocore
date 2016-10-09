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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import intents.PlayerEventIntent;
import intents.network.GalacticPacketIntent;
import intents.object.ObjectCreatedIntent;
import network.packets.Packet;
import network.packets.swg.zone.object_controller.DataTransform;
import resources.Location;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class CityService extends Service {
	
	private static final String GET_ALL_CITIES_FROM_TERRAIN = "SELECT * FROM cities WHERE terrain = ?";

	private final RelationalServerData spawnDatabase;
	private final PreparedStatement getAllCitiesStatement;
	
	public CityService() {
		spawnDatabase = RelationalServerFactory.getServerData("map/cities.db", "cities");
		if (spawnDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for StaticService");
		getAllCitiesStatement = spawnDatabase.prepareStatement(GET_ALL_CITIES_FROM_TERRAIN);
		
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case GalacticPacketIntent.TYPE: handleGalacticPacketIntent((GalacticPacketIntent) i); break;
			case PlayerEventIntent.TYPE: handlePlayerEventIntent((PlayerEventIntent) i); break;
			case ObjectCreatedIntent.TYPE: handleObjectCreatedIntent((ObjectCreatedIntent) i); break;
		}
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent i) {
		GalacticPacketIntent gpi = (GalacticPacketIntent) i;
		Packet p = gpi.getPacket();
		if (p instanceof DataTransform) {
			Player player = gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId());
			if (player == null) {
				Log.e("CityService", "Player is null in GalacticPacketIntent:DataTransform!");
				return;
			}
			CreatureObject creature = player.getCreatureObject();
			if (creature == null) {
				Log.e("CityService", "Creature is null in GalacticPacketIntent:DataTransform!");
				return;
			}
			DataTransform transform = (DataTransform) p;
			Location loc = transform.getLocation();
			performLocationUpdate(creature, (int) (loc.getX() + 0.5), (int) (loc.getZ() + 0.5));
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent i) {
		Player player = ((PlayerEventIntent) i).getPlayer();
		CreatureObject creature = player.getCreatureObject();
		if (((PlayerEventIntent) i).getEvent() == PlayerEvent.PE_ZONE_IN_CLIENT) {
			performLocationUpdate(creature, (int) (creature.getX() + 0.5), (int) (creature.getZ() + 0.5));
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent i) {
		SWGObject object = i.getObject();
		
		if(object instanceof TangibleObject) {
			TangibleObject tangibleObject = (TangibleObject) object;
			
			if(object.getTerrain() != null) {
				performLocationUpdate(tangibleObject, (int) (tangibleObject.getX() + 0.5), (int) (tangibleObject.getZ() + 0.5));
			}
		}
	}
	
	private void performLocationUpdate(TangibleObject object, int locX, int locZ) {
		String terrain = object.getTerrain().getName().toLowerCase(Locale.US);
		synchronized (spawnDatabase) {
			ResultSet set = null;
			try {
				getAllCitiesStatement.setString(1, terrain);
				set = getAllCitiesStatement.executeQuery();
				while (set.next()) {
					int x = set.getInt("x");
					int z = set.getInt("z");
					int radius = set.getInt("radius");
					if (distance(locX, locZ, x, z) <= radius) {
						object.setCurrentCity(set.getString("city"));
						return;
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					if (set != null)
						set.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		object.setCurrentCity("");
	}
	
	private double distance(int x1, int z1, int x2, int z2) {
		return Math.sqrt(square(x1-x2) + square(z1-z2));
	}
	
	private int square(int x) {
		return x * x;
	}
	
}
