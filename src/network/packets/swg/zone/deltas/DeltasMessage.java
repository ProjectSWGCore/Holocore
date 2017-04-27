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
package network.packets.swg.zone.deltas;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;
import network.packets.swg.zone.baselines.Baseline.BaselineType;

public class DeltasMessage extends SWGPacket {
	
	public static final int CRC = getCrc("DeltasMessage");
	
	private long objId;
	private BaselineType type;
	private int num;
	private byte[] deltaData;
	private int update;
	
	public DeltasMessage(long objId, BaselineType type, int typeNumber, int update, byte[] data) {
		this.objId = objId;
		this.type = type;
		this.num = typeNumber;
		this.deltaData = data;
	}
	
	public DeltasMessage(NetBuffer data) {
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		objId = data.getLong();
		type = BaselineType.valueOf(new StringBuffer(new String(data.getArray(4), ascii)).reverse().toString());
		num = data.getByte();
		NetBuffer deltaDataBuffer = NetBuffer.wrap(data.getArrayLarge());
		deltaDataBuffer.getShort();
		update = deltaDataBuffer.getShort();
		deltaData = data.getArray(deltaDataBuffer.remaining());
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(27 + deltaData.length);
		data.addShort(5);
		data.addInt(CRC);
		data.addLong(objId);
		data.addRawArray(new StringBuffer(type.toString()).reverse().toString().getBytes(ascii));
		data.addByte(num);
		data.addInt(deltaData.length + 4);
		data.addShort(1); // updates - only 1 cause we're boring
		data.addShort(update);
		data.addRawArray(deltaData);
		return data;
	}
	
	public long getObjectId() {
		return objId;
	}
	
	public BaselineType getType() {
		return type;
	}
	
	public int getNum() {
		return num;
	}
	
	public int getUpdate() {
		return update;
	}
	
	public byte[] getDeltaData() {
		return deltaData;
	}
	
	public void setType(BaselineType type) {
		this.type = type;
	}
	
	public void setNum(int num) {
		this.num = num;
	}
	
	public void setId(long id) {
		this.objId = id;
	}
	
	public void setData(byte[] data) {
		this.deltaData = data;
	}
	
	public void setUpdate(int update) {
		this.update = update;
	}
}
