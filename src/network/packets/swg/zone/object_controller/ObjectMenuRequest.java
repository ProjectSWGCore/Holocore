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
import java.util.ArrayList;
import java.util.List;

import resources.radial.RadialOption;

public class ObjectMenuRequest extends ObjectController {
	
	public static final int CRC = 0x0146;
	
	private long targetId;
	private long requesterId;
	private List <RadialOption> options;
	private byte counter;
	
	public ObjectMenuRequest(long objectId) {
		super(objectId, CRC);
		options = new ArrayList<RadialOption>();
	}
	
	public ObjectMenuRequest(ByteBuffer data) {
		super(CRC);
		options = new ArrayList<RadialOption>();
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		
		targetId = getLong(data);
		requesterId = getLong(data);
		
		int optionsCount = getInt(data);
		for (int i = 0; i < optionsCount; i++) {
			RadialOption option = new RadialOption();
			getByte(data); // option number
			option.setParentId(getByte(data));
			option.setId(getShort(data));
			option.setOptionType(getByte(data));
			option.setText(getUnicode(data));
			options.add(option);
		}
		counter = getByte(data);
	}
	
	public ByteBuffer encode() {
		int optSize = 21;
		for (RadialOption option : options)
			optSize += 7 + option.getText().length()*2;
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + optSize);
		encodeHeader(data);
		addLong(data, targetId);
		addLong(data, requesterId);
		addInt(data, options.size());
		int optNum = 1;
		for (RadialOption option : options) {
			addByte(data, optNum++); // option number
			addByte(data, option.getParentId());
			addShort(data, option.getId());
			addByte(data, option.getOptionType());
			addUnicode(data, option.getText());
		}
		addByte(data, counter);
		return data;
	}
	
	public long getTargetId() { return targetId; }
	public long getRequesterId() { return requesterId; }
	public int getCounter() { return counter; }
	public List <RadialOption> getOptions() { return options; }
	
	public void setTargetId(long targetId) { this.targetId = targetId; }
	public void setRequesterId(long requesterId) { this.requesterId = requesterId; }
	public void setCounter(byte counter) { this.counter = counter; }
	public void addOption(RadialOption opt) { options.add(opt); }
	
}
