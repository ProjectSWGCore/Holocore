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
package network.packets.swg.zone.object_controller;


import java.nio.ByteBuffer;

import com.projectswg.common.data.location.Location;

public class DataTransformWithParent extends ObjectController {
	
	public static final int CRC = 0x00F1;
	
	private int timestamp;
	private int counter;
	private long cellId;
	private Location l;
	private float speed;
	private float lookAtYaw;
	private boolean useLookAtYaw;
	
	public DataTransformWithParent(long objectId) {
		super(objectId, CRC);
	}
	
	public DataTransformWithParent(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		timestamp = getInt(data); // Timestamp
		counter = getInt(data);
		cellId = getLong(data);
		l = getEncodable(data, Location.class);
		speed = getFloat(data);
		lookAtYaw = getFloat(data);
		useLookAtYaw = getBoolean(data);
	}
	
	@Override
	public ByteBuffer encode() {
		return null;
	}
	
	public void setUpdateCounter(int counter) {
		this.counter = counter;
	}
	
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	
	public void setCellId(long cellId) {
		this.cellId = cellId;
	}
	
	public void setLocation(Location l) {
		this.l = l;
	}
	
	public void setSpeed(float speed) {
		this.speed = speed;
	}
	
	public void setLookAtYaw(float lookAtYaw) {
		this.lookAtYaw = lookAtYaw;
	}
	
	public void setUseLookAtYaw(boolean useLookAtYaw) {
		this.useLookAtYaw = useLookAtYaw;
	}
	
	public int getUpdateCounter() {
		return counter;
	}
	
	public long getCellId() {
		return cellId;
	}
	
	public Location getLocation() {
		return l;
	}
	
	public float getSpeed() {
		return speed;
	}
	
	public float getLookAtYaw() {
		return lookAtYaw;
	}
	
	public boolean isUseLookAtYaw() {
		return useLookAtYaw;
	}
	
	public int getTimestamp() {
		return timestamp;
	}
	
	public byte getMovementAngle() {
		byte movementAngle = (byte) 0.0f;
		double wOrient = l.getOrientationW();
		double yOrient = l.getOrientationY();
		double sq = Math.sqrt(1 - (wOrient * wOrient));
		
		if (sq != 0) {
			if (l.getOrientationW() > 0 && l.getOrientationY() < 0) {
				wOrient *= -1;
				yOrient *= -1;
			}
			movementAngle = (byte) ((yOrient / sq) * (2 * Math.acos(wOrient) / 0.06283f));
		}
		
		return movementAngle;
	}
}
