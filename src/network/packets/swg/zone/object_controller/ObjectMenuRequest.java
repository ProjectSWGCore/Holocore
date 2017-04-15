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
import java.util.List;

import resources.radial.RadialOption;
import resources.radial.RadialOptionList;

public class ObjectMenuRequest extends ObjectController {
	
	public static final int CRC = 0x0146;
	
	private long targetId;
	private long requestorId;
	private RadialOptionList options;
	private byte counter;
	
	public ObjectMenuRequest(long objectId) {
		super(objectId, CRC);
		options = new RadialOptionList();
	}
	
	public ObjectMenuRequest(ByteBuffer data) {
		super(CRC);
		options = new RadialOptionList();
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		targetId = getLong(data);
		requestorId = getLong(data);
		options = getEncodable(data, RadialOptionList.class);
		counter = getByte(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + options.getSize() + 17);
		encodeHeader(data);
		addLong(data, targetId);
		addLong(data, requestorId);
		addEncodable(data, options);
		addByte(data, counter);
		return data;
	}
	
	public long getTargetId() { return targetId; }
	public long getRequestorId() { return requestorId; }
	public int getCounter() { return counter; }
	public List <RadialOption> getOptions() { return options.getOptions(); }
	
	public void setTargetId(long targetId) { this.targetId = targetId; }
	public void setRequestorId(long requesterId) { this.requestorId = requesterId; }
	public void setCounter(byte counter) { this.counter = counter; }
	public void addOption(RadialOption opt) { options.addOption(opt); }
	
}
