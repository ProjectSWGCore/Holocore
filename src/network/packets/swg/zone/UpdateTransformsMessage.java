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
package network.packets.swg.zone;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class UpdateTransformsMessage extends SWGPacket {
	
	public static final int CRC = 0x1B24F808;
	private long objId;
	private short posX;
	private short posY;
	private short posZ;
	private int updateCounter;
	private byte direction;
	private float speed;
	private byte lookAtYaw;
	private boolean useLookAtYaw;

	public UpdateTransformsMessage() {
		this.objId = 0;
		this.posX = 0;
		this.posY = 0;
		this.posZ = 0;
		this.updateCounter = 0;
		this.direction = 0;
		this.speed = 0;
	}
	
	public UpdateTransformsMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objId = getLong(data);
		posX = getShort(data);
		posY = getShort(data);
		posZ = getShort(data);
		updateCounter = getInt(data);
		speed = getByte(data);
		direction = getByte(data);
		getByte(data);
		getByte(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(28);
		addShort(data, 10);
		addInt(  data, CRC);
		addLong( data, objId);
		addShort(data, posX);
		addShort(data, posY);
		addShort(data, posZ);
		addInt  (data, updateCounter);
		addByte (data, (byte) speed);
		addByte (data, direction);
		addByte (data, lookAtYaw); // lookAtYaw
		addBoolean (data, useLookAtYaw); // useLookAtYaw
		return data;
	}
	
	public void setObjectId(long objId) { this.objId = objId; }
	public void setX(short x) { this.posX = x; }
	public void setY(short y) { this.posY = y; }
	public void setZ(short z) { this.posZ = z; }
	public void setUpdateCounter(int count) { this.updateCounter = count; }
	public void setDirection(byte d) { this.direction = d; }
	public void setSpeed(float speed) { this.speed = speed; }
	
	public long getObjectId() { return objId; }
	public short getX() { return posX; }
	public short getY() { return posY; }
	public short getZ() { return posZ; }
	public int getUpdateCounter() { return updateCounter; }
	public byte getDirection() { return direction; }
	public float getSpeed() { return speed; }

	public boolean isUseLookAtYaw() {
		return useLookAtYaw;
	}

	public void setUseLookAtYaw(boolean useLookAtYaw) {
		this.useLookAtYaw = useLookAtYaw;
	}

	public byte getLookAtYaw() {
		return lookAtYaw;
	}

	public void setLookAtYaw(byte lookAtYaw) {
		this.lookAtYaw = lookAtYaw;
	}
}
