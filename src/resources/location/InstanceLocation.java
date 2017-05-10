/************************************************************************************
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
package resources.location;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Quaternion;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

import resources.objects.SWGObject;

public class InstanceLocation implements Persistable {
	
	private final Location location;
	
	private InstanceType instanceType;
	private int instanceNumber;
	
	public InstanceLocation() {
		this.location = new Location(0, 0, 0, null);
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
	
	public void setLocation(Location location) {
		this.location.mergeWith(location);
	}
	
	public void setPosition(Terrain terrain, double x, double y, double z) {
		setTerrain(terrain);
		setPosition(x, y, z);
	}
	
	public void setTerrain(Terrain terrain) {
		location.setTerrain(terrain);
	}
	
	public void setPosition(double x, double y, double z) {
		location.setPosition(x, y, z);
	}
	
	public void setOrientation(double x, double y, double z, double w) {
		location.setOrientation(x, y, z, w);
	}
	
	public void setHeading(double heading) {
		location.setHeading(heading);
	}
	
	public void setInstance(InstanceType instanceType, int instanceNumber) {
		this.instanceType = instanceType;
		this.instanceNumber = instanceNumber;
	}
	
	public void clearInstance() {
		setInstance(InstanceType.NONE, 0);
	}
	
	public Location getLocation() {
		return new Location(location);
	}
	
	public Point3D getPosition() {
		return location.getPosition();
	}
	
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
	
	public InstanceType getInstanceType() {
		return instanceType;
	}
	
	public int getInstanceNumber() {
		return instanceNumber;
	}
	
	public Location getWorldLocation(SWGObject self) {
		Location loc = getLocation();
		SWGObject parent = self.getParent();
		while (parent != null) {
			loc.translate(parent.getInstanceLocation().location); // avoids copies
			loc.setTerrain(parent.getTerrain());
			parent = parent.getParent();
		}
		return loc;
	}
	
}
