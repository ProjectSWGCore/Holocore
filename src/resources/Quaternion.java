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

public class Quaternion implements Encodable, Persistable {
	
	private final double [][] rotationMatrix;
	private double x;
	private double y;
	private double z;
	private double w;
	
	public Quaternion(Quaternion q) {
		this(q.x, q.y, q.z, q.w);
	}
	
	public Quaternion(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		this.rotationMatrix = new double[3][3];
		updateRotationMatrix();
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

	public double getW() {
		return w;
	}
	
	public double getYaw() {
		return Math.toDegrees(Math.atan2(y*y, w*w));
	}

	public void setX(double x) {
		this.x = x;
		updateRotationMatrix();
	}

	public void setY(double y) {
		this.y = y;
		updateRotationMatrix();
	}

	public void setZ(double z) {
		this.z = z;
		updateRotationMatrix();
	}

	public void setW(double w) {
		this.w = w;
		updateRotationMatrix();
	}
	
	public void set(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		updateRotationMatrix();
	}
	
	public void set(Quaternion q) {
		set(q.x, q.y, q.z, q.w);
	}
	
	public void setHeading(double degrees) {
		set(0, 0, 0, 1);
		rotateHeading(degrees);
	}
	
	public void rotateHeading(double degrees) {
		rotateDegrees(degrees, 0, 1, 0);
	}
	
	public void rotateDegrees(double degrees, double axisX, double axisY, double axisZ) {
		double rad = Math.toRadians(degrees) / 2;
		double sin = Math.sin(rad);
		w = Math.cos(rad);
		x = sin * axisX;
		y = sin * axisY;
		z = sin * axisZ;
		normalize();
	}
	
	public void rotateByQuaternion(Quaternion q) {
		double nW = w * q.w - x * q.x - y * q.y - z * q.z;
		double nX = w * q.x + x * q.w + y * q.z - z * q.y;
		double nY = w * q.y + y * q.w + z * q.x - x * q.z;
		double nZ = w * q.z + z * q.w + x * q.y - y * q.x;
		set(nX, nY, nZ, nW);
		normalize();
	}
	
	public void rotatePoint(Point3D p) {
		double nX = rotationMatrix[0][0]*p.getX() + rotationMatrix[0][1]*p.getY() + rotationMatrix[0][2]*p.getZ();
		double nY = rotationMatrix[1][0]*p.getX() + rotationMatrix[1][1]*p.getY() + rotationMatrix[1][2]*p.getZ();
		double nZ = rotationMatrix[2][0]*p.getX() + rotationMatrix[2][1]*p.getY() + rotationMatrix[2][2]*p.getZ();
		p.set(nX, nY, nZ);
	}
	
	public void normalize() {
		double mag = Math.sqrt(x * x + y * y + z * z + w * w);
		x /= mag;
		y /= mag;
		z /= mag;
		w /= mag;
		updateRotationMatrix();
	}

	@Override
	public byte[] encode() {
		ByteBuffer bb = ByteBuffer.allocate(16);
		Packet.addFloat(bb, (float) x);
		Packet.addFloat(bb, (float) y);
		Packet.addFloat(bb, (float) z);
		Packet.addFloat(bb, (float) w);
		return bb.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		x = Packet.getFloat(data);
		y = Packet.getFloat(data);
		z = Packet.getFloat(data);
		w = Packet.getFloat(data);
		updateRotationMatrix();
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addFloat((float) x);
		stream.addFloat((float) y);
		stream.addFloat((float) z);
		stream.addFloat((float) w);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		x = stream.getFloat();
		y = stream.getFloat();
		z = stream.getFloat();
		w = stream.getFloat();
		updateRotationMatrix();
	}

	public String toString() {
		return String.format("Quaternion[%.3f, %.3f, %.3f, %.3f]", x, y, z, w);
	}
	
	private void updateRotationMatrix() {
		double x2 = x * x;
		double y2 = y * y;
		double z2 = z * z;
		double w2 = w * w;
		updateRotationMatrixX(x2, y2, z2, w2);
		updateRotationMatrixY(x2, y2, z2, w2);
		updateRotationMatrixZ(x2, y2, z2, w2);
	}
	
	private void updateRotationMatrixX(double x2, double y2, double z2, double w2) {
		rotationMatrix[0][0] = x2 + w2 - y2 - z2;
		rotationMatrix[0][1] = 2*y*x - 2*z*w;
		rotationMatrix[0][2] = 2*y*w + 2*z*x;
	}
	
	private void updateRotationMatrixY(double x2, double y2, double z2, double w2) {
		rotationMatrix[1][0] = 2*x*y + 2*w*z;
		rotationMatrix[1][1] = y2 - z2 + w2 - x2;
		rotationMatrix[1][2] = 2*z*y - 2*x*w;
	}
	
	private void updateRotationMatrixZ(double x2, double y2, double z2, double w2) {
		rotationMatrix[2][0] = 2*x*z - 2*w*y;
		rotationMatrix[2][1] = 2*y*z + 2*w*x;
		rotationMatrix[2][2] = z2 + w2 - x2 - y2;
	}
}
