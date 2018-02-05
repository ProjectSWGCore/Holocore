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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;

import intents.object.ObjectCreatedIntent;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.server_info.SdbLoader;
import resources.server_info.SdbLoader.SdbResultSet;
import resources.server_info.StandardLog;
import services.objects.ObjectCreator;

public class StaticService extends Service {
	
	private final Map<String, List<SpawnedObject>> spawnableObjects;
	
	public StaticService() {
		spawnableObjects = new HashMap<>();
		
		registerForIntent(ObjectCreatedIntent.class, this::handleObjectCreatedIntent);
	}
	
	@Override
	public boolean initialize() {
		loadSupportingObjects();
		return super.initialize();
	}
	
	private void loadSupportingObjects() {
		long startTime = StandardLog.onStartLoad("static objects");
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/static/spawns.sdb"))) {
			Map<String, List<String>> typeToIff = createTypeMap();
			while (set.next()) {
				List<String> iffList = typeToIff.get(set.getText("iff_type"));
				if (iffList == null) {
					Log.w("Type defined in spawns.sdb but not in types.sdb: %s", set.getText("iff_type"));
					continue;
				}
				if (iffList.isEmpty())
					continue;
				
				List<SpawnedObject> objects = spawnableObjects.get(iffList.get(0)); // try to cut down on ArrayList objects
				if (objects == null)
					objects = new ArrayList<>();
				
				objects.add(new SpawnedObject(set));
				for (String iff : iffList) {
					spawnableObjects.put(iff, objects);
				}
			}
		} catch (IOException e) {
			Log.e(e);
		}
		StandardLog.onEndLoad(spawnableObjects.size(), "static objects", startTime);
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject object = oci.getObject();
		List<SpawnedObject> objects = spawnableObjects.get(object.getTemplate());
		if (objects == null) {
			return;
		}
		Location world = object.getWorldLocation();
		for (SpawnedObject spawn : objects) {
			spawn.createObject(object, world);
		}
	}
	
	private static Map<String, List<String>> createTypeMap() throws IOException {
		Map<String, List<String>> typeToIff = new HashMap<>();
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/static/types.sdb"))) {
			while (set.next()) {
				String iff = ClientFactory.formatToSharedFile(set.getText("iff"));
				String iffType = set.getText("iff_type");
				
				List<String> list = typeToIff.computeIfAbsent(iffType, k -> new ArrayList<>());
				list.add(iff);
			}
		}
		return typeToIff;
	}
	
	private static class SpawnedObject {
		
		private final String iff;
		private final String cell;
		private final double x;
		private final double y;
		private final double z;
		private final double heading;
		
		public SpawnedObject(SdbResultSet set) {
			this.iff = set.getText("child_iff");
			this.cell = set.getText("cell");
			this.x = set.getReal("x");
			this.y = set.getReal("y");
			this.z = set.getReal("z");
			this.heading = set.getReal("heading");
		}
		
		public void createObject(SWGObject building, Location parentLocation) {
			if (cell.isEmpty())
				createObject(parentLocation);
			else if (building instanceof BuildingObject)
				createObjectInParent(((BuildingObject) building).getCellByName(cell));
			else
				Log.e("Parent object with cell specified is not a BuildingObject!");
		}
		
		private SWGObject createObjectInParent(SWGObject parent) {
			Assert.notNull(parent);
			SWGObject obj = ObjectCreator.createObjectFromTemplate(iff);
			obj.setLocation(Location.builder()
					.setPosition(x, y, z)
					.setTerrain(parent.getTerrain())
					.setHeading(heading)
					.build());
			obj.moveToContainer(parent);
			ObjectCreatedIntent.broadcast(obj);
			return obj;
		}
		
		private SWGObject createObject(Location parentLocation) {
			SWGObject obj = ObjectCreator.createObjectFromTemplate(iff);
			obj.setLocation(Location.builder()
					.setPosition(x, y, z)
					.setTerrain(parentLocation.getTerrain())
					.setHeading(heading)
					.translateLocation(parentLocation)
					.build());
			ObjectCreatedIntent.broadcast(obj);
			return obj;
		}
		
	}
	
}
