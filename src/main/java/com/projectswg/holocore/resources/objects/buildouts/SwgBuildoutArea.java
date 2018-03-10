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

public class SwgBuildoutArea {
	
	// Datatable Information
	private String	name;
	private float	x1;
	private float	z1;
	private float	x2;
	private float	z2;
	private boolean	useClipRect;
	private float	clipRectX1;
	private float	clipRectZ1;
	private float	clipRectX2;
	private float	clipRectZ2;
	private int		envFlags;
	private int		envFlagsExclude;
	private boolean	useOrigin;
	private float	originX;
	private float	originZ;
	private float	compositeX1;
	private float	compositeZ1;
	private float	compositeX2;
	private float	compositeZ2;
	private String	compositeName;
	private boolean	isolated;
	private boolean	allowMap;
	private boolean	internal;
	private boolean	allowRadarTerrain;
	private String	eventRequired;
	// Calculated
	private int		index;
	// Used to calculate rows
	private long	currentBuilding;
	private long	currentCell;
	private long	buildingObjectId;
	private long	objectIdBase;
	
	public void load(Object [] datatableRow, int sceneNumber, int areaNumber) {
		name = (String) datatableRow[0];
		x1 = (Float) datatableRow[1];
		z1 = (Float) datatableRow[2];
		x2 = (Float) datatableRow[3];
		z2 = (Float) datatableRow[4];
		useClipRect = (Boolean) datatableRow[5];
		clipRectX1 = (Float) datatableRow[6];
		clipRectZ1 = (Float) datatableRow[7];
		clipRectX2 = (Float) datatableRow[8];
		clipRectZ2 = (Float) datatableRow[9];
		envFlags = (Integer) datatableRow[10];
		envFlagsExclude = (Integer) datatableRow[11];
		useOrigin = (Boolean) datatableRow[12];
		originX = (Float) datatableRow[13];
		originZ = (Float) datatableRow[14];
		compositeX1 = (Float) datatableRow[15];
		compositeZ1 = (Float) datatableRow[16];
		compositeX2 = (Float) datatableRow[17];
		compositeZ2 = (Float) datatableRow[18];
		compositeName = (String) datatableRow[19];
		isolated = (Boolean) datatableRow[20];
		allowMap = (Boolean) datatableRow[21];
		internal = (Boolean) datatableRow[22];
		allowRadarTerrain = (Boolean) datatableRow[23];
		eventRequired = (String) datatableRow[24];
		reset();
		calculate(sceneNumber, areaNumber);
	}
	
	private void reset() {
		currentBuilding = 0;
		currentCell = 0;
		buildingObjectId = 0;
		objectIdBase = 0;
	}
	
	private void calculate(int sceneNumber, int areaNumber) {
		index = sceneNumber * 100 + areaNumber;
		buildingObjectId = -(index + 1) * 30000L;
		objectIdBase = buildingObjectId + 2000;
	}
	
	public void setCurrentBuilding(long currentBuilding) {
		this.currentBuilding = currentBuilding;
	}
	
	public void setCurrentCell(long currentCell) {
		this.currentCell = currentCell;
	}
	
	public void setBuildingObjectId(long buildingObjectId) {
		this.buildingObjectId = buildingObjectId;
	}
	
	public void setObjectIdBase(long objectIdBase) {
		this.objectIdBase = objectIdBase;
	}

	public void incrementBuildingObjectId() {
		buildingObjectId++;
	}
	
	public void incrementObjectIdBase() {
		objectIdBase++;
	}
	
	public String getName() {
		return name;
	}
	
	public float getX1() {
		return x1;
	}
	
	public float getZ1() {
		return z1;
	}
	
	public float getX2() {
		return x2;
	}
	
	public float getZ2() {
		return z2;
	}
	
	public boolean isUseClipRect() {
		return useClipRect;
	}
	
	public float getClipRectX1() {
		return clipRectX1;
	}
	
	public float getClipRectZ1() {
		return clipRectZ1;
	}
	
	public float getClipRectX2() {
		return clipRectX2;
	}
	
	public float getClipRectZ2() {
		return clipRectZ2;
	}
	
	public int getEnvFlags() {
		return envFlags;
	}
	
	public int getEnvFlagsExclude() {
		return envFlagsExclude;
	}
	
	public boolean isUseOrigin() {
		return useOrigin;
	}
	
	public float getOriginX() {
		return originX;
	}
	
	public float getOriginZ() {
		return originZ;
	}
	
	public float getCompositeX1() {
		return compositeX1;
	}
	
	public float getCompositeZ1() {
		return compositeZ1;
	}
	
	public float getCompositeX2() {
		return compositeX2;
	}
	
	public float getCompositeZ2() {
		return compositeZ2;
	}
	
	public String getCompositeName() {
		return compositeName;
	}
	
	public boolean isIsolated() {
		return isolated;
	}
	
	public boolean isAllowMap() {
		return allowMap;
	}
	
	public boolean isInternal() {
		return internal;
	}
	
	public boolean isAllowRadarTerrain() {
		return allowRadarTerrain;
	}
	
	public String getEventRequired() {
		return eventRequired;
	}
	
	public int getIndex() {
		return index;
	}
	
	public long getCurrentBuilding() {
		return currentBuilding;
	}
	
	public long getCurrentCell() {
		return currentCell;
	}
	
	public long getBuildingObjectId() {
		return buildingObjectId;
	}
	
	public long getObjectIdBase() {
		return objectIdBase;
	}
	
}
