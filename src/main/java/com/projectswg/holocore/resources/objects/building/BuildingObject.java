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
package com.projectswg.holocore.resources.objects.building;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.data.swgfile.visitors.PortalLayoutData;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.services.objects.ObjectCreator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildingObject extends TangibleObject {
	
	private final Map<String, CellObject> nameToCell;
	private final Map<Integer, CellObject> idToCell;
	
	private WeakReference<PortalLayoutData> portalLayoutData;
	private int loadRange;
	
	public BuildingObject(long objectId) {
		super(objectId, BaselineType.BUIO);
		this.nameToCell = new HashMap<>();
		this.idToCell = new HashMap<>();
		this.portalLayoutData = null;
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
		super.addObject(object);
		
		CellObject cell = (CellObject) object;
		assert cell.getNumber() > 0 : "Cell Number must be greater than 0";
		assert cell.getNumber() < getCellCount() : "Cell Number must be less than the cell count!";
		assert idToCell.get(cell.getNumber()) == null : "Multiple cells have the same number!";
		
		cell.setCellName(getCellName(cell.getNumber()));
		idToCell.put(cell.getNumber(), cell);
		nameToCell.put(cell.getCellName(), cell); // Can be multiple cells with the same name
	}
	
	@Override
	protected int calculateLoadRange() {
		return loadRange != 0 ? loadRange : super.calculateLoadRange();
	}
	
	public void populateCells() {
		int cells = getCellCount();
		for (int i = 1; i < cells; i++) { // 0 is world
			if (idToCell.get(i) != null)
				continue;
			CellObject cell = (CellObject) ObjectCreator.createObjectFromTemplate("object/cell/shared_cell.iff");
			cell.setNumber(i);
			cell.setTerrain(getTerrain());
			addObject(cell);
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
	
	private int getCellCount() {
		PortalLayoutData data = getPortalLayoutData();
		if (data == null)
			return 0;
		return data.getCells().size();
	}
	
	private String getCellName(int cell) {
		PortalLayoutData data = getPortalLayoutData();
		if (data == null)
			return "";
		return data.getCells().get(cell).getName();
	}
	
	private PortalLayoutData getPortalLayoutData() {
		PortalLayoutData portalLayoutData = (this.portalLayoutData == null) ? null : this.portalLayoutData.get();
		if (portalLayoutData == null) {
			String portalFile = (String) getDataAttribute(ObjectDataAttribute.PORTAL_LAYOUT_FILENAME);
			if (portalFile == null || portalFile.isEmpty())
				return null;
			
			portalLayoutData = (PortalLayoutData) ClientFactory.getInfoFromFile(portalFile);
			this.portalLayoutData = new WeakReference<>(portalLayoutData);
		}
		assert portalLayoutData != null && portalLayoutData.getCells() != null && portalLayoutData.getCells().size() > 0 : "Invalid portal layout data!";
		return portalLayoutData;
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
	
}
