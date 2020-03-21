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
package com.projectswg.utility.clientdata.buildouts;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.CrcStringTableData;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import me.joshlarson.jlcommon.log.Log;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class BuildoutLoader {
	
	private static final CrcStringTableData crcTable = (CrcStringTableData) ClientFactory.getInfoFromFile("misc/object_template_crc_string_table.iff");
	private static final String BASE_PATH = "datatables/buildout/";
	
	private final Map<Long, Long> idMapping;
	private final Set<SWGObject> objects;
	private final AtomicLong objectIdGenerator;
	private final AtomicLong cellIdGenerator;
	
	public BuildoutLoader(AtomicLong objectIdGenerator, AtomicLong cellIdGenerator) {
		this.idMapping = new HashMap<>();
		this.objects = new HashSet<>();
		this.objectIdGenerator = objectIdGenerator;
		this.cellIdGenerator = cellIdGenerator;
	}
	
	public void loadAllBuildouts() {
		DatatableData table = (DatatableData) ClientFactory.getInfoFromFile(BASE_PATH+"buildout_scenes.iff");
		int areaId = 1;
		for (int sceneRow = 0; sceneRow < table.getRowCount(); sceneRow++) {
			Terrain t = Terrain.getTerrainFromName((String) table.getCell(sceneRow, 0));
			
			String file = "datatables/buildout/areas_"+t.getName()+".iff";
			DatatableData areaTable = (DatatableData) ClientFactory.getInfoFromFile(file);
			for (int row = 0; row < areaTable.getRowCount(); row++) {
				SwgBuildoutArea area = new SwgBuildoutArea();
				area.load(areaTable.getRow(row), sceneRow, row);
				loadArea(area, t, areaId);
				areaId++;
			}
		}
	}
	
	public long getPreviousId(long id) {
		return idMapping.getOrDefault(id, 0L);
	}
	
	public Set<SWGObject> getObjects() {
		return Collections.unmodifiableSet(objects);
	}
	
	private void loadArea(SwgBuildoutArea area, Terrain terrain, int areaId) {
		DatatableData areaTable = (DatatableData) ClientFactory.getInfoFromFile(BASE_PATH+terrain.getName()+"/"+area.getName().replace("server", "client")+".iff");
		
		SwgBuildoutRow buildoutRow = new SwgBuildoutRow(area);
		Map<Long, SWGObject> officialBuildoutTable = new HashMap<>();
		for (int row = 0; row < areaTable.getRowCount(); row++) {
			buildoutRow.load(areaTable.getRow(row), crcTable);
			
			long id = buildoutRow.getTemplate().equals("object/cell/shared_cell.iff") ? cellIdGenerator.getAndIncrement() : objectIdGenerator.getAndIncrement();
			SWGObject object = ObjectCreator.createObjectFromTemplate(id, buildoutRow.getTemplate());
			Location l = buildoutRow.getLocation();
			object.setPosition(terrain, l.getX(), l.getY(), l.getZ());
			object.setOrientation(l.getOrientationX(), l.getOrientationY(), l.getOrientationZ(), l.getOrientationW());
			object.setBuildoutAreaId(areaId);
			object.setBuildoutEvent(area.getEventRequired());
			
			setCellInformation(object, buildoutRow.getCellIndex());
			addObject(officialBuildoutTable, object, buildoutRow.getObjectId(), buildoutRow.getContainerId());
		}
	}
	
	private void setCellInformation(SWGObject object, int cellIndex) {
		if (!(object instanceof CellObject))
			return;
		CellObject cell = (CellObject) object;
		cell.setNumber(cellIndex);
	}
	
	private void addObject(Map<Long, SWGObject> objectTable, SWGObject object, long objectId, long containerId) {
		objects.add(object);
		idMapping.put(object.getObjectId(), objectId);
		objectTable.put(objectId, object);
		if (containerId != 0) {
			SWGObject container = objectTable.get(containerId);
			object.systemMove(container);
			if (container == null)
				Log.e("Failed to load object: " + object.getTemplate());
		}
	}
	
}
