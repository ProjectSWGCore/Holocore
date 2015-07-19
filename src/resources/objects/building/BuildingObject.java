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
package resources.objects.building;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.ObjectData;
import resources.client_info.visitors.PortalLayoutData;
import resources.objects.SWGObject;
import resources.objects.cell.CellObject;
import resources.objects.tangible.TangibleObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BuildingObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	public BuildingObject(long objectId) {
		super(objectId, BaselineType.BUIO);
	}
	
	public CellObject getCellByName(String cellName) {
		for (SWGObject cont : getContainedObjects()) {
			if (cont instanceof CellObject) {
				if (((CellObject) cont).getCellName().equals(cellName)) {
					return (CellObject) cont;
				}
			}
		}
		return null;
	}

	public List<CellObject> getCells() {
		List<CellObject> cells = new LinkedList<>();
		for (SWGObject object : getContainedObjects()) {
			if (object instanceof CellObject)
				cells.add((CellObject) object);
		}
		return Collections.unmodifiableList(cells);
	}

	@Override
	public boolean addObject(SWGObject object) {
		boolean added = super.addObject(object);
		if (!added || !(object instanceof CellObject))
			return added;

		String portalFile = String.valueOf(getTemplateAttribute(ObjectData.PORTAL_LAYOUT));
		if (portalFile == null || portalFile.isEmpty())
			return true;

		PortalLayoutData portalLayoutData = (PortalLayoutData) ClientFactory.getInfoFromFile(portalFile, true);
		if (portalLayoutData == null || portalLayoutData.getCells() == null || portalLayoutData.getCells().size() == 0)
			return true;

		populateCellData((CellObject) object, portalLayoutData.getCells().get(((CellObject) object).getNumber()));
		return true;
	}

	private void populateCellData(CellObject cellObject, PortalLayoutData.Cell cellData) {
		cellObject.setCellName(cellData.getName());
//		System.out.println(cellObject + " cell name " + cellObject.getCelName());
	}
}
