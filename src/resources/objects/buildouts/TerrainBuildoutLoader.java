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
package resources.objects.buildouts;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import resources.Location;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.CrcStringTableData;
import resources.client_info.visitors.DatatableData;
import resources.objects.SWGObject;
import resources.objects.cell.CellObject;
import resources.server_info.Log;
import services.objects.ObjectCreator;

class TerrainBuildoutLoader {
	
	private static final String BASE_PATH = "datatables/buildout/";

	private final CrcStringTableData crcTable;
	private final Terrain terrain;
	private final Map <Long, SWGObject> objectTable;
	private final List <SWGObject> objects;
	
	public TerrainBuildoutLoader(CrcStringTableData crcTable, Terrain terrain) {
		this.crcTable = crcTable;
		this.terrain = terrain;
		this.objectTable = new Hashtable<Long, SWGObject>(12*1024);
		this.objects = new LinkedList<SWGObject>();
	}
	
	public void load(int sceneNumber) {
		objects.clear();
		loadAreas(sceneNumber);
	}
	
	public List <SWGObject> getObjects() {
		return objects;
	}
	
	private void loadAreas(int sceneNumber) {
		objects.clear();
		String file = BASE_PATH+"areas_"+terrain.getName()+".iff";
		DatatableData areaTable = (DatatableData) ClientFactory.getInfoFromFile(file);
		for (int row = 0; row < areaTable.getRowCount(); row++) {
			BuildoutArea area = new BuildoutArea();
			area.load(areaTable.getRow(row), sceneNumber, row);
			if (!area.getName().startsWith(terrain.getName()))
				continue;
			loadArea(area);
		}
	}
	
	private void loadArea(BuildoutArea area) {
		String file = BASE_PATH+terrain.getName()+"/"+area.getName().replace("server", "client")+".iff";
		DatatableData areaTable = (DatatableData) ClientFactory.getInfoFromFile(file);
		BuildoutRow buildoutRow = new BuildoutRow(area);
		for (int row = 0; row < areaTable.getRowCount(); row++) {
			buildoutRow.load(areaTable.getRow(row), crcTable);
			SWGObject object = createObject(buildoutRow);
			addObject(object, buildoutRow.getContainerId());
			setCellInformation(object, buildoutRow.getCellIndex());
			updatePermissions(object);
		}
	}
	
	private SWGObject createObject(BuildoutRow row) {
		SWGObject object = ObjectCreator.createObjectFromTemplate(row.getObjectId(), row.getTemplate());
		Location l = row.getLocation();
		l.setTerrain(terrain);
		object.setLocation(l);
		return object;
	}
	
	private void addObject(SWGObject object, long containerId) {
		objectTable.put(object.getObjectId(), object);
		if (containerId != 0) {
			SWGObject container = objectTable.get(containerId);
			if (container != null)
				container.addObject(object);
			else {
				Log.e("TerrainBuildoutLoader", "Failed to load object: " + object.getTemplate());
//				objects.add(object);
			}
		} else {
			objects.add(object);
		}
	}
	
	private void setCellInformation(SWGObject object, int cellIndex) {
		if (!(object instanceof CellObject))
			return;
		CellObject cell = (CellObject) object;
		cell.setNumber(cellIndex);
	}
	
	private void updatePermissions(SWGObject object) {
		object.getContainerPermissions().addDefaultWorldPermissions();
	}
	
}
