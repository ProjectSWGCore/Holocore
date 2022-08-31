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

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuildingCellLoader.CellInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuildingCellLoader.PortalInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BuildingObject extends TangibleObject {
	
	private final Map<String, CellObject> nameToCell;
	private final Map<Integer, CellObject> idToCell;
	private final List<Portal> portals;
	
	private PlayerStructureInfo playerStructureInfo;
	
	public BuildingObject(long objectId) {
		super(objectId, BaselineType.BUIO);
		this.nameToCell = new HashMap<>();
		this.idToCell = new HashMap<>();
		this.portals = new ArrayList<>();
		this.playerStructureInfo = null;
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
	
	public List<Portal> getPortals() {
		return Collections.unmodifiableList(portals);
	}
	
	@Nullable
	public PlayerStructureInfo getPlayerStructureInfo() {
		return playerStructureInfo;
	}
	
	public void setPlayerStructureInfo(@Nullable PlayerStructureInfo playerStructureInfo) {
		this.playerStructureInfo = playerStructureInfo;
	}
	
	@Override
	public void addObject(SWGObject object) {
		assert object instanceof CellObject : "Object added to building is not a cell!";
		
		List<CellInfo> cellInfos = DataLoader.Companion.buildingCells().getBuilding(getTemplate());
		assert cellInfos != null : "No cells exist in this building";
		addObject((CellObject) object, cellInfos);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		super.saveMongo(data);
		if (playerStructureInfo != null)
			data.putDocument("playerStructureInfo", playerStructureInfo);
	}
	
	@Override
	public void readMongo(MongoData data) {
		super.readMongo(data);
		if (data.containsKey("playerStructureInfo"))
			this.playerStructureInfo = data.getDocument("playerStructureInfo", new PlayerStructureInfo(null));
		else
			this.playerStructureInfo = null;
	}
	
	public void populateCells() {
		List<CellInfo> cellInfos = DataLoader.Companion.buildingCells().getBuilding(getTemplate());
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
			portals.add(portal);
			cell.addPortal(portal);
			if (neighbor != null)
				neighbor.addPortal(portal);
		}
		idToCell.put(cell.getNumber(), cell);
		nameToCell.put(cell.getCellName(), cell); // Can be multiple cells with the same name
	}
	
}
