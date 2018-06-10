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
package com.projectswg.holocore.resources.support.objects.swg.waypoint;

import com.projectswg.common.data.encodables.oob.OutOfBandPackage.Type;
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import org.jetbrains.annotations.NotNull;

public class WaypointObject extends IntangibleObject implements Encodable, Persistable {
	
	private WaypointPackage waypoint;
	
	public WaypointObject(long objectId) {
		super(objectId, BaselineType.WAYP);
		this.waypoint = new WaypointPackage();
		this.waypoint.setObjectId(objectId);
	}
	
	public void setOOB(WaypointPackage oob) {
		this.waypoint = oob;
	}
	
	public WaypointPackage getOOB() {
		return waypoint;
	}
	
	@Override
	public String getObjectName() {
		return waypoint.getName();
	}
	
	public Point3D getPosition() {
		return waypoint.getPosition();
	}
	
	@Override
	public long getObjectId() {
		return waypoint.getObjectId();
	}
	
	@NotNull
	@Override
	public Terrain getTerrain() {
		return waypoint.getTerrain();
	}
	
	public long getCellId() {
		return waypoint.getCellId();
	}
	
	public String getName() {
		return waypoint.getName();
	}
	
	public WaypointColor getColor() {
		return waypoint.getColor();
	}
	
	public boolean isActive() {
		return waypoint.isActive();
	}
	
	public void setObjectId(long objectId) {
		waypoint.setObjectId(objectId);
	}
	
	@Override
	public void setTerrain(@NotNull Terrain terrain) {
		waypoint.setTerrain(terrain);
	}
	
	public void setCellId(long cellId) {
		waypoint.setCellId(cellId);
	}
	
	public void setName(String name) {
		waypoint.setName(name);
	}
	
	public void setColor(WaypointColor color) {
		waypoint.setColor(color);
	}
	
	public void setActive(boolean active) {
		waypoint.setActive(active);
	}

	public Type getOobType() {
		return waypoint.getOobType();
	}

	@Override
	public void decode(NetBuffer data) {
		waypoint.decode(data);
	}
	
	@Override
	public byte[] encode() {
		return waypoint.encode();
	}
	
	@Override
	public int getLength() {
		return waypoint.getLength();
	}

	@Override
	public void save(NetBufferStream stream) {
		super.save(stream);
		stream.addByte(0);
		waypoint.save(stream);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		stream.getByte();
		waypoint.read(stream);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof WaypointObject))
			return false;
		WaypointObject wp = (WaypointObject) o;
		return wp.getObjectId() == getObjectId();
	}
	
	@Override
	public String toString() {
		return String.format("WaypointObject[name='%s', color=%s, active=%b, location=%s:%s", getName(), getColor(), isActive(), getTerrain(), getPosition());
	}
	
}
