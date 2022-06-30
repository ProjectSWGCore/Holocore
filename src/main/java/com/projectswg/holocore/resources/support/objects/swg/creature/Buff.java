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
package com.projectswg.holocore.resources.support.objects.swg.creature;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

public class Buff implements Encodable, Persistable, MongoPersistable {
	
	private int crc;
	private int endTime;
	
	public Buff() {
		this(0, 0);
	}
	
	public Buff(int crc, int endTime) {
		this.crc = crc;
		this.endTime = endTime;
	}
	
	@Override
	public void decode(NetBuffer data) {
		data.getInt();
		endTime = data.getInt();
	}
	
	@Override
	public byte[] encode() {
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addInt(0);
		data.addInt(endTime);
		return data.array();
	}
	
	@Override
	public int getLength() {
		return 8;
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putInteger("crc", crc);
		data.putInteger("endTime", endTime);
	}
	
	@Override
	public void readMongo(MongoData data) {
		crc = data.getInteger("crc", 0);
		endTime = data.getInteger("endTime", 0);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(1);
		stream.addInt(crc);
		stream.addInt(endTime);
	}
	
	public void readOld(NetBufferStream stream) {
		
	}
	
	@Override
	public void read(NetBufferStream stream) {
		
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
	
	@Override
	public String toString() {
		return String.format("Buff[End=%d]", endTime);
	}
	
	@Override
	public int hashCode() {
		return crc;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Buff))
			return false;
		return ((Buff) o).getCrc() == crc;
	}

}
