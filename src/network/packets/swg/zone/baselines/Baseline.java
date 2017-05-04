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
package network.packets.swg.zone.baselines;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class Baseline extends SWGPacket {
	public static final int CRC = getCrc("BaselinesMessage");

	private BaselineType type;
	private int num;
	private short opCount;
	private long objId;
	private byte [] baseData;
	
	public Baseline() {
		
	}
	
	public Baseline(long objId, Baseline subData) {
		this.objId = objId;
		type = subData.getType();
		num = subData.getNum();
		baseData = subData.encodeBaseline().array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		objId = data.getLong();
		type = BaselineType.valueOf(new StringBuffer(new String(data.getArray(4), ascii)).reverse().toString());
		num = data.getByte();
		baseData = data.getArrayLarge();
		if (baseData.length >= 2)
			opCount = NetBuffer.wrap(baseData).getShort();
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(25 + baseData.length);
		data.addShort(5);
		data.addInt(CRC);
		data.addLong(objId);
		data.addRawArray(new StringBuffer(type.toString()).reverse().toString().getBytes(ascii));
		data.addByte(num);
		data.addInt(baseData.length + 2);
		data.addShort((opCount == 0 ? 5 : opCount));
		data.addRawArray(baseData);
		return data;
	}
	
	public NetBuffer encodeBaseline() { return NetBuffer.allocate(0); }
	
	public long getObjectId() { return objId; }
	
	public void setType(BaselineType type) { this.type = type; }
	public void setNum(int num) { this.num = num; }
	public void setId(long id) { this.objId = id; }
	public void setBaselineData(byte [] data) { this.baseData = data; }
	public void setOperandCount(int count) { this.opCount = (short) count;}
	
	public BaselineType getType() { return type; }
	public int getNum() { return num; }
	public long getId() { return objId; }
	public byte [] getBaselineData() { return baseData; }
	
	public enum BaselineType {
		BMRK, BUIO, CREO, FCYT,
		GILD, GRUP, HINO, INSO,
		ITNO, MINO, MISO, MSCO,
		PLAY, RCNO, SCLT, STAO,
		SHIP, TANO, WAYP, WEAO
	}
}
