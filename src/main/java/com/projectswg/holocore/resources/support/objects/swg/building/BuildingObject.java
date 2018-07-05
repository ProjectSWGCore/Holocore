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
package com.projectswg.holocore.resources.support.objects.swg.building;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuildingCellLoader.CellInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuildingCellLoader.PortalInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildingObject extends TangibleObject {
	
	private final Map<String, CellObject> nameToCell;
	private final Map<Integer, CellObject> idToCell;
	
	private int loadRange;
	
	public BuildingObject(long objectId) {
		super(objectId, BaselineType.BUIO);
		this.nameToCell = new HashMap<>();
		this.idToCell = new HashMap<>();
		this.loadRange = 0;
	}
	
	public CellObject getCellByName(String cellName) {
		return nameToCell.get(cellName);
	}
	
	public CellObject getCellByNumber(int cellNumber) {
		return idToCell.get(cellNumber);
	}
	
	public List<CellObject> getCells() {
		return new ArrayList<>(idToCell.values());
	}
	
	@Override
	public void addObject(SWGObject object) {
		assert object instanceof CellObject : "Object added to building is not a cell!";
		
		List<CellInfo> cellInfos = DataLoader.buildingCells().getBuilding(getTemplate());
		assert cellInfos != null : "No cells exist in this building";
		addObject((CellObject) object, cellInfos);
	}
	
	public void populateCells() {
		List<CellInfo> cellInfos = DataLoader.buildingCells().getBuilding(getTemplate());
		if (cellInfos == null)
			return; // No cells to populate
		for (CellInfo cellInfo : cellInfos) { // 0 is world
			if (idToCell.get(cellInfo.getId()) != null || cellInfo.getId() == 0)
				continue;
			CellObject cell = (CellObject) ObjectCreator.createObjectFromTemplate("object/cell/shared_cell.iff");
			cell.setNumber(cellInfo.getId());
			cell.setTerrain(getTerrain());
			addObject(cell, cellInfos);
		}
	}
	
	@Override
	public void setTemplate(String template) {
		super.setTemplate(template);
		if (template.equals("object/building/tatooine/shared_palace_tatooine_jabba.iff"))
			loadRange = Integer.MAX_VALUE;
		if (template.equals("object/building/tatooine/shared_tower_jabbas_palace.iff"))
			loadRange = Integer.MAX_VALUE;
	}
	
	@Override
	public void save(NetBufferStream stream) {
		super.save(stream);
		stream.addByte(0);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		stream.getByte();
	}
	
	@Override
	protected int calculateLoadRange() {
		return loadRange != 0 ? loadRange : super.calculateLoadRange();
	}
	
	private void addObject(CellObject cell, List<CellInfo> cellInfos) {
		super.addObject(cell);
		assert cell.getNumber() > 0 : "Cell Number must be greater than 0";
		assert cell.getNumber() < cellInfos.size() : "Cell Number must be less than the cell count!";
		assert idToCell.get(cell.getNumber()) == null : "Multiple cells have the same number!";
		
		CellInfo cellInfo = cellInfos.get(cell.getNumber());
		cell.setCellName(cellInfo.getName());
		for (PortalInfo portalInfo : cellInfo.getNeighbors()) {
			int otherCell = portalInfo.getOtherCell(cellInfo.getId());
			CellObject neighbor = null;
			if (otherCell != 0) {
				neighbor = idToCell.get(otherCell);
				if (neighbor == null)
					continue;
			}
			Portal portal = new Portal(cell, neighbor, portalInfo.getFrame1(), portalInfo.getFrame2(), portalInfo.getHeight());
			cell.addPortal(portal);
			if (neighbor != null)
				neighbor.addPortal(portal);
		}
		idToCell.put(cell.getNumber(), cell);
		nameToCell.put(cell.getCellName(), cell); // Can be multiple cells with the same name
	}
	
}
