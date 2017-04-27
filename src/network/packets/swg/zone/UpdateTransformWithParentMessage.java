/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package network.packets.swg.zone;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

/**
 * @author Waverunner
 */
public class UpdateTransformWithParentMessage extends SWGPacket {
	public static final int CRC = getCrc("UpdateTransformWithParentMessage");

	private long cellId;
	private long objectId;
	private short x;
	private short y;
	private short z;
	private int updateCounter;
	private byte speed;
	private byte direction;
	private byte lookDirection;
	private boolean useLookDirection;
	
	public UpdateTransformWithParentMessage() {
		
	}
	
	public UpdateTransformWithParentMessage(long cellId, long objectId) {
		this.cellId = cellId;
		this.objectId = objectId;
	}

	public UpdateTransformWithParentMessage(long cellId, long objectId, Location location, int updateCounter, byte speed, byte direction, byte lookDirection, boolean useLookDirection) {
		this.cellId = cellId;
		this.objectId = objectId;
		this.updateCounter = updateCounter;
		this.speed = speed;
		this.direction = direction;
		this.lookDirection = lookDirection;
		this.useLookDirection = useLookDirection;
		setLocation(location);
	}

	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		cellId			= data.getLong();
		objectId		= data.getLong();
		x				= data.getShort();
		y				= data.getShort();
		z				= data.getShort();
		updateCounter	= data.getInt();
		speed			= data.getByte();
		direction		= data.getByte();
		lookDirection	= data.getByte();
		useLookDirection= data.getBoolean();
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(36);
		data.addShort(11);
		data.addInt(CRC);
		data.addLong(cellId);
		data.addLong(objectId);
		data.addShort(x);
		data.addShort(y);
		data.addShort(z);
		data.addInt(updateCounter);
		data.addByte(speed);
		data.addByte(direction);
		data.addByte(lookDirection);
		data.addBoolean(useLookDirection);
		return data;
	}
	
	public void setCellId(long cellId) {
		this.cellId = cellId;
	}
	
	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}
	
	public void setUpdateCounter(int updateCounter) {
		this.updateCounter = updateCounter;
	}
	
	public void setSpeed(byte speed) {
		this.speed = speed;
	}
	
	public void setDirection(byte direction) {
		this.direction = direction;
	}
	
	public void setLookDirection(byte lookDirection) {
		this.lookDirection = lookDirection;
	}
	
	public void setUseLookDirection(boolean useLookDirection) {
		this.useLookDirection = useLookDirection;
	}
	
	public long getCellId() {
		return cellId;
	}
	
	public long getObjectId() {
		return objectId;
	}
	
	public int getUpdateCounter() {
		return updateCounter;
	}
	
	public byte getSpeed() {
		return speed;
	}
	
	public byte getDirection() {
		return direction;
	}
	
	public byte getLookDirection() {
		return lookDirection;
	}
	
	public boolean isUseLookDirection() {
		return useLookDirection;
	}
	
	public void setLocation(Location location) {
		this.x = (short) (location.getX() * 8 + 0.5);
		this.y = (short) (location.getY() * 8 + 0.5);
		this.z = (short) (location.getZ() * 8 + 0.5);
	}
}
