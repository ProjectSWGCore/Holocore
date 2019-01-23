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
package com.projectswg.holocore.resources.support.data.location;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Quaternion;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

public class InstanceLocation implements Persistable, MongoPersistable {
	
	private Location location;
	private InstanceType instanceType;
	private int instanceNumber;
	
	public InstanceLocation() {
		this.location = new Location(0, 0, 0, Terrain.GONE);
		this.instanceType = InstanceType.NONE;
		this.instanceNumber = 0;
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		location.save(stream);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		location.read(stream);
	}
	
	@Override
	public void read(MongoData data) {
		instanceNumber = data.getInteger("number", 0);
		instanceType = InstanceType.valueOf(data.getString("type", "NONE"));
		location = data.getDocument("location", new Location());
	}
	
	@Override
	public void save(MongoData data) {
		data.putInteger("number", instanceNumber);
		data.putString("type", instanceType.name());
		data.putDocument("location", location);
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public void setPosition(Terrain terrain, double x, double y, double z) {
		assert terrain != null : "terrain is null";
		this.location = Location.builder(this.location)
				.setTerrain(terrain)
				.setPosition(x, y, z)
				.build();
	}
	
	public void setTerrain(Terrain terrain) {
		assert terrain != null : "terrain is null";
		if (location.getTerrain() == terrain)
			return;
		setLocation(Location.builder(location).setTerrain(terrain).build());
	}
	
	public void setPosition(double x, double y, double z) {
		setLocation(Location.builder(location).setPosition(x, y, z).build());
	}
	
	public void setOrientation(double x, double y, double z, double w) {
		setLocation(Location.builder(location).setOrientation(x, y, z, w).build());
	}
	
	public void setHeading(double heading) {
		setLocation(Location.builder(location).setHeading(heading).build());
	}
	
	public void setInstance(InstanceType instanceType, int instanceNumber) {
		this.instanceType = instanceType;
		this.instanceNumber = instanceNumber;
	}
	
	public void clearInstance() {
		setInstance(InstanceType.NONE, 0);
	}
	
	@NotNull
	public Location getLocation() {
		return location;
	}
	
	@NotNull
	public Point3D getPosition() {
		return location.getPosition();
	}
	
	@NotNull
	public Terrain getTerrain() {
		return location.getTerrain();
	}
	
	public Quaternion getOrientation() {
		return location.getOrientation();
	}
	
	public double getPositionX() {
		return location.getX();
	}
	
	public double getPositionY() {
		return location.getY();
	}
	
	public double getPositionZ() {
		return location.getZ();
	}
	
	public double getOrientationX() {
		return location.getOrientationX();
	}
	
	public double getOrientationY() {
		return location.getOrientationY();
	}
	
	public double getOrientationZ() {
		return location.getOrientationZ();
	}
	
	public double getOrientationW() {
		return location.getOrientationW();
	}
	
	public double getHeadingTo(Location target) {
		return location.getHeadingTo(target);
	}
	
	public double getHeadingTo(Point3D target) {
		return location.getHeadingTo(target);
	}
	
	public InstanceType getInstanceType() {
		return instanceType;
	}
	
	public int getInstanceNumber() {
		return instanceNumber;
	}
	
	public Location getWorldLocation(SWGObject self) {
		SWGObject parent = self.getSuperParent();
		if (parent == null)
			return location;
		if (self.getSlotArrangement() != -1)
			return parent.getWorldLocation();
		return Location.builder(location).translateLocation(parent.getLocation()).setTerrain(parent.getTerrain()).build();
	}
	
}
