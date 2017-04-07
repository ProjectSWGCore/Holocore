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
package resources.objects.creature;

import java.nio.ByteBuffer;
import resources.encodables.Encodable;
import resources.network.NetBuffer;
import resources.network.NetBufferStream;
import resources.persistable.Persistable;

public class Buff implements Encodable, Persistable {
	
	private int crc;
	private int endTime;
	private float value;
	private int duration;
	private long bufferId;
	private int stackCount;
	
	public Buff() {
		this(0, 0, 0, 0, 0, 0);
	}
	
	public Buff(int crc, int endTime, float value, int duration, long buffer, int stackCount) {
		this.crc = crc;
		this.endTime = endTime;
		this.value = value;
		this.duration = duration;
		this.bufferId = buffer;
		this.stackCount = stackCount;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		endTime = data.getInt();
		value = data.getFloat();
		duration = data.getInt();
		bufferId = data.getLong();
		stackCount = data.getInt();
	}
	
	@Override
	public byte[] encode() {
		NetBuffer data = NetBuffer.allocate(24);
		data.addInt(endTime);
		data.addFloat(value);
		data.addInt(duration);
		data.addLong(bufferId);
		data.addInt(stackCount);
		return data.array();
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(1);
		stream.addInt(crc);
		stream.addInt(endTime);
		stream.addFloat(value);
		stream.addInt(duration);
		stream.addLong(bufferId);
		stream.addInt(stackCount);
	}
	
	public void readOld(NetBufferStream stream) {
		endTime = stream.getInt();
		value = stream.getFloat();
		duration = stream.getInt();
		bufferId = stream.getLong();
		stackCount = stream.getInt();
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte(); // version
		crc = stream.getInt();
		endTime = stream.getInt();
		value = stream.getFloat();
		duration = stream.getInt();
		bufferId = stream.getLong();
		stackCount = stream.getInt();
	}
	
	public int getCrc() {
		return crc;
	}
	
	public void setCrc(int crc) {
		this.crc = crc;
	}
	
	public int getEndTime() {
		return endTime;
	}
	
	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}
	
	public float getValue() {
		return value;
	}
	
	public void setValue(float value) {
		this.value = value;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public long getBuffer() {
		return bufferId;
	}
	
	public void setBuffer(long buffer) {
		this.bufferId = buffer;
	}
	
	public int getStackCount() {
		return stackCount;
	}
	
	public void setStackCount(int stackCount) {
		this.stackCount = stackCount;
	}
	
	public void adjustStackCount(int adjust) {
		this.stackCount += adjust;
	}
	
	public int getStartTime() {
		return endTime - duration;
	}
	
	@Override
	public String toString() {
		return String.format("Buff[End=%d Value=%f Duration=%d Buffer=%d StackCount=%d]", endTime, value, duration, bufferId, stackCount);
	}
	
	public int hashCode() {
		return crc;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Buff))
			return false;
		return ((Buff) o).getCrc() == crc;
	}

}
