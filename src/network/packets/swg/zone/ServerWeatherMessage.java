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

import network.packets.swg.SWGPacket;
import resources.WeatherType;

import java.nio.ByteBuffer;

public class ServerWeatherMessage extends SWGPacket {
	public static final int CRC = getCrc("ServerWeatherMessage");
	
	private WeatherType type;
	private float cloudVectorX;
	private float cloudVectorZ;
	private float cloudVectorY;
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		WeatherType type = WeatherType.CLEAR;
		
		switch(getInt(data)) {
			case 0:
				type = WeatherType.CLEAR;
				break;
			case 1:
				type = WeatherType.LIGHT;
				break;
			case 2:
				type = WeatherType.MEDIUM;
				break;
			case 3:
				type = WeatherType.HEAVY;
				break;
		}
		
		this.type = type;
		
		cloudVectorX = getFloat(data);
		cloudVectorZ = getFloat(data);
		cloudVectorY = getFloat(data);
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(22);
		
		addShort(data, 3);
		addInt(data, CRC);
		addInt(data, type.getValue());
		
		addFloat(data, cloudVectorX);
		addFloat(data, cloudVectorZ);
		addFloat(data, cloudVectorY);
		
		return data;
	}

	public WeatherType getType() {
		return type;
	}

	public void setType(WeatherType type) {
		this.type = type;
	}

	public float getCloudVectorX() {
		return cloudVectorX;
	}

	public void setCloudVectorX(float cloudVectorX) {
		this.cloudVectorX = cloudVectorX;
	}

	public float getCloudVectorZ() {
		return cloudVectorZ;
	}

	public void setCloudVectorZ(float cloudVectorZ) {
		this.cloudVectorZ = cloudVectorZ;
	}

	public float getCloudVectorY() {
		return cloudVectorY;
	}

	public void setCloudVectorY(float cloudVectorY) {
		this.cloudVectorY = cloudVectorY;
	}
	
}
