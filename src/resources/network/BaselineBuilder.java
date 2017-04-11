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
package resources.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.encodables.Encodable;
import resources.objects.SWGObject;
import resources.player.Player;

import com.projectswg.common.debug.Log;

public class BaselineBuilder {
	
	public static final Charset ASCII   = Charset.forName("UTF-8");
	public static final Charset UNICODE = Charset.forName("UTF-16LE");
	
	private SWGObject object;
	private BaselineType type;
	private int num;
	private int opCount = 0;
	private LittleEndianDataOutputStream dataStream;
	private ByteArrayOutputStream rawDataStream;
	
	public BaselineBuilder(SWGObject object, BaselineType type, int num) {
		this.object = object;
		this.type = type;
		this.num = num;
		rawDataStream = new ByteArrayOutputStream();
		dataStream = new LittleEndianDataOutputStream(rawDataStream);
	}
	
	public void sendTo(Player target) {
		byte [] data = build();
		Baseline baseline = new Baseline();
		baseline.setId(object.getObjectId());
		baseline.setType(type);
		baseline.setNum(num);
		baseline.setOperandCount(opCount);
		baseline.setBaselineData(data);
		target.sendPacket(baseline);
	}
	
	public Baseline buildAsBaselinePacket() {
		Baseline baseline = new Baseline();
		baseline.setId(object.getObjectId());
		baseline.setType(type);
		baseline.setNum(num);
		baseline.setOperandCount(opCount);
		baseline.setBaselineData(build());
		return baseline;
	}
	
	public byte [] build() {
		return rawDataStream.toByteArray();
	}
	
	public void addObject(Encodable e) {
		try {
			dataStream.write(e.encode());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void addBoolean(boolean b) {
		addByte(b ? 1 : 0);
	}
	
	public void addAscii(String str) {
		addShort(str.length());
		try {
			dataStream.write(str.getBytes(ASCII));
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public void addUnicode(String str) {
		addInt(str.length());
		try {
			dataStream.write(str.getBytes(UNICODE));
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public void addByte(int b) {
		try {
			dataStream.writeByte(b);
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public void addShort(int s) {
		try {
			dataStream.writeShort(s);
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public void addInt(int i) {
		try {
			dataStream.writeInt(i);
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public void addLong(long l) {
		try {
			dataStream.writeLong(l);
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public void addFloat(float f) {
		try {
			dataStream.writeFloat(f);
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public void addArray(byte [] array) {
		addShort(array.length);
		try {
			dataStream.write(array);
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	public int incrementOperandCount(int operands) {
		return opCount+=operands;
	}
	
}
