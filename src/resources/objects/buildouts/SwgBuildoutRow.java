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

import java.util.concurrent.atomic.AtomicReference;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.swgfile.visitors.CrcStringTableData;

public class SwgBuildoutRow {
	
	private static final int	cellCrc		= CRC.getCrc("object/cell/shared_cell.iff");
	
	private final SwgBuildoutArea	buildoutArea;
	private final AtomicReference<Location> location;
	private long				objectId;
	private long				containerId;
	private int					type;
	private int					sharedTemplateCrc;
	private int					cellIndex;
	private float				radius;
	private int					portalLayoutCrc;
	private String				template;
	
	public SwgBuildoutRow(SwgBuildoutArea buildoutArea) {
		this.buildoutArea = buildoutArea;
		this.location = new AtomicReference<>(null);
	}
	
	public void load(Object [] datatableRow, CrcStringTableData crcString) {
		switch (datatableRow.length) {
			case 11:
				loadSmall(datatableRow, crcString);
				break;
			case 14:
				loadLarge(datatableRow, crcString);
				break;
			default:
				throw new IllegalArgumentException("Datatable row must be either 11 or 14 columns!");
		}
		translateLocation();
	}
	
	private void loadSmall(Object [] datatableRow, CrcStringTableData crcString) {
		loadEndColumns(datatableRow, crcString, 0);
		if (portalLayoutCrc != 0) { // is building
			objectId = buildoutArea.getBuildingObjectId();
			buildoutArea.incrementBuildingObjectId();
			buildoutArea.setCurrentBuilding(objectId);
			containerId = 0;
		} else if (sharedTemplateCrc == cellCrc) {
			objectId = buildoutArea.getBuildingObjectId();
			buildoutArea.incrementBuildingObjectId();
			buildoutArea.setCurrentCell(objectId);
			containerId = buildoutArea.getCurrentBuilding();
		} else if (cellIndex > 0) { // is in cell
			objectId = buildoutArea.getObjectIdBase();
			buildoutArea.incrementObjectIdBase();
			containerId = buildoutArea.getCurrentCell();
		} else { // is somewhere else
			objectId = buildoutArea.getObjectIdBase();
			buildoutArea.incrementObjectIdBase();
			containerId = 0;
		}
	}
	
	private void loadLarge(Object [] datatableRow, CrcStringTableData crcString) {
		objectId = (Integer) datatableRow[0];
		containerId = (Integer) datatableRow[1];
		type = (Integer) datatableRow[2];
		loadEndColumns(datatableRow, crcString, 3);
		final long indexShifted = (buildoutArea.getIndex() + 1L) << 48;
		if (objectId < 0)
			objectId ^= indexShifted;
		if (containerId < 0)
			containerId ^= indexShifted;
	}
	
	private void loadEndColumns(Object [] datatableRow, CrcStringTableData crcString, int offset) {
		sharedTemplateCrc = (Integer) datatableRow[offset];
		cellIndex = (Integer) datatableRow[offset + 1];
		Location loc = Location.builder()
				.setX((Float) datatableRow[offset + 2])
				.setY((Float) datatableRow[offset + 3])
				.setZ((Float) datatableRow[offset + 4])
				.setOrientationW((Float) datatableRow[offset + 5])
				.setOrientationX((Float) datatableRow[offset + 6])
				.setOrientationY((Float) datatableRow[offset + 7])
				.setOrientationZ((Float) datatableRow[offset + 8])
				.build();
		location.set(loc);
		radius = (Float) datatableRow[offset + 9];
		portalLayoutCrc = (Integer) datatableRow[offset + 10];
		template = crcString.getTemplateString(sharedTemplateCrc);
	}
	
	private void translateLocation() {
		if (cellIndex != 0)
			return;
		location.set(Location.builder(location.get()).translatePosition(buildoutArea.getX1(), 0, buildoutArea.getZ1()).build());
	}
	
	public Location getLocation() {
		return location.get();
	}
	
	public long getObjectId() {
		return objectId;
	}
	
	public long getContainerId() {
		return containerId;
	}
	
	public int getType() {
		return type;
	}
	
	public int getSharedTemplateCrc() {
		return sharedTemplateCrc;
	}
	
	public int getCellIndex() {
		return cellIndex;
	}
	
	public float getRadius() {
		return radius;
	}
	
	public int getPortalLayoutCrc() {
		return portalLayoutCrc;
	}
	
	public String getTemplate() {
		return template;
	}
	
}
