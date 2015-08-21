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
package services.spawn;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import resources.Location;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.server_info.RelationalServerData;
import services.objects.ObjectManager;

public class StaticService extends Service {
	
	private static final String GET_SUPPORTING_SQL = "SELECT spawns.* FROM spawns, types WHERE types.iff = ? AND spawns.iff_type = types.iff_type";
	
	private final Object databaseMutex;
	private final ObjectManager objectManager;
	private RelationalServerData spawnDatabase;
	private PreparedStatement getSupportingStatement;
	
	public StaticService(ObjectManager objectManager) {
		this.databaseMutex = new Object();
		this.objectManager = objectManager;
		
		spawnDatabase = new RelationalServerData("serverdata/static/spawns.db");

		if (!spawnDatabase.linkTableWithSdb("spawns", "serverdata/static/spawns.sdb") ||
				!spawnDatabase.linkTableWithSdb("types", "serverdata/static/types.sdb")) {
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for StaticService");
		}
		getSupportingStatement = spawnDatabase.prepareStatement(GET_SUPPORTING_SQL);
	}
	
	public void createSupportingObjects(SWGObject object) {
		synchronized (databaseMutex) {
			try {
				getSupportingStatement.setString(1, object.getTemplate());
				ResultSet set = getSupportingStatement.executeQuery();
				Location world = object.getWorldLocation();
				while (set.next()) {
					String iff = set.getString("child_iff");
					String cell = set.getString("cell");
					double x = set.getDouble("x");
					double y = set.getDouble("y");
					double z = set.getDouble("z");
					double heading = set.getDouble("heading");
					if (cell.isEmpty()) {
						createObject(iff, world, x, y, z, heading);
					} else {
						BuildingObject buio = (BuildingObject) object;
						createObject(iff, buio.getCellByName(cell), x, y, z, heading);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private SWGObject createObject(String iff, SWGObject parent, double x, double y, double z, double heading) {
		Location loc = new Location(x, y, z, parent.getTerrain());
		loc.setHeading(heading);
		SWGObject obj = objectManager.createObject(iff, loc, false, false);
		parent.addObject(obj);
		return obj;
	}
	
	private SWGObject createObject(String iff, Location parentLoc, double x, double y, double z, double heading) {
		Location loc = new Location(x, y, z, parentLoc.getTerrain());
		loc.setHeading(heading);
		loc.translateLocation(parentLoc);
		return objectManager.createObject(iff, loc, true, false);
	}
	
}
