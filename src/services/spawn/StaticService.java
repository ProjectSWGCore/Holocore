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

import intents.object.ObjectCreatedIntent;

import java.lang.ref.SoftReference;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.Location;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;
import services.objects.ObjectCreator;

public class StaticService extends Service {
	
	private static final String GET_SUPPORTING_SQL = "SELECT spawns.* FROM spawns, types WHERE types.iff = ? AND spawns.iff_type = types.iff_type";
	
	private final Object databaseMutex;
	private final Map<String, SoftReference<List<SpawnedObject>>> spawnableObjects;
	private RelationalServerData spawnDatabase;
	private PreparedStatement getSupportingStatement;
	
	public StaticService() {
		this.databaseMutex = new Object();
		
		spawnDatabase = RelationalServerFactory.getServerData("static/spawns.db", "spawns", "types");
		if (spawnDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for StaticService");
		
		getSupportingStatement = spawnDatabase.prepareStatement(GET_SUPPORTING_SQL);
		spawnableObjects = new HashMap<>();
		
		registerForIntent(ObjectCreatedIntent.TYPE);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case ObjectCreatedIntent.TYPE:
				if (i instanceof ObjectCreatedIntent)
					createSupportingObjects(((ObjectCreatedIntent) i).getObject());
				break;
		}
	}
	
	private void createSupportingObjects(SWGObject object) {
		List<SpawnedObject> objects = null;
		synchronized (spawnableObjects) {
			SoftReference<List<SpawnedObject>> ref = spawnableObjects.get(object.getTemplate());
			if (ref != null)
				objects = ref.get();
			if (objects == null) {
				objects = fetchFromDatabase(object.getTemplate());
				spawnableObjects.put(object.getTemplate(), new SoftReference<List<SpawnedObject>>(objects));
			}
		}
		Location world = object.getWorldLocation();
		for (SpawnedObject spawn : objects) {
			spawn.createObject(object, world);
		}
	}
	
	private List<SpawnedObject> fetchFromDatabase(String template) {
		List<SpawnedObject> objects = new ArrayList<>();
		synchronized (databaseMutex) {
			try {
				getSupportingStatement.setString(1, template);
				try (ResultSet set = getSupportingStatement.executeQuery()) {
					while (set.next()) {
						objects.add(new SpawnedObject(set));
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return objects;
	}
	
	private static class SpawnedObject {
		
		private final String iff;
		private final String cell;
		private final double x;
		private final double y;
		private final double z;
		private final double heading;
		
		public SpawnedObject(ResultSet set) throws SQLException {
			this.iff = set.getString("child_iff");
			this.cell = set.getString("cell");
			this.x = set.getDouble("x");
			this.y = set.getDouble("y");
			this.z = set.getDouble("z");
			this.heading = set.getDouble("heading");
		}
		
		public void createObject(SWGObject building, Location parentLocation) {
			if (cell.isEmpty())
				createObject(parentLocation);
			else if (building instanceof BuildingObject)
				createObjectInParent(((BuildingObject) building).getCellByName(cell));
			else
				Log.e("StaticService", "Parent object with cell specified is not a BuildingObject!");
		}
		
		private SWGObject createObjectInParent(SWGObject parent) {
			if (parent == null)
				return null;
			Location loc = new Location(x, y, z, parent.getTerrain());
			loc.setHeading(heading);
			SWGObject obj = ObjectCreator.createObjectFromTemplate(iff);
			obj.setParent(parent);
			obj.setLocation(loc);
			new ObjectCreatedIntent(obj).broadcast();
			return obj;
		}
		
		private SWGObject createObject(Location parentLocation) {
			Location loc = new Location(x, y, z, parentLocation.getTerrain());
			loc.setHeading(heading);
			loc.translateLocation(parentLocation);
			SWGObject obj = ObjectCreator.createObjectFromTemplate(iff);
			obj.setLocation(loc);
			new ObjectCreatedIntent(obj).broadcast();
			return obj;
		}
		
	}
	
}
