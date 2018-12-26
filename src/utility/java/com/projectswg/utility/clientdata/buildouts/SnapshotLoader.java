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
import com.projectswg.common.data.swgfile.visitors.WorldSnapshotData;
import com.projectswg.common.data.swgfile.visitors.WorldSnapshotData.Node;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import me.joshlarson.jlcommon.log.Log;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SnapshotLoader {
	
	private static final Set <Terrain> TERRAINS = EnumSet.of(
			Terrain.CORELLIA,	Terrain.DANTOOINE,	Terrain.DATHOMIR,
			Terrain.DUNGEON1,	Terrain.ENDOR,		Terrain.LOK,
			Terrain.NABOO,		Terrain.RORI,		Terrain.TALUS,
			Terrain.TATOOINE,	Terrain.YAVIN4
	);
	private static final String BASE_PATH = "snapshot/";
	
	private final Map<Long, SWGObject> snapshotTable;
	private final Map<Long, Long> idMapping;
	private final List<SWGObject> objects;
	private final AtomicLong objectIdGenerator;
	private final AtomicLong cellIdGenerator;
	
	public SnapshotLoader(AtomicLong objectIdGenerator, AtomicLong cellIdGenerator) {
		this.snapshotTable = new HashMap<>();
		this.idMapping = new HashMap<>();
		this.objects = new ArrayList<>();
		this.objectIdGenerator = objectIdGenerator;
		this.cellIdGenerator = cellIdGenerator;
	}
	
	public void loadAllSnapshots() {
		for (Terrain t : TERRAINS) {
			loadTerrain(t);
		}
	}
	
	public long getPreviousId(long id) {
		return idMapping.getOrDefault(id, 0L);
	}
	
	public List<SWGObject> getObjects() {
		return Collections.unmodifiableList(objects);
	}
	
	private void loadTerrain(Terrain terrain) {
		String path = BASE_PATH + terrain.getName() + ".ws";
		WorldSnapshotData data = (WorldSnapshotData) ClientFactory.getInfoFromFile(path);
		Map <Integer, String> templates = data.getObjectTemplateNames();
		for (Node node : data.getNodes()) {
			createFromNode(templates, terrain, node);
		}
	}
	
	private void createFromNode(Map<Integer, String> templates, Terrain terrain, Node node) {
		SWGObject object = createObject(templates, terrain, node);
		object.setBuildoutAreaId(-1);
		setCellInformation(object, node.getCellIndex());
		addObject(object, node.getId(), node.getContainerId());
		
		for (Node child : node.getChildren()) {
			createFromNode(templates, terrain, child);
		}
	}
	
	private SWGObject createObject(Map <Integer, String> templateMap, Terrain terrain, Node row) {
		String template = templateMap.get(row.getObjectTemplateNameIndex());
		long id = template.equals("object/cell/shared_cell.iff") ? cellIdGenerator.getAndIncrement() : objectIdGenerator.getAndIncrement();
		SWGObject object = ObjectCreator.createObjectFromTemplate(id, template);
		Location l = row.getLocation();
		object.setPosition(terrain, l.getX(), l.getY(), l.getZ());
		object.setOrientation(l.getOrientationX(), l.getOrientationY(), l.getOrientationZ(), l.getOrientationW());
		return object;
	}
	
	private void addObject(SWGObject object, long objectId, long containerId) {
		objects.add(object);
		snapshotTable.put(objectId, object);
		idMapping.put(object.getObjectId(), objectId);
		if (containerId != 0) {
			SWGObject container = snapshotTable.get(containerId);
			if (!(object instanceof CellObject) && container instanceof BuildingObject) {
				Log.w("Not adding: %s to %s - invalid type for BuildingObject", object, container);
				return;
			}
			object.systemMove(container);
			if (container == null)
				Log.e("Failed to load object: " + object.getTemplate());
		}
	}
	
	private void setCellInformation(SWGObject object, int cellIndex) {
		if (!(object instanceof CellObject))
			return;
		CellObject cell = (CellObject) object;
		cell.setNumber(cellIndex);
	}
	
}
