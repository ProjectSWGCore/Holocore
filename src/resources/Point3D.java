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
package resources;

import network.packets.Packet;
import resources.encodables.Encodable;
import resources.network.NetBufferStream;
import resources.persistable.Persistable;

import java.nio.ByteBuffer;

public class Point3D implements Encodable, Persistable {
	
	private double x;
	private double y;
	private double z;
	
	public Point3D() {
		this(0, 0, 0);
	}

	public Point3D(Point3D p) {
		this(p.getX(), p.getY(), p.getZ());
	}
	
	public Point3D(double x, double y, double z) {
		set(x, y, z);
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getZ() {
		return z;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public void setZ(double z) {
		this.z = z;
	}
	
	public void set(double x, double y, double z) {
		setX(x);
		setY(y);
		setZ(z);
	}
	
	public void translate(Point3D p) {
		translate(p.getX(), p.getY(), p.getZ());
	}
	
	public void translate(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
	}
	
	public void rotateAround(double x, double y, double z, Quaternion rot) {
		rot.rotatePoint(this);
		translate(x, y, z);
	}

	@Override
	public byte[] encode() {
		ByteBuffer bb = ByteBuffer.allocate(12);
		Packet.addFloat(bb, (float) x);
		Packet.addFloat(bb, (float) y);
		Packet.addFloat(bb, (float) z);
		return bb.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		x = Packet.getFloat(data);
		y = Packet.getFloat(data);
		z = Packet.getFloat(data);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addFloat((float) x);
		stream.addFloat((float) y);
		stream.addFloat((float) z);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		x = stream.getFloat();
		y = stream.getFloat();
		z = stream.getFloat();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Point3D))
			return false;
		if (Math.abs(((Point3D) o).getX()-getX()) > 1E-7)
			return false;
		if (Math.abs(((Point3D) o).getY()-getY()) > 1E-7)
			return false;
		if (Math.abs(((Point3D) o).getZ()-getZ()) > 1E-7)
			return false;
		return true;
	}
	
	public int hashCode() {
		return Double.hashCode(getX()) ^ Double.hashCode(getY()) ^ Double.hashCode(getZ());
	}

	public String toString() {
		return String.format("Point3D[%.2f, %.2f, %.2f]", x, y, z);
	}
}
