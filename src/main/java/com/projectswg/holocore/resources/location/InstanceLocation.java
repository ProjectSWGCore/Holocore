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
package com.projectswg.holocore.resources.location;

import java.util.concurrent.atomic.AtomicReference;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Location.LocationBuilder;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Quaternion;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

import com.projectswg.holocore.resources.objects.SWGObject;

import javax.annotation.Nonnull;

public class InstanceLocation implements Persistable {
	
	private final AtomicReference<Location> location;
	
	private InstanceType instanceType;
	private int instanceNumber;
	
	public InstanceLocation() {
		this.location = new AtomicReference<>(new Location(0, 0, 0, Terrain.GONE));
		this.instanceType = InstanceType.NONE;
		this.instanceNumber = 0;
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		location.get().save(stream);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		location.get().read(stream);
	}
	
	public void setLocation(Location location) {
		this.location.set(location);
	}
	
	public void setPosition(Terrain terrain, double x, double y, double z) {
		assert terrain != null : "terrain is null";
		Location location = Location.builder(getLocation())
				.setTerrain(terrain)
				.setPosition(x, y, z)
				.build();
		setLocation(location);
	}
	
	public void setTerrain(Terrain terrain) {
		assert terrain != null : "terrain is null";
		if (getLocation().getTerrain() == terrain)
			return;
		setLocation(Location.builder(getLocation()).setTerrain(terrain).build());
	}
	
	public void setPosition(double x, double y, double z) {
		setLocation(Location.builder(getLocation()).setPosition(x, y, z).build());
	}
	
	public void setOrientation(double x, double y, double z, double w) {
		setLocation(Location.builder(getLocation()).setOrientation(x, y, z, w).build());
	}
	
	public void setHeading(double heading) {
		setLocation(Location.builder(getLocation()).setHeading(heading).build());
	}
	
	public void setInstance(InstanceType instanceType, int instanceNumber) {
		this.instanceType = instanceType;
		this.instanceNumber = instanceNumber;
	}
	
	public void clearInstance() {
		setInstance(InstanceType.NONE, 0);
	}
	
	@Nonnull
	public Location getLocation() {
		return location.get();
	}
	
	@Nonnull
	public Point3D getPosition() {
		return getLocation().getPosition();
	}
	
	@Nonnull
	public Terrain getTerrain() {
		return getLocation().getTerrain();
	}
	
	public Quaternion getOrientation() {
		return getLocation().getOrientation();
	}
	
	public double getPositionX() {
		return getLocation().getX();
	}
	
	public double getPositionY() {
		return getLocation().getY();
	}
	
	public double getPositionZ() {
		return getLocation().getZ();
	}
	
	public double getOrientationX() {
		return getLocation().getOrientationX();
	}
	
	public double getOrientationY() {
		return getLocation().getOrientationY();
	}
	
	public double getOrientationZ() {
		return getLocation().getOrientationZ();
	}
	
	public double getOrientationW() {
		return getLocation().getOrientationW();
	}
	
	public InstanceType getInstanceType() {
		return instanceType;
	}
	
	public int getInstanceNumber() {
		return instanceNumber;
	}
	
	public Location getWorldLocation(SWGObject self) {
		SWGObject parent = self.getParent();
		LocationBuilder builder = Location.builder(getLocation());
		while (parent != null) {
			builder.translateLocation(parent.getInstanceLocation().getLocation());
			builder.setTerrain(parent.getTerrain());
			parent = parent.getParent();
		}
		return builder.build();
	}
	
}
