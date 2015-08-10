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

public class ObjectMenuResponse extends ObjectController {
	
	public static final int CRC = 0x0147;
	
	private RadialOptionList options;
	private long targetId;
	private long requestorId;
	private int counter;
	
	public ObjectMenuResponse(long objectId) {
		super(objectId, CRC);
		this.options = new RadialOptionList();
	}
	
	public ObjectMenuResponse(long objectId, long targetId, long requestorId, List<RadialOption> options, int counter) {
		super(objectId, CRC);
		this.targetId = targetId;
		this.requestorId = requestorId;
		this.options = new RadialOptionList(options);
		this.counter = counter;
		setTargetId(targetId);
		setRequestorId(requestorId);
		setRadialOptions(options);
		setCounter(counter);
	}
	
	public ObjectMenuResponse(ByteBuffer data) {
		super(CRC);
		this.options = new RadialOptionList();
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

	public void setTargetId(long targetId) {
		this.targetId = targetId;
	}
	
	public void setRequestorId(long requestorId) {
		this.requestorId = requestorId;
	}
	
	public void setRadialOptions(List<RadialOption> options) {
		this.options.setOptions(options);
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	public long getTargetId() {
		return targetId;
	}
	
	public long getRequestorId() {
		return requestorId;
	}
	
	public List<RadialOption> getOptions() {
		return options.getOptions();
	}
	
	public int getCounter() {
		return counter;
	}
	
}
