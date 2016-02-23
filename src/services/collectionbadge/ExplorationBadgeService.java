
/***********************************************************************************
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
package services.collectionbadge;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import intents.GrantBadgeIntent;
import intents.player.PlayerTransformedIntent;

import java.util.TreeMap;

import resources.Point3D;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class ExplorationBadgeService extends Service {

	private static final String GET_BADGES_SQL = "SELECT * FROM explorationBadges";
	private Map<String, Map<String, ExplorationRegion>> explorationLocations = new TreeMap<String, Map<String, ExplorationRegion>>();
	
	public ExplorationBadgeService(){
		registerExplorationBadge();
		registerForIntent(PlayerTransformedIntent.TYPE);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof PlayerTransformedIntent) {
			String badgeName = checkExplorationRegions(((PlayerTransformedIntent) i).getPlayer());
			if (badgeName != null){
				new GrantBadgeIntent(((PlayerTransformedIntent) i).getPlayer(), badgeName).broadcast();
			}
		}
	}
	
	private String checkExplorationRegions(CreatureObject creature) {
		String planet = "";
		
		if (creature.getTerrain().getName() != null){
			planet = creature.getTerrain().getName();
		}
		
		if (explorationLocations.containsKey(planet)) {
			for (Entry<String, ExplorationRegion> badge : explorationLocations.get(planet).entrySet()) {
				if (creature.getWorldLocation().isWithinFlatDistance(badge.getValue().location, badge.getValue().range)){
					return badge.getKey();
				}
			}
		}
		return null;
	}	
	
	private void registerExplorationBadge() {
		
		try (RelationalServerData explorerBadgeDatabase = RelationalServerFactory.getServerData("badges/explorationBadges.db", "explorationBadges")) {
			try(ResultSet set =  explorerBadgeDatabase.executeQuery(GET_BADGES_SQL)){
				while (set.next()) {
					String planet = set.getString(set.findColumn("planet")).toLowerCase();
					String badgeName = set.getString(set.findColumn("badge"));
					int x = set.getInt(set.findColumn("x"));
					int y = set.getInt(set.findColumn("y"));
					int range = 5; //arbitrary number used
					
					if (!explorationLocations.containsKey(planet)) {
						explorationLocations.put(planet, new TreeMap<String, ExplorationRegion>());
					}
					explorationLocations.get(planet).put(badgeName, new ExplorationRegion(new Point3D(x, 0, y), range));
				}
			}catch (SQLException e) {
				e.printStackTrace();
			}			
		}
	}	
	
	private class ExplorationRegion {
		
		public Point3D location;
		public float range;
		
		public ExplorationRegion(Point3D location, float range) {
			this.location = location;
			this.range = range;
		}
	}		
}
