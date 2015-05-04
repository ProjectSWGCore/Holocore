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

import java.nio.ByteBuffer;

public class CREO3 extends Baseline {
	
	private String dName = "";
	private String cName = "";
	private byte [] customization = new byte[0];
	private int posture = 1;
	private float height = 0;
	private int fatigue = 0;
	
	public CREO3() {
		
	}
	
	public CREO3(String dName, String cName, byte [] customization, int posture, float height, int fatigue) {
		this.dName = dName;
		this.cName = cName;
		this.customization = customization;
		this.posture = posture;
		this.height = height;
		this.fatigue = fatigue;
	}
	
	public void decodeBaseline(ByteBuffer data) {
		getInt(data);
		getShort(data);
		getAscii(data);
		getInt(data);
		dName = getAscii(data);
		cName = getUnicode(data);
		getInt(data);
		getLong(data);
		customization = getArray(data);
		getInt(data);
		getInt(data);
		getInt(data);
		getInt(data);
		getInt(data);
		getInt(data);
		getInt(data);
		posture = getShort(data);
		getByte(data);
		getLong(data);
		height = getFloat(data);
		fatigue = getInt(data);
	}
	
	public ByteBuffer encodeBaseline() {
		int length = 91 + "species".length() + dName.length() + cName.length() * 2 + customization.length;
		ByteBuffer data = ByteBuffer.allocate(length);
		addInt(    data,   0x13); // Object Count?
		addShort(  data, 0x3F80); // Scale?
		addAscii(  data, "species"); // Unknown..
		addInt(    data, 0); // Spacer
		addAscii(  data, dName); // Default Name
		addUnicode(data, cName); // Custom Name
		addInt(    data, 0x000F4240); // Unknown
		addLong(   data, 0); // Unknown
		addArray(  data, customization); // Object Customization Data
		addInt(    data, 1); // Unknown
		addInt(    data, 0); // Unknown
		addInt(    data, 0); // Unknown
		addInt(    data, 0x80); // Unknown
		addInt(    data, 0); // Unknown
		addInt(    data, 0); // Unknown
		addInt(    data, 0x00003A98); // Unknown
		addShort(  data, posture); // Posture for Object
		addByte(   data, 1); // Unknown
		addLong(   data, 0); // Target Object Id
		addFloat(  data, height); // Height of Object
		addInt(    data, fatigue); // Battle Fatigue
		addLong(   data, 0); // State?
		return data;
	}
	
	public String getDefaultName() { return dName; }
	public String getCustomName() { return cName; }
}
