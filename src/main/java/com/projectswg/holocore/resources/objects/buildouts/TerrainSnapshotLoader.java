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
package com.projectswg.holocore.resources.objects.buildouts;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.WorldSnapshotData;
import com.projectswg.common.data.swgfile.visitors.WorldSnapshotData.Node;
import com.projectswg.common.debug.Log;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.SWGObject.ObjectClassification;
import com.projectswg.holocore.resources.objects.building.BuildingObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.services.objects.ObjectCreator;

public class TerrainSnapshotLoader {
	
	private static final String BASE_PATH = "snapshot/";
	
	private final Terrain terrain;
	private final Map <Long, SWGObject> objectTable;
	private final List <SWGObject> objects;
	
	public TerrainSnapshotLoader(Terrain terrain) {
		this.terrain = terrain;
		this.objectTable = new Hashtable<>(12 * 1024);
		this.objects = new LinkedList<>();
	}

	public Map <Long, SWGObject> getObjectTable() {
		return objectTable;
	}
	
	public List <SWGObject> getObjects() {
		return objects;
	}
	
	public void load() {
		objects.clear();
		String path = BASE_PATH + terrain.getName() + ".ws";
		WorldSnapshotData data = (WorldSnapshotData) ClientFactory.getInfoFromFile(path);
		Map <Integer, String> templates = data.getObjectTemplateNames();
		for (Node node : data.getNodes()) {
			createFromNode(templates, node);
		}
	}
	
	private void createFromNode(Map<Integer, String> templates, Node node) {
		SWGObject object = createObject(templates, node);
		object.setClassification(ObjectClassification.SNAPSHOT);
		object.setBuildoutAreaId(-1);
		setCellInformation(object, node.getCellIndex());
		addObject(object, node.getContainerId());
		
		for (Node child : node.getChildren()) {
			createFromNode(templates, child);
		}
	}
	
	private SWGObject createObject(Map <Integer, String> templateMap, Node row) {
		SWGObject object = ObjectCreator.createObjectFromTemplate(row.getId(), templateMap.get(row.getObjectTemplateNameIndex()));
		Location l = row.getLocation();
		object.setPosition(terrain, l.getX(), l.getY(), l.getZ());
		object.setOrientation(l.getOrientationX(), l.getOrientationY(), l.getOrientationZ(), l.getOrientationW());
		return object;
	}
	
	private void addObject(SWGObject object, long containerId) {
		objectTable.put(object.getObjectId(), object);
		if (containerId != 0) {
			SWGObject container = objectTable.get(containerId);
			if (!(object instanceof CellObject) && container instanceof BuildingObject) {
				Log.w("Not adding: %s to %s - invalid type for BuildingObject", object, container);
				return;
			}
			object.moveToContainer(container);
			if (container == null)
				Log.e("Failed to load object: " + object.getTemplate());
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
	
}
